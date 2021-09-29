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

import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_FAILED_OPS;
import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_GENERAL;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import com.amaze.filemanager.R;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.root.MakeDirectoryCommand;
import com.amaze.filemanager.filesystem.root.MakeFileCommand;
import com.amaze.filemanager.filesystem.root.RenameFileCommand;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.documentfile.provider.DocumentFile;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import net.schmizz.sshj.sftp.SFTPClient;

public class Operations {

  private static Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;

  private static final String TAG = Operations.class.getSimpleName();

  // reserved characters by OS, shall not be allowed in file names
  private static final String FOREWARD_SLASH = "/";
  private static final String BACKWARD_SLASH = "\\";
  private static final String COLON = ":";
  private static final String ASTERISK = "*";
  private static final String QUESTION_MARK = "?";
  private static final String QUOTE = "\"";
  private static final String GREATER_THAN = ">";
  private static final String LESS_THAN = "<";

  private static final String FAT = "FAT";

  public interface ErrorCallBack {

    /** Callback fired when file being created in process already exists */
    void exists(HybridFile file);

    /**
     * Callback fired when creating new file/directory and required storage access framework
     * permission to access SD Card is not available
     */
    void launchSAF(HybridFile file);

    /**
     * Callback fired when renaming file and required storage access framework permission to access
     * SD Card is not available
     */
    void launchSAF(HybridFile file, HybridFile file1);

    /**
     * Callback fired when we're done processing the operation
     *
     * @param b defines whether operation was successful
     */
    void done(HybridFile hFile, boolean b);

    /** Callback fired when an invalid file name is found. */
    void invalidName(HybridFile file);
  }

  public static void mkdir(
      final HybridFile parentFile,
      @NonNull final HybridFile file,
      final Context context,
      final boolean rootMode,
      @NonNull final ErrorCallBack errorCallBack) {

    new AsyncTask<Void, Void, Void>() {

      private DataUtils dataUtils = DataUtils.getInstance();

      private Function<DocumentFile, Void> safCreateDirectory =
          input -> {
            if (input != null && input.isDirectory()) {
              boolean result = false;
              try {
                result = input.createDirectory(file.getName(context)) != null;
              } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "Failed to make directory", e);
              }
              errorCallBack.done(file, result);
            } else errorCallBack.done(file, false);
            return null;
          };

      @Override
      protected Void doInBackground(Void... params) {
        // checking whether filename is valid or a recursive call possible
        if (!Operations.isFileNameValid(file.getName(context))) {
          errorCallBack.invalidName(file);
          return null;
        }

        if (file.exists()) {
          errorCallBack.exists(file);
          return null;
        }
        if (file.isSftp()) {
          file.mkdir(context);
          return null;
        }
        if (file.isSmb()) {
          try {
            file.getSmbFile(2000).mkdirs();
          } catch (SmbException e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
            return null;
          }
          errorCallBack.done(file, file.exists());
          return null;
        } else if (file.isOtgFile()) {
          if (checkOtgNewFileExists(file, context)) {
            errorCallBack.exists(file);
            return null;
          }
          safCreateDirectory.apply(OTGUtil.getDocumentFile(parentFile.getPath(), context, false));
          return null;
        } else if (file.isDocumentFile()) {
          if (checkDocumentFileNewFileExists(file, context)) {
            errorCallBack.exists(file);
            return null;
          }
          safCreateDirectory.apply(
              OTGUtil.getDocumentFile(
                  parentFile.getPath(),
                  SafRootHolder.getUriRoot(),
                  context,
                  OpenMode.DOCUMENT_FILE,
                  false));
          return null;
        } else if (file.isDropBoxFile()) {
          CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
          try {
            cloudStorageDropbox.createFolder(CloudUtil.stripPath(OpenMode.DROPBOX, file.getPath()));
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isBoxFile()) {
          CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
          try {
            cloudStorageBox.createFolder(CloudUtil.stripPath(OpenMode.BOX, file.getPath()));
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isOneDriveFile()) {
          CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
          try {
            cloudStorageOneDrive.createFolder(
                CloudUtil.stripPath(OpenMode.ONEDRIVE, file.getPath()));
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isGoogleDriveFile()) {
          CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
          try {
            cloudStorageGdrive.createFolder(CloudUtil.stripPath(OpenMode.GDRIVE, file.getPath()));
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else {
          if (file.isLocal() || file.isRoot()) {
            int mode = checkFolder(new File(file.getParent(context)), context);
            if (mode == 2) {
              errorCallBack.launchSAF(file);
              return null;
            }
            if (mode == 1 || mode == 0) MakeDirectoryOperation.mkdir(file.getFile(), context);
            if (!file.exists() && rootMode) {
              file.setMode(OpenMode.ROOT);
              if (file.exists()) errorCallBack.exists(file);
              try {
                MakeDirectoryCommand.INSTANCE.makeDirectory(
                    file.getParent(context), file.getName(context));
              } catch (ShellNotRunningException e) {
                e.printStackTrace();
              }
              errorCallBack.done(file, file.exists());
              return null;
            }
            errorCallBack.done(file, file.exists());
            return null;
          }

          errorCallBack.done(file, file.exists());
        }
        return null;
      }
    }.executeOnExecutor(executor);
  }

  public static void mkfile(
      final HybridFile parentFile,
      @NonNull final HybridFile file,
      final Context context,
      final boolean rootMode,
      @NonNull final ErrorCallBack errorCallBack) {

    new AsyncTask<Void, Void, Void>() {

      private DataUtils dataUtils = DataUtils.getInstance();

      private Function<DocumentFile, Void> safCreateFile =
          input -> {
            if (input != null && input.isDirectory()) {
              boolean result = false;
              try {
                result =
                    input.createFile(
                            file.getName(context).substring(file.getName(context).lastIndexOf(".")),
                            file.getName(context))
                        != null;
              } catch (Exception e) {
                Log.w(getClass().getSimpleName(), "Failed to make file", e);
              }
              errorCallBack.done(file, result);
            } else errorCallBack.done(file, false);
            return null;
          };

      @Override
      protected Void doInBackground(Void... params) {
        // check whether filename is valid or not
        if (!Operations.isFileNameValid(file.getName(context))) {
          errorCallBack.invalidName(file);
          return null;
        }

        if (file.exists()) {
          errorCallBack.exists(file);
          return null;
        }
        if (file.isSftp()) {
          OutputStream out = file.getOutputStream(context);
          if (out == null) {
            errorCallBack.done(file, false);
            return null;
          }
          try {
            out.close();
            errorCallBack.done(file, true);
            return null;
          } catch (IOException e) {
            errorCallBack.done(file, false);
            return null;
          }
        }
        if (file.isSmb()) {
          try {
            file.getSmbFile(2000).createNewFile();
          } catch (SmbException e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
            return null;
          }
          errorCallBack.done(file, file.exists());
          return null;
        } else if (file.isDropBoxFile()) {
          CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
          try {
            byte[] tempBytes = new byte[0];
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
            cloudStorageDropbox.upload(
                CloudUtil.stripPath(OpenMode.DROPBOX, file.getPath()),
                byteArrayInputStream,
                0l,
                true);
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isBoxFile()) {
          CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
          try {
            byte[] tempBytes = new byte[0];
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
            cloudStorageBox.upload(
                CloudUtil.stripPath(OpenMode.BOX, file.getPath()), byteArrayInputStream, 0l, true);
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isOneDriveFile()) {
          CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
          try {
            byte[] tempBytes = new byte[0];
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
            cloudStorageOneDrive.upload(
                CloudUtil.stripPath(OpenMode.ONEDRIVE, file.getPath()),
                byteArrayInputStream,
                0l,
                true);
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isGoogleDriveFile()) {
          CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
          try {
            byte[] tempBytes = new byte[0];
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tempBytes);
            cloudStorageGdrive.upload(
                CloudUtil.stripPath(OpenMode.GDRIVE, file.getPath()),
                byteArrayInputStream,
                0l,
                true);
            errorCallBack.done(file, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(file, false);
          }
        } else if (file.isOtgFile()) {
          if (checkOtgNewFileExists(file, context)) {
            errorCallBack.exists(file);
            return null;
          }
          safCreateFile.apply(OTGUtil.getDocumentFile(parentFile.getPath(), context, false));
          return null;
        } else if (file.isDocumentFile()) {
          if (checkDocumentFileNewFileExists(file, context)) {
            errorCallBack.exists(file);
            return null;
          }
          safCreateFile.apply(
              OTGUtil.getDocumentFile(
                  parentFile.getPath(),
                  SafRootHolder.getUriRoot(),
                  context,
                  OpenMode.DOCUMENT_FILE,
                  false));
          return null;
        } else {
          if (file.isLocal() || file.isRoot()) {
            int mode = checkFolder(new File(file.getParent(context)), context);
            if (mode == 2) {
              errorCallBack.launchSAF(file);
              return null;
            }
            if (mode == 1 || mode == 0) MakeFileOperation.mkfile(file.getFile(), context);
            if (!file.exists() && rootMode) {
              file.setMode(OpenMode.ROOT);
              if (file.exists()) errorCallBack.exists(file);
              try {
                MakeFileCommand.INSTANCE.makeFile(file.getPath());
              } catch (ShellNotRunningException e) {
                e.printStackTrace();
              }
              errorCallBack.done(file, file.exists());
              return null;
            }
            errorCallBack.done(file, file.exists());
            return null;
          }
          errorCallBack.done(file, file.exists());
        }
        return null;
      }
    }.executeOnExecutor(executor);
  }

  public static void rename(
      @NonNull final HybridFile oldFile,
      @NonNull final HybridFile newFile,
      final boolean rootMode,
      @NonNull final Context context,
      @NonNull final ErrorCallBack errorCallBack) {

    new AsyncTask<Void, Void, Void>() {

      private final DataUtils dataUtils = DataUtils.getInstance();

      private Function<DocumentFile, Void> safRenameFile =
          input -> {
            boolean result = false;
            try {
              result = input.renameTo(newFile.getName(context));
            } catch (Exception e) {
              Log.w(getClass().getSimpleName(), "Failed to rename", e);
            }
            errorCallBack.done(newFile, result);
            return null;
          };

      @Override
      protected Void doInBackground(Void... params) {
        // check whether file names for new file are valid or recursion occurs.
        // If rename is on OTG, we are skipping
        if (!Operations.isFileNameValid(newFile.getName(context))) {
          errorCallBack.invalidName(newFile);
          return null;
        }

        if (newFile.exists()) {
          errorCallBack.exists(newFile);
          return null;
        }

        if (oldFile.isSmb()) {
          try {
            SmbFile smbFile = oldFile.getSmbFile();
            // FIXME: smbFile1 should be created from SmbUtil too so it can be mocked
            SmbFile smbFile1 = new SmbFile(new URL(newFile.getPath()), smbFile.getContext());
            if (newFile.exists()) {
              errorCallBack.exists(newFile);
              return null;
            }
            smbFile.renameTo(smbFile1);
            if (!smbFile.exists() && smbFile1.exists()) errorCallBack.done(newFile, true);
          } catch (SmbException | MalformedURLException e) {
            String errmsg =
                context.getString(
                    R.string.cannot_rename_file,
                    HybridFile.parseAndFormatUriForDisplay(oldFile.getPath()),
                    e.getMessage());
            try {
              ArrayList<HybridFileParcelable> failedOps = new ArrayList<>();
              failedOps.add(new HybridFileParcelable(oldFile.getSmbFile()));
              context.sendBroadcast(
                  new Intent(TAG_INTENT_FILTER_GENERAL)
                      .putParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS, failedOps));
            } catch (SmbException exceptionThrownDuringBuildParcelable) {
              Log.e(
                  TAG, "Error creating HybridFileParcelable", exceptionThrownDuringBuildParcelable);
            }
            Log.e(TAG, errmsg, e);
          }
          return null;
        } else if (oldFile.isSftp()) {
          SshClientUtils.execute(
              new SFtpClientTemplate<Void>(oldFile.getPath()) {
                @Override
                public Void execute(@NonNull SFTPClient client) {
                  try {
                    client.rename(
                        SshClientUtils.extractRemotePathFrom(oldFile.getPath()),
                        SshClientUtils.extractRemotePathFrom(newFile.getPath()));
                    errorCallBack.done(newFile, true);
                  } catch (IOException e) {
                    String errmsg =
                        context.getString(
                            R.string.cannot_rename_file,
                            HybridFile.parseAndFormatUriForDisplay(oldFile.getPath()),
                            e.getMessage());
                    Log.e(TAG, errmsg);
                    ArrayList<HybridFileParcelable> failedOps = new ArrayList<>();
                    // Nobody care the size or actual permission here. Put a simple "r" and zero
                    // here
                    failedOps.add(
                        new HybridFileParcelable(
                            oldFile.getPath(),
                            "r",
                            oldFile.lastModified(),
                            0,
                            oldFile.isDirectory(context)));
                    context.sendBroadcast(
                        new Intent(TAG_INTENT_FILTER_GENERAL)
                            .putParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS, failedOps));
                    errorCallBack.done(newFile, false);
                  }
                  return null;
                }
              });
        } else if (oldFile.isDropBoxFile()) {
          CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
          try {
            cloudStorageDropbox.move(
                CloudUtil.stripPath(OpenMode.DROPBOX, oldFile.getPath()),
                CloudUtil.stripPath(OpenMode.DROPBOX, newFile.getPath()));
            errorCallBack.done(newFile, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(newFile, false);
          }
        } else if (oldFile.isBoxFile()) {
          CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
          try {
            cloudStorageBox.move(
                CloudUtil.stripPath(OpenMode.BOX, oldFile.getPath()),
                CloudUtil.stripPath(OpenMode.BOX, newFile.getPath()));
            errorCallBack.done(newFile, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(newFile, false);
          }
        } else if (oldFile.isOneDriveFile()) {
          CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
          try {
            cloudStorageOneDrive.move(
                CloudUtil.stripPath(OpenMode.ONEDRIVE, oldFile.getPath()),
                CloudUtil.stripPath(OpenMode.ONEDRIVE, newFile.getPath()));
            errorCallBack.done(newFile, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(newFile, false);
          }
        } else if (oldFile.isGoogleDriveFile()) {
          CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
          try {
            cloudStorageGdrive.move(
                CloudUtil.stripPath(OpenMode.GDRIVE, oldFile.getPath()),
                CloudUtil.stripPath(OpenMode.GDRIVE, newFile.getPath()));
            errorCallBack.done(newFile, true);
          } catch (Exception e) {
            e.printStackTrace();
            errorCallBack.done(newFile, false);
          }
        } else if (oldFile.isOtgFile()) {
          if (checkOtgNewFileExists(newFile, context)) {
            errorCallBack.exists(newFile);
            return null;
          }
          safRenameFile.apply(OTGUtil.getDocumentFile(oldFile.getPath(), context, false));
          return null;
        } else if (oldFile.isDocumentFile()) {
          if (checkDocumentFileNewFileExists(newFile, context)) {
            errorCallBack.exists(newFile);
            return null;
          }
          safRenameFile.apply(
              OTGUtil.getDocumentFile(
                  oldFile.getPath(),
                  SafRootHolder.getUriRoot(),
                  context,
                  OpenMode.DOCUMENT_FILE,
                  false));
          return null;
        } else {
          File file = new File(oldFile.getPath());
          File file1 = new File(newFile.getPath());
          switch (oldFile.getMode()) {
            case FILE:
              int mode = checkFolder(file.getParentFile(), context);
              if (mode == 2) {
                errorCallBack.launchSAF(oldFile, newFile);
              } else if (mode == 1 || mode == 0) {
                try {
                  RenameOperation.renameFolder(file, file1, context);
                } catch (ShellNotRunningException e) {
                  e.printStackTrace();
                }
                boolean a = !file.exists() && file1.exists();
                if (!a && rootMode) {
                  try {
                    RenameFileCommand.INSTANCE.renameFile(file.getPath(), file1.getPath());
                  } catch (ShellNotRunningException e) {
                    e.printStackTrace();
                  }
                  oldFile.setMode(OpenMode.ROOT);
                  newFile.setMode(OpenMode.ROOT);
                  a = !file.exists() && file1.exists();
                }
                errorCallBack.done(newFile, a);
                return null;
              }
              break;
            case ROOT:
              try {
                RenameFileCommand.INSTANCE.renameFile(file.getPath(), file1.getPath());
              } catch (ShellNotRunningException e) {
                e.printStackTrace();
              }

              newFile.setMode(OpenMode.ROOT);
              errorCallBack.done(newFile, true);
              break;
          }
        }
        return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (newFile != null && oldFile != null) {
          HybridFile[] hybridFiles = {newFile, oldFile};
          FileUtils.scanFile(context, hybridFiles);
        }
      }
    }.executeOnExecutor(executor);
  }

  private static boolean checkOtgNewFileExists(HybridFile newFile, Context context) {
    boolean doesFileExist = false;
    try {
      doesFileExist = OTGUtil.getDocumentFile(newFile.getPath(), context, false) != null;
    } catch (Exception e) {
      Log.d(Operations.class.getSimpleName(), "Failed find existing file", e);
    }
    return doesFileExist;
  }

  private static boolean checkDocumentFileNewFileExists(HybridFile newFile, Context context) {
    boolean doesFileExist = false;
    try {
      doesFileExist =
          OTGUtil.getDocumentFile(
                  newFile.getPath(),
                  SafRootHolder.getUriRoot(),
                  context,
                  OpenMode.DOCUMENT_FILE,
                  false)
              != null;
    } catch (Exception e) {
      Log.w(Operations.class.getSimpleName(), "Failed to find existing file", e);
    }
    return doesFileExist;
  }

  private static int checkFolder(final File folder, Context context) {
    boolean lol = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    if (lol) {

      boolean ext = ExternalSdCardOperation.isOnExtSdCard(folder, context);
      if (ext) {

        if (!folder.exists() || !folder.isDirectory()) {
          return 0;
        }

        // On Android 5, trigger storage access framework.
        if (!FileProperties.isWritableNormalOrSaf(folder, context)) {
          return 2;
        }
        return 1;
      }
    } else if (Build.VERSION.SDK_INT == 19) {
      // Assume that Kitkat workaround works
      if (ExternalSdCardOperation.isOnExtSdCard(folder, context)) return 1;
    }

    // file not on external sd card
    if (FileProperties.isWritable(new File(folder, FileUtils.DUMMY_FILE))) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Well, we wouldn't want to copy when the target is inside the source otherwise it'll end into a
   * loop
   *
   * @return true when copy loop is possible
   */
  public static boolean isCopyLoopPossible(HybridFileParcelable sourceFile, HybridFile targetFile) {
    return targetFile.getPath().contains(sourceFile.getPath());
  }

  /**
   * Validates file name special reserved characters shall not be allowed in the file names on FAT
   * filesystems
   *
   * @param fileName the filename, not the full path!
   * @return boolean if the file name is valid or invalid
   */
  public static boolean isFileNameValid(String fileName) {

    // Trim the trailing slash if there is one.
    if (fileName.endsWith("/")) fileName = fileName.substring(0, fileName.lastIndexOf('/') - 1);
    // Trim the leading slashes if there is any.
    if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf('/') + 1);

    return !TextUtils.isEmpty(fileName)
        && !(fileName.contains(ASTERISK)
            || fileName.contains(BACKWARD_SLASH)
            || fileName.contains(COLON)
            || fileName.contains(FOREWARD_SLASH)
            || fileName.contains(GREATER_THAN)
            || fileName.contains(LESS_THAN)
            || fileName.contains(QUESTION_MARK)
            || fileName.contains(QUOTE));
  }

  private static boolean isFileSystemFAT(String mountPoint) {
    String[] args =
        new String[] {
          "/bin/bash",
          "-c",
          "df -DO_NOT_REPLACE | awk '{print $1,$2,$NF}' | grep \"^" + mountPoint + "\""
        };
    try {
      Process proc = new ProcessBuilder(args).start();
      OutputStream outputStream = proc.getOutputStream();
      String buffer = null;
      outputStream.write(buffer.getBytes());
      return buffer != null && buffer.contains(FAT);
    } catch (IOException e) {
      e.printStackTrace();
      // process interrupted, returning true, as a word of cation
      return true;
    }
  }
}
