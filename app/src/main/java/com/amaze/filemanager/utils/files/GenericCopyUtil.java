/*
 * GenericCopyUtil.java
 *
 * Copyright © 2016-2018 Vishal Nehra (vishalmeham2 at gmail.com),
 * Raymond Lai (airwave209gt at gmail.com)
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils.files;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * 
 * Base class to handle file copy.
 */

public class GenericCopyUtil {

    private HybridFileParcelable mSourceFile;
    private HybridFile mTargetFile;
    private Context mContext;   // context needed to find the DocumentFile in otg/sd card
    private DataUtils dataUtils = DataUtils.getInstance();
    private ProgressHandler progressHandler;
    public static final String PATH_FILE_DESCRIPTOR = "/proc/self/fd/";

    public static final int DEFAULT_BUFFER_SIZE =  8192;

    public GenericCopyUtil(Context context, ProgressHandler progressHandler) {
        this.mContext = context;
        this.progressHandler = progressHandler;
    }

    /**
     * Starts copy of file
     * Supports : {@link File}, {@link jcifs.smb.SmbFile}, {@link DocumentFile}, {@link CloudStorage}
     * @param lowOnMemory defines whether system is running low on memory, in which case we'll switch to
     *                    using streams instead of channel which maps the who buffer in memory.
     *                    TODO: Use buffers even on low memory but don't map the whole file to memory but
     *                          parts of it, and transfer each part instead.
     */
    private void startCopy(boolean lowOnMemory) throws IOException {

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        try {

            // initializing the input channels based on file types
            if (mSourceFile.isOtgFile()) {
                // source is in otg
                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentSourceFile = OTGUtil.getDocumentFile(mSourceFile.getPath(),
                        mContext, false);

                bufferedInputStream = new BufferedInputStream(contentResolver
                        .openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
            } else if (mSourceFile.isSmb()) {

                // source is in smb
                bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(), DEFAULT_BUFFER_SIZE);
            } else if (mSourceFile.isSftp()) {
                bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else if (mSourceFile.isDropBoxFile()) {

                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
                bufferedInputStream = new BufferedInputStream(cloudStorageDropbox
                        .download(CloudUtil.stripPath(OpenMode.DROPBOX,
                                mSourceFile.getPath())));
            } else if (mSourceFile.isBoxFile()) {

                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
                bufferedInputStream = new BufferedInputStream(cloudStorageBox
                        .download(CloudUtil.stripPath(OpenMode.BOX,
                                mSourceFile.getPath())));
            } else if (mSourceFile.isGoogleDriveFile()) {

                CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
                bufferedInputStream = new BufferedInputStream(cloudStorageGdrive
                        .download(CloudUtil.stripPath(OpenMode.GDRIVE,
                                mSourceFile.getPath())));
            } else if (mSourceFile.isOneDriveFile()) {

                CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
                bufferedInputStream = new BufferedInputStream(cloudStorageOnedrive
                        .download(CloudUtil.stripPath(OpenMode.ONEDRIVE,
                                mSourceFile.getPath())));
            } else {

                // source file is neither smb nor otg; getting a channel from direct file instead of stream
                File file = new File(mSourceFile.getPath());
                if (FileUtil.isReadable(file)) {

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
                    ContentResolver contentResolver = mContext.getContentResolver();
                    DocumentFile documentSourceFile = FileUtil.getDocumentFile(file,
                            mSourceFile.isDirectory(), mContext);

                    bufferedInputStream = new BufferedInputStream(contentResolver
                            .openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
                }
            }

            // initializing the output channels based on file types
            if (mTargetFile.isOtgFile()) {

                // target in OTG, obtain streams from DocumentFile Uri's

                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentTargetFile = OTGUtil.getDocumentFile(mTargetFile.getPath(),
                        mContext, true);

                bufferedOutputStream = new BufferedOutputStream(contentResolver
                        .openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
            } else if (mTargetFile.isSftp()) {
                bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else if (mTargetFile.isSmb()) {

                bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else if (mTargetFile.isDropBoxFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);

                if (mSourceFile.isDropBoxFile()) {
                    // we're in the same provider, use api method
                    cloudStorageDropbox.copy(CloudUtil.stripPath(OpenMode.DROPBOX, mSourceFile.getPath()),
                            CloudUtil.stripPath(OpenMode.DROPBOX, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageDropbox.upload(CloudUtil.stripPath(OpenMode.DROPBOX, mTargetFile.getPath()),
                            bufferedInputStream, mSourceFile.getSize(), true);
                    return;
                }
            } else if (mTargetFile.isBoxFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);

                if (mSourceFile.isBoxFile()) {
                    // we're in the same provider, use api method
                    cloudStorageBox.copy(CloudUtil.stripPath(OpenMode.BOX, mSourceFile.getPath()),
                            CloudUtil.stripPath(OpenMode.BOX, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageBox.upload(CloudUtil.stripPath(OpenMode.BOX, mTargetFile.getPath()),
                            bufferedInputStream, mSourceFile.getSize(), true);
                    bufferedInputStream.close();
                    return;
                }
            } else if (mTargetFile.isGoogleDriveFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);


                if (mSourceFile.isGoogleDriveFile()) {
                    // we're in the same provider, use api method
                    cloudStorageGdrive.copy(CloudUtil.stripPath(OpenMode.GDRIVE, mSourceFile.getPath()),
                            CloudUtil.stripPath(OpenMode.GDRIVE, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageGdrive.upload(CloudUtil.stripPath(OpenMode.GDRIVE, mTargetFile.getPath()),
                            bufferedInputStream, mSourceFile.getSize(), true);
                    bufferedInputStream.close();
                    return;
                }
            } else if (mTargetFile.isOneDriveFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

                if (mSourceFile.isOneDriveFile()) {
                    // we're in the same provider, use api method
                    cloudStorageOnedrive.copy(CloudUtil.stripPath(OpenMode.ONEDRIVE, mSourceFile.getPath()),
                            CloudUtil.stripPath(OpenMode.ONEDRIVE, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageOnedrive.upload(CloudUtil.stripPath(OpenMode.ONEDRIVE, mTargetFile.getPath()),
                            bufferedInputStream, mSourceFile.getSize(), true);
                    bufferedInputStream.close();
                    return;
                }
            } else {
                // copying normal file, target not in OTG
                File file = new File(mTargetFile.getPath());
                if (FileUtil.isWritable(file)) {

                    if (lowOnMemory) {
                        bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                    } else {

                        outChannel = new RandomAccessFile(file, "rw").getChannel();
                    }
                } else {
                    ContentResolver contentResolver = mContext.getContentResolver();
                    DocumentFile documentTargetFile = FileUtil.getDocumentFile(file,
                            mTargetFile.isDirectory(), mContext);

                    bufferedOutputStream = new BufferedOutputStream(contentResolver
                            .openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
                }
            }

            if (bufferedInputStream!=null) {
                if (bufferedOutputStream!=null) copyFile(bufferedInputStream, bufferedOutputStream);
                else if (outChannel!=null) {
                    copyFile(bufferedInputStream, outChannel);
                }
            } else if (inChannel!=null) {
                if (bufferedOutputStream!=null) copyFile(inChannel, bufferedOutputStream);
                else if (outChannel!=null)  copyFile(inChannel, outChannel);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(getClass().getSimpleName(), "I/O Error!");
            throw new IOException();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();

            // we ran out of memory to map the whole channel, let's switch to streams
            AppConfig.toast(mContext, mContext.getString(R.string.copy_low_memory));

            startCopy(true);
        } finally {

            try {
                if (inChannel!=null) inChannel.close();
                if (outChannel!=null) outChannel.close();
                if (inputStream!=null) inputStream.close();
                if (outputStream!=null) outputStream.close();
                if (bufferedInputStream!=null) bufferedInputStream.close();
                if (bufferedOutputStream!=null) bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                // failure in closing stream
            }

            //If target file is copied onto the device and copy was successful, trigger media store
            //rescan

            if((mTargetFile.isLocal() || mTargetFile.isOtgFile()) && mTargetFile.exists(mContext)) {

                DocumentFile documentFile = FileUtil.getDocumentFile(mTargetFile.getFile(), false, mContext);
                //If FileUtil.getDocumentFile() returns null, fall back to DocumentFile.fromFile()
                if(documentFile == null)
                    documentFile = DocumentFile.fromFile(mTargetFile.getFile());

                FileUtils.scanFile(documentFile.getUri(), mContext);
            }
        }
    }

    /**
     * Method exposes this class to initiate copy
     * @param sourceFile the source file, which is to be copied
     * @param targetFile the target file
     */
    public void copy(HybridFileParcelable sourceFile, HybridFile targetFile) throws IOException {

        this.mSourceFile = sourceFile;
        this.mTargetFile = targetFile;

        startCopy(false);
    }

    private void copyFile(BufferedInputStream bufferedInputStream, FileChannel outChannel)
            throws IOException {

        MappedByteBuffer byteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0,
                mSourceFile.getSize());
        int count = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (count != -1) {

            count = bufferedInputStream.read(buffer);
            if (count!=-1 && !progressHandler.getCancelled()) {

                byteBuffer.put(buffer, 0, count);
                ServiceWatcherUtil.position +=count;
            } else break;
        }
    }

    private void copyFile(FileChannel inChannel, FileChannel outChannel) throws IOException {

        //MappedByteBuffer inByteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        //MappedByteBuffer outByteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());

        ReadableByteChannel inByteChannel = new CustomReadableByteChannel(inChannel);
        outChannel.transferFrom(inByteChannel, 0, mSourceFile.getSize());
    }

    private void copyFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream)
            throws IOException {
        int count = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        try {
            while (count != -1) {

                count = bufferedInputStream.read(buffer);
                if (count!=-1 && !progressHandler.getCancelled()) {

                    bufferedOutputStream.write(buffer, 0 , count);
                    ServiceWatcherUtil.position +=count;
                } else break;
            }
        } finally {
            bufferedOutputStream.flush();
        }
    }

    private void copyFile(FileChannel inChannel, BufferedOutputStream bufferedOutputStream)
            throws IOException {
        MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, mSourceFile.getSize());

        int count = -1;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            while (inBuffer.hasRemaining() && count != 0) {

                int tempPosition = inBuffer.position();

                try {

                    // try normal way of getting bytes
                    ByteBuffer tempByteBuffer = inBuffer.get(buffer);
                    count = tempByteBuffer.position() - tempPosition;
                } catch (BufferUnderflowException exception) {
                    exception.printStackTrace();

                    // not enough bytes left in the channel to read, iterate over each byte and store
                    // in the buffer

                    // reset the counter bytes
                    count = 0;
                    for (int i=0; i<buffer.length && inBuffer.hasRemaining(); i++) {
                        buffer[i] = inBuffer.get();
                        count++;
                    }
                }

                if (count != -1 && !progressHandler.getCancelled()) {

                    bufferedOutputStream.write(buffer, 0, count);
                    ServiceWatcherUtil.position = inBuffer.position();
                } else break;

            }
        } finally {
            bufferedOutputStream.flush();
        }
    }

    /**
     * Inner class responsible for getting a {@link ReadableByteChannel} from the input channel
     * and to watch over the read progress
     */
    private class CustomReadableByteChannel implements ReadableByteChannel {

        ReadableByteChannel byteChannel;


        CustomReadableByteChannel(ReadableByteChannel byteChannel) {
            this.byteChannel = byteChannel;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int bytes;
            if (((bytes = byteChannel.read(dst))>0) && !progressHandler.getCancelled()) {

                ServiceWatcherUtil.position += bytes;
                return bytes;

            } else
                return 0;
        }

        @Override
        public boolean isOpen() {
            return byteChannel.isOpen();
        }

        @Override
        public void close() throws IOException {

            byteChannel.close();
        }
    }
}
