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

package com.amaze.filemanager.ui.activities;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.KITKAT_WATCH;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.WRITABLE_OR_ON_SDCARD;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.COMPRESS;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.COPY;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.DELETE;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.EXTRACT;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.MOVE;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.NEW_FILE;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.NEW_FOLDER;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.RENAME;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.SAVE_FILE;
import static com.amaze.filemanager.file_operations.filesystem.OperationTypeKt.UNDEFINED;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_ADDRESS;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_DEFAULT_PATH;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_EDIT;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_HAS_PASSWORD;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_KEYPAIR_NAME;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_NAME;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_PASSWORD;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_PORT;
import static com.amaze.filemanager.ui.dialogs.SftpConnectDialog.ARG_USERNAME;
import static com.amaze.filemanager.ui.fragments.FtpServerFragment.REQUEST_CODE_SAF_FTP;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_BOOKMARKS_ADDED;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_VIEW;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.amaze.filemanager.LogHelper;
import com.amaze.filemanager.R;
import com.amaze.filemanager.TagsHelper;
import com.amaze.filemanager.adapters.data.StorageDirectoryParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.asynchronous.SaveOnDataUtilsChange;
import com.amaze.filemanager.asynchronous.asynctasks.CloudLoaderAsyncTask;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.asynctasks.MoveFiles;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.database.CloudContract;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.SortHandler;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.OperationData;
import com.amaze.filemanager.database.models.explorer.CloudEntry;
import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.file_operations.filesystem.StorageNaming;
import com.amaze.filemanager.file_operations.filesystem.usb.SingletonUsbOtg;
import com.amaze.filemanager.file_operations.filesystem.usb.UsbOtgRepresentation;
import com.amaze.filemanager.filesystem.ExternalSdCardOperation;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.MakeFileOperation;
import com.amaze.filemanager.filesystem.PasteHelper;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool;
import com.amaze.filemanager.ui.activities.superclasses.PermissionsActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.dialogs.RenameBookmark;
import com.amaze.filemanager.ui.dialogs.RenameBookmark.BookmarkCallback;
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog;
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog;
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog.SmbConnectionListener;
import com.amaze.filemanager.ui.drag.TabFragmentBottomDragListener;
import com.amaze.filemanager.ui.fragments.AppsListFragment;
import com.amaze.filemanager.ui.fragments.CloudSheetFragment;
import com.amaze.filemanager.ui.fragments.CloudSheetFragment.CloudConnectionCallbacks;
import com.amaze.filemanager.ui.fragments.CompressedExplorerFragment;
import com.amaze.filemanager.ui.fragments.FtpServerFragment;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.ProcessViewerFragment;
import com.amaze.filemanager.ui.fragments.SearchWorkerFragment;
import com.amaze.filemanager.ui.fragments.TabFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.strings.StorageNamingHelper;
import com.amaze.filemanager.ui.views.CustomZoomFocusChange;
import com.amaze.filemanager.ui.views.appbar.AppBar;
import com.amaze.filemanager.ui.views.drawer.Drawer;
import com.amaze.filemanager.utils.AppConstants;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Utils;
import com.cloudrail.si.CloudRail;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.FabWithLabelView;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialOverlayLayout;
import com.leinardi.android.speeddial.SpeedDialView;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.service.quicksettings.TileService;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.arch.core.util.Function;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import eu.chainfire.libsuperuser.Shell;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends PermissionsActivity
    implements SmbConnectionListener,
        BookmarkCallback,
        SearchWorkerFragment.HelperCallbacks,
        CloudConnectionCallbacks,
        LoaderManager.LoaderCallbacks<Cursor>,
        FolderChooserDialog.FolderCallback {

  private static final String TAG = TagsHelper.getTag(MainActivity.class);

  public static final Pattern DIR_SEPARATOR = Pattern.compile("/");
  public static final String TAG_ASYNC_HELPER = "async_helper";

  private DataUtils dataUtils;

  public String path = "";
  public boolean mReturnIntent = false;
  public boolean openzip = false;
  public boolean mRingtonePickerIntent = false;
  public int skinStatusBar;

  private SpeedDialView floatingActionButton;

  public MainActivityHelper mainActivityHelper;

  public int operation = -1;
  public ArrayList<HybridFileParcelable> oparrayList;
  public ArrayList<ArrayList<HybridFileParcelable>> oparrayListList;

  // oppathe - the path at which certain operation needs to be performed
  // oppathe1 - the new path which user wants to create/modify
  // oppathList - the paths at which certain operation needs to be performed (pairs with
  // oparrayList)
  public String oppathe, oppathe1;
  public ArrayList<String> oppatheList;

  // This holds the Uris to be written at initFabToSave()
  private ArrayList<Uri> urisToBeSaved;

  public static final String PASTEHELPER_BUNDLE = "pasteHelper";

  private static final String KEY_DRAWER_SELECTED = "selectitem";
  private static final String KEY_OPERATION_PATH = "oppathe";
  private static final String KEY_OPERATED_ON_PATH = "oppathe1";
  private static final String KEY_OPERATIONS_PATH_LIST = "oparraylist";
  private static final String KEY_OPERATION = "operation";

  private AppBar appbar;
  private Drawer drawer;
  // private HistoryManager history, grid;
  private MainActivity mainActivity = this;
  private String zippath;
  private boolean openProcesses = false;
  private MaterialDialog materialDialog;
  private boolean backPressedToExitOnce = false;
  private WeakReference<Toast> toast = new WeakReference<>(null);
  private Intent intent;
  private View indicator_layout;

  private AppBarLayout appBarLayout;

  private SpeedDialOverlayLayout fabBgView;
  private UtilsHandler utilsHandler;
  private CloudHandler cloudHandler;
  private CloudLoaderAsyncTask cloudLoaderAsyncTask;
  /**
   * This is for a hack.
   *
   * @see MainActivity#onLoadFinished(Loader, Cursor)
   */
  private Cursor cloudCursorData = null;

  public static final int REQUEST_CODE_SAF = 223;

  public static final String KEY_INTENT_PROCESS_VIEWER = "openprocesses";
  public static final String TAG_INTENT_FILTER_FAILED_OPS = "failedOps";
  public static final String TAG_INTENT_FILTER_GENERAL = "general_communications";
  public static final String ARGS_KEY_LOADER = "loader_cloud_args_service";

  /**
   * Broadcast which will be fired after every file operation, will denote list loading Registered
   * by {@link MainFragment}
   */
  public static final String KEY_INTENT_LOAD_LIST = "loadlist";

  /**
   * Extras carried by the list loading intent Contains path of parent directory in which operation
   * took place, so that we can run media scanner on it
   */
  public static final String KEY_INTENT_LOAD_LIST_FILE = "loadlist_file";

  /**
   * Mime type in intent that apps need to pass when trying to open file manager from a specific
   * directory Should be clubbed with {@link Intent#ACTION_VIEW} and send in path to open in intent
   * data field
   */
  public static final String ARGS_INTENT_ACTION_VIEW_MIME_FOLDER = "resource/folder";

  public static final String CLOUD_AUTHENTICATOR_GDRIVE = "android.intent.category.BROWSABLE";
  public static final String CLOUD_AUTHENTICATOR_REDIRECT_URI = "com.amaze.filemanager:/auth";

  // the current visible tab, either 0 or 1
  public static int currentTab;

  public static Shell.Interactive shellInteractive;
  public static Handler handler;

  private static HandlerThread handlerThread;

  public static final int REQUEST_CODE_CLOUD_LIST_KEYS = 5463;
  public static final int REQUEST_CODE_CLOUD_LIST_KEY = 5472;

  private PasteHelper pasteHelper;

  private static final String DEFAULT_FALLBACK_STORAGE_PATH = "/storage/sdcard0";
  private static final String INTERNAL_SHARED_STORAGE = "Internal shared storage";
  private static final String INTENT_ACTION_OPEN_QUICK_ACCESS =
      "com.amaze.filemanager.openQuickAccess";
  private static final String INTENT_ACTION_OPEN_RECENT = "com.amaze.filemanager.openRecent";
  private static final String INTENT_ACTION_OPEN_FTP_SERVER = "com.amaze.filemanager.openFTPServer";
  private static final String INTENT_ACTION_OPEN_APP_MANAGER =
      "com.amaze.filemanager.openAppManager";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_toolbar);

    intent = getIntent();

    dataUtils = DataUtils.getInstance();

    initialisePreferences();
    initializeInteractiveShell();

    dataUtils.registerOnDataChangedListener(new SaveOnDataUtilsChange(drawer));

    AppConfig.getInstance().setMainActivityContext(this);

    initialiseViews();
    utilsHandler = AppConfig.getInstance().getUtilsHandler();
    cloudHandler = new CloudHandler(this, AppConfig.getInstance().getExplorerDatabase());

    initialiseFab(); // TODO: 7/12/2017 not init when actionIntent != null
    mainActivityHelper = new MainActivityHelper(this);

    if (CloudSheetFragment.isCloudProviderAvailable(this)) {

      LoaderManager.getInstance(this).initLoader(REQUEST_CODE_CLOUD_LIST_KEYS, null, this);
    }

    path = intent.getStringExtra("path");
    openProcesses = intent.getBooleanExtra(KEY_INTENT_PROCESS_VIEWER, false);

    if (intent.getStringArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS) != null) {
      ArrayList<HybridFileParcelable> failedOps =
          intent.getParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS);
      if (failedOps != null) {
        mainActivityHelper.showFailedOperationDialog(failedOps, this);
      }
    }

    checkForExternalIntent(intent);

    drawer.setDrawerIndicatorEnabled();

    if (!getBoolean(PREFERENCE_BOOKMARKS_ADDED)) {
      utilsHandler.addCommonBookmarks();
      getPrefs().edit().putBoolean(PREFERENCE_BOOKMARKS_ADDED, true).commit();
    }

    checkForExternalPermission();

    Completable.fromRunnable(
            () -> {
              dataUtils.setHiddenFiles(utilsHandler.getHiddenFilesConcurrentRadixTree());
              dataUtils.setHistory(utilsHandler.getHistoryLinkedList());
              dataUtils.setGridfiles(utilsHandler.getGridViewList());
              dataUtils.setListfiles(utilsHandler.getListViewList());
              dataUtils.setBooks(utilsHandler.getBookmarksList());
              ArrayList<String[]> servers = new ArrayList<>();
              servers.addAll(utilsHandler.getSmbList());
              servers.addAll(utilsHandler.getSftpList());
              dataUtils.setServers(servers);
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new CompletableObserver() {
              @Override
              public void onSubscribe(Disposable d) {}

              @Override
              public void onComplete() {
                drawer.refreshDrawer();
                invalidateFragmentAndBundle(savedInstanceState);
              }

              @Override
              public void onError(Throwable e) {
                e.printStackTrace();
              }
            });
    initStatusBarResources(findViewById(R.id.drawer_layout));
  }

  private void invalidateFragmentAndBundle(Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      if (openProcesses) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(
            R.id.content_frame, new ProcessViewerFragment(), KEY_INTENT_PROCESS_VIEWER);
        // transaction.addToBackStack(null);
        openProcesses = false;
        // title.setText(utils.getString(con, R.string.process_viewer));
        // Commit the transaction
        transaction.commit();
        supportInvalidateOptionsMenu();
      } else if (intent.getAction() != null
          && (intent.getAction().equals(TileService.ACTION_QS_TILE_PREFERENCES)
              || INTENT_ACTION_OPEN_FTP_SERVER.equals(intent.getAction()))) {
        // tile preferences, open ftp fragment

        FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
        transaction2.replace(R.id.content_frame, new FtpServerFragment());
        appBarLayout
            .animate()
            .translationY(0)
            .setInterpolator(new DecelerateInterpolator(2))
            .start();

        drawer.deselectEverything();
        transaction2.commit();
      } else if (intent.getAction() != null
          && INTENT_ACTION_OPEN_APP_MANAGER.equals(intent.getAction())) {
        FragmentTransaction transaction3 = getSupportFragmentManager().beginTransaction();
        transaction3.replace(R.id.content_frame, new AppsListFragment());
        appBarLayout
            .animate()
            .translationY(0)
            .setInterpolator(new DecelerateInterpolator(2))
            .start();

        drawer.deselectEverything();
        transaction3.commit();
      } else {
        if (path != null && path.length() > 0) {
          HybridFile file = new HybridFile(OpenMode.UNKNOWN, path);
          file.generateMode(MainActivity.this);
          if (file.isDirectory(MainActivity.this)) goToMain(path);
          else {
            goToMain(null);
            FileUtils.openFile(new File(path), MainActivity.this, getPrefs());
          }
        } else {
          goToMain(null);
        }
      }
    } else {
      pasteHelper = savedInstanceState.getParcelable(PASTEHELPER_BUNDLE);
      oppathe = savedInstanceState.getString(KEY_OPERATION_PATH);
      oppathe1 = savedInstanceState.getString(KEY_OPERATED_ON_PATH);
      oparrayList = savedInstanceState.getParcelableArrayList(KEY_OPERATIONS_PATH_LIST);
      operation = savedInstanceState.getInt(KEY_OPERATION);
      int selectedStorage = savedInstanceState.getInt(KEY_DRAWER_SELECTED, 0);
      getDrawer().selectCorrectDrawerItem(selectedStorage);
    }
  }

  private void checkForExternalPermission() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkStoragePermission()) {
      requestStoragePermission(
          () -> {
            drawer.refreshDrawer();
            TabFragment tabFragment = getTabFragment();
            boolean b = getBoolean(PREFERENCE_NEED_TO_SET_HOME);
            // reset home and current paths according to new storages
            if (b) {
              TabHandler tabHandler = TabHandler.getInstance();
              tabHandler
                  .clear()
                  .subscribe(
                      () -> {
                        if (tabFragment != null) {
                          tabFragment.refactorDrawerStorages(false);
                          Fragment main = tabFragment.getFragmentAtIndex(0);
                          if (main != null)
                            ((MainFragment) main).updateTabWithDb(tabHandler.findTab(1));
                          Fragment main1 = tabFragment.getFragmentAtIndex(1);
                          if (main1 != null)
                            ((MainFragment) main1).updateTabWithDb(tabHandler.findTab(2));
                        }
                        getPrefs().edit().putBoolean(PREFERENCE_NEED_TO_SET_HOME, false).commit();
                      });
            } else {
              // just refresh list
              if (tabFragment != null) {
                Fragment main = tabFragment.getFragmentAtIndex(0);
                if (main != null) ((MainFragment) main).updateList();
                Fragment main1 = tabFragment.getFragmentAtIndex(1);
                if (main1 != null) ((MainFragment) main1).updateList();
              }
            }
          },
          true);
    }
  }

  /** Checks for the action to take when Amaze receives an intent from external source */
  private void checkForExternalIntent(Intent intent) {
    final String actionIntent = intent.getAction();
    if (actionIntent == null) {
      return;
    }

    final String type = intent.getType();

    if (actionIntent.equals(Intent.ACTION_GET_CONTENT)) {
      // file picker intent
      mReturnIntent = true;
      Toast.makeText(this, getString(R.string.pick_a_file), Toast.LENGTH_LONG).show();

      // disable screen rotation just for convenience purpose
      // TODO: Support screen rotation when picking file
      Utils.disableScreenRotation(this);
    } else if (actionIntent.equals(RingtoneManager.ACTION_RINGTONE_PICKER)) {
      // ringtone picker intent
      mReturnIntent = true;
      mRingtonePickerIntent = true;
      Toast.makeText(this, getString(R.string.pick_a_file), Toast.LENGTH_LONG).show();

      // disable screen rotation just for convenience purpose
      // TODO: Support screen rotation when picking file
      Utils.disableScreenRotation(this);
    } else if (actionIntent.equals(Intent.ACTION_VIEW)) {
      // zip viewer intent
      Uri uri = intent.getData();

      if (type != null && type.equals(ARGS_INTENT_ACTION_VIEW_MIME_FOLDER)) {
        // support for syncting or intents from external apps that
        // need to start file manager from a specific path

        if (uri != null) {

          path = Utils.sanitizeInput(uri.getPath());
        } else {
          // no data field, open home for the tab in later processing
          path = null;
        }
      } else {
        // we don't have folder resource mime type set, supposed to be zip/rar
        openzip = true;
        zippath = Utils.sanitizeInput(uri.toString());
        if (FileUtils.isCompressedFile(zippath)) {
          openCompressed(zippath);
        }
      }

    } else if (actionIntent.equals(Intent.ACTION_SEND)) {
      if ("text/plain".equals(type)) {
        initFabToSave(null);
      } else {
        // save a single file to filesystem
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(uri);
        initFabToSave(uris);
      }
      // disable screen rotation just for convenience purpose
      // TODO: Support screen rotation when saving a file
      Utils.disableScreenRotation(this);

    } else if (actionIntent.equals(Intent.ACTION_SEND_MULTIPLE) && type != null) {
      // save multiple files to filesystem

      ArrayList<Uri> arrayList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
      initFabToSave(arrayList);

      // disable screen rotation just for convenience purpose
      // TODO: Support screen rotation when saving a file
      Utils.disableScreenRotation(this);
    }
  }

  /** Initializes the floating action button to act as to save data from an external intent */
  private void initFabToSave(final ArrayList<Uri> uris) {
    Utils.showThemedSnackbar(
        this,
        getString(R.string.select_save_location),
        BaseTransientBottomBar.LENGTH_INDEFINITE,
        R.string.save,
        () -> saveExternalIntent(uris));
  }

  private void saveExternalIntent(final ArrayList<Uri> uris) {
    executeWithMainFragment(
        mainFragment -> {
          if (uris != null && uris.size() > 0) {
            if (SDK_INT >= LOLLIPOP) {
              File folder = new File(mainFragment.getCurrentPath());
              int result = mainActivityHelper.checkFolder(folder, MainActivity.this);
              if (result == WRITABLE_OR_ON_SDCARD) {
                FileUtil.writeUriToStorage(
                    MainActivity.this, uris, getContentResolver(), mainFragment.getCurrentPath());
                finish();
              } else {
                // Trigger SAF intent, keep uri until finish
                operation = SAVE_FILE;
                urisToBeSaved = uris;
                mainActivityHelper.checkFolder(folder, MainActivity.this);
              }
            } else {
              FileUtil.writeUriToStorage(
                  MainActivity.this, uris, getContentResolver(), mainFragment.getCurrentPath());
            }
          } else {
            saveExternalIntentExtras();
          }
          Toast.makeText(
                  MainActivity.this,
                  getResources().getString(R.string.saving)
                      + " to "
                      + mainFragment.getCurrentPath(),
                  Toast.LENGTH_LONG)
              .show();
          finish();
          return null;
        });
  }

  private void saveExternalIntentExtras() {
    executeWithMainFragment(
        mainFragment -> {
          Bundle extras = intent.getExtras();
          StringBuilder data = new StringBuilder();
          if (!Utils.isNullOrEmpty(extras.getString(Intent.EXTRA_SUBJECT))) {
            data.append(extras.getString(Intent.EXTRA_SUBJECT));
          }
          if (!Utils.isNullOrEmpty(extras.getString(Intent.EXTRA_TEXT))) {
            data.append(AppConstants.NEW_LINE).append(extras.getString(Intent.EXTRA_TEXT));
          }
          String fileName = Long.toString(System.currentTimeMillis());
          AppConfig.getInstance()
              .runInBackground(
                  () ->
                      MakeFileOperation.mktextfile(
                          data.toString(), mainFragment.getCurrentPath(), fileName));
          return null;
        });
  }

  public void clearFabActionItems() {
    floatingActionButton.removeActionItemById(R.id.menu_new_folder);
    floatingActionButton.removeActionItemById(R.id.menu_new_file);
    floatingActionButton.removeActionItemById(R.id.menu_new_cloud);
  }

  /**
   * Initializes an interactive shell, which will stay throughout the app lifecycle The shell is
   * associated with a handler thread which maintain the message queue from the callbacks of shell
   * as we certainly cannot allow the callbacks to run on same thread because of possible deadlock
   * situation and the asynchronous behaviour of LibSuperSU
   */
  private void initializeInteractiveShell() {
    // only one looper can be associated to a thread. So we are making sure not to create new
    // handler threads every time the code relaunch.
    if (isRootExplorer()) {
      handlerThread = new HandlerThread("handler");
      handlerThread.start();
      handler = new Handler(handlerThread.getLooper());
      shellInteractive = (new Shell.Builder()).useSU().setHandler(handler).open();
    }
  }

  /** @return paths to all available volumes in the system (include emulated) */
  public synchronized ArrayList<StorageDirectoryParcelable> getStorageDirectories() {
    ArrayList<StorageDirectoryParcelable> volumes;
    if (SDK_INT >= N) {
      volumes = getStorageDirectoriesNew();
    } else {
      volumes = getStorageDirectoriesLegacy();
    }
    if (isRootExplorer()) {
      volumes.add(
          new StorageDirectoryParcelable(
              "/",
              getResources().getString(R.string.root_directory),
              R.drawable.ic_drawer_root_white));
    }
    return volumes;
  }

  /**
   * @return All available storage volumes (including internal storage, SD-Cards and USB devices)
   */
  @TargetApi(N)
  public synchronized ArrayList<StorageDirectoryParcelable> getStorageDirectoriesNew() {
    // Final set of paths
    ArrayList<StorageDirectoryParcelable> volumes = new ArrayList<>();
    StorageManager sm = getSystemService(StorageManager.class);
    for (StorageVolume volume : sm.getStorageVolumes()) {
      if (!volume.getState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)
          && !volume.getState().equalsIgnoreCase(Environment.MEDIA_MOUNTED_READ_ONLY)) {
        continue;
      }
      File path = Utils.getVolumeDirectory(volume);
      String name = volume.getDescription(this);
      if (INTERNAL_SHARED_STORAGE.equalsIgnoreCase(name)) {
        name = getString(R.string.storage_internal);
      }
      int icon;
      if (!volume.isRemovable()) {
        icon = R.drawable.ic_phone_android_white_24dp;
      } else {
        // HACK: There is no reliable way to distinguish USB and SD external storage
        // However it is often enough to check for "USB" String
        if (name.toUpperCase().contains("USB") || path.getPath().toUpperCase().contains("USB")) {
          icon = R.drawable.ic_usb_white_24dp;
        } else {
          icon = R.drawable.ic_sd_storage_white_24dp;
        }
      }
      volumes.add(new StorageDirectoryParcelable(path.getPath(), name, icon));
    }
    return volumes;
  }

  /**
   * Returns all available SD-Cards in the system (include emulated)
   *
   * <p>Warning: Hack! Based on Android source code of version 4.3 (API 18) Because there was no
   * standard way to get it before android N
   *
   * @return All available SD-Cards in the system (include emulated)
   */
  public synchronized ArrayList<StorageDirectoryParcelable> getStorageDirectoriesLegacy() {
    List<String> rv = new ArrayList<>();

    // Primary physical SD-CARD (not emulated)
    final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
    // All Secondary SD-CARDs (all exclude primary) separated by ":"
    final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
    // Primary emulated SD-CARD
    final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
    if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
      // Device has physical external storage; use plain paths.
      if (TextUtils.isEmpty(rawExternalStorage)) {
        // EXTERNAL_STORAGE undefined; falling back to default.
        // Check for actual existence of the directory before adding to list
        if (new File(DEFAULT_FALLBACK_STORAGE_PATH).exists()) {
          rv.add(DEFAULT_FALLBACK_STORAGE_PATH);
        } else {
          // We know nothing else, use Environment's fallback
          rv.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        }
      } else {
        rv.add(rawExternalStorage);
      }
    } else {
      // Device has emulated storage; external storage paths should have
      // userId burned into them.
      final String rawUserId;
      if (SDK_INT < JELLY_BEAN_MR1) {
        rawUserId = "";
      } else {
        final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String[] folders = DIR_SEPARATOR.split(path);
        final String lastFolder = folders[folders.length - 1];
        boolean isDigit = false;
        try {
          Integer.valueOf(lastFolder);
          isDigit = true;
        } catch (NumberFormatException ignored) {
        }
        rawUserId = isDigit ? lastFolder : "";
      }
      // /storage/emulated/0[1,2,...]
      if (TextUtils.isEmpty(rawUserId)) {
        rv.add(rawEmulatedStorageTarget);
      } else {
        rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
      }
    }
    // Add all secondary storages
    if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
      // All Secondary SD-CARDs splited into array
      final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
      Collections.addAll(rv, rawSecondaryStorages);
    }
    if (SDK_INT >= M && checkStoragePermission()) rv.clear();
    if (SDK_INT >= KITKAT) {
      String strings[] = ExternalSdCardOperation.getExtSdCardPathsForActivity(this);
      for (String s : strings) {
        File f = new File(s);
        if (!rv.contains(s) && FileUtils.canListFiles(f)) rv.add(s);
      }
    }
    File usb = getUsbDrive();
    if (usb != null && !rv.contains(usb.getPath())) rv.add(usb.getPath());

    if (SDK_INT >= KITKAT) {
      if (SingletonUsbOtg.getInstance().isDeviceConnected()) {
        rv.add(OTGUtil.PREFIX_OTG + "/");
      }
    }

    // Assign a label and icon to each directory
    ArrayList<StorageDirectoryParcelable> volumes = new ArrayList<>();
    for (String file : rv) {
      File f = new File(file);
      @DrawableRes int icon;

      if ("/storage/emulated/legacy".equals(file)
          || "/storage/emulated/0".equals(file)
          || "/mnt/sdcard".equals(file)) {
        icon = R.drawable.ic_phone_android_white_24dp;
      } else if ("/storage/sdcard1".equals(file)) {
        icon = R.drawable.ic_sd_storage_white_24dp;
      } else if ("/".equals(file)) {
        icon = R.drawable.ic_drawer_root_white;
      } else {
        icon = R.drawable.ic_sd_storage_white_24dp;
      }

      @StorageNaming.DeviceDescription
      int deviceDescription = StorageNaming.getDeviceDescriptionLegacy(f);
      String name = StorageNamingHelper.getNameForDeviceDescription(this, f, deviceDescription);

      volumes.add(new StorageDirectoryParcelable(file, name, icon));
    }

    return volumes;
  }

  @Override
  public void onBackPressed() {
    if (!drawer.isLocked() && drawer.isOpen()) {
      drawer.close();
      return;
    }

    Fragment fragment = getFragmentAtFrame();
    if (getAppbar().getSearchView().isShown()) {
      // hide search view if visible, with an animation
      getAppbar().getSearchView().hideSearchView();
    } else if (fragment instanceof TabFragment) {
      if (floatingActionButton.isOpen()) {
        floatingActionButton.close(true);
      } else {
        executeWithMainFragment(
            mainFragment -> {
              mainFragment.goBack();
              return null;
            });
      }
    } else if (fragment instanceof CompressedExplorerFragment) {
      CompressedExplorerFragment compressedExplorerFragment =
          (CompressedExplorerFragment) getFragmentAtFrame();
      if (compressedExplorerFragment.mActionMode == null) {
        if (compressedExplorerFragment.canGoBack()) {
          compressedExplorerFragment.goBack();
        } else if (openzip) {
          openzip = false;
          finish();
        } else {
          FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
          fragmentTransaction.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
          fragmentTransaction.remove(compressedExplorerFragment);
          fragmentTransaction.commit();
          supportInvalidateOptionsMenu();
          floatingActionButton.show();
        }
      } else {
        compressedExplorerFragment.mActionMode.finish();
      }
    } else if (fragment instanceof FtpServerFragment) {
      // returning back from FTP server
      if (path != null && path.length() > 0) {
        HybridFile file = new HybridFile(OpenMode.UNKNOWN, path);
        file.generateMode(this);
        if (file.isDirectory(this)) goToMain(path);
        else {
          goToMain(null);
          FileUtils.openFile(new File(path), this, getPrefs());
        }
      } else {
        goToMain(null);
      }
    } else {
      goToMain(null);
    }
  }

  public void invalidatePasteSnackbar(boolean showSnackbar) {
    if (pasteHelper != null) {
      pasteHelper.invalidateSnackbar(this, showSnackbar);
    }
  }

  public void exit() {
    if (backPressedToExitOnce) {
      SshConnectionPool.INSTANCE.shutdown();
      finish();
      if (isRootExplorer()) {
        // TODO close all shells
      }
    } else {
      this.backPressedToExitOnce = true;
      final Toast toast = Toast.makeText(this, getString(R.string.press_again), Toast.LENGTH_SHORT);
      this.toast = new WeakReference<>(toast);
      toast.show();
      new Handler()
          .postDelayed(
              () -> {
                backPressedToExitOnce = false;
              },
              2000);
    }
  }

  public void goToMain(String path) {
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    // title.setText(R.string.app_name);
    TabFragment tabFragment = new TabFragment();
    if (intent != null && intent.getAction() != null) {
      if (INTENT_ACTION_OPEN_QUICK_ACCESS.equals(intent.getAction())) {
        path = "5";
      } else if (INTENT_ACTION_OPEN_RECENT.equals(intent.getAction())) {
        path = "6";
      }
    }
    if (path != null && path.length() > 0) {
      Bundle b = new Bundle();
      b.putString("path", path);
      tabFragment.setArguments(b);
    }
    transaction.replace(R.id.content_frame, tabFragment);
    // Commit the transaction
    transaction.addToBackStack("tabt" + 1);
    transaction.commitAllowingStateLoss();
    appbar.setTitle(null);
    floatingActionButton.show();
    if (openzip && zippath != null) {
      openCompressed(zippath);
      zippath = null;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater menuInflater = getMenuInflater();
    menuInflater.inflate(R.menu.activity_extra, menu);
    /*
    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    searchView.setIconifiedByDefault(false);

    MenuItem search = menu.findItem(R.id.search);
    MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            // Stretching the SearchView across width of the Toolbar
            toolbar.setContentInsetsRelative(0, 0);
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            // Restoring
            toolbar.setContentInsetsRelative(TOOLBAR_START_INSET, 0);
            return true;
        }
    });
    */
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem s = menu.findItem(R.id.view);
    MenuItem search = menu.findItem(R.id.search);
    Fragment fragment = getFragmentAtFrame();
    if (fragment instanceof TabFragment) {
      appbar.setTitle(R.string.appbar_name);
      if (getBoolean(PREFERENCE_VIEW)) {
        s.setTitle(getResources().getString(R.string.gridview));
      } else {
        s.setTitle(getResources().getString(R.string.listview));
      }
      try {
        executeWithMainFragment(
            mainFragment -> {
              if (mainFragment.getMainFragmentViewModel().isList()) {
                s.setTitle(R.string.gridview);
              } else {
                s.setTitle(R.string.listview);
              }
              appbar
                  .getBottomBar()
                  .updatePath(
                      mainFragment.getCurrentPath(),
                      mainFragment.getMainFragmentViewModel().getResults(),
                      MainActivityHelper.SEARCH_TEXT,
                      mainFragment.getMainFragmentViewModel().getOpenMode(),
                      mainFragment.getMainFragmentViewModel().getFolderCount(),
                      mainFragment.getMainFragmentViewModel().getFileCount(),
                      mainFragment);
              return null;
            });
      } catch (Exception e) {
        e.printStackTrace();
      }

      appbar.getBottomBar().setClickListener();

      search.setVisible(true);
      if (indicator_layout != null) indicator_layout.setVisibility(View.VISIBLE);
      menu.findItem(R.id.search).setVisible(true);
      menu.findItem(R.id.home).setVisible(true);
      menu.findItem(R.id.history).setVisible(true);
      menu.findItem(R.id.sethome).setVisible(true);
      menu.findItem(R.id.sort).setVisible(true);
      menu.findItem(R.id.hiddenitems).setVisible(true);
      menu.findItem(R.id.view).setVisible(true);
      menu.findItem(R.id.extract).setVisible(false);
      invalidatePasteSnackbar(true);
      findViewById(R.id.buttonbarframe).setVisibility(View.VISIBLE);
    } else if (fragment instanceof AppsListFragment
        || fragment instanceof ProcessViewerFragment
        || fragment instanceof FtpServerFragment) {
      appBarLayout.setExpanded(true);
      menu.findItem(R.id.sethome).setVisible(false);
      if (indicator_layout != null) indicator_layout.setVisibility(View.GONE);
      findViewById(R.id.buttonbarframe).setVisibility(View.GONE);
      menu.findItem(R.id.search).setVisible(false);
      menu.findItem(R.id.home).setVisible(false);
      menu.findItem(R.id.history).setVisible(false);
      menu.findItem(R.id.extract).setVisible(false);
      if (fragment instanceof ProcessViewerFragment) {
        menu.findItem(R.id.sort).setVisible(false);
      } else if (fragment instanceof FtpServerFragment) {
        menu.findItem(R.id.sort).setVisible(false);
      } else {
        menu.findItem(R.id.dsort).setVisible(false);
        menu.findItem(R.id.sortby).setVisible(false);
      }
      menu.findItem(R.id.hiddenitems).setVisible(false);
      menu.findItem(R.id.view).setVisible(false);
      invalidatePasteSnackbar(false);
    } else if (fragment instanceof CompressedExplorerFragment) {
      appbar.setTitle(R.string.appbar_name);
      menu.findItem(R.id.sethome).setVisible(false);
      if (indicator_layout != null) indicator_layout.setVisibility(View.GONE);
      getAppbar().getBottomBar().resetClickListener();
      menu.findItem(R.id.search).setVisible(false);
      menu.findItem(R.id.home).setVisible(false);
      menu.findItem(R.id.history).setVisible(false);
      menu.findItem(R.id.sort).setVisible(false);
      menu.findItem(R.id.hiddenitems).setVisible(false);
      menu.findItem(R.id.view).setVisible(false);
      menu.findItem(R.id.extract).setVisible(true);
      invalidatePasteSnackbar(false);
    }
    return super.onPrepareOptionsMenu(menu);
  }

  // called when the user exits the action mode
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // The action bar home/up action should open or close the drawer.
    // ActionBarDrawerToggle will take care of this.
    if (drawer.onOptionsItemSelected(item)) return true;
    // Same thing goes to other Fragments loaded.
    // If they have handled the options, we don't need to.
    if (getFragmentAtFrame().onOptionsItemSelected(item)) return true;

    // Handle action buttons
    executeWithMainFragment(
        mainFragment -> {
          switch (item.getItemId()) {
            case R.id.home:
              mainFragment.home();
              break;
            case R.id.history:
              GeneralDialogCreation.showHistoryDialog(
                  dataUtils, getPrefs(), mainFragment, getAppTheme());
              break;
            case R.id.sethome:
              if (mainFragment.getMainFragmentViewModel().getOpenMode() != OpenMode.FILE
                  && mainFragment.getMainFragmentViewModel().getOpenMode() != OpenMode.ROOT) {
                Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
                break;
              }
              final MaterialDialog dialog =
                  GeneralDialogCreation.showBasicDialog(
                      mainActivity,
                      R.string.question_set_path_as_home,
                      R.string.set_as_home,
                      R.string.yes,
                      R.string.no);
              dialog
                  .getActionButton(DialogAction.POSITIVE)
                  .setOnClickListener(
                      (v) -> {
                        mainFragment
                            .getMainFragmentViewModel()
                            .setHome(mainFragment.getCurrentPath());
                        updatePaths(mainFragment.getMainFragmentViewModel().getNo());
                        dialog.dismiss();
                      });
              dialog.show();
              break;
            case R.id.exit:
              finish();
              break;
            case R.id.sortby:
              GeneralDialogCreation.showSortDialog(mainFragment, getAppTheme(), getPrefs());
              break;
            case R.id.dsort:
              String[] sort = getResources().getStringArray(R.array.directorysortmode);
              MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);
              builder.theme(getAppTheme().getMaterialDialogTheme());
              builder.title(R.string.directorysort);
              int current =
                  Integer.parseInt(
                      getPrefs()
                          .getString(PreferencesConstants.PREFERENCE_DIRECTORY_SORT_MODE, "0"));

              builder
                  .items(sort)
                  .itemsCallbackSingleChoice(
                      current,
                      (dialog1, view, which, text) -> {
                        getPrefs()
                            .edit()
                            .putString(
                                PreferencesConstants.PREFERENCE_DIRECTORY_SORT_MODE, "" + which)
                            .commit();
                        mainFragment
                            .getMainFragmentViewModel()
                            .initSortModes(
                                SortHandler.getSortType(
                                    this, mainFragment.getMainFragmentViewModel().getCurrentPath()),
                                getPrefs());
                        mainFragment.updateList();
                        dialog1.dismiss();
                        return true;
                      });
              builder.build().show();
              break;
            case R.id.hiddenitems:
              GeneralDialogCreation.showHiddenDialog(
                  dataUtils, getPrefs(), mainFragment, getAppTheme());
              break;
            case R.id.view:
              int pathLayout =
                  dataUtils.getListOrGridForPath(mainFragment.getCurrentPath(), DataUtils.LIST);
              if (mainFragment.getMainFragmentViewModel().isList()) {
                if (pathLayout == DataUtils.LIST) {
                  AppConfig.getInstance()
                      .runInBackground(
                          () -> {
                            utilsHandler.removeFromDatabase(
                                new OperationData(
                                    UtilsHandler.Operation.LIST, mainFragment.getCurrentPath()));
                          });
                }
                utilsHandler.saveToDatabase(
                    new OperationData(UtilsHandler.Operation.GRID, mainFragment.getCurrentPath()));

                dataUtils.setPathAsGridOrList(mainFragment.getCurrentPath(), DataUtils.GRID);
              } else {
                if (pathLayout == DataUtils.GRID) {
                  AppConfig.getInstance()
                      .runInBackground(
                          () -> {
                            utilsHandler.removeFromDatabase(
                                new OperationData(
                                    UtilsHandler.Operation.GRID, mainFragment.getCurrentPath()));
                          });
                }

                utilsHandler.saveToDatabase(
                    new OperationData(UtilsHandler.Operation.LIST, mainFragment.getCurrentPath()));

                dataUtils.setPathAsGridOrList(mainFragment.getCurrentPath(), DataUtils.LIST);
              }
              mainFragment.switchView();
              break;
            case R.id.extract:
              Fragment fragment1 = getFragmentAtFrame();
              if (fragment1 instanceof CompressedExplorerFragment) {
                mainActivityHelper.extractFile(
                    ((CompressedExplorerFragment) fragment1).compressedFile);
              }
              break;
            case R.id.search:
              getAppbar().getSearchView().revealSearchView();
              break;
          }
          return null;
        },
        false);

    return super.onOptionsItemSelected(item);
  }

  /*@Override
  public void onRestoreInstanceState(Bundle savedInstanceState){
      COPY_PATH=savedInstanceState.getStringArrayList("COPY_PATH");
      MOVE_PATH=savedInstanceState.getStringArrayList("MOVE_PATH");
      oppathe = savedInstanceState.getString(KEY_OPERATION_PATH);
      oppathe1 = savedInstanceState.getString(KEY_OPERATED_ON_PATH);
      oparrayList = savedInstanceState.getStringArrayList(KEY_OPERATIONS_PATH_LIST);
      opnameList=savedInstanceState.getStringArrayList("opnameList");
      operation = savedInstanceState.getInt(KEY_OPERATION);
      selectedStorage = savedInstanceState.getInt(KEY_DRAWER_SELECTED, 0);
  }*/

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    drawer.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Pass any configuration change to the drawer toggls
    drawer.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_DRAWER_SELECTED, getDrawer().getDrawerSelectedItem());
    if (pasteHelper != null) {
      outState.putParcelable(PASTEHELPER_BUNDLE, pasteHelper);
    }

    if (oppathe != null) {
      outState.putString(KEY_OPERATION_PATH, oppathe);
      outState.putString(KEY_OPERATED_ON_PATH, oppathe1);
      outState.putParcelableArrayList(KEY_OPERATIONS_PATH_LIST, (oparrayList));
      outState.putInt(KEY_OPERATION, operation);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(mainActivityHelper.mNotificationReceiver);
    unregisterReceiver(receiver2);

    if (SDK_INT >= KITKAT) {
      unregisterReceiver(mOtgReceiver);
    }

    final Toast toast = this.toast.get();
    if (toast != null) {
      toast.cancel();
    }
    this.toast = new WeakReference<>(null);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (materialDialog != null && !materialDialog.isShowing()) {
      materialDialog.show();
      materialDialog = null;
    }

    drawer.refreshDrawer();
    drawer.refactorDrawerLockMode();

    IntentFilter newFilter = new IntentFilter();
    newFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    newFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    newFilter.addDataScheme(ContentResolver.SCHEME_FILE);
    registerReceiver(mainActivityHelper.mNotificationReceiver, newFilter);
    registerReceiver(receiver2, new IntentFilter(TAG_INTENT_FILTER_GENERAL));

    if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
      updateUsbInformation();
    }
  }

  /** Updates everything related to USB devices MUST ALWAYS be called after onResume() */
  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  private void updateUsbInformation() {
    boolean isInformationUpdated = false;
    List<UsbOtgRepresentation> connectedDevices = OTGUtil.getMassStorageDevicesConnected(this);

    if (!connectedDevices.isEmpty()) {
      if (SingletonUsbOtg.getInstance().getUsbOtgRoot() != null
          && OTGUtil.isUsbUriAccessible(this)) {
        for (UsbOtgRepresentation device : connectedDevices) {
          if (SingletonUsbOtg.getInstance().checkIfRootIsFromDevice(device)) {
            isInformationUpdated = true;
            break;
          }
        }

        if (!isInformationUpdated) {
          SingletonUsbOtg.getInstance().resetUsbOtgRoot();
        }
      }

      if (!isInformationUpdated) {
        SingletonUsbOtg.getInstance().setConnectedDevice(connectedDevices.get(0));
        isInformationUpdated = true;
      }
    }

    if (!isInformationUpdated) {
      SingletonUsbOtg.getInstance().resetUsbOtgRoot();
      drawer.refreshDrawer();
    }

    // Registering intent filter for OTG
    IntentFilter otgFilter = new IntentFilter();
    otgFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    otgFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    registerReceiver(mOtgReceiver, otgFilter);
  }

  /** Receiver to check if a USB device is connected at the runtime of application */
  BroadcastReceiver mOtgReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            List<UsbOtgRepresentation> connectedDevices =
                OTGUtil.getMassStorageDevicesConnected(MainActivity.this);
            if (!connectedDevices.isEmpty()) {
              SingletonUsbOtg.getInstance().resetUsbOtgRoot();
              SingletonUsbOtg.getInstance().setConnectedDevice(connectedDevices.get(0));
              drawer.refreshDrawer();
            }
          } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            SingletonUsbOtg.getInstance().resetUsbOtgRoot();
            drawer.refreshDrawer();
            goToMain(null);
          }
        }
      };

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU) {
      /*
      ImageView ib = findViewById(R.id.action_overflow);
      if (ib.getVisibility() == View.VISIBLE) {
          ib.performClick();
      }
      */
      // return 'true' to prevent further propagation of the key event
      return true;
    }

    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // TODO: 6/5/2017 Android may choose to not call this method before destruction
    // TODO: https://developer.android.com/reference/android/app/Activity.html#onDestroy%28%29
    closeInteractiveShell();
    SshConnectionPool.INSTANCE.shutdown();
    if (drawer != null && drawer.getBilling() != null) {
      drawer.getBilling().destroyBillingInstance();
    }
  }

  /** Closes the interactive shell and threads associated */
  private void closeInteractiveShell() {
    if (isRootExplorer()) {
      // close interactive shell and handler thread associated with it
      if (SDK_INT >= JELLY_BEAN_MR2) {
        // let it finish up first with what it's doing
        handlerThread.quitSafely();
      } else handlerThread.quit();
      shellInteractive.close();
    }
  }

  public void updatePaths(int pos) {
    TabFragment tabFragment = getTabFragment();
    if (tabFragment != null) tabFragment.updatepaths(pos);
  }

  public void openCompressed(String path) {
    appBarLayout.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    fragmentTransaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_in_bottom);
    Fragment zipFragment = new CompressedExplorerFragment();
    Bundle bundle = new Bundle();
    bundle.putString(CompressedExplorerFragment.KEY_PATH, path);
    zipFragment.setArguments(bundle);
    fragmentTransaction.add(R.id.content_frame, zipFragment);
    fragmentTransaction.commitAllowingStateLoss();
  }

  public @Nullable MainFragment getCurrentMainFragment() {
    TabFragment tab = getTabFragment();

    if (tab != null && tab.getCurrentTabFragment() instanceof MainFragment) {
      return (MainFragment) tab.getCurrentTabFragment();
    } else return null;
  }

  public TabFragment getTabFragment() {
    Fragment fragment = getFragmentAtFrame();

    if (!(fragment instanceof TabFragment)) return null;
    else return (TabFragment) fragment;
  }

  public Fragment getFragmentAtFrame() {
    return getSupportFragmentManager().findFragmentById(R.id.content_frame);
  }

  public void setPagingEnabled(boolean b) {
    getTabFragment().mViewPager.setPagingEnabled(b);
  }

  public File getUsbDrive() {
    File parent = new File("/storage");

    try {
      for (File f : parent.listFiles())
        if (f.exists() && f.getName().toLowerCase().contains("usb") && f.canExecute()) return f;
    } catch (Exception e) {
    }

    parent = new File("/mnt/sdcard/usbStorage");
    if (parent.exists() && parent.canExecute()) return parent;
    parent = new File("/mnt/sdcard/usb_storage");
    if (parent.exists() && parent.canExecute()) return parent;

    return null;
  }

  public SpeedDialView getFAB() {
    return floatingActionButton;
  }

  public AppBar getAppbar() {
    return appbar;
  }

  public Drawer getDrawer() {
    return drawer;
  }

  protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
    super.onActivityResult(requestCode, responseCode, intent);
    if (requestCode == Drawer.image_selector_request_code) {
      drawer.onActivityResult(requestCode, responseCode, intent);
    } else if (requestCode == 3) {
      Uri treeUri;
      if (responseCode == Activity.RESULT_OK) {
        // Get Uri from Storage Access Framework.
        treeUri = intent.getData();
        // Persist URI - this is required for verification of writability.
        if (treeUri != null)
          getPrefs()
              .edit()
              .putString(PreferencesConstants.PREFERENCE_URI, treeUri.toString())
              .apply();
      } else {
        // If not confirmed SAF, or if still not writable, then revert settings.
        /* DialogUtil.displayError(getActivity(), R.string.message_dialog_cannot_write_to_folder_saf, false, currentFolder);
        ||!FileUtil.isWritableNormalOrSaf(currentFolder)*/
        return;
      }

      // After confirmation, update stored value of folder.
      // Persist access permissions.

      if (SDK_INT >= KITKAT) {
        getContentResolver()
            .takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      }

      executeWithMainFragment(
          mainFragment -> {
            switch (operation) {
              case DELETE: // deletion
                new DeleteTask(mainActivity).execute((oparrayList));
                break;
              case COPY: // copying
                // legacy compatibility
                if (oparrayList != null && oparrayList.size() != 0) {
                  oparrayListList = new ArrayList<>();
                  oparrayListList.add(oparrayList);
                  oparrayList = null;
                  oppatheList = new ArrayList<>();
                  oppatheList.add(oppathe);
                  oppathe = "";
                }
                for (int i = 0; i < oparrayListList.size(); i++) {
                  ArrayList<HybridFileParcelable> sourceList = oparrayListList.get(i);
                  Intent intent1 = new Intent(this, CopyService.class);
                  intent1.putExtra(CopyService.TAG_COPY_SOURCES, sourceList);
                  intent1.putExtra(CopyService.TAG_COPY_TARGET, oppatheList.get(i));
                  ServiceWatcherUtil.runService(this, intent1);
                }
                break;
              case MOVE: // moving
                // legacy compatibility
                if (oparrayList != null && oparrayList.size() != 0) {
                  oparrayListList = new ArrayList<>();
                  oparrayListList.add(oparrayList);
                  oparrayList = null;
                  oppatheList = new ArrayList<>();
                  oppatheList.add(oppathe);
                  oppathe = "";
                }

                new MoveFiles(
                        oparrayListList,
                        isRootExplorer(),
                        mainFragment.getCurrentPath(),
                        mainFragment.getActivity(),
                        OpenMode.FILE)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, oppatheList);
                break;
              case NEW_FOLDER: // mkdir
                mainActivityHelper.mkDir(
                    new HybridFile(OpenMode.FILE, oppathe),
                    RootHelper.generateBaseFile(new File(oppathe), true),
                    mainFragment);
                break;
              case RENAME:
                mainActivityHelper.rename(
                    mainFragment.getMainFragmentViewModel().getOpenMode(),
                    (oppathe),
                    (oppathe1),
                    null,
                    false,
                    mainActivity,
                    isRootExplorer());
                mainFragment.updateList();
                break;
              case NEW_FILE:
                mainActivityHelper.mkFile(
                    new HybridFile(OpenMode.FILE, oppathe),
                    new HybridFile(OpenMode.FILE, oppathe),
                    mainFragment);
                break;
              case EXTRACT:
                mainActivityHelper.extractFile(new File(oppathe));
                break;
              case COMPRESS:
                mainActivityHelper.compressFiles(new File(oppathe), oparrayList);
                break;
              case SAVE_FILE:
                FileUtil.writeUriToStorage(
                    this, urisToBeSaved, getContentResolver(), mainFragment.getCurrentPath());
                urisToBeSaved = null;
                finish();
                break;
              default:
                LogHelper.logOnProductionOrCrash(TAG, "Incorrect value for switch");
            }
            return null;
          },
          true);
      operation = UNDEFINED;
    } else if (requestCode == REQUEST_CODE_SAF) {
      executeWithMainFragment(
          mainFragment -> {
            if (responseCode == Activity.RESULT_OK && intent.getData() != null) {
              // otg access
              Uri usbOtgRoot = intent.getData();
              SingletonUsbOtg.getInstance().setUsbOtgRoot(usbOtgRoot);
              mainFragment.loadlist(OTGUtil.PREFIX_OTG, false, OpenMode.OTG);
              drawer.closeIfNotLocked();
              if (drawer.isLocked()) drawer.onDrawerClosed();
            } else if (requestCode == REQUEST_CODE_SAF_FTP) {
              FtpServerFragment ftpServerFragment = (FtpServerFragment) getFragmentAtFrame();
              ftpServerFragment.changeFTPServerPath(intent.getData().toString());
              Toast.makeText(this, R.string.ftp_path_change_success, Toast.LENGTH_SHORT).show();

            } else {
              Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
              // otg access not provided
              drawer.resetPendingPath();
            }
            return null;
          },
          true);
    }
  }

  void initialisePreferences() {
    currentTab = getCurrentTab();
    skinStatusBar = PreferenceUtils.getStatusColor(getPrimary());
  }

  void initialiseViews() {

    appbar =
        new AppBar(
            this,
            getPrefs(),
            queue -> {
              if (!queue.isEmpty()) {
                mainActivityHelper.search(getPrefs(), queue);
              }
            });
    appBarLayout = getAppbar().getAppbarLayout();

    setSupportActionBar(getAppbar().getToolbar());
    drawer = new Drawer(this);

    indicator_layout = findViewById(R.id.indicator_layout);

    getSupportActionBar().setDisplayShowTitleEnabled(false);
    fabBgView = findViewById(R.id.fabs_overlay_layout);

    switch (getAppTheme().getSimpleTheme()) {
      case DARK:
        fabBgView.setBackgroundResource(R.drawable.fab_shadow_dark);
        break;
      case BLACK:
        fabBgView.setBackgroundResource(R.drawable.fab_shadow_black);
        break;
    }

    fabBgView.setOnClickListener(
        view -> {
          if (getAppbar().getSearchView().isEnabled()) getAppbar().getSearchView().hideSearchView();
        });

    drawer.setDrawerHeaderBackground();
  }

  /**
   * Call this method when you need to update the MainActivity view components' colors based on
   * update in the {@link MainActivity#currentTab} Warning - All the variables should be initialised
   * before calling this method!
   */
  public void updateViews(ColorDrawable colorDrawable) {
    // appbar view color
    appbar.getBottomBar().setBackgroundColor(colorDrawable.getColor());
    // action bar color
    mainActivity.getSupportActionBar().setBackgroundDrawable(colorDrawable);

    drawer.setBackgroundColor(colorDrawable.getColor());

    if (SDK_INT >= LOLLIPOP) {
      // for lollipop devices, the status bar color
      mainActivity.getWindow().setStatusBarColor(colorDrawable.getColor());
      if (getBoolean(PREFERENCE_COLORED_NAVIGATION))
        mainActivity
            .getWindow()
            .setNavigationBarColor(PreferenceUtils.getStatusColor(colorDrawable.getColor()));
    } else if (SDK_INT == KITKAT_WATCH || SDK_INT == KITKAT) {

      // for kitkat devices, the status bar color
      SystemBarTintManager tintManager = new SystemBarTintManager(this);
      tintManager.setStatusBarTintEnabled(true);
      tintManager.setStatusBarTintColor(colorDrawable.getColor());
    }
  }

  void initialiseFab() {
    int colorAccent = getAccent();

    floatingActionButton = findViewById(R.id.fabs_menu);
    floatingActionButton.setMainFabClosedBackgroundColor(colorAccent);
    floatingActionButton.setMainFabOpenedBackgroundColor(colorAccent);
    initializeFabActionViews();
  }

  public void initializeFabActionViews() {
    // NOTE: SpeedDial inverts insert index than FABsmenu
    FabWithLabelView cloudFab =
        initFabTitle(
            R.id.menu_new_cloud, R.string.cloud_connection, R.drawable.ic_cloud_white_24dp);
    FabWithLabelView newFileFab =
        initFabTitle(R.id.menu_new_file, R.string.file, R.drawable.ic_insert_drive_file_white_48dp);
    FabWithLabelView newFolderFab =
        initFabTitle(R.id.menu_new_folder, R.string.folder, R.drawable.folder_fab);

    floatingActionButton.setOnActionSelectedListener(new FabActionListener(this));
    floatingActionButton.setOnClickListener(
        view -> {
          fabButtonClick(cloudFab);
        });
    floatingActionButton.setOnFocusChangeListener(new CustomZoomFocusChange());
    floatingActionButton.getMainFab().setOnFocusChangeListener(new CustomZoomFocusChange());
    floatingActionButton.setNextFocusUpId(cloudFab.getId());
    floatingActionButton.getMainFab().setNextFocusUpId(cloudFab.getId());
    floatingActionButton.setOnKeyListener(
        (v, keyCode, event) -> {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
              if (getCurrentTab() == 0 && getFAB().isFocused()) {
                getTabFragment().mViewPager.setCurrentItem(1);
              }
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
              findViewById(R.id.content_frame).requestFocus();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
              if (pasteHelper != null
                  && pasteHelper.getSnackbar() != null
                  && pasteHelper.getSnackbar().isShown())
                ((Snackbar.SnackbarLayout) pasteHelper.getSnackbar().getView())
                    .findViewById(R.id.snackBarActionButton)
                    .requestFocus();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
              fabButtonClick(cloudFab);
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
              onBackPressed();
            } else {
              return false;
            }
          }
          return true;
        });
    cloudFab.setNextFocusDownId(floatingActionButton.getMainFab().getId());
    cloudFab.setNextFocusUpId(newFileFab.getId());
    cloudFab.setOnFocusChangeListener(new CustomZoomFocusChange());
    newFileFab.setNextFocusDownId(cloudFab.getId());
    newFileFab.setNextFocusUpId(newFolderFab.getId());
    newFileFab.setOnFocusChangeListener(new CustomZoomFocusChange());
    newFolderFab.setNextFocusDownId(newFileFab.getId());
    newFolderFab.setOnFocusChangeListener(new CustomZoomFocusChange());
  }

  private void fabButtonClick(FabWithLabelView cloudFab) {
    if (floatingActionButton.isOpen()) {
      floatingActionButton.close(true);
    } else {
      floatingActionButton.open(true);
      cloudFab.requestFocus();
    }
  }

  private FabWithLabelView initFabTitle(
      @IdRes int id, @StringRes int fabTitle, @DrawableRes int icon) {
    int iconSkin = getCurrentColorPreference().getIconSkin();

    SpeedDialActionItem.Builder builder =
        new SpeedDialActionItem.Builder(id, icon)
            .setLabel(fabTitle)
            .setFabBackgroundColor(iconSkin);

    switch (getAppTheme().getSimpleTheme()) {
      case LIGHT:
        fabBgView.setBackgroundResource(R.drawable.fab_shadow_light);
        break;
      case DARK:
        builder
            .setLabelBackgroundColor(Utils.getColor(this, R.color.holo_dark_background))
            .setLabelColor(Utils.getColor(this, R.color.text_dark));
        fabBgView.setBackgroundResource(R.drawable.fab_shadow_dark);
        break;
      case BLACK:
        builder
            .setLabelBackgroundColor(Color.BLACK)
            .setLabelColor(Utils.getColor(this, R.color.text_dark));
        fabBgView.setBackgroundResource(R.drawable.fab_shadow_black);
        break;
    }

    return floatingActionButton.addActionItem(builder.create());
  }

  public boolean copyToClipboard(Context context, String text) {
    try {
      android.content.ClipboardManager clipboard =
          (android.content.ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
      android.content.ClipData clip =
          android.content.ClipData.newPlainText("Path copied to clipboard", text);
      clipboard.setPrimaryClip(clip);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void renameBookmark(final String title, final String path) {
    if (dataUtils.containsBooks(new String[] {title, path}) != -1) {
      RenameBookmark renameBookmark = RenameBookmark.getInstance(title, path, getAccent());
      if (renameBookmark != null) renameBookmark.show(getFragmentManager(), "renamedialog");
    }
  }

  public PasteHelper getPaste() {
    return pasteHelper;
  }

  public void setPaste(PasteHelper p) {
    pasteHelper = p;
  }

  @Override
  public void onNewIntent(Intent i) {
    super.onNewIntent(i);
    intent = i;
    path = i.getStringExtra("path");

    if (path != null) {
      if (new File(path).isDirectory()) {
        final MainFragment mainFragment = getCurrentMainFragment();
        if (mainFragment != null) {
          mainFragment.loadlist(path, false, OpenMode.FILE);
        } else {
          goToMain(path);
        }
      } else FileUtils.openFile(new File(path), mainActivity, getPrefs());
    } else if (i.getStringArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS) != null) {
      ArrayList<HybridFileParcelable> failedOps =
          i.getParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS);
      if (failedOps != null) {
        mainActivityHelper.showFailedOperationDialog(failedOps, this);
      }
    } else if (i.getCategories() != null
        && i.getCategories().contains(CLOUD_AUTHENTICATOR_GDRIVE)) {
      // we used an external authenticator instead of APIs. Probably for Google Drive
      CloudRail.setAuthenticationResponse(intent);
    } else if ((openProcesses = i.getBooleanExtra(KEY_INTENT_PROCESS_VIEWER, false))) {
      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
      transaction.replace(
          R.id.content_frame, new ProcessViewerFragment(), KEY_INTENT_PROCESS_VIEWER);
      //   transaction.addToBackStack(null);
      openProcesses = false;
      // title.setText(utils.getString(con, R.string.process_viewer));
      // Commit the transaction
      transaction.commitAllowingStateLoss();
      supportInvalidateOptionsMenu();
    } else if (intent.getAction() != null) {
      checkForExternalIntent(intent);

      if (SDK_INT >= KITKAT) {
        if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
          SingletonUsbOtg.getInstance().resetUsbOtgRoot();
          drawer.refreshDrawer();
        }
      }
    }
  }

  private BroadcastReceiver receiver2 =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
          if (i.getStringArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS) != null) {
            ArrayList<HybridFileParcelable> failedOps =
                i.getParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS);
            if (failedOps != null) {
              mainActivityHelper.showFailedOperationDialog(failedOps, mainActivity);
            }
          }
        }
      };

  public void showSMBDialog(String name, String path, boolean edit) {
    if (path.length() > 0 && name.length() == 0) {
      int i = dataUtils.containsServer(new String[] {name, path});
      if (i != -1) name = dataUtils.getServers().get(i)[0];
    }
    SmbConnectDialog smbConnectDialog = new SmbConnectDialog();
    Bundle bundle = new Bundle();
    bundle.putString("name", name);
    bundle.putString("path", path);
    bundle.putBoolean("edit", edit);
    smbConnectDialog.setArguments(bundle);
    smbConnectDialog.show(getFragmentManager(), "smbdailog");
  }

  public void showSftpDialog(String name, String path, boolean edit) {
    if (path.length() > 0 && name.length() == 0) {
      int i = dataUtils.containsServer(new String[] {name, path});
      if (i != -1) name = dataUtils.getServers().get(i)[0];
    }
    SftpConnectDialog sftpConnectDialog = new SftpConnectDialog();
    Uri uri = Uri.parse(path);
    String userinfo = uri.getUserInfo();
    Bundle bundle = new Bundle();
    bundle.putString(ARG_NAME, name);
    bundle.putString(ARG_ADDRESS, uri.getHost());
    bundle.putInt(ARG_PORT, uri.getPort());
    if (!TextUtils.isEmpty(uri.getPath())) {
      bundle.putString(ARG_DEFAULT_PATH, uri.getPath());
    }
    bundle.putString(
        ARG_USERNAME,
        userinfo.indexOf(':') > 0 ? userinfo.substring(0, userinfo.indexOf(':')) : userinfo);

    if (userinfo.indexOf(':') < 0) {
      bundle.putBoolean(ARG_HAS_PASSWORD, false);
      bundle.putString(ARG_KEYPAIR_NAME, utilsHandler.getSshAuthPrivateKeyName(path));
    } else {
      bundle.putBoolean(ARG_HAS_PASSWORD, true);
      bundle.putString(ARG_PASSWORD, userinfo.substring(userinfo.indexOf(':') + 1));
    }
    bundle.putBoolean(ARG_EDIT, edit);
    sftpConnectDialog.setArguments(bundle);
    sftpConnectDialog.show(getSupportFragmentManager(), "sftpdialog");
  }

  /**
   * Shows a view that goes from white at it's lowest part to transparent a the top. It covers the
   * fragment.
   */
  public void showSmokeScreen() {
    fabBgView.show();
  }

  public void hideSmokeScreen() {
    fabBgView.hide();
  }

  @Override
  public void addConnection(
      boolean edit,
      @NonNull final String name,
      @NonNull final String path,
      @Nullable final String encryptedPath,
      @Nullable final String oldname,
      @Nullable final String oldPath) {
    String[] s = new String[] {name, path};
    if (!edit) {
      if ((dataUtils.containsServer(path)) == -1) {
        dataUtils.addServer(s);
        drawer.refreshDrawer();

        utilsHandler.saveToDatabase(
            new OperationData(UtilsHandler.Operation.SMB, name, encryptedPath));

        // grid.addPath(name, encryptedPath, DataUtils.SMB, 1);
        executeWithMainFragment(
            mainFragment -> {
              mainFragment.loadlist(path, false, OpenMode.UNKNOWN);
              return null;
            },
            true);
      } else {
        Snackbar.make(
                findViewById(R.id.navigation),
                getString(R.string.connection_exists),
                Snackbar.LENGTH_SHORT)
            .show();
      }
    } else {
      int i = dataUtils.containsServer(new String[] {oldname, oldPath});
      if (i != -1) {
        dataUtils.removeServer(i);

        AppConfig.getInstance()
            .runInBackground(
                () -> {
                  utilsHandler.renameSMB(oldname, oldPath, name, path);
                });
        // mainActivity.grid.removePath(oldname, oldPath, DataUtils.SMB);
      }
      dataUtils.addServer(s);
      Collections.sort(dataUtils.getServers(), new BookSorter());
      drawer.refreshDrawer();
      // mainActivity.grid.addPath(name, encryptedPath, DataUtils.SMB, 1);
    }
  }

  @Override
  public void deleteConnection(final String name, final String path) {

    int i = dataUtils.containsServer(new String[] {name, path});
    if (i != -1) {
      dataUtils.removeServer(i);

      AppConfig.getInstance()
          .runInBackground(
              () -> {
                utilsHandler.removeFromDatabase(
                    new OperationData(UtilsHandler.Operation.SMB, name, path));
              });
      // grid.removePath(name, path, DataUtils.SMB);
      drawer.refreshDrawer();
    }
  }

  @Override
  public void delete(String title, String path) {
    utilsHandler.removeFromDatabase(
        new OperationData(UtilsHandler.Operation.BOOKMARKS, title, path));
    drawer.refreshDrawer();
  }

  @Override
  public void modify(String oldpath, String oldname, String newPath, String newname) {
    utilsHandler.renameBookmark(oldname, oldpath, newname, newPath);
    drawer.refreshDrawer();
  }

  @Override
  public void onPreExecute(String query) {
    executeWithMainFragment(
        mainFragment -> {
          mainFragment.mSwipeRefreshLayout.setRefreshing(true);
          mainFragment.onSearchPreExecute(query);
          return null;
        });
  }

  @Override
  public void onPostExecute(String query) {
    final MainFragment mainFragment = getCurrentMainFragment();
    if (mainFragment == null) {
      // TODO cancel search
      return;
    }

    mainFragment.onSearchCompleted(query);
    mainFragment.mSwipeRefreshLayout.setRefreshing(false);
  }

  @Override
  public void onProgressUpdate(HybridFileParcelable val, String query) {
    final MainFragment mainFragment = getCurrentMainFragment();
    if (mainFragment == null) {
      // TODO cancel search
      return;
    }

    mainFragment.addSearchResult(val, query);
  }

  @Override
  public void onCancelled() {
    final MainFragment mainFragment = getCurrentMainFragment();
    if (mainFragment == null) {
      return;
    }

    mainFragment.reloadListElements(
        false, false, !mainFragment.getMainFragmentViewModel().isList());
    mainFragment.mSwipeRefreshLayout.setRefreshing(false);
  }

  @Override
  public void addConnection(OpenMode service) {
    try {
      if (cloudHandler.findEntry(service) != null) {
        // cloud entry already exists
        Toast.makeText(
                this, getResources().getString(R.string.connection_exists), Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(
                MainActivity.this,
                getResources().getString(R.string.please_wait),
                Toast.LENGTH_LONG)
            .show();
        Bundle args = new Bundle();
        args.putInt(ARGS_KEY_LOADER, service.ordinal());

        // check if we already had done some work on the loader
        Loader loader = getSupportLoaderManager().getLoader(REQUEST_CODE_CLOUD_LIST_KEY);
        if (loader != null && loader.isStarted()) {

          // making sure that loader is not started
          getSupportLoaderManager().destroyLoader(REQUEST_CODE_CLOUD_LIST_KEY);
        }

        getSupportLoaderManager().initLoader(REQUEST_CODE_CLOUD_LIST_KEY, args, this);
      }
    } catch (CloudPluginException e) {
      e.printStackTrace();
      Toast.makeText(this, getResources().getString(R.string.cloud_error_plugin), Toast.LENGTH_LONG)
          .show();
    }
  }

  @Override
  public void deleteConnection(OpenMode service) {
    cloudHandler.clear(service);
    dataUtils.removeAccount(service);

    runOnUiThread(drawer::refreshDrawer);
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Uri uri =
        Uri.withAppendedPath(
            Uri.parse("content://" + CloudContract.PROVIDER_AUTHORITY), "/keys.db/secret_keys");

    String[] projection =
        new String[] {
          CloudContract.COLUMN_ID,
          CloudContract.COLUMN_CLIENT_ID,
          CloudContract.COLUMN_CLIENT_SECRET_KEY
        };

    switch (id) {
      case REQUEST_CODE_CLOUD_LIST_KEY:
        Uri uriAppendedPath = uri;
        switch (OpenMode.getOpenMode(args.getInt(ARGS_KEY_LOADER, 2))) {
          case GDRIVE:
            uriAppendedPath = ContentUris.withAppendedId(uri, 2);
            break;
          case DROPBOX:
            uriAppendedPath = ContentUris.withAppendedId(uri, 3);
            break;
          case BOX:
            uriAppendedPath = ContentUris.withAppendedId(uri, 4);
            break;
          case ONEDRIVE:
            uriAppendedPath = ContentUris.withAppendedId(uri, 5);
            break;
        }
        return new CursorLoader(this, uriAppendedPath, projection, null, null, null);
      case REQUEST_CODE_CLOUD_LIST_KEYS:
        // we need a list of all secret keys

        try {
          List<CloudEntry> cloudEntries = cloudHandler.getAllEntries();

          // we want keys for services saved in database, and the cloudrail app key which
          // is at index 1
          String ids[] = new String[cloudEntries.size() + 1];

          ids[0] = 1 + "";
          for (int i = 1; i <= cloudEntries.size(); i++) {

            // we need to get only those cloud details which user wants
            switch (cloudEntries.get(i - 1).getServiceType()) {
              case GDRIVE:
                ids[i] = 2 + "";
                break;
              case DROPBOX:
                ids[i] = 3 + "";
                break;
              case BOX:
                ids[i] = 4 + "";
                break;
              case ONEDRIVE:
                ids[i] = 5 + "";
                break;
            }
          }
          return new CursorLoader(this, uri, projection, CloudContract.COLUMN_ID, ids, null);
        } catch (CloudPluginException e) {
          e.printStackTrace();

          Toast.makeText(
                  this, getResources().getString(R.string.cloud_error_plugin), Toast.LENGTH_LONG)
              .show();
        }
      default:
        Uri undefinedUriAppendedPath = ContentUris.withAppendedId(uri, 7);
        return new CursorLoader(this, undefinedUriAppendedPath, projection, null, null, null);
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
    if (data == null) {
      Toast.makeText(
              this,
              getResources().getString(R.string.cloud_error_failed_restart),
              Toast.LENGTH_LONG)
          .show();
      return;
    }

    /*
     * This is hack for repeated calls to onLoadFinished(),
     * we take the Cursor provided to check if the function
     * has already been called on it.
     *
     * TODO: find a fix for repeated callbacks to onLoadFinished()
     */
    if (cloudCursorData != null && cloudCursorData == data) return;
    cloudCursorData = data;

    if (cloudLoaderAsyncTask != null
        && cloudLoaderAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
      return;
    }
    cloudLoaderAsyncTask = new CloudLoaderAsyncTask(this, cloudHandler, cloudCursorData);
    cloudLoaderAsyncTask.execute();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    // For passing code check
  }

  public void initCornersDragListener(boolean destroy, boolean shouldInvokeLeftAndRight) {
    initBottomDragListener(destroy);
    initLeftRightAndTopDragListeners(destroy, shouldInvokeLeftAndRight);
  }

  private void initBottomDragListener(boolean destroy) {
    View bottomPlaceholder = findViewById(R.id.placeholder_drag_bottom);
    if (destroy) {
      bottomPlaceholder.setOnDragListener(null);
      bottomPlaceholder.setVisibility(View.GONE);
    } else {
      bottomPlaceholder.setVisibility(View.VISIBLE);
      bottomPlaceholder.setOnDragListener(
          new TabFragmentBottomDragListener(
              () -> {
                getCurrentMainFragment().smoothScrollListView(false);
                return null;
              },
              () -> {
                getCurrentMainFragment().stopSmoothScrollListView();
                return null;
              }));
    }
  }

  private void initLeftRightAndTopDragListeners(boolean destroy, boolean shouldInvokeLeftAndRight) {
    TabFragment tabFragment = getTabFragment();
    tabFragment.initLeftRightAndTopDragListeners(destroy, shouldInvokeLeftAndRight);
  }

  private static final class FabActionListener implements SpeedDialView.OnActionSelectedListener {

    MainActivity mainActivity;
    SpeedDialView floatingActionButton;

    FabActionListener(MainActivity mainActivity) {
      this.mainActivity = mainActivity;
      this.floatingActionButton = mainActivity.floatingActionButton;
    }

    @Override
    public boolean onActionSelected(SpeedDialActionItem actionItem) {
      final MainFragment ma =
          (MainFragment)
              ((TabFragment)
                      mainActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame))
                  .getCurrentTabFragment();
      final String path = ma.getCurrentPath();

      switch (actionItem.getId()) {
        case R.id.menu_new_folder:
          mainActivity.mainActivityHelper.mkdir(
              ma.getMainFragmentViewModel().getOpenMode(), path, ma);
          break;
        case R.id.menu_new_file:
          mainActivity.mainActivityHelper.mkfile(
              ma.getMainFragmentViewModel().getOpenMode(), path, ma);
          break;
        case R.id.menu_new_cloud:
          BottomSheetDialogFragment fragment = new CloudSheetFragment();
          fragment.show(
              ma.getActivity().getSupportFragmentManager(), CloudSheetFragment.TAG_FRAGMENT);
          break;
      }

      floatingActionButton.close(true);
      return true;
    }
  }
  /**
   * Invoke {@link FtpServerFragment#changeFTPServerPath(String)} to change FTP server share path.
   *
   * @see FtpServerFragment#changeFTPServerPath(String)
   * @see FolderChooserDialog
   * @see com.afollestad.materialdialogs.folderselector.FolderChooserDialog.FolderCallback
   * @param dialog
   * @param folder selected folder
   */
  @Override
  public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
    switch (dialog.getTag()) {
      case FtpServerFragment.TAG:
        FtpServerFragment ftpServerFragment = (FtpServerFragment) getFragmentAtFrame();
        if (folder.exists() && folder.isDirectory()) {
          ftpServerFragment.changeFTPServerPath(folder.getPath());
          Toast.makeText(this, R.string.ftp_path_change_success, Toast.LENGTH_SHORT).show();
        } else {
          // try to get parent
          File pathParentFile = new File(folder.getParent());
          if (pathParentFile.exists() && pathParentFile.isDirectory()) {

            ftpServerFragment.changeFTPServerPath(pathParentFile.getPath());
            Toast.makeText(this, R.string.ftp_path_change_success, Toast.LENGTH_SHORT).show();
          } else {
            // don't have access, print error
            Toast.makeText(this, R.string.ftp_path_change_error_invalid, Toast.LENGTH_SHORT).show();
          }
        }
        dialog.dismiss();
        break;
      default:
        dialog.dismiss();
        break;
    }
  }

  /**
   * Do nothing other than dismissing the folder selection dialog.
   *
   * @see com.afollestad.materialdialogs.folderselector.FolderChooserDialog.FolderCallback
   * @param dialog
   */
  @Override
  public void onFolderChooserDismissed(@NonNull FolderChooserDialog dialog) {
    dialog.dismiss();
  }

  private void executeWithMainFragment(@NonNull Function<MainFragment, Void> lambda) {
    executeWithMainFragment(lambda, false);
  }

  @Nullable
  private void executeWithMainFragment(
      @NonNull Function<MainFragment, Void> lambda, boolean showToastIfMainFragmentIsNull) {
    final MainFragment mainFragment = getCurrentMainFragment();
    if (mainFragment != null && mainFragment.getMainFragmentViewModel() != null) {
      lambda.apply(mainFragment);
    } else {
      Log.w(TAG, "MainFragment is null");
      if (showToastIfMainFragmentIsNull) {
        AppConfig.toast(this, R.string.operation_unsuccesful);
      }
    }
  }
}
