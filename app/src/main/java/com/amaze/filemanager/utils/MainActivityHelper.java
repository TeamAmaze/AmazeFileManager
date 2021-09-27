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

package com.amaze.filemanager.utils;

import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.CAN_CREATE_FILES;
import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.DOESNT_EXIST;
import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.WRITABLE_OR_ON_SDCARD;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.COMPRESS;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.DELETE;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.EXTRACT;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.NEW_FILE;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.NEW_FOLDER;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.RENAME;

import java.io.File;
import java.util.ArrayList;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.ZipService;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.explorer.EncryptedEntry;
import com.amaze.filemanager.file_operations.filesystem.FolderState;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.ExternalSdCardOperation;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.SearchWorkerFragment;
import com.amaze.filemanager.ui.fragments.TabFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.views.WarnableTextInputValidator;
import com.leinardi.android.speeddial.SpeedDialView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

/** Created by root on 11/22/15, modified by Emmanuel Messulam<emmanuelbendavid@gmail.com> */
public class MainActivityHelper {

  private MainActivity mainActivity;
  private DataUtils dataUtils = DataUtils.getInstance();
  private int accentColor;
  private SpeedDialView.OnActionSelectedListener fabActionListener;

  /*
   * A static string which saves the last searched query. Used to retain search task after
   * user presses back button from pressing on any list item of search results
   */
  public static String SEARCH_TEXT;

  public MainActivityHelper(MainActivity mainActivity) {
    this.mainActivity = mainActivity;
    accentColor = mainActivity.getAccent();
  }

  public void showFailedOperationDialog(
      ArrayList<HybridFileParcelable> failedOps, Context context) {
    MaterialDialog.Builder mat = new MaterialDialog.Builder(context);
    mat.title(context.getString(R.string.operation_unsuccesful));
    mat.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
    mat.positiveColor(accentColor);
    mat.positiveText(R.string.cancel);
    String content = context.getString(R.string.operation_fail_following);
    int k = 1;
    for (HybridFileParcelable s : failedOps) {
      content = content + "\n" + (k) + ". " + s.getName(context);
      k++;
    }
    mat.content(content);
    mat.build().show();
  }

  public final BroadcastReceiver mNotificationReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (intent != null) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
              Toast.makeText(mainActivity, "Media Mounted", Toast.LENGTH_SHORT).show();
              String a = intent.getData().getPath();
              if (a != null
                  && a.trim().length() != 0
                  && new File(a).exists()
                  && new File(a).canExecute()) {
                dataUtils.getStorages().add(a);
                mainActivity.getDrawer().refreshDrawer();
              } else {
                mainActivity.getDrawer().refreshDrawer();
              }
            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {

              mainActivity.getDrawer().refreshDrawer();
            }
          }
        }
      };

  /**
   * Prompt a dialog to user to input directory name
   *
   * @param path current path at which directory to create
   * @param ma {@link MainFragment} current fragment
   */
  public void mkdir(final OpenMode openMode, final String path, final MainFragment ma) {
    mk(
        R.string.newfolder,
        "",
        (dialog, which) -> {
          EditText textfield = dialog.getCustomView().findViewById(R.id.singleedittext_input);
          mkDir(
              new HybridFile(openMode, path),
              new HybridFile(openMode, path, textfield.getText().toString().trim(), true),
              ma);
          dialog.dismiss();
        },
        (text) -> {
          boolean isValidFilename = FileProperties.isValidFilename(text);

          if (!isValidFilename || text.startsWith(" ")) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.invalid_name);
          } else if (text.length() < 1) {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
          }

          return new WarnableTextInputValidator.ReturnState();
        });
  }

  /**
   * Prompt a dialog to user to input file name
   *
   * @param path current path at which file to create
   * @param ma {@link MainFragment} current fragment
   */
  public void mkfile(final OpenMode openMode, final String path, final MainFragment ma) {
    mk(
        R.string.newfile,
        AppConstants.NEW_FILE_DELIMITER.concat(AppConstants.NEW_FILE_EXTENSION_TXT),
        (dialog, which) -> {
          EditText textfield = dialog.getCustomView().findViewById(R.id.singleedittext_input);
          mkFile(
              new HybridFile(openMode, path),
              new HybridFile(openMode, path, textfield.getText().toString().trim(), false),
              ma);
          dialog.dismiss();
        },
        (text) -> {
          boolean isValidFilename = FileProperties.isValidFilename(text);

          // The redundant equalsIgnoreCase() is needed since ".txt" itself does not end with .txt
          // (i.e. recommended as ".txt.txt"
          if (text.length() > 0) {
            if (!isValidFilename || text.startsWith(" ")) {
              return new WarnableTextInputValidator.ReturnState(
                  WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.invalid_name);
            } else {
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
              if (text.startsWith(".")
                  && !prefs.getBoolean(PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES, false)) {
                return new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_WARNING,
                    R.string.create_hidden_file_warn);
              } else if (!text.toLowerCase()
                  .endsWith(
                      AppConstants.NEW_FILE_DELIMITER.concat(
                          AppConstants.NEW_FILE_EXTENSION_TXT))) {
                return new WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_WARNING,
                    R.string.create_file_suggest_txt_extension);
              }
            }
          } else {
            return new WarnableTextInputValidator.ReturnState(
                WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty);
          }
          return new WarnableTextInputValidator.ReturnState();
        });
  }

  private void mk(
      @StringRes int newText,
      String prefill,
      final MaterialDialog.SingleButtonCallback onPositiveAction,
      final WarnableTextInputValidator.OnTextValidate validator) {
    MaterialDialog dialog =
        GeneralDialogCreation.showNameDialog(
            mainActivity,
            mainActivity.getResources().getString(R.string.entername),
            prefill,
            mainActivity.getResources().getString(newText),
            mainActivity.getResources().getString(R.string.create),
            mainActivity.getResources().getString(R.string.cancel),
            null,
            onPositiveAction,
            validator);
    dialog.show();

    // place cursor at the beginning
    EditText textfield = dialog.getCustomView().findViewById(R.id.singleedittext_input);
    textfield.post(
        () -> {
          textfield.setSelection(0);
        });
  }

  public String getIntegralNames(String path) {
    String newPath = "";
    switch (Integer.parseInt(path)) {
      case 0:
        newPath = mainActivity.getString(R.string.images);
        break;
      case 1:
        newPath = mainActivity.getString(R.string.videos);
        break;
      case 2:
        newPath = mainActivity.getString(R.string.audio);
        break;
      case 3:
        newPath = mainActivity.getString(R.string.documents);
        break;
      case 4:
        newPath = mainActivity.getString(R.string.apks);
        break;
      case 5:
        newPath = mainActivity.getString(R.string.quick);
        break;
      case 6:
        newPath = mainActivity.getString(R.string.recent);
        break;
    }
    return newPath;
  }

  public void guideDialogForLEXA(String path) {
    guideDialogForLEXA(path, 3);
  }

  public void guideDialogForLEXA(String path, int requestCode) {
    final MaterialDialog.Builder x = new MaterialDialog.Builder(mainActivity);
    x.theme(mainActivity.getAppTheme().getMaterialDialogTheme());
    x.title(R.string.needs_access);
    LayoutInflater layoutInflater =
        (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View view = layoutInflater.inflate(R.layout.lexadrawer, null);
    x.customView(view, true);
    // textView
    TextView textView = view.findViewById(R.id.description);
    textView.setText(
        mainActivity.getString(R.string.needs_access_summary)
            + path
            + mainActivity.getString(R.string.needs_access_summary1));
    ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.sd_operate_step);
    x.positiveText(R.string.open)
        .negativeText(R.string.cancel)
        .positiveColor(accentColor)
        .negativeColor(accentColor)
        .onPositive((dialog, which) -> triggerStorageAccessFramework(requestCode))
        .onNegative(
            (dialog, which) ->
                Toast.makeText(mainActivity, R.string.error, Toast.LENGTH_SHORT).show());
    final MaterialDialog y = x.build();
    y.show();
  }

  private void triggerStorageAccessFramework(int requestCode) {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    mainActivity.startActivityForResult(intent, requestCode);
  }

  public void rename(
      OpenMode mode,
      final String oldPath,
      final String newPath,
      final String newName,
      final boolean isDirectory,
      final Activity context,
      boolean rootmode) {
    final Toast toast =
        Toast.makeText(context, context.getString(R.string.renaming), Toast.LENGTH_SHORT);
    toast.show();
    HybridFile oldFile = new HybridFile(mode, oldPath);
    HybridFile newFile;
    if (Utils.isNullOrEmpty(newName)) {
      newFile = new HybridFile(mode, newPath);
    } else {
      newFile = new HybridFile(mode, newPath, newName, isDirectory);
    }
    Operations.rename(
        oldFile,
        newFile,
        rootmode,
        context,
        new Operations.ErrorCallBack() {
          @Override
          public void exists(HybridFile file) {
            context.runOnUiThread(
                () -> {
                  if (toast != null) toast.cancel();
                  Toast.makeText(
                          mainActivity, context.getString(R.string.fileexist), Toast.LENGTH_SHORT)
                      .show();
                });
          }

          @Override
          public void launchSAF(HybridFile file) {}

          @Override
          public void launchSAF(final HybridFile file, final HybridFile file1) {
            context.runOnUiThread(
                () -> {
                  if (toast != null) toast.cancel();
                  mainActivity.oppathe = file.getPath();
                  mainActivity.oppathe1 = file1.getPath();
                  mainActivity.operation = RENAME;
                  guideDialogForLEXA(mainActivity.oppathe1);
                });
          }

          @Override
          public void done(final HybridFile hFile, final boolean b) {
            context.runOnUiThread(
                () -> {
                  /*
                   * DocumentFile.renameTo() may return false even when rename is successful. Hence we need an extra check
                   * instead of merely looking at the return value
                   */
                  if (b || newFile.exists(context)) {
                    Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);

                    intent.putExtra(
                        MainActivity.KEY_INTENT_LOAD_LIST_FILE, hFile.getParent(context));
                    mainActivity.sendBroadcast(intent);

                    // update the database entry to reflect rename for encrypted file
                    if (oldPath.endsWith(CryptUtil.CRYPT_EXTENSION)) {
                      try {
                        CryptHandler cryptHandler = CryptHandler.getInstance();
                        EncryptedEntry oldEntry = cryptHandler.findEntry(oldPath);
                        EncryptedEntry newEntry = new EncryptedEntry();
                        newEntry.setId(oldEntry.getId());
                        newEntry.setPassword(oldEntry.getPassword());
                        newEntry.setPath(newPath);
                        cryptHandler.updateEntry(oldEntry, newEntry);
                      } catch (Exception e) {
                        e.printStackTrace();
                        // couldn't change the entry, leave it alone
                      }
                    }
                  } else
                    Toast.makeText(
                            context,
                            context.getString(R.string.operation_unsuccesful),
                            Toast.LENGTH_SHORT)
                        .show();
                });
          }

          @Override
          public void invalidName(final HybridFile file) {
            context.runOnUiThread(
                () -> {
                  if (toast != null) toast.cancel();
                  Toast.makeText(
                          context,
                          context.getString(R.string.invalid_name) + ": " + file.getName(context),
                          Toast.LENGTH_LONG)
                      .show();
                });
          }
        });
  }

  public @FolderState int checkFolder(final File folder, Context context) {
    return checkFolder(folder.getAbsolutePath(), OpenMode.FILE, context);
  }

  public @FolderState int checkFolder(final String path, OpenMode openMode, Context context) {
    if (OpenMode.SMB.equals(openMode)) {
      return SmbUtil.checkFolder(path);
    } else if (OpenMode.SFTP.equals(openMode)) {
      return SshClientUtils.checkFolder(path);
    } else if (OpenMode.DOCUMENT_FILE.equals(openMode)) {
      DocumentFile d =
          DocumentFile.fromTreeUri(AppConfig.getInstance(), SafRootHolder.getUriRoot());
      if (d == null) return DOESNT_EXIST;
      else {
        return WRITABLE_OR_ON_SDCARD;
      }
    } else {
      File folder = new File(path);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (ExternalSdCardOperation.isOnExtSdCard(folder, context)) {
          if (!folder.exists() || !folder.isDirectory()) {
            return DOESNT_EXIST;
          }

          // On Android 5, trigger storage access framework.
          if (!FileProperties.isWritableNormalOrSaf(folder, context)) {
            guideDialogForLEXA(folder.getPath());
            return CAN_CREATE_FILES;
          }

          return WRITABLE_OR_ON_SDCARD;
        } else if (FileProperties.isWritable(new File(folder, FileUtils.DUMMY_FILE))) {
          return WRITABLE_OR_ON_SDCARD;
        } else return DOESNT_EXIST;
      } else if (Build.VERSION.SDK_INT == 19) {
        if (ExternalSdCardOperation.isOnExtSdCard(folder, context)) {
          // Assume that Kitkat workaround works
          return WRITABLE_OR_ON_SDCARD;
        } else if (FileProperties.isWritable(new File(folder, FileUtils.DUMMY_FILE))) {
          return WRITABLE_OR_ON_SDCARD;
        } else return DOESNT_EXIST;
      } else if (FileProperties.isWritable(new File(folder, FileUtils.DUMMY_FILE))) {
        return WRITABLE_OR_ON_SDCARD;
      } else {
        return DOESNT_EXIST;
      }
    }
  }

  /**
   * Helper method to start Compress service
   *
   * @param file the new compressed file
   * @param baseFiles list of {@link HybridFileParcelable} to be compressed
   */
  public void compressFiles(File file, ArrayList<HybridFileParcelable> baseFiles) {
    int mode = checkFolder(file.getParentFile(), mainActivity);
    if (mode == 2) {
      mainActivity.oppathe = (file.getPath());
      mainActivity.operation = COMPRESS;
      mainActivity.oparrayList = baseFiles;
    } else if (mode == 1) {
      Intent intent2 = new Intent(mainActivity, ZipService.class);
      intent2.putExtra(ZipService.KEY_COMPRESS_PATH, file.getPath());
      intent2.putExtra(ZipService.KEY_COMPRESS_FILES, baseFiles);
      ServiceWatcherUtil.runService(mainActivity, intent2);
    } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
  }

  public void mkFile(final HybridFile parentFile, final HybridFile path, final MainFragment ma) {
    final Toast toast =
        Toast.makeText(ma.getActivity(), ma.getString(R.string.creatingfile), Toast.LENGTH_SHORT);
    toast.show();
    Operations.mkfile(
        parentFile,
        path,
        ma.getActivity(),
        mainActivity.isRootExplorer(),
        new Operations.ErrorCallBack() {
          @Override
          public void exists(final HybridFile file) {
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (toast != null) toast.cancel();
                      Toast.makeText(
                              mainActivity,
                              mainActivity.getString(R.string.fileexist),
                              Toast.LENGTH_SHORT)
                          .show();
                      if (ma != null && ma.getActivity() != null) {
                        // retry with dialog prompted again
                        mkfile(
                            file.getMode(),
                            file.getParent(mainActivity.getApplicationContext()),
                            ma);
                      }
                    });
          }

          @Override
          public void launchSAF(HybridFile file) {

            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (toast != null) toast.cancel();
                      mainActivity.oppathe = path.getPath();
                      mainActivity.operation = NEW_FILE;
                      guideDialogForLEXA(mainActivity.oppathe);
                    });
          }

          @Override
          public void launchSAF(HybridFile file, HybridFile file1) {}

          @Override
          public void done(HybridFile hFile, final boolean b) {
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (b) {
                        ma.updateList();
                      } else {
                        Toast.makeText(
                                ma.getActivity(),
                                ma.getString(R.string.operation_unsuccesful),
                                Toast.LENGTH_SHORT)
                            .show();
                      }
                    });
          }

          @Override
          public void invalidName(final HybridFile file) {
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (toast != null) toast.cancel();
                      Toast.makeText(
                              ma.getActivity(),
                              ma.getString(R.string.invalid_name)
                                  + ": "
                                  + file.getName(ma.getMainActivity()),
                              Toast.LENGTH_LONG)
                          .show();
                    });
          }
        });
  }

  public void mkDir(final HybridFile parentPath, final HybridFile path, final MainFragment ma) {
    final Toast toast =
        Toast.makeText(ma.getActivity(), ma.getString(R.string.creatingfolder), Toast.LENGTH_SHORT);
    toast.show();
    Operations.mkdir(
        parentPath,
        path,
        ma.getActivity(),
        mainActivity.isRootExplorer(),
        new Operations.ErrorCallBack() {
          @Override
          public void exists(final HybridFile file) {
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (toast != null) toast.cancel();
                      Toast.makeText(
                              mainActivity,
                              mainActivity.getString(R.string.fileexist),
                              Toast.LENGTH_SHORT)
                          .show();
                      if (ma != null && ma.getActivity() != null) {
                        // retry with dialog prompted again
                        mkdir(
                            file.getMode(),
                            file.getParent(mainActivity.getApplicationContext()),
                            ma);
                      }
                    });
          }

          @Override
          public void launchSAF(HybridFile file) {
            if (toast != null) toast.cancel();
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      mainActivity.oppathe = path.getPath();
                      mainActivity.operation = NEW_FOLDER;
                      guideDialogForLEXA(mainActivity.oppathe);
                    });
          }

          @Override
          public void launchSAF(HybridFile file, HybridFile file1) {}

          @Override
          public void done(HybridFile hFile, final boolean b) {
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (b) {
                        ma.updateList();
                      } else {
                        Toast.makeText(
                                ma.getActivity(),
                                ma.getString(R.string.operation_unsuccesful),
                                Toast.LENGTH_SHORT)
                            .show();
                      }
                    });
          }

          @Override
          public void invalidName(final HybridFile file) {
            ma.getActivity()
                .runOnUiThread(
                    () -> {
                      if (toast != null) toast.cancel();
                      Toast.makeText(
                              ma.getActivity(),
                              ma.getString(R.string.invalid_name)
                                  + ": "
                                  + file.getName(ma.getMainActivity()),
                              Toast.LENGTH_LONG)
                          .show();
                    });
          }
        });
  }

  public void deleteFiles(ArrayList<HybridFileParcelable> files) {
    if (files == null || files.size() == 0) return;
    if (files.get(0).isSmb()) {
      new DeleteTask(mainActivity).execute((files));
      return;
    }
    @FolderState
    int mode =
        checkFolder(files.get(0).getParent(mainActivity), files.get(0).getMode(), mainActivity);
    if (mode == CAN_CREATE_FILES) {
      mainActivity.oparrayList = (files);
      mainActivity.operation = DELETE;
    } else if (mode == WRITABLE_OR_ON_SDCARD || mode == DOESNT_EXIST)
      new DeleteTask(mainActivity).execute((files));
    else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
  }

  public void extractFile(File file) {
    int mode = checkFolder(file.getParentFile(), mainActivity);
    if (mode == 2) {
      mainActivity.oppathe = (file.getPath());
      mainActivity.operation = EXTRACT;
    } else if (mode == 1) {
      Decompressor decompressor = CompressedHelper.getCompressorInstance(mainActivity, file);
      decompressor.decompress(file.getPath());
    } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
  }

  /** Retrieve a path with {@link OTGUtil#PREFIX_OTG} as prefix */
  public String parseOTGPath(String path) {
    if (path.contains(OTGUtil.PREFIX_OTG)) return path;
    else return OTGUtil.PREFIX_OTG + path.substring(path.indexOf(":") + 1);
  }

  public String parseCloudPath(OpenMode serviceType, String path) {
    switch (serviceType) {
      case DROPBOX:
        if (path.contains(CloudHandler.CLOUD_PREFIX_DROPBOX)) return path;
        else
          return CloudHandler.CLOUD_PREFIX_DROPBOX
              + path.substring(path.indexOf(":") + 1, path.length());
      case BOX:
        if (path.contains(CloudHandler.CLOUD_PREFIX_BOX)) return path;
        else
          return CloudHandler.CLOUD_PREFIX_BOX
              + path.substring(path.indexOf(":") + 1, path.length());
      case GDRIVE:
        if (path.contains(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) return path;
        else
          return CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE
              + path.substring(path.indexOf(":") + 1, path.length());
      case ONEDRIVE:
        if (path.contains(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) return path;
        else
          return CloudHandler.CLOUD_PREFIX_ONE_DRIVE
              + path.substring(path.indexOf(":") + 1, path.length());
      default:
        return path;
    }
  }

  /**
   * Creates a fragment which will handle the search AsyncTask {@link SearchWorkerFragment}
   *
   * @param query the text query entered the by user
   */
  public void search(SharedPreferences sharedPrefs, String query) {
    TabFragment tabFragment = mainActivity.getTabFragment();
    if (tabFragment == null) {
      Log.w(getClass().getSimpleName(), "Failed to search: tab fragment not available");
      return;
    }
    final MainFragment ma = (MainFragment) tabFragment.getCurrentTabFragment();
    if (ma == null || ma.getMainFragmentViewModel() == null) {
      Log.w(getClass().getSimpleName(), "Failed to search: main fragment not available");
      return;
    }
    final String fpath = ma.getCurrentPath();

    /*SearchTask task = new SearchTask(ma.searchHelper, ma, query);
    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fpath);*/
    // ma.searchTask = task;
    SEARCH_TEXT = query;
    FragmentManager fm = mainActivity.getSupportFragmentManager();
    SearchWorkerFragment fragment =
        (SearchWorkerFragment) fm.findFragmentByTag(MainActivity.TAG_ASYNC_HELPER);

    if (fragment != null) {
      if (fragment.searchAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
        fragment.searchAsyncTask.cancel(true);
      }
      fm.beginTransaction().remove(fragment).commit();
    }

    addSearchFragment(
        fm,
        new SearchWorkerFragment(),
        fpath,
        query,
        ma.getMainFragmentViewModel().getOpenMode(),
        mainActivity.isRootExplorer(),
        sharedPrefs.getBoolean(SearchWorkerFragment.KEY_REGEX, false),
        sharedPrefs.getBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, false));
  }

  /**
   * Adds a search fragment that can persist it's state on config change
   *
   * @param fragmentManager fragmentManager
   * @param fragment current fragment
   * @param path current path
   * @param input query typed by user
   * @param openMode defines the file type
   * @param rootMode is root enabled
   * @param regex is regular expression search enabled
   * @param matches is matches enabled for patter matching
   */
  public static void addSearchFragment(
      FragmentManager fragmentManager,
      Fragment fragment,
      String path,
      String input,
      OpenMode openMode,
      boolean rootMode,
      boolean regex,
      boolean matches) {
    Bundle args = new Bundle();
    args.putString(SearchWorkerFragment.KEY_INPUT, input);
    args.putString(SearchWorkerFragment.KEY_PATH, path);
    args.putInt(SearchWorkerFragment.KEY_OPEN_MODE, openMode.ordinal());
    args.putBoolean(SearchWorkerFragment.KEY_ROOT_MODE, rootMode);
    args.putBoolean(SearchWorkerFragment.KEY_REGEX, regex);
    args.putBoolean(SearchWorkerFragment.KEY_REGEX_MATCHES, matches);

    fragment.setArguments(args);
    fragmentManager.beginTransaction().add(fragment, MainActivity.TAG_ASYNC_HELPER).commit();
  }
}
