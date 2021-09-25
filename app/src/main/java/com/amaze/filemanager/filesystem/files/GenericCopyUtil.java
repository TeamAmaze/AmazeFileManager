/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.file_operations.utils.OnLowMemory;
import com.amaze.filemanager.file_operations.utils.UpdatePosition;
import com.amaze.filemanager.filesystem.ExternalSdCardOperation;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.MediaStoreHack;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.ProgressHandler;
import com.cloudrail.si.interfaces.CloudStorage;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.documentfile.provider.DocumentFile;

/** Base class to handle file copy. */
public class GenericCopyUtil {

  private HybridFileParcelable mSourceFile;
  private HybridFile mTargetFile;
  private Context mContext; // context needed to find the DocumentFile in otg/sd card
  private DataUtils dataUtils = DataUtils.getInstance();
  private ProgressHandler progressHandler;
  public static final String PATH_FILE_DESCRIPTOR = "/proc/self/fd/";

  public static final int DEFAULT_BUFFER_SIZE = 8192;

  /*
     Defines the block size per transfer over NIO channels.

     Cannot modify DEFAULT_BUFFER_SIZE since it's used by other classes, will have undesired
     effect on other functions
  */
  private static final int DEFAULT_TRANSFER_QUANTUM = 65536;

  public GenericCopyUtil(Context context, ProgressHandler progressHandler) {
    this.mContext = context;
    this.progressHandler = progressHandler;
  }

  /**
   * Starts copy of file Supports : {@link File}, {@link jcifs.smb.SmbFile}, {@link DocumentFile},
   * {@link CloudStorage}
   *
   * @param lowOnMemory defines whether system is running low on memory, in which case we'll switch
   *     to using streams instead of channel which maps the who buffer in memory. TODO: Use buffers
   *     even on low memory but don't map the whole file to memory but parts of it, and transfer
   *     each part instead.
   */
  private void startCopy(
      boolean lowOnMemory, @NonNull OnLowMemory onLowMemory, @NonNull UpdatePosition updatePosition)
      throws IOException {

    ReadableByteChannel inChannel = null;
    WritableByteChannel outChannel = null;
    BufferedInputStream bufferedInputStream = null;
    BufferedOutputStream bufferedOutputStream = null;

    try {
      // initializing the input channels based on file types
      if (mSourceFile.isOtgFile() || mSourceFile.isDocumentFile()) {
        // source is in otg
        ContentResolver contentResolver = mContext.getContentResolver();
        DocumentFile documentSourceFile =
            mSourceFile.isDocumentFile()
                ? OTGUtil.getDocumentFile(
                    mSourceFile.getPath(),
                    SafRootHolder.getUriRoot(),
                    mContext,
                    mSourceFile.isOtgFile() ? OpenMode.OTG : OpenMode.DOCUMENT_FILE,
                    false)
                : OTGUtil.getDocumentFile(mSourceFile.getPath(), mContext, false);

        bufferedInputStream =
            new BufferedInputStream(
                contentResolver.openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
      } else if (mSourceFile.isSmb() || mSourceFile.isSftp()) {
        bufferedInputStream =
            new BufferedInputStream(mSourceFile.getInputStream(mContext), DEFAULT_TRANSFER_QUANTUM);
      } else if (mSourceFile.isDropBoxFile()
          || mSourceFile.isBoxFile()
          || mSourceFile.isGoogleDriveFile()
          || mSourceFile.isOneDriveFile()) {
        OpenMode openMode = mSourceFile.getMode();

        CloudStorage cloudStorage = dataUtils.getAccount(openMode);
        bufferedInputStream =
            new BufferedInputStream(
                cloudStorage.download(CloudUtil.stripPath(openMode, mSourceFile.getPath())));
      } else {

        // source file is neither smb nor otg; getting a channel from direct file instead of stream
        File file = new File(mSourceFile.getPath());
        if (FileProperties.isReadable(file)) {

          if (mTargetFile.isOneDriveFile()
              || mTargetFile.isDropBoxFile()
              || mTargetFile.isGoogleDriveFile()
              || mTargetFile.isBoxFile()
              || lowOnMemory) {
            // our target is cloud, we need a stream not channel
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
          } else {

            inChannel = new RandomAccessFile(file, "r").getChannel();
          }
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ContentResolver contentResolver = mContext.getContentResolver();
            DocumentFile documentSourceFile =
                ExternalSdCardOperation.getDocumentFile(file, mSourceFile.isDirectory(), mContext);

            bufferedInputStream =
                new BufferedInputStream(
                    contentResolver.openInputStream(documentSourceFile.getUri()),
                    DEFAULT_BUFFER_SIZE);
          } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            InputStream inputStream1 =
                MediaStoreHack.getInputStream(mContext, file, mSourceFile.getSize());
            bufferedInputStream = new BufferedInputStream(inputStream1);
          }
        }
      }

      // initializing the output channels based on file types
      if (mTargetFile.isOtgFile() || mTargetFile.isDocumentFile()) {
        // target in OTG, obtain streams from DocumentFile Uri's
        ContentResolver contentResolver = mContext.getContentResolver();
        DocumentFile documentTargetFile =
            mTargetFile.isDocumentFile()
                ? OTGUtil.getDocumentFile(
                    mTargetFile.getPath(),
                    SafRootHolder.getUriRoot(),
                    mContext,
                    mTargetFile.isOtgFile() ? OpenMode.OTG : OpenMode.DOCUMENT_FILE,
                    true)
                : OTGUtil.getDocumentFile(mTargetFile.getPath(), mContext, true);

        bufferedOutputStream =
            new BufferedOutputStream(
                contentResolver.openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
      } else if (mTargetFile.isSftp() || mTargetFile.isSmb()) {
        bufferedOutputStream =
            new BufferedOutputStream(
                mTargetFile.getOutputStream(mContext), DEFAULT_TRANSFER_QUANTUM);
      } else if (mTargetFile.isDropBoxFile()
          || mTargetFile.isBoxFile()
          || mTargetFile.isGoogleDriveFile()
          || mTargetFile.isOneDriveFile()) {
        cloudCopy(mTargetFile.getMode(), bufferedInputStream);
        return;
      } else {
        // copying normal file, target not in OTG
        File file = new File(mTargetFile.getPath());
        if (FileProperties.isWritable(file)) {

          if (lowOnMemory) {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
          } else {

            outChannel = new RandomAccessFile(file, "rw").getChannel();
          }
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ContentResolver contentResolver = mContext.getContentResolver();
            DocumentFile documentTargetFile =
                ExternalSdCardOperation.getDocumentFile(
                    file, mTargetFile.isDirectory(mContext), mContext);

            bufferedOutputStream =
                new BufferedOutputStream(
                    contentResolver.openOutputStream(documentTargetFile.getUri()),
                    DEFAULT_BUFFER_SIZE);
          } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // Workaround for Kitkat ext SD card
            bufferedOutputStream =
                new BufferedOutputStream(MediaStoreHack.getOutputStream(mContext, file.getPath()));
          }
        }
      }

      if (bufferedInputStream != null) {
        inChannel = Channels.newChannel(bufferedInputStream);
      }

      if (bufferedOutputStream != null) {
        outChannel = Channels.newChannel(bufferedOutputStream);
      }

      Objects.requireNonNull(inChannel);
      Objects.requireNonNull(outChannel);

      doCopy(inChannel, outChannel, updatePosition);
    } catch (IOException e) {
      e.printStackTrace();
      Log.d(getClass().getSimpleName(), "I/O Error!");
      throw new IOException();
    } catch (OutOfMemoryError e) {
      e.printStackTrace();

      onLowMemory.onLowMemory();

      startCopy(true, onLowMemory, updatePosition);
    } finally {

      try {
        if (inChannel != null) inChannel.close();
        if (outChannel != null) outChannel.close();
        if (bufferedInputStream != null) bufferedInputStream.close();
        if (bufferedOutputStream != null) bufferedOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
        // failure in closing stream
      }

      // If target file is copied onto the device and copy was successful, trigger media store
      // rescan
      if (mTargetFile != null) {
        FileUtils.scanFile(mContext, new HybridFile[] {mTargetFile});
      }
    }
  }

  private void cloudCopy(
      @NonNull OpenMode openMode, @NonNull BufferedInputStream bufferedInputStream)
      throws IOException {
    DataUtils dataUtils = DataUtils.getInstance();
    // API doesn't support output stream, we'll upload the file directly
    CloudStorage cloudStorage = dataUtils.getAccount(openMode);

    if (mSourceFile.getMode() == openMode) {
      // we're in the same provider, use api method
      cloudStorage.copy(
          CloudUtil.stripPath(openMode, mSourceFile.getPath()),
          CloudUtil.stripPath(openMode, mTargetFile.getPath()));
    } else {
      cloudStorage.upload(
          CloudUtil.stripPath(openMode, mTargetFile.getPath()),
          bufferedInputStream,
          mSourceFile.getSize(),
          true);
      bufferedInputStream.close();
    }
  }

  /**
   * Method exposes this class to initiate copy
   *
   * @param sourceFile the source file, which is to be copied
   * @param targetFile the target file
   */
  public void copy(
      HybridFileParcelable sourceFile,
      HybridFile targetFile,
      @NonNull OnLowMemory onLowMemory,
      @NonNull UpdatePosition updatePosition)
      throws IOException {
    this.mSourceFile = sourceFile;
    this.mTargetFile = targetFile;

    startCopy(false, onLowMemory, updatePosition);
  }

  /**
   * Calls {@link #doCopy(ReadableByteChannel, WritableByteChannel, UpdatePosition)}.
   *
   * @see Channels#newChannel(InputStream)
   * @param bufferedInputStream source
   * @param outChannel target
   * @throws IOException
   */
  @VisibleForTesting
  void copyFile(
      @NonNull BufferedInputStream bufferedInputStream,
      @NonNull FileChannel outChannel,
      @NonNull UpdatePosition updatePosition)
      throws IOException {
    doCopy(Channels.newChannel(bufferedInputStream), outChannel, updatePosition);
  }

  /**
   * Calls {@link #doCopy(ReadableByteChannel, WritableByteChannel, UpdatePosition)}.
   *
   * @param inChannel source
   * @param outChannel target
   * @throws IOException
   */
  @VisibleForTesting
  void copyFile(
      @NonNull FileChannel inChannel,
      @NonNull FileChannel outChannel,
      @NonNull UpdatePosition updatePosition)
      throws IOException {
    // MappedByteBuffer inByteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0,
    // inChannel.size());
    // MappedByteBuffer outByteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0,
    // inChannel.size());
    doCopy(inChannel, outChannel, updatePosition);
  }

  /**
   * Calls {@link #doCopy(ReadableByteChannel, WritableByteChannel, UpdatePosition)}.
   *
   * @see Channels#newChannel(InputStream)
   * @see Channels#newChannel(OutputStream)
   * @param bufferedInputStream source
   * @param bufferedOutputStream target
   * @throws IOException
   */
  @VisibleForTesting
  void copyFile(
      @NonNull BufferedInputStream bufferedInputStream,
      @NonNull BufferedOutputStream bufferedOutputStream,
      @NonNull UpdatePosition updatePosition)
      throws IOException {
    doCopy(
        Channels.newChannel(bufferedInputStream),
        Channels.newChannel(bufferedOutputStream),
        updatePosition);
  }

  /**
   * Calls {@link #doCopy(ReadableByteChannel, WritableByteChannel, UpdatePosition)}.
   *
   * @see Channels#newChannel(OutputStream)
   * @param inChannel source
   * @param bufferedOutputStream target
   * @throws IOException
   */
  @VisibleForTesting
  void copyFile(
      @NonNull FileChannel inChannel,
      @NonNull BufferedOutputStream bufferedOutputStream,
      @NonNull UpdatePosition updatePosition)
      throws IOException {
    doCopy(inChannel, Channels.newChannel(bufferedOutputStream), updatePosition);
  }

  @VisibleForTesting
  void doCopy(
      @NonNull ReadableByteChannel from,
      @NonNull WritableByteChannel to,
      @NonNull UpdatePosition updatePosition)
      throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_TRANSFER_QUANTUM);
    long count;
    while ((from.read(buffer) != -1 || buffer.position() > 0) && !progressHandler.getCancelled()) {
      buffer.flip();
      count = to.write(buffer);
      updatePosition.updatePosition(count);
      buffer.compact();
    }

    buffer.flip();
    while (buffer.hasRemaining()) to.write(buffer);

    from.close();
    to.close();
  }
}
