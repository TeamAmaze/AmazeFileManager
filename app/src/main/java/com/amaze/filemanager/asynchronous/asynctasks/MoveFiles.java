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

package com.amaze.filemanager.asynchronous.asynctasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.explorer.EncryptedEntry;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.RenameFileCommand;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.utils.DataUtils;
import com.cloudrail.si.interfaces.CloudStorage;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * AsyncTask that moves files from source to destination by trying to rename files first, if they're
 * in the same filesystem, else starting the copy service. Be advised - do not start this AsyncTask
 * directly but use {@link PrepareCopyTask} instead
 */
public class MoveFiles extends AsyncTask<ArrayList<String>, String, Boolean> {

  private ArrayList<ArrayList<HybridFileParcelable>> files;
  private ArrayList<String> paths;
  private Context context;
  private OpenMode mode;
  private long totalBytes = 0l;
  private long destinationSize = 0l;
  private boolean invalidOperation = false;
  private boolean isRootExplorer;
  private String currentPath;

  public MoveFiles(
      ArrayList<ArrayList<HybridFileParcelable>> files,
      boolean isRootExplorer,
      String currentPath,
      Context context,
      OpenMode mode) {
    this.context = context;
    this.files = files;
    this.mode = mode;
    this.isRootExplorer = isRootExplorer;
    this.currentPath = currentPath;
  }

  @Override
  protected Boolean doInBackground(ArrayList<String>... strings) {
    paths = strings[0];

    if (files.size() == 0) return true;

    for (ArrayList<HybridFileParcelable> filesCurrent : files) {
      totalBytes += FileUtils.getTotalBytes(filesCurrent, context);
    }
    HybridFile destination = new HybridFile(mode, paths.get(0));
    destinationSize = destination.getUsableSpace();

    for (int i = 0; i < paths.size(); i++) {
      for (HybridFileParcelable baseFile : files.get(i)) {
        String destPath = paths.get(i) + "/" + baseFile.getName(context);
        if (baseFile.getPath().indexOf('?') > 0)
          destPath += baseFile.getPath().substring(baseFile.getPath().indexOf('?'));
        if (!isMoveOperationValid(baseFile, new HybridFile(mode, paths.get(i)))) {
          // TODO: 30/06/20 Replace runtime exception with generic exception
          Log.w(
              getClass().getSimpleName(), "Some files failed to be moved", new RuntimeException());
          invalidOperation = true;
          continue;
        }
        switch (mode) {
          case FILE:
            File dest = new File(destPath);
            File source = new File(baseFile.getPath());
            if (!source.renameTo(dest)) {

              // check if we have root
              if (isRootExplorer) {
                try {
                  if (!RenameFileCommand.INSTANCE.renameFile(baseFile.getPath(), destPath))
                    return false;
                } catch (ShellNotRunningException e) {
                  e.printStackTrace();
                  return false;
                }
              } else return false;
            }
            break;
          case DROPBOX:
          case BOX:
          case ONEDRIVE:
          case GDRIVE:
            DataUtils dataUtils = DataUtils.getInstance();

            CloudStorage cloudStorage = dataUtils.getAccount(mode);
            if (baseFile.getMode() == mode) {
              // source and target both in same filesystem, use API method
              try {

                cloudStorage.move(
                    CloudUtil.stripPath(mode, baseFile.getPath()),
                    CloudUtil.stripPath(mode, destPath));
              } catch (Exception e) {
                e.printStackTrace();
                return false;
              }
            } else {
              // not in same filesystem, execute service
              return false;
            }
          default:
            return false;
        }
      }
    }
    return true;
  }

  @Override
  public void onPostExecute(Boolean movedCorrectly) {
    if (movedCorrectly) {
      if (currentPath.equals(paths.get(0))) {
        // mainFrag.updateList();
        Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);

        intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, paths.get(0));
        context.sendBroadcast(intent);
      }

      if (invalidOperation) {
        Toast.makeText(context, R.string.some_files_failed_invalid_operation, Toast.LENGTH_LONG)
            .show();
      }

      for (int i = 0; i < paths.size(); i++) {
        List<HybridFile> targetFiles = new ArrayList<>();
        List<HybridFileParcelable> sourcesFiles = new ArrayList<>();
        for (HybridFileParcelable f : files.get(i)) {
          targetFiles.add(new HybridFile(OpenMode.FILE, paths.get(i) + "/" + f.getName(context)));
        }
        for (List<HybridFileParcelable> hybridFileParcelables : files) {
          sourcesFiles.addAll(hybridFileParcelables);
        }
        FileUtils.scanFile(
            context, sourcesFiles.toArray(new HybridFileParcelable[sourcesFiles.size()]));
        FileUtils.scanFile(context, targetFiles.toArray(new HybridFile[targetFiles.size()]));
      }

      // updating encrypted db entry if any encrypted file was moved
      AppConfig.getInstance()
          .runInBackground(
              () -> {
                for (int i = 0; i < paths.size(); i++) {
                  for (HybridFileParcelable file : files.get(i)) {
                    if (file.getName(context).endsWith(CryptUtil.CRYPT_EXTENSION)) {
                      try {
                        CryptHandler cryptHandler = CryptHandler.getInstance();
                        EncryptedEntry oldEntry = cryptHandler.findEntry(file.getPath());
                        EncryptedEntry newEntry = new EncryptedEntry();
                        newEntry.setId(oldEntry.getId());
                        newEntry.setPassword(oldEntry.getPassword());
                        newEntry.setPath(paths.get(i) + "/" + file.getName(context));
                        cryptHandler.updateEntry(oldEntry, newEntry);
                      } catch (Exception e) {
                        e.printStackTrace();
                        // couldn't change the entry, leave it alone
                      }
                    }
                  }
                }
              });

    } else {

      if (destinationSize < totalBytes) {
        // destination don't have enough space; return
        Toast.makeText(
                context, context.getResources().getString(R.string.in_safe), Toast.LENGTH_LONG)
            .show();
        return;
      }

      for (int i = 0; i < paths.size(); i++) {
        Intent intent = new Intent(context, CopyService.class);
        intent.putExtra(CopyService.TAG_COPY_SOURCES, files.get(i));
        intent.putExtra(CopyService.TAG_COPY_TARGET, paths.get(i));
        intent.putExtra(CopyService.TAG_COPY_MOVE, true);
        intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, mode.ordinal());
        intent.putExtra(CopyService.TAG_IS_ROOT_EXPLORER, isRootExplorer);

        ServiceWatcherUtil.runService(context, intent);
      }
    }
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
