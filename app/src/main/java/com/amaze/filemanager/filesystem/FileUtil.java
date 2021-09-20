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

package com.amaze.filemanager.filesystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.exceptions.NotAllowedException;
import com.amaze.filemanager.exceptions.OperationWouldOverwriteException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.SmbUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jcifs.smb.SmbFile;
import kotlin.NotImplementedError;

/** Utility class for helping parsing file systems. */
public abstract class FileUtil {

  /**
   * Determine the camera folder. There seems to be no Android API to work for real devices, so this
   * is a best guess.
   *
   * @return the default camera folder.
   */
  // TODO the function?

  @Nullable
  public static OutputStream getOutputStream(final File target, Context context)
      throws FileNotFoundException {
    OutputStream outStream = null;
    // First try the normal way
    if (FileProperties.isWritable(target)) {
      // standard way
      outStream = new FileOutputStream(target);
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        // Storage Access Framework
        DocumentFile targetDocument =
            ExternalSdCardOperation.getDocumentFile(target, false, context);
        if (targetDocument == null) return null;
        outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
      } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
        // Workaround for Kitkat ext SD card
        return MediaStoreHack.getOutputStream(context, target.getPath());
      }
    }
    return outStream;
  }

  /** Writes uri stream from external application to the specified path */
  public static final void writeUriToStorage(
      @NonNull final MainActivity mainActivity,
      @NonNull final ArrayList<Uri> uris,
      @NonNull final ContentResolver contentResolver,
      @NonNull final String currentPath) {

    MaybeOnSubscribe<List<String>> writeUri =
        (MaybeOnSubscribe<List<String>>)
            emitter -> {
              List<String> retval = new ArrayList<>();

              for (Uri uri : uris) {

                BufferedInputStream bufferedInputStream = null;
                try {
                  bufferedInputStream =
                      new BufferedInputStream(contentResolver.openInputStream(uri));
                } catch (FileNotFoundException e) {
                  emitter.onError(e);
                  return;
                }

                BufferedOutputStream bufferedOutputStream = null;

                try {
                  DocumentFile documentFile = DocumentFile.fromSingleUri(mainActivity, uri);
                  String filename = documentFile.getName();
                  if (filename == null) {
                    filename = uri.getLastPathSegment();

                    // For cleaning up slashes. Back in #1217 there is a case of
                    // Uri.getLastPathSegment() end up with a full file path
                    if (filename.contains("/"))
                      filename = filename.substring(filename.lastIndexOf('/') + 1);
                  }

                  String finalFilePath = currentPath + "/" + filename;
                  DataUtils dataUtils = DataUtils.getInstance();

                  HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, currentPath);
                  hFile.generateMode(mainActivity);

                  switch (hFile.getMode()) {
                    case FILE:
                    case ROOT:
                      File targetFile = new File(finalFilePath);
                      if (!FileProperties.isWritableNormalOrSaf(
                          targetFile.getParentFile(), mainActivity.getApplicationContext())) {
                        emitter.onError(new NotAllowedException());
                        return;
                      }

                      DocumentFile targetDocumentFile =
                          ExternalSdCardOperation.getDocumentFile(
                              targetFile, false, mainActivity.getApplicationContext());

                      // Fallback, in case getDocumentFile() didn't properly return a
                      // DocumentFile
                      // instance
                      if (targetDocumentFile == null) {
                        targetDocumentFile = DocumentFile.fromFile(targetFile);
                      }

                      // Lazy check... and in fact, different apps may pass in URI in different
                      // formats, so we could only check filename matches
                      // FIXME?: Prompt overwrite instead of simply blocking
                      if (targetDocumentFile.exists() && targetDocumentFile.length() > 0) {
                        emitter.onError(new OperationWouldOverwriteException());
                        return;
                      }

                      bufferedOutputStream =
                          new BufferedOutputStream(
                              contentResolver.openOutputStream(targetDocumentFile.getUri()));
                      retval.add(targetFile.getPath());
                      break;
                    case SMB:
                      SmbFile targetSmbFile = SmbUtil.create(finalFilePath);
                      if (targetSmbFile.exists()) {
                        emitter.onError(new OperationWouldOverwriteException());
                        return;
                      } else {
                        OutputStream outputStream = targetSmbFile.getOutputStream();
                        bufferedOutputStream = new BufferedOutputStream(outputStream);
                        retval.add(HybridFile.parseAndFormatUriForDisplay(targetSmbFile.getPath()));
                      }
                      break;
                    case SFTP:
                      // FIXME: implement support
                      AppConfig.toast(mainActivity, mainActivity.getString(R.string.not_allowed));
                      emitter.onError(new NotImplementedError());
                      return;
                    case DROPBOX:
                    case BOX:
                    case ONEDRIVE:
                    case GDRIVE:
                      OpenMode mode = hFile.getMode();

                      CloudStorage cloudStorage = dataUtils.getAccount(mode);
                      String path = CloudUtil.stripPath(mode, finalFilePath);
                      cloudStorage.upload(path, bufferedInputStream, documentFile.length(), true);
                      retval.add(path);
                      break;
                    case OTG:
                      DocumentFile documentTargetFile =
                          OTGUtil.getDocumentFile(finalFilePath, mainActivity, true);

                      if (documentTargetFile.exists()) {
                        emitter.onError(new OperationWouldOverwriteException());
                        return;
                      }

                      bufferedOutputStream =
                          new BufferedOutputStream(
                              contentResolver.openOutputStream(documentTargetFile.getUri()),
                              GenericCopyUtil.DEFAULT_BUFFER_SIZE);

                      retval.add(documentTargetFile.getUri().getPath());
                      break;
                    default:
                      return;
                  }

                  int count = 0;
                  byte[] buffer = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];

                  while (count != -1) {
                    count = bufferedInputStream.read(buffer);
                    if (count != -1) {

                      bufferedOutputStream.write(buffer, 0, count);
                    }
                  }
                  bufferedOutputStream.flush();

                } catch (IOException e) {
                  emitter.onError(e);
                  return;
                } finally {
                  try {
                    if (bufferedInputStream != null) {
                      bufferedInputStream.close();
                    }
                    if (bufferedOutputStream != null) {
                      bufferedOutputStream.close();
                    }
                  } catch (IOException e) {
                    emitter.onError(e);
                  }
                }
              }

              if (retval.size() > 0) {
                emitter.onSuccess(retval);
              } else {
                emitter.onError(new Exception());
              }
            };

    Maybe.create(writeUri)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new MaybeObserver<List<String>>() {
              @Override
              public void onSubscribe(@NonNull Disposable d) {}

              @Override
              public void onSuccess(@NonNull List<String> paths) {
                MediaScannerConnection.scanFile(
                    mainActivity.getApplicationContext(),
                    paths.toArray(new String[0]),
                    new String[paths.size()],
                    null);
                if (paths.size() == 1) {
                  Toast.makeText(
                          mainActivity,
                          mainActivity.getString(R.string.saved_single_file, paths.get(0)),
                          Toast.LENGTH_LONG)
                      .show();
                } else {
                  Toast.makeText(
                          mainActivity,
                          mainActivity.getString(R.string.saved_multi_files, paths.size()),
                          Toast.LENGTH_LONG)
                      .show();
                }
              }

              @Override
              public void onError(@NonNull Throwable e) {
                if (e instanceof OperationWouldOverwriteException) {
                  AppConfig.toast(mainActivity, mainActivity.getString(R.string.cannot_overwrite));
                  return;
                }
                if (e instanceof NotAllowedException) {
                  AppConfig.toast(
                      mainActivity, mainActivity.getResources().getString(R.string.not_allowed));
                }

                Log.e(
                    getClass().getSimpleName(),
                    "Failed to write uri to storage due to " + e.getCause());
                e.printStackTrace();
              }

              @Override
              public void onComplete() {}
            });
  }
}
