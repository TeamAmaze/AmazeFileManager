/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 *                      Emmanuel Messulam<emmanuelbendavid@gmail.com>
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

package com.amaze.filemanager.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.service.quicksettings.TileService;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.adapters.data.DrawerItem;
import com.amaze.filemanager.asynchronous.asynctasks.DeleteTask;
import com.amaze.filemanager.asynchronous.asynctasks.MoveFiles;
import com.amaze.filemanager.asynchronous.asynctasks.PrepareCopyTask;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.database.CloudContract;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.database.models.CloudEntry;
import com.amaze.filemanager.database.models.Tab;
import com.amaze.filemanager.exceptions.CloudPluginException;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.PasteHelper;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig;
import com.amaze.filemanager.fragments.AppsListFragment;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.fragments.CloudSheetFragment.CloudConnectionCallbacks;
import com.amaze.filemanager.fragments.CompressedExplorerFragment;
import com.amaze.filemanager.fragments.FTPServerFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.fragments.SearchWorkerFragment;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.fragments.preference_fragments.QuickAccessPref;
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.dialogs.RenameBookmark;
import com.amaze.filemanager.ui.dialogs.RenameBookmark.BookmarkCallback;
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog;
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog;
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog.SmbConnectionListener;
import com.amaze.filemanager.ui.views.ScrimInsetsRelativeLayout;
import com.amaze.filemanager.ui.views.appbar.AppBar;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.DataUtils.DataChangeListener;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.TinyDB;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.cloudrail.si.CloudRail;
import com.cloudrail.si.exceptions.AuthenticationException;
import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import eu.chainfire.libsuperuser.Shell;
import jahirfiquitiva.libs.fabsmenu.FABsMenu;
import jahirfiquitiva.libs.fabsmenu.FABsMenuListener;
import jahirfiquitiva.libs.fabsmenu.TitleFAB;

import static android.os.Build.VERSION.SDK_INT;

public class MainActivity extends ThemedActivity implements OnRequestPermissionsResultCallback,
        SmbConnectionListener, DataChangeListener, BookmarkCallback,
        SearchWorkerFragment.HelperCallbacks, CloudConnectionCallbacks,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final Pattern DIR_SEPARATOR = Pattern.compile("/");
    public static final String TAG_ASYNC_HELPER = "async_helper";

    private DataUtils dataUtils = DataUtils.getInstance();

    public DrawerLayout mDrawerLayout;
    public ListView mDrawerList;
    public ScrimInsetsRelativeLayout mDrawerLinear;
    public String path = "", launchPath;
    public FrameLayout frameLayout;
    public boolean mReturnIntent = false;
    public boolean useGridView, openzip = false;
    public boolean mRingtonePickerIntent = false, colourednavigation = false;
    public int skinStatusBar;

    public volatile int storage_count = 0; // number of storage available (internal/external/otg etc)

    public FABsMenu floatingActionButton;
    public LinearLayout pathbar;
    public FrameLayout buttonBarFrame;
    public boolean isDrawerLocked = false;

    public DrawerAdapter adapter;
    public MainActivityHelper mainActivityHelper;

    public int operation = -1;
    public ArrayList<HybridFileParcelable> oparrayList;
    public ArrayList<ArrayList<HybridFileParcelable>> oparrayListList;

    // oppathe - the path at which certain operation needs to be performed
    // oppathe1 - the new path which user wants to create/modify
    // oppathList - the paths at which certain operation needs to be performed (pairs with oparrayList)
    public String oppathe, oppathe1;
    public ArrayList<String> oppatheList;
    public RelativeLayout drawerHeaderParent;

    /**
     * @deprecated use getCurrentMainFragment()
     */
    public MainFragment mainFragment;

    public static final String KEY_PREF_OTG = "uri_usb_otg";

    public static final String PASTEHELPER_BUNDLE = "pasteHelper";

    private static final String KEY_DRAWER_SELECTED = "selectitem";
    private static final String KEY_OPERATION_PATH = "oppathe";
    private static final String KEY_OPERATED_ON_PATH = "oppathe1";
    private static final String KEY_OPERATIONS_PATH_LIST = "oparraylist";
    private static final String KEY_OPERATION = "operation";

    private static final int image_selector_request_code = 31;

    private AppBar appbar;
    //private HistoryManager history, grid;
    private MainActivity mainActivity = this;
    private Context con = this;
    private String zippath;
    private FragmentTransaction pending_fragmentTransaction;
    private String pendingPath;
    private boolean openProcesses = false;
    private int hidemode;
    private MaterialDialog materialDialog;
    private String newPath = null;
    private boolean backPressedToExitOnce = false;
    private Toast toast = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private Intent intent;
    private View drawerHeaderLayout;
    private View drawerHeaderView, indicator_layout;
    private ImageLoader mImageLoader;

    private TabHandler tabHandler;
    /* A flag indicating that a PendingIntent is in progress and prevents
   * us from starting further intents.
   */
    private boolean showHidden = false;
    private AsyncTask<Void, Void, Boolean> cloudSyncTask;

    private AppBarLayout appBarLayout;

    /**
     * In drawer nothing is selected.
     */
    private static final int DRAWER_SELECTED_NONE = -1;
    /**
     * In drawer first storage is selected.
     */
    private static final int DRAWER_SELECTED_DEFAULT = 0;
    /**
     * In drawer {@link ProcessViewerFragment} is selected (which is a special case
     * of {@link #DRAWER_SELECTED_NONE} as ProcessViewer has no drawer item). //TODO might be wrong
     */
    private static final int DRAWER_SELECTED_PROCESSVIEWER = 102;
    /**
     * In drawer FTP or Apps list (also Settings for a brief second) are selected.
     */
    private static final int DRAWER_SELECTED_LASTSECTION = -2;

    /**
     * Which item in nav drawer is selected values go from 0 to the length of the nav drawer list,
     * special values are {@link #DRAWER_SELECTED_DEFAULT}, {@link #DRAWER_SELECTED_NONE},
     * {@link #DRAWER_SELECTED_PROCESSVIEWER} and {@link #DRAWER_SELECTED_LASTSECTION}.
     */
    private int selectedStorage;

    private CoordinatorLayout mScreenLayout;
    private View fabBgView;
    private UtilsHandler utilsHandler;
    private CloudHandler cloudHandler;

    private static final int REQUEST_CODE_SAF = 223;
    private static final String VALUE_PREF_OTG_NULL = "n/a";

    public static final String KEY_INTENT_PROCESS_VIEWER = "openprocesses";
    public static final String TAG_INTENT_FILTER_FAILED_OPS = "failedOps";
    public static final String TAG_INTENT_FILTER_GENERAL = "general_communications";
    public static final String ARGS_KEY_LOADER = "loader_cloud_args_service";

    /**
     * Broadcast which will be fired after every file operation, will denote list loading
     * Registered by {@link MainFragment}
     */
    public static final String KEY_INTENT_LOAD_LIST = "loadlist";

    /**
     * Extras carried by the list loading intent
     * Contains path of parent directory in which operation took place, so that we can run
     * media scanner on it
     */
    public static final String KEY_INTENT_LOAD_LIST_FILE = "loadlist_file";

    /**
     * Mime type in intent that apps need to pass when trying to open file manager from a specific directory
     * Should be clubbed with {@link Intent#ACTION_VIEW} and send in path to open in intent data field
     */
    public static final String ARGS_INTENT_ACTION_VIEW_MIME_FOLDER = "resource/folder";

    private static final String CLOUD_AUTHENTICATOR_GDRIVE = "android.intent.category.BROWSABLE";
    private static final String CLOUD_AUTHENTICATOR_REDIRECT_URI = "com.amaze.filemanager:/oauth2redirect";

    // the current visible tab, either 0 or 1
    public static int currentTab;

    public static Shell.Interactive shellInteractive;
    public static Handler handler;

    private static HandlerThread handlerThread;

    private static final int REQUEST_CODE_CLOUD_LIST_KEYS = 5463;
    private static final int REQUEST_CODE_CLOUD_LIST_KEY = 5472;

    private static final String KEY_PREFERENCE_BOOKMARKS_ADDED = "books_added";

    private PasteHelper pasteHelper;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialisePreferences();
        initializeInteractiveShell();

        dataUtils.registerOnDataChangedListener(this);

        CustomSshJConfig.init();
        AppConfig.setActivityContext(con);

        setContentView(R.layout.main_toolbar);
        appbar = new AppBar(this, getPrefs(), queue -> {
            if(!queue.isEmpty()) {
                mainActivityHelper.search(getPrefs(), queue);
            }
        });
        initialiseViews();
        tabHandler = new TabHandler(this);
        utilsHandler = AppConfig.getInstance().getUtilsHandler();
        cloudHandler = new CloudHandler(this);

        mImageLoader = AppConfig.getInstance().getImageLoader();
        mainActivityHelper = new MainActivityHelper(this);
        initialiseFab();// TODO: 7/12/2017 not init when actionIntent != null

        if (CloudSheetFragment.isCloudProviderAvailable(this)) {

            getSupportLoaderManager().initLoader(REQUEST_CODE_CLOUD_LIST_KEYS, null, this);
        }

        path = getIntent().getStringExtra("path");
        openProcesses = getIntent().getBooleanExtra(KEY_INTENT_PROCESS_VIEWER, false);
        intent = getIntent();

        if (intent.getStringArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS) != null) {
            ArrayList<HybridFileParcelable> failedOps = intent.getParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS);
            if (failedOps != null) {
                mainActivityHelper.showFailedOperationDialog(failedOps, this);
            }
        }

        checkForExternalIntent(intent);

        if (savedInstanceState != null) {

            selectedStorage = savedInstanceState.getInt(KEY_DRAWER_SELECTED, DRAWER_SELECTED_DEFAULT);
        }

        // setting window background color instead of each item, in order to reduce pixel overdraw
        if (getAppTheme().equals(AppTheme.LIGHT)) {
            /*if(Main.IS_LIST)
                getWindow().setBackgroundDrawableResource(android.R.color.white);
            else
                getWindow().setBackgroundDrawableResource(R.color.grid_background_light);
            */
            getWindow().setBackgroundDrawableResource(android.R.color.white);
        } else if (getAppTheme().equals(AppTheme.BLACK)) {
            getWindow().setBackgroundDrawableResource(android.R.color.black);
        } else {
            getWindow().setBackgroundDrawableResource(R.color.holo_dark_background);
        }

        if (getAppTheme().equals(AppTheme.DARK)) {
            mDrawerList.setBackgroundColor(ContextCompat.getColor(this, R.color.holo_dark_background));
        } else if (getAppTheme().equals(AppTheme.BLACK)) {
            mDrawerList.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black));
        }
        mDrawerList.setDivider(null);
        if (!isDrawerLocked) {
            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer_l,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            ) {
                public void onDrawerClosed(View view) {
                    mainActivity.onDrawerClosed();
                }

                public void onDrawerOpened(View drawerView) {
                    //title.setText("Amaze File Manager");
                    // creates call to onPrepareOptionsMenu()
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer_l);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            mDrawerToggle.syncState();
        }
        /*findViewById(R.id.drawer_buttton).setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDrawerLayout.isDrawerOpen(mDrawerLinear)) {
                    mDrawerLayout.closeDrawer(mDrawerLinear);
                } else mDrawerLayout.openDrawer(mDrawerLinear);
            }
        });*/
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_drawer_l);
        }
        //recents header color implementation
        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze",
                    ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(),
                    getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            setTaskDescription(taskDescription);
        }



        if (!getPrefs().getBoolean(KEY_PREFERENCE_BOOKMARKS_ADDED, false)) {
            utilsHandler.addCommonBookmarks();
            getPrefs().edit().putBoolean(KEY_PREFERENCE_BOOKMARKS_ADDED, true).commit();
        }

        AppConfig.runInBackground(new AppConfig.CustomAsyncCallbacks() {
            @Override
            public <E> E doInBackground() {

                dataUtils.setHiddenFiles(utilsHandler.getHiddenFilesConcurrentRadixTree());
                dataUtils.setHistory(utilsHandler.getHistoryLinkedList());
                dataUtils.setGridfiles(utilsHandler.getGridViewList());
                dataUtils.setListfiles(utilsHandler.getListViewList());
                dataUtils.setBooks(utilsHandler.getBookmarksList());
                ArrayList<String[]> servers = new ArrayList<String[]>();
                servers.addAll(utilsHandler.getSmbList());
                servers.addAll(utilsHandler.getSftpList());
                dataUtils.setServers(servers);

                return null;
            }

            @Override
            public Void onPostExecute(Object result) {

                refreshDrawer();

                if (savedInstanceState == null) {
                    if (openProcesses) {
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.content_frame, new ProcessViewerFragment(), KEY_INTENT_PROCESS_VIEWER);
                        //transaction.addToBackStack(null);
                        selectedStorage = DRAWER_SELECTED_PROCESSVIEWER;
                        openProcesses = false;
                        //title.setText(utils.getString(con, R.string.process_viewer));
                        //Commit the transaction
                        transaction.commit();
                        supportInvalidateOptionsMenu();
                    }  else if (intent.getAction() != null &&
                            intent.getAction().equals(TileService.ACTION_QS_TILE_PREFERENCES)) {
                        // tile preferences, open ftp fragment

                        FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                        transaction2.replace(R.id.content_frame, new FTPServerFragment());
                        appBarLayout.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();

                        selectedStorage = DRAWER_SELECTED_LASTSECTION;
                        adapter.deselectEverything();
                        transaction2.commit();
                    } else {
                        if (path != null && path.length() > 0) {
                            HybridFile file = new HybridFile(OpenMode.UNKNOWN, path);
                            file.generateMode(MainActivity.this);
                            if (file.isDirectory(MainActivity.this))
                                goToMain(path);
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
                    selectedStorage = savedInstanceState.getInt(KEY_DRAWER_SELECTED, DRAWER_SELECTED_DEFAULT);
                    //mainFragment = (Main) savedInstanceState.getParcelable("main_fragment");
                    adapter.toggleChecked(selectedStorage);
                }
                return null;
            }

            @Override
            public Void onPreExecute() {
                return null;
            }

            @Override
            public Void publishResult(Object... result) {
                return null;
            }

            @Override
            public <T> T[] params() {
                return null;
            }
        });
    }

    /**
     * Checks for the action to take when Amaze receives an intent from external source
     * @param intent
     */
    private void checkForExternalIntent(Intent intent) {

        String actionIntent = intent.getAction();
        String type = intent.getType();

        if (actionIntent != null) {
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
                }

            } else if (actionIntent.equals(Intent.ACTION_SEND) && type != null) {
                // save a single file to filesystem

                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                ArrayList<Uri> uris = new ArrayList<>();
                uris.add(uri);
                initFabToSave(uris);

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
    }

    /**
     * Initializes the floating action button to act as to save data from an external intent
     */
    private void initFabToSave(final ArrayList<Uri> uris) {
        floatingActionButton.removeButton(findViewById(R.id.menu_new_folder));
        floatingActionButton.removeButton(findViewById(R.id.menu_new_file));
        floatingActionButton.removeButton(findViewById(R.id.menu_new_cloud));

        floatingActionButton.setMenuButtonIcon(R.drawable.ic_file_download_white_24dp);
        floatingActionButton.getMenuButton().setOnClickListener(v -> {
            FileUtil.writeUriToStorage(MainActivity.this, uris, getContentResolver(), getCurrentMainFragment().getCurrentPath());
            Toast.makeText(MainActivity.this, getResources().getString(R.string.saving), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    /**
     * Initializes an interactive shell, which will stay throughout the app lifecycle
     * The shell is associated with a handler thread which maintain the message queue from the
     * callbacks of shell as we certainly cannot allow the callbacks to run on same thread because
     * of possible deadlock situation and the asynchronous behaviour of LibSuperSU
     */
    private void initializeInteractiveShell() {
        // only one looper can be associated to a thread. So we are making sure not to create new
        // handler threads every time the code relaunch.
        if (rootMode) {
            handlerThread = new HandlerThread("handler");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            shellInteractive = (new Shell.Builder()).useSU().setHandler(handler).open();

            // TODO: check for busybox
            /*try {
                if (!RootUtils.isBusyboxAvailable()) {
                    Toast.makeText(this, getString(R.string.error_busybox), Toast.LENGTH_LONG).show();
                    closeInteractiveShell();
                    sharedPref.edit().putBoolean(PreferenceUtils.KEY_ROOT, false).apply();
                }
            } catch (ShellNotRunningException e) {
                e.printStackTrace();
                sharedPref.edit().putBoolean(PreferenceUtils.KEY_ROOT, false).apply();
            }*/
        }
    }

    /**
     * Returns all available SD-Cards in the system (include emulated)
     * <p>
     * Warning: Hack! Based on Android source code of version 4.3 (API 18)
     * Because there is no standard way to get it.
     * TODO: Test on future Android versions 4.4+
     *
     * @return paths to all available SD-Cards in the system (include emulated)
     */
    public synchronized ArrayList<String> getStorageDirectories() {
        // Final set of paths
        final ArrayList<String> rv = new ArrayList<>();
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
                rv.add("/storage/sdcard0");
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            if (SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
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
        if (SDK_INT >= Build.VERSION_CODES.M && checkStoragePermission())
            rv.clear();
        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String strings[] = FileUtil.getExtSdCardPathsForActivity(this);
            for (String s : strings) {
                File f = new File(s);
                if (!rv.contains(s) && FileUtils.canListFiles(f))
                    rv.add(s);
            }
        }
        if (ThemedActivity.rootMode)
            rv.add("/");
        File usb = getUsbDrive();
        if (usb != null && !rv.contains(usb.getPath())) rv.add(usb.getPath());

        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isUsbDeviceConnected()) rv.add(OTGUtil.PREFIX_OTG + "/");
        }
        return rv;
    }

    /**
     * Method finds whether a USB device is connected or not
     * @return true if device is connected
     */
    private boolean isUsbDeviceConnected() {
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        if (usbManager.getDeviceList().size()!=0) {
            // we need to set this every time as there is no way to know that whether USB device was
            // disconnected after closing the app and another one was connected
            // in that case the URI will obviously change
            // other wise we could persist the URI even after reopening the app by not writing
            // this preference when it's not null
            getPrefs().edit().putString(KEY_PREF_OTG, VALUE_PREF_OTG_NULL).apply();
            return true;
        } else {
            getPrefs().edit().putString(KEY_PREF_OTG, null).apply();
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isDrawerLocked) {
            if (mDrawerLayout.isDrawerOpen(mDrawerLinear)) {
                mDrawerLayout.closeDrawer(mDrawerLinear);
            } else {
                onbackpressed();
            }
        } else onbackpressed();
    }

    void onbackpressed() {
        Fragment fragment = getFragmentAtFrame();
        if (getAppbar().getSearchView().isShown()) {
            // hide search view if visible, with an animation
            getAppbar().getSearchView().hideSearchView();
        } else if (fragment instanceof TabFragment) {
            if (floatingActionButton.isExpanded()) {
                floatingActionButton.collapse();
            } else {
                getCurrentMainFragment().goBack();
            }
        } else if (fragment instanceof CompressedExplorerFragment) {
            CompressedExplorerFragment compressedExplorerFragment = (CompressedExplorerFragment)  getFragmentAtFrame();
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
                    floatingActionButton.setVisibility(View.VISIBLE);
                    floatingActionButton.getMenuButton().show();
                }
            } else {
                compressedExplorerFragment.mActionMode.finish();
            }
        } else if (fragment instanceof FTPServerFragment) {
            //returning back from FTP server
            if (path != null && path.length() > 0) {
                HybridFile file = new HybridFile(OpenMode.UNKNOWN, path);
                file.generateMode(this);
                if (file.isDirectory(this))
                    goToMain(path);
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

    public void invalidatePasteButton(MenuItem paste) {
        if (pasteHelper != null) {
            paste.setVisible(true);
        } else {
            paste.setVisible(false);
        }
    }

    public void exit() {
        if (backPressedToExitOnce) {
            SshConnectionPool.getInstance().expungeAllConnections();
            finish();
            if (ThemedActivity.rootMode) {
                // TODO close all shells
            }
        } else {
            this.backPressedToExitOnce = true;
            showToast(getString(R.string.pressagain));
            new Handler().postDelayed(() -> {
                backPressedToExitOnce = false;
            }, 2000);
        }
    }

    public void updateDrawer(String path) {
        new AsyncTask<String, Void, Integer>() {
            @Override
            protected Integer doInBackground(String... strings) {
                String path = strings[0];
                int k = 0, i = 0;
                String entryItemPathOld = "";
                for (DrawerItem drawerItem : dataUtils.getList()) {
                    if (drawerItem.type == DrawerItem.ITEM_ENTRY) {

                        String entryItemPath = drawerItem.path;

                        if (path.contains(drawerItem.path)) {

                            if (entryItemPath.length() > entryItemPathOld.length()) {


                                // we don't need to match with the quick search drawer items
                                // whether current entry item path is bigger than the older one found,
                                // for eg. when we have /storage and /storage/Movies as entry items
                                // we would choose to highlight /storage/Movies in drawer adapter
                                k = i;

                                entryItemPathOld = entryItemPath;
                            }
                        }
                    }
                    i++;
                }
                return k;
            }

            @Override
            public void onPostExecute(Integer integers) {
                if (adapter != null)
                    adapter.toggleChecked(integers);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);

    }

    public void goToMain(String path) {
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //title.setText(R.string.app_name);
        TabFragment tabFragment = new TabFragment();
        if (path != null && path.length() > 0) {
            Bundle b = new Bundle();
            b.putString("path", path);
            tabFragment.setArguments(b);
        }
        transaction.replace(R.id.content_frame, tabFragment);
        // Commit the transaction
        selectedStorage = DRAWER_SELECTED_DEFAULT;
        transaction.addToBackStack("tabt" + 1);
        transaction.commitAllowingStateLoss();
        appbar.setTitle(null);
        floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.getMenuButton().show();
        if (openzip && zippath != null) {
            if (zippath.endsWith(".zip") || zippath.endsWith(".apk")) openZip(zippath);
            else {
                openRar(zippath);
            }
            zippath = null;
        }
    }

    public void selectItem(final int i) {
        ArrayList<DrawerItem> directoryDrawerItems = dataUtils.getList();
        switch (directoryDrawerItems.get(i).type) {
            case DrawerItem.ITEM_ENTRY:
                if ((selectedStorage == DRAWER_SELECTED_NONE || selectedStorage >= directoryDrawerItems.size())) {
                    TabFragment tabFragment = new TabFragment();
                    Bundle a = new Bundle();
                    a.putString("path", directoryDrawerItems.get(i).path);

                    tabFragment.setArguments(a);

                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.content_frame, tabFragment);

                    transaction.addToBackStack("tabt1" + 1);
                    pending_fragmentTransaction = transaction;
                    selectedStorage = i;
                    adapter.toggleChecked(selectedStorage);
                    if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
                    else onDrawerClosed();
                    floatingActionButton.setVisibility(View.VISIBLE);
                    floatingActionButton.getMenuButton().show();
                } else {
                    pendingPath = directoryDrawerItems.get(i).path;

                    selectedStorage = i;
                    adapter.toggleChecked(selectedStorage);

                    if (directoryDrawerItems.get(i).path.contains(OTGUtil.PREFIX_OTG) &&
                            getPrefs().getString(KEY_PREF_OTG, null).equals(VALUE_PREF_OTG_NULL)) {
                        // we've not gotten otg path yet
                        // start system request for storage access framework
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.otg_access), Toast.LENGTH_LONG).show();
                        Intent safIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(safIntent, REQUEST_CODE_SAF);
                    } else {
                        if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
                        else onDrawerClosed();
                    }
                }
                break;
            case DrawerItem.ITEM_INTENT:
                directoryDrawerItems.get(i).onClickListener.onClick();
                selectedStorage = i;
                adapter.toggleChecked(selectedStorage);
                break;
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
        MenuItem paste = menu.findItem(R.id.paste);
        Fragment fragment = getFragmentAtFrame();
        if (fragment instanceof TabFragment) {
            appbar.setTitle(R.string.appbar_name);
            if (useGridView) {
                s.setTitle(getResources().getString(R.string.gridview));
            } else {
                s.setTitle(getResources().getString(R.string.listview));
            }
            try {
                TabFragment tabFragment = (TabFragment) fragment;
                MainFragment ma = getCurrentMainFragment();
                if (ma.IS_LIST) s.setTitle(R.string.gridview);
                else s.setTitle(R.string.listview);
                appbar.getBottomBar().updatePath(ma.getCurrentPath(), ma.results,
                        MainActivityHelper.SEARCH_TEXT, ma.openMode, ma.folder_count, ma.file_count, ma);
            } catch (Exception e) {}

            appbar.getBottomBar().setClickListener();

            invalidatePasteButton(paste);
            search.setVisible(true);
            if (indicator_layout != null) indicator_layout.setVisibility(View.VISIBLE);
            menu.findItem(R.id.search).setVisible(true);
            menu.findItem(R.id.home).setVisible(true);
            menu.findItem(R.id.history).setVisible(true);
            menu.findItem(R.id.sethome).setVisible(true);
            menu.findItem(R.id.sort).setVisible(true);
            if (showHidden) menu.findItem(R.id.hiddenitems).setVisible(true);
            menu.findItem(R.id.view).setVisible(true);
            menu.findItem(R.id.extract).setVisible(false);
            invalidatePasteButton(menu.findItem(R.id.paste));
            findViewById(R.id.buttonbarframe).setVisibility(View.VISIBLE);
        } else if (fragment instanceof AppsListFragment || fragment instanceof ProcessViewerFragment
                || fragment instanceof FTPServerFragment) {
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
            } else {
                menu.findItem(R.id.dsort).setVisible(false);
                menu.findItem(R.id.sortby).setVisible(false);
            }
            menu.findItem(R.id.hiddenitems).setVisible(false);
            menu.findItem(R.id.view).setVisible(false);
            menu.findItem(R.id.paste).setVisible(false);
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
            menu.findItem(R.id.paste).setVisible(false);
            menu.findItem(R.id.extract).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    void showToast(String message) {
        if (this.toast == null) {
            // Create toast if found null, it would he the case of first call only
            this.toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else if (this.toast.getView() == null) {
            // Toast not showing, so create new one
            this.toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        } else {
            // Updating toast message is showing
            this.toast.setText(message);
        }

        // Showing toast finally
        this.toast.show();
    }

    void killToast() {
        if (this.toast != null)
            this.toast.cancel();
    }

    // called when the user exits the action mode
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        MainFragment ma = getCurrentMainFragment();

        switch (item.getItemId()) {
            case R.id.home:
                if (ma != null)
                    ma.home();
                break;
            case R.id.history:
                if (ma != null)
                    GeneralDialogCreation.showHistoryDialog(dataUtils, getPrefs(), ma, getAppTheme());
                break;
            case R.id.sethome:
                if (ma == null) return super.onOptionsItemSelected(item);
                final MainFragment main = ma;
                if (main.openMode != OpenMode.FILE && main.openMode != OpenMode.ROOT) {
                    Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
                    break;
                }
                final MaterialDialog dialog = GeneralDialogCreation.showBasicDialog(mainActivity,
                        new String[]{getResources().getString(R.string.questionset),
                                getResources().getString(R.string.setashome), getResources().getString(R.string.yes), getResources().getString(R.string.no), null});
                dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener((v) -> {
                    main.home = main.getCurrentPath();
                    updatePaths(main.no);
                    dialog.dismiss();
                });
                dialog.show();
                break;
            case R.id.exit:
                finish();
                break;
            case R.id.sort:
                Fragment fragment = getFragmentAtFrame();
                if (fragment instanceof AppsListFragment) {
                    GeneralDialogCreation.showSortDialog((AppsListFragment) fragment, getAppTheme());
                }
                break;
            case R.id.sortby:
                if (ma != null)
                    GeneralDialogCreation.showSortDialog(ma, getAppTheme(), getPrefs());
                break;
            case R.id.dsort:
                if (ma == null) return super.onOptionsItemSelected(item);
                String[] sort = getResources().getStringArray(R.array.directorysortmode);
                MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);
                builder.theme(getAppTheme().getMaterialDialogTheme());
                builder.title(R.string.directorysort);
                int current = Integer.parseInt(getPrefs().getString(PreferencesConstants.PREFERENCE_DIRECTORY_SORT_MODE, "0"));

                final MainFragment mainFrag = ma;

                builder.items(sort).itemsCallbackSingleChoice(current, (dialog1, view, which, text) -> {
                    getPrefs().edit().putString(PreferencesConstants.PREFERENCE_DIRECTORY_SORT_MODE, "" + which).commit();
                    mainFrag.getSortModes();
                    mainFrag.updateList();
                    dialog1.dismiss();
                    return true;
                });
                builder.build().show();
                break;
            case R.id.hiddenitems:
                GeneralDialogCreation.showHiddenDialog(dataUtils, getPrefs(), ma, getAppTheme());
                break;
            case R.id.view:
                final MainFragment mainFragment = ma;
                if (ma.IS_LIST) {
                    if (dataUtils.getListfiles().contains(ma.getCurrentPath())) {
                        dataUtils.getListfiles().remove(ma.getCurrentPath());

                        AppConfig.runInBackground(() -> {
                            utilsHandler.removeListViewPath(mainFragment.getCurrentPath());
                        });
                        //grid.removePath(ma.CURRENT_PATH, DataUtils.LIST);
                    }

                    AppConfig.runInBackground(() -> {
                        utilsHandler.addGridView(mainFragment.getCurrentPath());
                    });
                    //grid.addPath(null, ma.CURRENT_PATH, DataUtils.GRID, 0);
                    dataUtils.getGridFiles().add(ma.getCurrentPath());
                } else {
                    if (dataUtils.getGridFiles().contains(ma.getCurrentPath())) {
                        dataUtils.getGridFiles().remove(ma.getCurrentPath());
                        //grid.removePath(ma.CURRENT_PATH, DataUtils.GRID);

                        AppConfig.runInBackground(() -> {
                            utilsHandler.removeGridViewPath(mainFragment.getCurrentPath());
                        });
                    }

                    AppConfig.runInBackground(() -> {
                        utilsHandler.addListView(mainFragment.getCurrentPath());
                    });
                    //grid.addPath(null, ma.CURRENT_PATH, DataUtils.LIST, 0);
                    dataUtils.getListfiles().add(ma.getCurrentPath());
                }
                ma.switchView();
                break;
            case R.id.paste:
                String path = ma.getCurrentPath();
                ArrayList<HybridFileParcelable> arrayList = new ArrayList<>(Arrays.asList(pasteHelper.paths));
                boolean move = pasteHelper.operation == PasteHelper.OPERATION_CUT;
                new PrepareCopyTask(ma, path, move, mainActivity, ThemedActivity.rootMode)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, arrayList);
                pasteHelper = null;
                invalidatePasteButton(item);
                break;
            case R.id.extract:
                Fragment fragment1 = getFragmentAtFrame();
                if (fragment1 instanceof CompressedExplorerFragment) {
                    mainActivityHelper.extractFile(((CompressedExplorerFragment) fragment1).compressedFile);
                }
                break;
            case R.id.search:
                getAppbar().getSearchView().revealSearchView();
                break;
        }
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
        if (mDrawerToggle != null) mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedStorage != DRAWER_SELECTED_NONE)
            outState.putInt(KEY_DRAWER_SELECTED, selectedStorage);
        if(pasteHelper != null) {
            outState.putParcelable(PASTEHELPER_BUNDLE, pasteHelper);
        }

        if (oppathe != null) {
            outState.putString(KEY_OPERATION_PATH, oppathe);
            outState.putString(KEY_OPERATED_ON_PATH, oppathe1);
            outState.putParcelableArrayList(KEY_OPERATIONS_PATH_LIST, (oparrayList));
            outState.putInt(KEY_OPERATION, operation);
        }
        /*if (mainFragment!=null) {
            outState.putParcelable("main_fragment", mainFragment);
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mainActivityHelper.mNotificationReceiver);
        unregisterReceiver(receiver2);

        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            unregisterReceiver(mOtgReceiver);
        }
        killToast();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (materialDialog != null && !materialDialog.isShowing()) {
            materialDialog.show();
            materialDialog = null;
        }

        IntentFilter newFilter = new IntentFilter();
        newFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        newFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        newFilter.addDataScheme(ContentResolver.SCHEME_FILE);
        registerReceiver(mainActivityHelper.mNotificationReceiver, newFilter);
        registerReceiver(receiver2, new IntentFilter(TAG_INTENT_FILTER_GENERAL));

        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Registering intent filter for OTG
            IntentFilter otgFilter = new IntentFilter();
            otgFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            otgFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(mOtgReceiver, otgFilter);
        }

        // TODO: 24/12/2017 this is a hack to fix a glitch when rotating the screen 
        updateViews(new ColorDrawable(MainActivity.currentTab == 1 ?
                getColorPreference().getColor(ColorUsage.PRIMARY):getColorPreference().getColor(ColorUsage.PRIMARY_TWO)));
    }

    /**
     * Receiver to check if a USB device is connected at the runtime of application
     * If device is not connected at runtime (i.e. it was connected when the app was closed)
     * then {@link #isUsbDeviceConnected()} method handles the connection through
     * {@link #getStorageDirectories()}
     */
    BroadcastReceiver mOtgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                getPrefs().edit().putString(KEY_PREF_OTG, VALUE_PREF_OTG_NULL).apply();
                refreshDrawer();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                getPrefs().edit().putString(KEY_PREF_OTG, null).apply();
                refreshDrawer();
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

        tabHandler.close();
        utilsHandler.close();
        cloudHandler.close();

        CryptHandler cryptHandler = new CryptHandler(this);
        cryptHandler.close();
        
        SshConnectionPool.getInstance().expungeAllConnections();

        /*if (mainFragment!=null)
            mainFragment = null;*/
    }

    /**
     * Closes the interactive shell and threads associated
     */
    private void closeInteractiveShell() {
        if (rootMode) {
            // close interactive shell and handler thread associated with it
            if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                // let it finish up first with what it's doing
                handlerThread.quitSafely();
            } else handlerThread.quit();
            shellInteractive.close();
        }
    }

    public void updatePaths(int pos) {
        TabFragment tabFragment = getTabFragment();
        if (tabFragment != null)
            tabFragment.updatepaths(pos);
    }

    public void openZip(String path) {
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

    public void openRar(String path) {
        openZip(path);
    }

    public MainFragment getCurrentMainFragment() {
        TabFragment tab = getTabFragment();

        if(tab != null && tab.getCurrentTabFragment() instanceof MainFragment) {
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
                if (f.exists() && f.getName().toLowerCase().contains("usb") && f.canExecute())
                    return f;
        } catch (Exception e) {}

        parent = new File("/mnt/sdcard/usbStorage");
        if (parent.exists() && parent.canExecute())
            return parent;
        parent = new File("/mnt/sdcard/usb_storage");
        if (parent.exists() && parent.canExecute())
            return parent;

        return null;
    }
    
    public void refreshDrawer() {

        ArrayList<DrawerItem> sectionDrawerItems = new ArrayList<>();
        ArrayList<String> storageDirectories = getStorageDirectories();
        storage_count = 0;
        for (String file : storageDirectories) {
            File f = new File(file);
            String name;
            Drawable icon1 = ContextCompat.getDrawable(this, R.drawable.ic_sd_storage_white_24dp);
            if ("/storage/emulated/legacy".equals(file) || "/storage/emulated/0".equals(file)) {
                name = getResources().getString(R.string.storage);
            } else if ("/storage/sdcard1".equals(file)) {
                name = getResources().getString(R.string.extstorage);
            } else if ("/".equals(file)) {
                name = getResources().getString(R.string.rootdirectory);
                icon1 = ContextCompat.getDrawable(this, R.drawable.ic_drawer_root_white);
            } else if (file.contains(OTGUtil.PREFIX_OTG)) {
                name = "OTG";
                icon1 = ContextCompat.getDrawable(this, R.drawable.ic_usb_white_24dp);
            } else name = f.getName();
            if (!f.isDirectory() || f.canExecute()) {
                storage_count++;
                sectionDrawerItems.add(new DrawerItem(name, file, icon1));
            }
        }
        dataUtils.setStorages(storageDirectories);
        sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));

        if (dataUtils.getServers().size() > 0) {
            Collections.sort(dataUtils.getServers(), new BookSorter());
            synchronized (dataUtils.getServers()) {
                for (String[] file : dataUtils.getServers()) {
                    sectionDrawerItems.add(new DrawerItem(file[0], file[1], ContextCompat.getDrawable(this,
                            (file[1].startsWith(SshConnectionPool.SSH_URI_PREFIX)) ?
                                    R.drawable.ic_linux_grey600_24dp : R.drawable.ic_settings_remote_white_24dp)));
                }
            }
            sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));
        }

        ArrayList<String[]> accountAuthenticationList = new ArrayList<>();

        if (CloudSheetFragment.isCloudProviderAvailable(this)) {
            for (CloudStorage cloudStorage : dataUtils.getAccounts()) {
                if (cloudStorage instanceof Dropbox) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_DROPBOX,
                            CloudHandler.CLOUD_PREFIX_DROPBOX + "/",
                            ContextCompat.getDrawable(this, R.drawable.ic_dropbox_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_DROPBOX,
                            CloudHandler.CLOUD_PREFIX_DROPBOX + "/",
                    });
                } else if (cloudStorage instanceof Box) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_BOX,
                            CloudHandler.CLOUD_PREFIX_BOX + "/",
                            ContextCompat.getDrawable(this, R.drawable.ic_box_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_BOX,
                            CloudHandler.CLOUD_PREFIX_BOX + "/",
                    });
                } else if (cloudStorage instanceof OneDrive) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_ONE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/",
                            ContextCompat.getDrawable(this, R.drawable.ic_onedrive_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_ONE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/",
                    });
                } else if (cloudStorage instanceof GoogleDrive) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_GOOGLE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/",
                            ContextCompat.getDrawable(this, R.drawable.ic_google_drive_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_GOOGLE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/",
                    });
                }
            }
            Collections.sort(accountAuthenticationList, new BookSorter());

            if (accountAuthenticationList.size() != 0)
                sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));
        }

        if (getPrefs().getBoolean(PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS, true)) {
            if (dataUtils.getBooks().size() > 0) {

                Collections.sort(dataUtils.getBooks(), new BookSorter());

                synchronized (dataUtils.getBooks()) {
                    for (String[] file : dataUtils.getBooks()) {
                        sectionDrawerItems.add(new DrawerItem(file[0], file[1],
                                ContextCompat.getDrawable(this, R.drawable.ic_folder_white_24dp)));
                    }
                }
                sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));
            }
        }

        Boolean[] quickAccessPref = TinyDB.getBooleanArray(getPrefs(), QuickAccessPref.KEY,
                QuickAccessPref.DEFAULT);

        if (getPrefs().getBoolean(PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES, true)) {
            if (quickAccessPref[0])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.quick), "5",
                        ContextCompat.getDrawable(this, R.drawable.ic_star_white_24dp)));
            if (quickAccessPref[1])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.recent), "6",
                        ContextCompat.getDrawable(this, R.drawable.ic_history_white_24dp)));
            if (quickAccessPref[2])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.images), "0",
                        ContextCompat.getDrawable(this, R.drawable.ic_photo_library_white_24dp)));
            if (quickAccessPref[3])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.videos), "1",
                        ContextCompat.getDrawable(this, R.drawable.ic_video_library_white_24dp)));
            if (quickAccessPref[4])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.audio), "2",
                        ContextCompat.getDrawable(this, R.drawable.ic_library_music_white_24dp)));
            if (quickAccessPref[5])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.documents), "3",
                        ContextCompat.getDrawable(this, R.drawable.ic_library_books_white_24dp)));
            if (quickAccessPref[6])
                sectionDrawerItems.add(new DrawerItem(getResources().getString(R.string.apks), "4",
                        ContextCompat.getDrawable(this, R.drawable.ic_apk_library_white_24dp)));
        } else {
            sectionDrawerItems.remove(sectionDrawerItems.size() - 1); //Deletes last divider
        }

        sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));

        sectionDrawerItems.add(new DrawerItem(getString(R.string.ftp),
                ContextCompat.getDrawable(this, R.drawable.ic_ftp_white_24dp), () -> {
                    FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                    transaction2.replace(R.id.content_frame, new FTPServerFragment());
                    appBarLayout.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
                    pending_fragmentTransaction = transaction2;
                    if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
                    else onDrawerClosed();
        }));
        sectionDrawerItems.add(new DrawerItem(getString(R.string.apps),
                ContextCompat.getDrawable(this, R.drawable.ic_android_white_24dp), () -> {
                    FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                    transaction2.replace(R.id.content_frame, new AppsListFragment());
                    appBarLayout.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
                    pending_fragmentTransaction = transaction2;
                    if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
                    else onDrawerClosed();
        }));
        sectionDrawerItems.add(new DrawerItem(getString(R.string.setting),
                ContextCompat.getDrawable(this, R.drawable.ic_settings_white_24dp), () -> {
                    Intent in = new Intent(MainActivity.this, PreferencesActivity.class);
                    startActivity(in);
                    finish();
        }));

        dataUtils.setList(sectionDrawerItems);

        adapter = new DrawerAdapter(this, this, sectionDrawerItems, this);
        mDrawerList.setAdapter(adapter);
    }

    public AppBar getAppbar() {
        return appbar;
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == image_selector_request_code) {
            if (getPrefs() != null && intent != null && intent.getData() != null) {
                if (SDK_INT >= Build.VERSION_CODES.KITKAT)
                    getContentResolver().takePersistableUriPermission(intent.getData(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getPrefs().edit().putString(PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH,
                        intent.getData().toString()).commit();
                setDrawerHeaderBackground();
            }
        } else if (requestCode == 3) {
            Uri treeUri;
            if (responseCode == Activity.RESULT_OK) {
                // Get Uri from Storage Access Framework.
                treeUri = intent.getData();
                // Persist URI - this is required for verification of writability.
                if (treeUri != null) getPrefs().edit().putString(PreferencesConstants.PREFERENCE_URI,
                        treeUri.toString()).commit();
            } else {
                // If not confirmed SAF, or if still not writable, then revert settings.
                /* DialogUtil.displayError(getActivity(), R.string.message_dialog_cannot_write_to_folder_saf, false, currentFolder);
                        ||!FileUtil.isWritableNormalOrSaf(currentFolder)*/
                return;
            }

            // After confirmation, update stored value of folder.
            // Persist access permissions.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            switch (operation) {
                case DataUtils.DELETE://deletion
                    new DeleteTask(null, mainActivity).execute((oparrayList));
                    break;
                case DataUtils.COPY://copying
                    //legacy compatibility
                    if(oparrayList != null && oparrayList.size() != 0) {
                        oparrayListList = new ArrayList<>();
                        oparrayListList.add(oparrayList);
                        oparrayList = null;
                        oppatheList = new ArrayList<>();
                        oppatheList.add(oppathe);
                        oppathe = "";
                    }
                    for (int i = 0; i < oparrayListList.size(); i++) {
                        ArrayList<HybridFileParcelable> sourceList = oparrayListList.get(i);
                        Intent intent1 = new Intent(con, CopyService.class);
                        intent1.putExtra(CopyService.TAG_COPY_SOURCES, sourceList);
                        intent1.putExtra(CopyService.TAG_COPY_TARGET, oppatheList.get(i));
                        ServiceWatcherUtil.runService(this, intent1);
                    }
                    break;
                case DataUtils.MOVE://moving
                    //legacy compatibility
                    if(oparrayList != null && oparrayList.size() != 0) {
                        oparrayListList = new ArrayList<>();
                        oparrayListList.add(oparrayList);
                        oparrayList = null;
                        oppatheList = new ArrayList<>();
                        oppatheList.add(oppathe);
                        oppathe = "";
                    }

                    new MoveFiles(oparrayListList, getCurrentMainFragment(),
                            getCurrentMainFragment().getActivity(), OpenMode.FILE)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, oppatheList);
                    break;
                case DataUtils.NEW_FOLDER://mkdir
                    mainActivityHelper.mkDir(RootHelper.generateBaseFile(new File(oppathe), true),
                            getCurrentMainFragment());
                    break;
                case DataUtils.RENAME:
                    MainFragment ma = getCurrentMainFragment();
                    mainActivityHelper.rename(ma.openMode, (oppathe),
                            (oppathe1), mainActivity, ThemedActivity.rootMode);
                    ma.updateList();
                    break;
                case DataUtils.NEW_FILE:
                    mainActivityHelper.mkFile(new HybridFile(OpenMode.FILE, oppathe), getCurrentMainFragment());

                    break;
                case DataUtils.EXTRACT:
                    mainActivityHelper.extractFile(new File(oppathe));
                    break;
                case DataUtils.COMPRESS:
                    mainActivityHelper.compressFiles(new File(oppathe), oparrayList);
            }
            operation = -1;
        } else if (requestCode == REQUEST_CODE_SAF && responseCode == Activity.RESULT_OK) {
            // otg access
            getPrefs().edit().putString(KEY_PREF_OTG, intent.getData().toString()).apply();

            if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
            else onDrawerClosed();
        } else if (requestCode == REQUEST_CODE_SAF && responseCode != Activity.RESULT_OK) {
            // otg access not provided
            pendingPath = null;
        }
    }

    void initialisePreferences() {
        hidemode = getPrefs().getInt(PreferencesConstants.PREFERENCE_HIDEMODE, 0);
        showHidden = getPrefs().getBoolean(PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES, false);
        useGridView = getPrefs().getBoolean(PreferencesConstants.PREFERENCE_VIEW, true);
        currentTab = getPrefs().getInt(PreferenceUtils.KEY_CURRENT_TAB, PreferenceUtils.DEFAULT_CURRENT_TAB);
        skinStatusBar = (PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));
        colourednavigation = getPrefs().getBoolean(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION, false);
    }

    void initialiseViews() {
        appBarLayout = getAppbar().getAppbarLayout();

        mScreenLayout = findViewById(R.id.main_frame);
        buttonBarFrame = findViewById(R.id.buttonbarframe);

        //buttonBarFrame.setBackgroundColor(Color.parseColor(currentTab==1 ? skinTwo : skin));
        drawerHeaderLayout = getLayoutInflater().inflate(R.layout.drawerheader, null);
        drawerHeaderParent = (RelativeLayout) drawerHeaderLayout.findViewById(R.id.drawer_header_parent);
        drawerHeaderView = drawerHeaderLayout.findViewById(R.id.drawer_header);
        drawerHeaderView.setOnLongClickListener(v -> {
            Intent intent1;
            if (SDK_INT < Build.VERSION_CODES.KITKAT) {
                intent1 = new Intent();
                intent1.setAction(Intent.ACTION_GET_CONTENT);
            } else {
                intent1 = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            }
            intent1.addCategory(Intent.CATEGORY_OPENABLE);
            intent1.setType("image/*");
            startActivityForResult(intent1, image_selector_request_code);
            return false;
        });
        setSupportActionBar(getAppbar().getToolbar());
        frameLayout = findViewById(R.id.content_frame);
        indicator_layout = findViewById(R.id.indicator_layout);
        mDrawerLinear = findViewById(R.id.left_drawer);
        if (getAppTheme().equals(AppTheme.DARK)) mDrawerLinear.setBackgroundColor(Utils.getColor(this, R.color.holo_dark_background));
        else if (getAppTheme().equals(AppTheme.BLACK)) mDrawerLinear.setBackgroundColor(Utils.getColor(this, android.R.color.black));
        else mDrawerLinear.setBackgroundColor(Color.WHITE);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        //mDrawerLayout.setStatusBarBackgroundColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));
        mDrawerList = findViewById(R.id.menu_drawer);
        drawerHeaderView.setBackgroundResource(R.drawable.amaze_header);
        //drawerHeaderParent.setBackgroundColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));

        if (findViewById(R.id.tab_frame) != null) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerLinear);
            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
            mDrawerLayout.post(() -> mDrawerLayout.openDrawer(mDrawerLinear));
            isDrawerLocked = true;
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLinear);
            mDrawerLayout.post(() -> mDrawerLayout.closeDrawer(mDrawerLinear));
            isDrawerLocked = false;
        }

        mDrawerList.addHeaderView(drawerHeaderLayout);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        fabBgView = findViewById(R.id.fab_bg);
        if (getAppTheme().equals(AppTheme.DARK) || getAppTheme().equals(AppTheme.BLACK)) {
            fabBgView.setBackgroundResource(R.drawable.fab_shadow_dark);
        }

        fabBgView.setOnClickListener(view -> {
            if (getAppbar().getSearchView().isEnabled()) getAppbar().getSearchView().hideSearchView();
        });

        setDrawerHeaderBackground();
        //getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor((currentTab==1 ? skinTwo : skin))));

        // status bar0
        if (SDK_INT == Build.VERSION_CODES.KITKAT_WATCH || SDK_INT == Build.VERSION_CODES.KITKAT) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            //tintManager.setStatusBarTintColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.drawer_layout).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            if (!isDrawerLocked) p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (isDrawerLocked) {
                window.setStatusBarColor((skinStatusBar));
            } else window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (colourednavigation)
                window.setNavigationBarColor(skinStatusBar);
        }
    }

    /**
     * Call this method when you need to update the MainActivity view components' colors based on
     * update in the {@link MainActivity#currentTab}
     * Warning - All the variables should be initialised before calling this method!
     */
    public void updateViews(ColorDrawable colorDrawable) {
        // appbar view color
        mainActivity.buttonBarFrame.setBackgroundColor(colorDrawable.getColor());
        // action bar color
        mainActivity.getSupportActionBar().setBackgroundDrawable(colorDrawable);
        // drawer status bar I guess
        mainActivity.mDrawerLayout.setStatusBarBackgroundColor(colorDrawable.getColor());
        // drawer header background
        mainActivity.drawerHeaderParent.setBackgroundColor(colorDrawable.getColor());

        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // for lollipop devices, the status bar color
            mainActivity.getWindow().setStatusBarColor(colorDrawable.getColor());
            if (colourednavigation)
                mainActivity.getWindow().setNavigationBarColor(PreferenceUtils
                        .getStatusColor(colorDrawable.getColor()));
        } else if (SDK_INT == Build.VERSION_CODES.KITKAT_WATCH || SDK_INT == Build.VERSION_CODES.KITKAT) {

            // for kitkat devices, the status bar color
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(colorDrawable.getColor());
        }
    }

    void initialiseFab() {
        int colorAccent = getColorPreference().getColor(ColorUsage.ACCENT);

        floatingActionButton = findViewById(R.id.fabs_menu);
        floatingActionButton.getMenuButton().setBackgroundColor(colorAccent);
        floatingActionButton.getMenuButton().setRippleColor(Utils.getColor(this, R.color.white_translucent));
        floatingActionButton.setAnimationDuration(500);
        floatingActionButton.setMenuListener(new FABsMenuListener() {
            @Override
            public void onMenuExpanded(FABsMenu fabsMenu) {
                showSmokeScreen();
            }

            @Override
            public void onMenuCollapsed(FABsMenu fabsMenu) {
                hideSmokeScreen();
            }
        });

        floatingActionButton.setMenuListener(new FABsMenuListener() {
            @Override
            public void onMenuExpanded(FABsMenu fabsMenu) {
                FileUtils.revealShow(fabBgView, true);
            }

            @Override
            public void onMenuCollapsed(FABsMenu fabsMenu) {
                FileUtils.revealShow(fabBgView, false);
            }
        });

        initFabTitle(findViewById(R.id.menu_new_folder), MainActivityHelper.NEW_FOLDER);
        initFabTitle(findViewById(R.id.menu_new_file), MainActivityHelper.NEW_FILE);
        initFabTitle(findViewById(R.id.menu_new_cloud), MainActivityHelper.NEW_CLOUD);
    }

    private void initFabTitle(TitleFAB fabTitle, int type) {
        int iconSkin = getColorPreference().getColor(ColorUsage.ICON_SKIN);

        fabTitle.setBackgroundColor(iconSkin);
        fabTitle.setRippleColor(Utils.getColor(this, R.color.white_translucent));
        fabTitle.setOnClickListener(view -> {
            mainActivityHelper.add(type);
            floatingActionButton.collapse();
        });

        if(getAppTheme().getSimpleTheme() == AppTheme.DARK) {
            fabTitle.setTitleBackgroundColor(Utils.getColor(this, R.color.holo_dark_background));
            fabTitle.setTitleTextColor(Utils.getColor(this, R.color.text_dark));
        }
    }

    public boolean copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("Path copied to clipboard", text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void renameBookmark(final String title, final String path) {
        if (dataUtils.containsBooks(new String[]{title, path}) != -1) {
            RenameBookmark renameBookmark = RenameBookmark.getInstance(title, path, getColorPreference().getColor(ColorUsage.ACCENT));
            if (renameBookmark != null)
                renameBookmark.show(getFragmentManager(), "renamedialog");
        }
    }

    void onDrawerClosed() {
        if (pending_fragmentTransaction != null) {
            pending_fragmentTransaction.commit();
            pending_fragmentTransaction = null;
        }

        if (pendingPath != null) {
            HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, pendingPath);
            hFile.generateMode(this);
            if (hFile.isSimpleFile()) {
                FileUtils.openFile(new File(pendingPath), mainActivity, getPrefs());
                pendingPath = null;
                return;
            }

            MainFragment mainFrag = getCurrentMainFragment();
            if (mainFrag != null) {
                mainFrag.loadlist(pendingPath, false, OpenMode.UNKNOWN);
            } else {
                goToMain(pendingPath);
                return;
            }
            pendingPath = null;
        }
        supportInvalidateOptionsMenu();
    }

    public PasteHelper getPaste() {
        return pasteHelper;
    }

    public void setPaste(PasteHelper p) {
        pasteHelper = p;
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onNewIntent(Intent i) {
        intent = i;
        path = i.getStringExtra("path");

        if (path != null) {
            if (new File(path).isDirectory()) {
                MainFragment ma = getCurrentMainFragment();
                if (ma != null) {
                    ma.loadlist(path, false, OpenMode.FILE);
                } else goToMain(path);
            } else FileUtils.openFile(new File(path), mainActivity, getPrefs());
        } else if (i.getStringArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS) != null) {
            ArrayList<HybridFileParcelable> failedOps = i.getParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS);
            if (failedOps != null) {
                mainActivityHelper.showFailedOperationDialog(failedOps, this);
            }
        } else if (i.getCategories() != null && i.getCategories().contains(CLOUD_AUTHENTICATOR_GDRIVE)) {

            // we used an external authenticator instead of APIs. Probably for Google Drive
            CloudRail.setAuthenticationResponse(intent);

        } else if ((openProcesses = i.getBooleanExtra(KEY_INTENT_PROCESS_VIEWER, false))) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, new ProcessViewerFragment(), KEY_INTENT_PROCESS_VIEWER);
            //   transaction.addToBackStack(null);
            selectedStorage = DRAWER_SELECTED_PROCESSVIEWER;
            openProcesses = false;
            //title.setText(utils.getString(con, R.string.process_viewer));
            //Commit the transaction
            transaction.commitAllowingStateLoss();
            supportInvalidateOptionsMenu();
        } else if (intent.getAction() != null) {

            checkForExternalIntent(intent);

            if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    if (getPrefs().getString(KEY_PREF_OTG, null) == null) {
                        getPrefs().edit().putString(KEY_PREF_OTG, VALUE_PREF_OTG_NULL).apply();
                        refreshDrawer();
                    }
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    getPrefs().edit().putString(KEY_PREF_OTG, null).apply();
                    refreshDrawer();
                }
            }
        }
    }

    void setDrawerHeaderBackground() {
        String path1 = getPrefs().getString(PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH,
                null);
        if (path1 == null) return;
        try {
            final ImageView headerImageView = new ImageView(MainActivity.this);
            headerImageView.setImageDrawable(drawerHeaderParent.getBackground());
            mImageLoader.get(path1, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    headerImageView.setImageBitmap(response.getBitmap());
                    drawerHeaderView.setBackgroundResource(R.drawable.amaze_header_2);
                }

                @Override
                public void onErrorResponse(VolleyError error) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver receiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            if (i.getStringArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS) != null) {
                ArrayList<HybridFileParcelable> failedOps = i.getParcelableArrayListExtra(TAG_INTENT_FILTER_FAILED_OPS);
                if (failedOps != null) {
                    mainActivityHelper.showFailedOperationDialog(failedOps, mainActivity);
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == 77) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshDrawer();
                TabFragment tabFragment = getTabFragment();
                boolean b = getPrefs().getBoolean(PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME, true);
                //reset home and current paths according to new storages
                if (b) {
                    tabHandler.clear();
                    if (storage_count > 1)
                        tabHandler.addTab(new Tab(1, "", dataUtils.getList().get(1).path, "/"));
                    else
                        tabHandler.addTab(new Tab(1, "", "/", "/"));
                    if (!dataUtils.getList().get(0).isSection()) {
                        String pa = dataUtils.getList().get(0).path;
                        tabHandler.addTab(new Tab(2, "", pa, pa));
                    } else
                        tabHandler.addTab(new Tab(2, "", dataUtils.getList().get(1).path, "/"));
                    if (tabFragment != null) {
                        Fragment main = tabFragment.getFragmentAtIndex(0);
                        if (main != null)
                            ((MainFragment) main).updateTabWithDb(tabHandler.findTab(1));
                        Fragment main1 = tabFragment.getFragmentAtIndex(1);
                        if (main1 != null)
                            ((MainFragment) main1).updateTabWithDb(tabHandler.findTab(2));
                    }
                    getPrefs().edit().putBoolean(PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME, false).commit();
                } else {
                    //just refresh list
                    if (tabFragment != null) {
                        Fragment main = tabFragment.getFragmentAtIndex(0);
                        if (main != null)
                            ((MainFragment) main).updateList();
                        Fragment main1 = tabFragment.getFragmentAtIndex(1);
                        if (main1 != null)
                            ((MainFragment) main1).updateList();
                    }
                }
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show();
                requestStoragePermission();
            }
        }
    }

    public void showSMBDialog(String name, String path, boolean edit) {
        if (path.length() > 0 && name.length() == 0) {
            int i = dataUtils.containsServer(new String[]{name, path});
            if (i != -1)
                name = dataUtils.getServers().get(i)[0];
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
            int i = dataUtils.containsServer(new String[]{name, path});
            if (i != -1)
                name = dataUtils.getServers().get(i)[0];
        }
        SftpConnectDialog sftpConnectDialog = new SftpConnectDialog();
        Uri uri = Uri.parse(path);
        String userinfo = uri.getUserInfo();
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        bundle.putString("address", uri.getHost());
        bundle.putString("port", Integer.toString(uri.getPort()));
        bundle.putString("path", path);
        bundle.putString("username", userinfo.indexOf(':') > 0 ?
                userinfo.substring(0, userinfo.indexOf(':')) : userinfo);

        if(userinfo.indexOf(':') < 0) {
            bundle.putBoolean("hasPassword", false);
            bundle.putString("keypairName", utilsHandler.getSshAuthPrivateKeyName(path));
        } else {
            bundle.putBoolean("hasPassword", true);
            bundle.putString("password", userinfo.substring(userinfo.indexOf(':')+1));
        }
        bundle.putBoolean("edit", edit);
        sftpConnectDialog.setArguments(bundle);
        sftpConnectDialog.show(getFragmentManager(), "sftpdialog");
    }

    /**
     * Shows a view that goes from white at it's lowest part to transparent a the top.
     * It covers the fragment.
     */
    public void showSmokeScreen() {
        FileUtils.revealShow(fabBgView, true);
    }

    public void hideSmokeScreen() {
        FileUtils.revealShow(fabBgView, false);
    }

    @Override
    public void addConnection(boolean edit, final String name, final String path, final String encryptedPath,
                              final String oldname, final String oldPath) {

        String[] s = new String[]{name, path};
        if (!edit) {
            if ((dataUtils.containsServer(path)) == -1) {
                dataUtils.addServer(s);
                refreshDrawer();

                AppConfig.runInBackground(() -> {
                    utilsHandler.addSmb(name, encryptedPath);
                });
                //grid.addPath(name, encryptedPath, DataUtils.SMB, 1);
                MainFragment ma = getCurrentMainFragment();
                if (ma != null) getCurrentMainFragment().loadlist(path, false, OpenMode.UNKNOWN);
            } else {
                Snackbar.make(frameLayout, getResources().getString(R.string.connection_exists), Snackbar.LENGTH_SHORT).show();
            }
        } else {
            int i = dataUtils.containsServer(new String[]{oldname, oldPath});
            if (i != -1) {
                dataUtils.removeServer(i);

                AppConfig.runInBackground(() -> {
                    utilsHandler.renameSMB(oldname, oldPath, name, path);
                });
                //mainActivity.grid.removePath(oldname, oldPath, DataUtils.SMB);
            }
            dataUtils.addServer(s);
            Collections.sort(dataUtils.getServers(), new BookSorter());
            mainActivity.refreshDrawer();
            //mainActivity.grid.addPath(name, encryptedPath, DataUtils.SMB, 1);
        }
    }

    @Override
    public void deleteConnection(final String name, final String path) {

        int i = dataUtils.containsServer(new String[]{name, path});
        if (i != -1) {
            dataUtils.removeServer(i);

            AppConfig.runInBackground(() -> {
                utilsHandler.removeSmbPath(name, path);
            });
            //grid.removePath(name, path, DataUtils.SMB);
            refreshDrawer();
        }

    }

    @Override
    public void onHiddenFileAdded(String path) {

        utilsHandler.addHidden(path);
    }

    @Override
    public void onHiddenFileRemoved(String path) {

        utilsHandler.removeHiddenPath(path);
    }

    @Override
    public void onHistoryAdded(String path) {

        utilsHandler.addHistory(path);
    }

    @Override
    public void onBookAdded(String[] path, boolean refreshdrawer) {

        utilsHandler.addBookmark(path[0], path[1]);
        if (refreshdrawer)
            refreshDrawer();
    }

    @Override
    public void onHistoryCleared() {

        utilsHandler.clearHistoryTable();
    }

    @Override
    public void delete(String title, String path) {

        utilsHandler.removeBookmarksPath(title, path);
        refreshDrawer();

    }

    @Override
    public void modify(String oldpath, String oldname, String newPath, String newname) {

        utilsHandler.renameBookmark(oldname, oldpath, newname, newPath);
        refreshDrawer();
    }

    @Override
    public void onPreExecute(String query) {
        mainFragment.mSwipeRefreshLayout.setRefreshing(true);
        mainFragment.onSearchPreExecute(query);
    }

    @Override
    public void onPostExecute(String query) {
        mainFragment.onSearchCompleted(query);
        mainFragment.mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onProgressUpdate(HybridFileParcelable val , String query) {
        mainFragment.addSearchResult(val,query);
    }

    @Override
    public void onCancelled() {
        mainFragment.reloadListElements(false, false, !mainFragment.IS_LIST);
        mainFragment.mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void addConnection(OpenMode service) {

        try {
            if (cloudHandler.findEntry(service) != null) {
                // cloud entry already exists
                Toast.makeText(this, getResources().getString(R.string.connection_exists),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.please_wait), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, getResources().getString(R.string.cloud_error_plugin),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void deleteConnection(OpenMode service) {

        cloudHandler.clear(service);
        dataUtils.removeAccount(service);

        runOnUiThread(this::refreshDrawer);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (cloudSyncTask != null && cloudSyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            cloudSyncTask.cancel(true);

        }

        Uri uri = Uri.withAppendedPath(Uri.parse("content://" + CloudContract.PROVIDER_AUTHORITY), "/keys.db/secret_keys");

        String[] projection = new String[] {
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
                    for (int i=1; i<=cloudEntries.size(); i++) {

                        // we need to get only those cloud details which user wants
                        switch (cloudEntries.get(i-1).getServiceType()) {
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

                    Toast.makeText(this, getResources().getString(R.string.cloud_error_plugin),
                            Toast.LENGTH_LONG).show();
                }
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {

        if (data == null) {
            Toast.makeText(this, getResources().getString(R.string.cloud_error_failed_restart),
                    Toast.LENGTH_LONG).show();
            return;
        }

        cloudSyncTask = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {

                if (data.getCount() > 0 && data.moveToFirst()) {
                    do {

                        switch (data.getInt(0)) {
                            case 1:
                                try {
                                    CloudRail.setAppKey(data.getString(1));
                                } catch (Exception e) {
                                    // any other exception due to network conditions or other error
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.failed_cloud_api_key));
                                    return false;
                                }
                                break;
                            case 2:
                                // DRIVE
                                try {

                                    CloudEntry cloudEntryGdrive = null;
                                    CloudEntry savedCloudEntryGdrive;


                                    GoogleDrive cloudStorageDrive = new GoogleDrive(getApplicationContext(),
                                            data.getString(1), "", CLOUD_AUTHENTICATOR_REDIRECT_URI, data.getString(2));
                                    cloudStorageDrive.useAdvancedAuthentication();

                                    if ((savedCloudEntryGdrive = cloudHandler.findEntry(OpenMode.GDRIVE)) != null) {
                                        // we already have the entry and saved state, get it

                                        try {
                                            cloudStorageDrive.loadAsString(savedCloudEntryGdrive.getPersistData());
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            // we need to update the persist string as existing one is been compromised

                                            cloudStorageDrive.login();
                                            cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                                            cloudHandler.updateEntry(OpenMode.GDRIVE, cloudEntryGdrive);
                                        }

                                    } else {

                                        cloudStorageDrive.login();
                                        cloudEntryGdrive = new CloudEntry(OpenMode.GDRIVE, cloudStorageDrive.saveAsString());
                                        cloudHandler.addEntry(cloudEntryGdrive);
                                    }

                                    dataUtils.addAccount(cloudStorageDrive);
                                } catch (CloudPluginException e) {

                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_error_plugin));
                                    deleteConnection(OpenMode.GDRIVE);
                                    return false;
                                } catch (AuthenticationException e) {
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_fail_authenticate));
                                    deleteConnection(OpenMode.GDRIVE);
                                    return false;
                                } catch (Exception e) {
                                    // any other exception due to network conditions or other error
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.failed_cloud_new_connection));
                                    deleteConnection(OpenMode.GDRIVE);
                                    return false;
                                }
                                break;
                            case 3:
                                // DROPBOX
                                try {

                                    CloudEntry cloudEntryDropbox = null;
                                    CloudEntry savedCloudEntryDropbox;

                                    CloudStorage cloudStorageDropbox = new Dropbox(getApplicationContext(),
                                            data.getString(1), data.getString(2));

                                    if ((savedCloudEntryDropbox = cloudHandler.findEntry(OpenMode.DROPBOX)) != null) {
                                        // we already have the entry and saved state, get it

                                        try {
                                            cloudStorageDropbox.loadAsString(savedCloudEntryDropbox.getPersistData());
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            // we need to persist data again

                                            cloudStorageDropbox.login();
                                            cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                                            cloudHandler.updateEntry(OpenMode.DROPBOX, cloudEntryDropbox);
                                        }

                                    } else {

                                        cloudStorageDropbox.login();
                                        cloudEntryDropbox = new CloudEntry(OpenMode.DROPBOX, cloudStorageDropbox.saveAsString());
                                        cloudHandler.addEntry(cloudEntryDropbox);
                                    }

                                    dataUtils.addAccount(cloudStorageDropbox);
                                } catch (CloudPluginException e) {
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_error_plugin));
                                    deleteConnection(OpenMode.DROPBOX);
                                    return false;
                                } catch (AuthenticationException e) {
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_fail_authenticate));
                                    deleteConnection(OpenMode.DROPBOX);
                                    return false;
                                } catch (Exception e) {
                                    // any other exception due to network conditions or other error
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.failed_cloud_new_connection));
                                    deleteConnection(OpenMode.DROPBOX);
                                    return false;
                                }
                                break;
                            case 4:
                                // BOX
                                try {

                                    CloudEntry cloudEntryBox = null;
                                    CloudEntry savedCloudEntryBox;

                                    CloudStorage cloudStorageBox = new Box(getApplicationContext(),
                                            data.getString(1), data.getString(2));

                                    if ((savedCloudEntryBox = cloudHandler.findEntry(OpenMode.BOX)) != null) {
                                        // we already have the entry and saved state, get it

                                        try {
                                            cloudStorageBox.loadAsString(savedCloudEntryBox.getPersistData());
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            // we need to persist data again

                                            cloudStorageBox.login();
                                            cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                                            cloudHandler.updateEntry(OpenMode.BOX, cloudEntryBox);
                                        }

                                    } else {

                                        cloudStorageBox.login();
                                        cloudEntryBox = new CloudEntry(OpenMode.BOX, cloudStorageBox.saveAsString());
                                        cloudHandler.addEntry(cloudEntryBox);
                                    }

                                    dataUtils.addAccount(cloudStorageBox);
                                } catch (CloudPluginException e) {

                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_error_plugin));
                                    deleteConnection(OpenMode.BOX);
                                    return false;
                                } catch (AuthenticationException e) {
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_fail_authenticate));
                                    deleteConnection(OpenMode.BOX);
                                    return false;
                                } catch (Exception e) {
                                    // any other exception due to network conditions or other error
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.failed_cloud_new_connection));
                                    deleteConnection(OpenMode.BOX);
                                    return false;
                                }
                                break;
                            case 5:
                                // ONEDRIVE
                                try {

                                    CloudEntry cloudEntryOnedrive = null;
                                    CloudEntry savedCloudEntryOnedrive;

                                    CloudStorage cloudStorageOnedrive = new OneDrive(getApplicationContext(),
                                            data.getString(1), data.getString(2));

                                    if ((savedCloudEntryOnedrive = cloudHandler.findEntry(OpenMode.ONEDRIVE)) != null) {
                                        // we already have the entry and saved state, get it

                                        try {
                                            cloudStorageOnedrive.loadAsString(savedCloudEntryOnedrive.getPersistData());
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            // we need to persist data again

                                            cloudStorageOnedrive.login();
                                            cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                                            cloudHandler.updateEntry(OpenMode.ONEDRIVE, cloudEntryOnedrive);
                                        }

                                    } else {

                                        cloudStorageOnedrive.login();
                                        cloudEntryOnedrive = new CloudEntry(OpenMode.ONEDRIVE, cloudStorageOnedrive.saveAsString());
                                        cloudHandler.addEntry(cloudEntryOnedrive);
                                    }

                                    dataUtils.addAccount(cloudStorageOnedrive);
                                } catch (CloudPluginException e) {

                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_error_plugin));
                                    deleteConnection(OpenMode.ONEDRIVE);
                                    return false;
                                } catch (AuthenticationException e) {
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.cloud_fail_authenticate));
                                    deleteConnection(OpenMode.ONEDRIVE);
                                    return false;
                                } catch (Exception e) {
                                    // any other exception due to network conditions or other error
                                    e.printStackTrace();
                                    AppConfig.toast(MainActivity.this, getResources().getString(R.string.failed_cloud_new_connection));
                                    deleteConnection(OpenMode.ONEDRIVE);
                                    return false;
                                }
                                break;
                            default:
                                Toast.makeText(MainActivity.this, getResources().getString(R.string.cloud_error_failed_restart),
                                        Toast.LENGTH_LONG).show();
                                return false;
                        }
                    } while (data.moveToNext());
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean refreshDrawer) {
                super.onPostExecute(refreshDrawer);
                if (refreshDrawer) {
                    refreshDrawer();
                }
            }
        }.execute();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
