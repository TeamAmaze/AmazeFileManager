/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.movecopy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.RenameFileCommand;
import com.amaze.filemanager.utils.omh.OMHClientHelper;
import com.amaze.filemanager.utils.omh.OmhAuthClientExtKt;
import com.amaze.filemanager.utils.omh.OmhStorageClientExtKt;
import com.openmobilehub.android.storage.core.OmhStorageClient;
import com.openmobilehub.android.storage.core.model.OmhStorageEntity;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import kotlin.Unit;
import kotlin.text.StringsKt;

/**
 * AsyncTask that moves files from source to destination by trying to rename files first, if they're
 * in the same filesystem, else starting the copy service. Be advised - do not start this AsyncTask
 * directly but use {@link PreparePasteTask} instead
 */
public class MoveFiles implements Callable<MoveFilesReturn> {

  private final Logger LOG = LoggerFactory.getLogger(MoveFiles.class);

  private final ArrayList<ArrayList<HybridFileParcelable>> files;
  private final ArrayList<String> paths;
  private final Context context;
  private final OpenMode mode;
  private long totalBytes = 0L;
  private final boolean isRootExplorer;

  public MoveFiles(
      ArrayList<ArrayList<HybridFileParcelable>> files,
      boolean isRootExplorer,
      Context context,
      OpenMode mode,
      ArrayList<String> paths) {
    this.context = context;
    this.files = files;
    this.mode = mode;
    this.isRootExplorer = isRootExplorer;
    this.paths = paths;
  }

  @WorkerThread
  @Override
  public MoveFilesReturn call() {
    if (files.size() == 0) {
      return new MoveFilesReturn(true, false, 0, 0);
    }

    for (ArrayList<HybridFileParcelable> filesCurrent : files) {
      totalBytes += FileUtils.getTotalBytes(filesCurrent, context);
    }
    HybridFile destination = new HybridFile(mode, paths.get(0));
    long destinationSize = destination.getUsableSpace();

    for (int i = 0; i < paths.size(); i++) {
      for (HybridFileParcelable baseFile : files.get(i)) {
        final MoveFilesReturn r = processFile(baseFile, paths.get(i), destinationSize);
        if (r != null) {
          return r;
        }
      }
    }
    return new MoveFilesReturn(true, false, destinationSize, totalBytes);
  }

  @Nullable
  private MoveFilesReturn processFile(
      HybridFileParcelable baseFile, String path, long destinationSize) {
    String destPath = path + "/" + baseFile.getName(context);
    if (baseFile.getPath().indexOf('?') > 0)
      destPath += baseFile.getPath().substring(baseFile.getPath().indexOf('?'));
    if (!isMoveOperationValid(baseFile, new HybridFile(mode, path))) {
      // TODO: 30/06/20 Replace runtime exception with generic exception
      LOG.warn("Some files failed to be moved", new RuntimeException());
      return new MoveFilesReturn(false, true, destinationSize, totalBytes);
    }
    switch (mode) {
      case FILE:
        File dest = new File(destPath);
        File source = new File(baseFile.getPath());
        if (!source.renameTo(dest)) {

          // check if we have root
          if (isRootExplorer) {
            try {
              if (!RenameFileCommand.INSTANCE.renameFile(baseFile.getPath(), destPath)) {
                return new MoveFilesReturn(false, false, destinationSize, totalBytes);
              }
            } catch (ShellNotRunningException e) {
              LOG.warn("failed to move file in local filesystem", e);
              return new MoveFilesReturn(false, false, destinationSize, totalBytes);
            }
          } else {
            return new MoveFilesReturn(false, false, destinationSize, totalBytes);
          }
        }
        break;
      case DROPBOX:
      case BOX:
      case ONEDRIVE:
      case GDRIVE:
        OmhStorageClient storageClient = OMHClientHelper.getStorageClient(mode);
        if (storageClient == null) {
          LOG.warn("No storage client available for mode: {}", mode);
          return new MoveFilesReturn(false, false, destinationSize, totalBytes);
        }
        try {
          OmhAuthClientExtKt.retryOnUnauthorizedBlocking(
              mode,
              AppConfig.getInstance().getCloudAuthTrigger(),
              () -> {
                // 1. Download source file into a local temp file
                String name = baseFile.getName(context);
                String baseName = StringsKt.substringBeforeLast(name, ".", name);
                String ext = StringsKt.substringAfterLast(name, ".", "");
                File tmpFile;
                try {
                  tmpFile =
                      File.createTempFile(
                          baseName, "." + ext, AppConfig.getInstance().getCacheDir());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
                tmpFile.deleteOnExit();
                try {
                  OmhStorageClientExtKt.downloadFileBlocking(
                          storageClient, baseFile.getCloudFileId())
                      .writeTo(new FileOutputStream(tmpFile));
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
                // 2. Resolve destination parent folder
                OmhStorageEntity destFolder =
                    OmhStorageClientExtKt.resolvePathBlocking(
                        storageClient, CloudUtil.stripCloudPath(mode, path));
                String parentId =
                    (destFolder != null && destFolder.getId() != null)
                        ? destFolder.getId()
                        : storageClient.getRootFolder();
                // 3. Upload to destination
                OmhStorageClientExtKt.uploadFileBlocking(storageClient, tmpFile, parentId);
                tmpFile.delete();
                // 4. Delete original source
                OmhStorageClientExtKt.deleteFileBlocking(storageClient, baseFile.getCloudFileId());
                return Unit.INSTANCE;
              });
        } catch (Exception e) {
          LOG.warn("Cloud move failed for {}", baseFile.getPath(), e);
          return new MoveFilesReturn(false, false, destinationSize, totalBytes);
        }
        break;
      default:
        return new MoveFilesReturn(false, false, destinationSize, totalBytes);
    }

    return null;
  }

  private boolean isMoveOperationValid(HybridFileParcelable sourceFile, HybridFile targetFile) {
    return !Operations.isCopyLoopPossible(sourceFile, targetFile) && sourceFile.exists(context);
  }

  /**
   * Maintains a list of filesystems supporting the move/rename implementation. Please update to
   * return your {@link OpenMode} type if it is supported here
   *
   * @return
   */
  public static HashSet<OpenMode> getOperationSupportedFileSystem() {
    HashSet<OpenMode> hashSet = new HashSet<>();
    hashSet.add(OpenMode.SMB);
    hashSet.add(OpenMode.FILE);
    hashSet.add(OpenMode.DROPBOX);
    hashSet.add(OpenMode.BOX);
    hashSet.add(OpenMode.GDRIVE);
    hashSet.add(OpenMode.ONEDRIVE);
    return hashSet;
  }
}
