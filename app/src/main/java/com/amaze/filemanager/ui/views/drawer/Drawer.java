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

package com.amaze.filemanager.ui.views.drawer;

import static android.os.Build.VERSION.SDK_INT;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.StorageDirectoryParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.file_operations.filesystem.usb.SingletonUsbOtg;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.PreferencesActivity;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.fragments.AppsListFragment;
import com.amaze.filemanager.ui.fragments.CloudSheetFragment;
import com.amaze.filemanager.ui.fragments.FtpServerFragment;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.fragments.preference_fragments.QuickAccessPref;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.Billing;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.ScreenUtils;
import com.amaze.filemanager.utils.TinyDB;
import com.amaze.filemanager.utils.Utils;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.google.android.material.navigation.NavigationView;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.lifecycle.ViewModelProvider;

public class Drawer implements NavigationView.OnNavigationItemSelectedListener {

  public static final int image_selector_request_code = 31;

  public static final int STORAGES_GROUP = 0,
      SERVERS_GROUP = 1,
      CLOUDS_GROUP = 2,
      FOLDERS_GROUP = 3,
      QUICKACCESSES_GROUP = 4,
      LASTGROUP = 5;
  public static final int[] GROUPS = {
    STORAGES_GROUP, SERVERS_GROUP, CLOUDS_GROUP, FOLDERS_GROUP, QUICKACCESSES_GROUP, LASTGROUP
  };

  @NonNull private final MainActivity mainActivity;
  private Resources resources;
  private DataUtils dataUtils;

  private ActionViewStateManager actionViewStateManager;
  private volatile int phoneStorageCount =
      0; // number of storage available (internal/external/otg etc)
  private boolean isDrawerLocked = false;
  private FragmentTransaction pending_fragmentTransaction;
  private String pendingPath;
  private ImageLoader mImageLoader;
  private String firstPath = null, secondPath = null;

  private DrawerLayout mDrawerLayout;
  private ActionBarDrawerToggle mDrawerToggle;
  private CustomNavigationView navView;
  private RelativeLayout drawerHeaderParent;
  private View drawerHeaderLayout, drawerHeaderView;
  private ImageView donateImageView;
  private ImageView telegramImageView;
  private TextView appVersion;

  /** Tablet is defined as 'width > 720dp' */
  private boolean isOnTablet = false;

  private Billing billing;

  public Drawer(MainActivity mainActivity) {
    this.mainActivity = mainActivity;
    resources = mainActivity.getResources();
    dataUtils = DataUtils.getInstance();

    drawerHeaderLayout = mainActivity.getLayoutInflater().inflate(R.layout.drawerheader, null);
    drawerHeaderParent = drawerHeaderLayout.findViewById(R.id.drawer_header_parent);
    drawerHeaderView = drawerHeaderLayout.findViewById(R.id.drawer_header);
    donateImageView = drawerHeaderLayout.findViewById(R.id.donate);
    telegramImageView = drawerHeaderLayout.findViewById(R.id.telegram);
    appVersion = drawerHeaderLayout.findViewById(R.id.app_version);
    if (BuildConfig.DEBUG) {
      appVersion.setVisibility(View.VISIBLE);
    }
    donateImageView.setOnClickListener(v -> new Billing(mainActivity));
    telegramImageView.setOnClickListener(v -> Utils.openTelegramURL(mainActivity));
    initDrawerFocusItems();
    /*drawerHeaderView.setOnLongClickListener(
    v -> {
      Intent intent1;
      if (SDK_INT < Build.VERSION_CODES.KITKAT) {
        intent1 = new Intent();
        intent1.setAction(Intent.ACTION_GET_CONTENT);
      } else {
        intent1 = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      }
      intent1.addCategory(Intent.CATEGORY_OPENABLE);
      intent1.setType("image/*");
      mainActivity.startActivityForResult(intent1, image_selector_request_code);
      return false;
    });*/

    mImageLoader = AppConfig.getInstance().getImageLoader();

    navView = mainActivity.findViewById(R.id.navigation);

    // set width of drawer in portrait to follow material guidelines
    /*if(!Utils.isDeviceInLandScape(mainActivity)){
        setNavViewDimension(navView);
    }*/

    navView.setNavigationItemSelectedListener(this);

    int accentColor = mainActivity.getAccent(), idleColor;

    if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
      idleColor = mainActivity.getResources().getColor(R.color.item_light_theme);
    } else {
      idleColor = Color.WHITE;
    }

    actionViewStateManager = new ActionViewStateManager(idleColor, accentColor);

    ColorStateList drawerColors =
        new ColorStateList(
            new int[][] {
              new int[] {android.R.attr.state_checked},
              new int[] {android.R.attr.state_enabled},
              new int[] {android.R.attr.state_pressed},
              new int[] {android.R.attr.state_focused},
              new int[] {android.R.attr.state_pressed}
            },
            new int[] {accentColor, idleColor, idleColor, idleColor, idleColor});

    navView.setItemTextColor(drawerColors);
    navView.setItemIconTintList(drawerColors);

    if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {
      navView.setBackgroundColor(Utils.getColor(mainActivity, R.color.holo_dark_background));
    } else if (mainActivity.getAppTheme().equals(AppTheme.BLACK)) {
      navView.setBackgroundColor(Utils.getColor(mainActivity, android.R.color.black));
    } else {
      navView.setBackgroundColor(Color.WHITE);
    }

    mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);
    // mDrawerLayout.setStatusBarBackgroundColor(Color.parseColor((currentTab==1 ? skinTwo :
    // skin)));
    drawerHeaderView.setBackgroundResource(R.drawable.amaze_header);
    // drawerHeaderParent.setBackgroundColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));
    if (mainActivity.findViewById(R.id.tab_frame) != null) {
      isOnTablet = true;
      mDrawerLayout.setScrimColor(Color.TRANSPARENT);
    }
    navView.addHeaderView(drawerHeaderLayout);

    if (!isOnTablet) {
      mDrawerToggle =
          new ActionBarDrawerToggle(
              mainActivity, /* host Activity */
              mDrawerLayout, /* DrawerLayout object */
              R.drawable.ic_drawer_l, /* nav drawer image to replace 'Up' caret */
              R.string.drawer_open, /* "open drawer" description for accessibility */
              R.string.drawer_close /* "close drawer" description for accessibility */) {
            public void onDrawerClosed(View view) {
              Drawer.this.onDrawerClosed();
            }

            public void onDrawerOpened(View drawerView) {
              // title.setText("Amaze File Manager");
              // creates call to onPrepareOptionsMenu()
            }
          };
      mDrawerLayout.setDrawerListener(mDrawerToggle);
      mainActivity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer_l);
      mainActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      mainActivity.getSupportActionBar().setHomeButtonEnabled(true);
      mDrawerToggle.syncState();
    }
  }

  private void setNavViewDimension(CustomNavigationView navView) {
    int screenWidth = AppConfig.getInstance().getScreenUtils().getScreenWidthInDp();
    int desiredWidthInDp = screenWidth - ScreenUtils.TOOLBAR_HEIGHT_IN_DP;
    int desiredWidthInPx = AppConfig.getInstance().getScreenUtils().convertDbToPx(desiredWidthInDp);

    navView.setLayoutParams(
        new DrawerLayout.LayoutParams(
            desiredWidthInPx, LinearLayout.LayoutParams.MATCH_PARENT, Gravity.START));
  }

  /** Refactors lock mode based on orientation */
  public void refactorDrawerLockMode() {
    if (mainActivity.findViewById(R.id.tab_frame) != null) {
      isOnTablet = true;
      mDrawerLayout.setScrimColor(Color.TRANSPARENT);
      open();
      lock(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
    } else {
      unlockIfNotOnTablet();
      close();
    }
  }

  public void refreshDrawer() {
    Menu menu = navView.getMenu();
    menu.clear();
    actionViewStateManager.deselectCurrentActionView();

    int order = 0;
    ArrayList<StorageDirectoryParcelable> storageDirectories = mainActivity.getStorageDirectories();
    ArrayList<String> storageDirectoryPaths = new ArrayList<>();
    phoneStorageCount = 0;
    for (StorageDirectoryParcelable storageDirectory : storageDirectories) {
      String file = storageDirectory.path;
      File f = new File(file);
      String name = storageDirectory.name;
      int icon = storageDirectory.iconRes;

      storageDirectoryPaths.add(file);

      if (file.contains(OTGUtil.PREFIX_OTG) || file.startsWith(OTGUtil.PREFIX_MEDIA_REMOVABLE)) {
        addNewItem(
            menu,
            STORAGES_GROUP,
            order++,
            "OTG",
            new MenuMetadata(file),
            R.drawable.ic_usb_white_24dp,
            R.drawable.ic_show_chart_black_24dp);
        continue;
      }

      if (f.isDirectory() || f.canExecute()) {
        addNewItem(
            menu,
            STORAGES_GROUP,
            order++,
            name,
            new MenuMetadata(file),
            icon,
            R.drawable.ic_show_chart_black_24dp);
        if (phoneStorageCount == 0) firstPath = file;
        else if (phoneStorageCount == 1) secondPath = file;

        phoneStorageCount++;
      }
    }
    dataUtils.setStorages(storageDirectoryPaths);

    if (dataUtils.getServers().size() > 0) {
      Collections.sort(dataUtils.getServers(), new BookSorter());
      synchronized (dataUtils.getServers()) {
        for (String[] file : dataUtils.getServers()) {
          addNewItem(
              menu,
              SERVERS_GROUP,
              order++,
              file[0],
              new MenuMetadata(file[1]),
              R.drawable.ic_settings_remote_white_24dp,
              R.drawable.ic_edit_24dp);
        }
      }
    }

    ArrayList<String[]> accountAuthenticationList = new ArrayList<>();

    if (CloudSheetFragment.isCloudProviderAvailable(mainActivity)) {
      for (CloudStorage cloudStorage : dataUtils.getAccounts()) {
        @DrawableRes int deleteIcon = R.drawable.ic_delete_grey_24dp;

        if (cloudStorage instanceof Dropbox) {
          addNewItem(
              menu,
              CLOUDS_GROUP,
              order++,
              CloudHandler.CLOUD_NAME_DROPBOX,
              new MenuMetadata(CloudHandler.CLOUD_PREFIX_DROPBOX + "/"),
              R.drawable.ic_dropbox_white_24dp,
              deleteIcon);

          accountAuthenticationList.add(
              new String[] {
                CloudHandler.CLOUD_NAME_DROPBOX, CloudHandler.CLOUD_PREFIX_DROPBOX + "/",
              });
        } else if (cloudStorage instanceof Box) {
          addNewItem(
              menu,
              CLOUDS_GROUP,
              order++,
              CloudHandler.CLOUD_NAME_BOX,
              new MenuMetadata(CloudHandler.CLOUD_PREFIX_BOX + "/"),
              R.drawable.ic_box_white_24dp,
              deleteIcon);

          accountAuthenticationList.add(
              new String[] {
                CloudHandler.CLOUD_NAME_BOX, CloudHandler.CLOUD_PREFIX_BOX + "/",
              });
        } else if (cloudStorage instanceof OneDrive) {
          addNewItem(
              menu,
              CLOUDS_GROUP,
              order++,
              CloudHandler.CLOUD_NAME_ONE_DRIVE,
              new MenuMetadata(CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/"),
              R.drawable.ic_onedrive_white_24dp,
              deleteIcon);

          accountAuthenticationList.add(
              new String[] {
                CloudHandler.CLOUD_NAME_ONE_DRIVE, CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/",
              });
        } else if (cloudStorage instanceof GoogleDrive) {
          addNewItem(
              menu,
              CLOUDS_GROUP,
              order++,
              CloudHandler.CLOUD_NAME_GOOGLE_DRIVE,
              new MenuMetadata(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/"),
              R.drawable.ic_google_drive_white_24dp,
              deleteIcon);

          accountAuthenticationList.add(
              new String[] {
                CloudHandler.CLOUD_NAME_GOOGLE_DRIVE, CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/",
              });
        }
      }
      Collections.sort(accountAuthenticationList, new BookSorter());
    }

    if (mainActivity.getBoolean(PREFERENCE_SHOW_SIDEBAR_FOLDERS)) {
      if (dataUtils.getBooks().size() > 0) {

        Collections.sort(dataUtils.getBooks(), new BookSorter());

        synchronized (dataUtils.getBooks()) {
          for (String[] file : dataUtils.getBooks()) {
            addNewItem(
                menu,
                FOLDERS_GROUP,
                order++,
                file[0],
                new MenuMetadata(file[1]),
                R.drawable.ic_folder_white_24dp,
                R.drawable.ic_edit_24dp);
          }
        }
      }
    }

    Boolean[] quickAccessPref =
        TinyDB.getBooleanArray(
            mainActivity.getPrefs(), QuickAccessPref.KEY, QuickAccessPref.Companion.getDEFAULT());

    if (mainActivity.getBoolean(PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES)) {
      if (quickAccessPref[0]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.quick,
            new MenuMetadata("5"),
            R.drawable.ic_star_white_24dp,
            null);
      }
      if (quickAccessPref[1]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.recent,
            new MenuMetadata("6"),
            R.drawable.ic_history_white_24dp,
            null);
      }
      if (quickAccessPref[2]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.images,
            new MenuMetadata("0"),
            R.drawable.ic_photo_library_white_24dp,
            null);
      }
      if (quickAccessPref[3]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.videos,
            new MenuMetadata("1"),
            R.drawable.ic_video_library_white_24dp,
            null);
      }
      if (quickAccessPref[4]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.audio,
            new MenuMetadata("2"),
            R.drawable.ic_library_music_white_24dp,
            null);
      }
      if (quickAccessPref[5]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.documents,
            new MenuMetadata("3"),
            R.drawable.ic_library_books_white_24dp,
            null);
      }
      if (quickAccessPref[6]) {
        addNewItem(
            menu,
            QUICKACCESSES_GROUP,
            order++,
            R.string.apks,
            new MenuMetadata("4"),
            R.drawable.ic_apk_library_white_24dp,
            null);
      }
    }

    addNewItem(
        menu,
        LASTGROUP,
        order++,
        R.string.ftp,
        new MenuMetadata(
            () -> {
              FragmentTransaction transaction2 =
                  mainActivity.getSupportFragmentManager().beginTransaction();
              transaction2.replace(R.id.content_frame, new FtpServerFragment());
              mainActivity
                  .getAppbar()
                  .getAppbarLayout()
                  .animate()
                  .translationY(0)
                  .setInterpolator(new DecelerateInterpolator(2))
                  .start();
              pending_fragmentTransaction = transaction2;
              if (!isDrawerLocked) close();
              else onDrawerClosed();
            }),
        R.drawable.ic_ftp_white_24dp,
        null);

    addNewItem(
        menu,
        LASTGROUP,
        order++,
        R.string.apps,
        new MenuMetadata(
            () -> {
              FragmentTransaction transaction2 =
                  mainActivity.getSupportFragmentManager().beginTransaction();
              transaction2.replace(R.id.content_frame, new AppsListFragment());
              mainActivity
                  .getAppbar()
                  .getAppbarLayout()
                  .animate()
                  .translationY(0)
                  .setInterpolator(new DecelerateInterpolator(2))
                  .start();
              pending_fragmentTransaction = transaction2;
              if (!isDrawerLocked) close();
              else onDrawerClosed();
            }),
        R.drawable.ic_android_white_24dp,
        null);

    addNewItem(
        menu,
        LASTGROUP,
        order++,
        R.string.setting,
        new MenuMetadata(
            () -> {
              Intent in = new Intent(mainActivity, PreferencesActivity.class);
              mainActivity.startActivity(in);
              mainActivity.finish();
            }),
        R.drawable.ic_settings_white_24dp,
        null);

    for (int i = 0; i < navView.getMenu().size(); i++) {
      navView.getMenu().getItem(i).setEnabled(true);
    }

    for (int group : GROUPS) {
      menu.setGroupCheckable(group, true, true);
    }

    MenuItem item = navView.getSelected();
    if (item != null) {
      item.setChecked(true);
      actionViewStateManager.selectActionView(item);
    }
  }

  private void initDrawerFocusItems() {
    donateImageView.setOnKeyListener(
        (v, keyCode, event) -> {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
              mainActivity.getAppbar().getAppbarLayout().requestFocus();
              mainActivity.getAppbar().getToolbar().requestFocus();
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
              new Billing(mainActivity);
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
              mainActivity.onBackPressed();
            } else {
              return false;
            }
          }
          return true;
        });
  }

  public ImageView getDonateImageView() {
    return this.donateImageView;
  }

  private void addNewItem(
      Menu menu,
      int group,
      int order,
      @StringRes int text,
      MenuMetadata meta,
      @DrawableRes int icon,
      @DrawableRes Integer actionViewIcon) {
    addNewItem(menu, group, order, mainActivity.getString(text), meta, icon, actionViewIcon);
  }

  private void addNewItem(
      Menu menu,
      int group,
      int order,
      String text,
      MenuMetadata meta,
      @DrawableRes int icon,
      @DrawableRes Integer actionViewIcon) {
    if (BuildConfig.DEBUG && menu.findItem(order) != null)
      throw new IllegalStateException("Item already id exists: " + order);
    MenuItem item = menu.add(group, order, order, text).setIcon(icon);
    if (TextUtils.isEmpty(meta.path)) {
      DrawerViewModel model = new ViewModelProvider(mainActivity).get(DrawerViewModel.class);
      model.putDrawerMetadata(item, meta);
    } else {
      boolean success = dataUtils.putDrawerPath(item, meta.path);
      if (success) {
        DrawerViewModel model = new ViewModelProvider(mainActivity).get(DrawerViewModel.class);
        model.putDrawerMetadata(item, meta);
      }
    }

    if (actionViewIcon != null) {
      item.setActionView(R.layout.layout_draweractionview);

      ImageView imageView = item.getActionView().findViewById(R.id.imageButton);
      imageView.setImageResource(actionViewIcon);
      if (!mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
        imageView.setColorFilter(Color.WHITE);
      }
      item.getActionView().setOnClickListener((view) -> onNavigationItemActionClick(item));
    }
  }

  public void onActivityResult(int requestCode, int responseCode, Intent intent) {
    if (mainActivity.getPrefs() != null && intent != null && intent.getData() != null) {
      if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
        mainActivity
            .getContentResolver()
            .takePersistableUriPermission(intent.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
      }
      mainActivity
          .getPrefs()
          .edit()
          .putString(
              PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH, intent.getData().toString())
          .commit();
      setDrawerHeaderBackground();
    }
  }

  public void closeIfNotLocked() {
    if (!isLocked()) {
      close();
    }
  }

  public boolean isLocked() {
    return isDrawerLocked;
  }

  public boolean isOnTablet() {
    return isOnTablet;
  }

  public boolean isOpen() {
    return mDrawerLayout.isDrawerOpen(navView);
  }

  public void open() {
    mDrawerLayout.openDrawer(navView);
  }

  public void close() {
    mDrawerLayout.closeDrawer(navView);
  }

  public void onDrawerClosed() {
    if (pending_fragmentTransaction != null) {
      pending_fragmentTransaction.commit();
      pending_fragmentTransaction = null;
    }

    if (pendingPath != null) {
      HybridFile hFile = new HybridFile(OpenMode.UNKNOWN, pendingPath);
      hFile.generateMode(mainActivity);
      if (hFile.isSimpleFile()) {
        FileUtils.openFile(new File(pendingPath), mainActivity, mainActivity.getPrefs());
        pendingPath = null;
        return;
      }

      MainFragment mainFragment = mainActivity.getCurrentMainFragment();
      if (mainFragment != null) {
        mainFragment.loadlist(pendingPath, false, OpenMode.UNKNOWN);
      } else {
        mainActivity.goToMain(pendingPath);
        return;
      }
      pendingPath = null;
    }
    mainActivity.supportInvalidateOptionsMenu();
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    actionViewStateManager.deselectCurrentActionView();
    actionViewStateManager.selectActionView(item);

    DrawerViewModel model = new ViewModelProvider(mainActivity).get(DrawerViewModel.class);
    String title = item.getTitle().toString();
    MenuMetadata meta = model.getDrawerMetadata(item);

    switch (meta.type) {
      case MenuMetadata.ITEM_ENTRY:
        if (dataUtils.containsBooks(new String[] {title, meta.path}) != -1) {
          FileUtils.checkForPath(mainActivity, meta.path, mainActivity.isRootExplorer());
        }

        if (dataUtils.getAccounts().size() > 0
            && (meta.path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)
                || meta.path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)
                || meta.path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)
                || meta.path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE))) {
          // we have cloud accounts, try see if token is expired or not
          CloudUtil.checkToken(meta.path, mainActivity);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            && (meta.path.contains(OTGUtil.PREFIX_OTG)
                || meta.path.startsWith(OTGUtil.PREFIX_MEDIA_REMOVABLE))
            && SingletonUsbOtg.getInstance().getUsbOtgRoot() == null) {
          MaterialDialog dialog = GeneralDialogCreation.showOtgSafExplanationDialog(mainActivity);
          dialog
              .getActionButton(DialogAction.POSITIVE)
              .setOnClickListener(
                  (v) -> {
                    Intent safIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    mainActivity.startActivityForResult(safIntent, MainActivity.REQUEST_CODE_SAF);
                    dialog.dismiss();
                  });
          dialog.show();
        } else {
          pendingPath = meta.path;
          closeIfNotLocked();
          if (isLocked()) {
            onDrawerClosed();
          }
        }

        break;
      case MenuMetadata.ITEM_INTENT:
        meta.onClickListener.onClick();
        break;
    }

    return true;
  }

  public void onNavigationItemActionClick(MenuItem item) {
    DrawerViewModel model = new ViewModelProvider(mainActivity).get(DrawerViewModel.class);
    String title = item.getTitle().toString();
    MenuMetadata meta = model.getDrawerMetadata(item);
    String path = meta.path;

    switch (item.getGroupId()) {
      case STORAGES_GROUP:
        if (!path.equals("/")) {
          GeneralDialogCreation.showPropertiesDialogForStorage(
              RootHelper.generateBaseFile(new File(path), true),
              mainActivity,
              mainActivity.getAppTheme());
        }
        break;
        // not to remove the first bookmark (storage) and permanent bookmarks
      case SERVERS_GROUP:
      case CLOUDS_GROUP:
      case FOLDERS_GROUP:
        if (dataUtils.containsBooks(new String[] {title, path}) != -1) {
          mainActivity.renameBookmark(title, path);
        } else if (path.startsWith("smb:/")) {
          mainActivity.showSMBDialog(title, path, true);
        } else if (path.startsWith("ssh:/")) {
          mainActivity.showSftpDialog(title, path, true);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
          GeneralDialogCreation.showCloudDialog(
              mainActivity, mainActivity.getAppTheme(), OpenMode.DROPBOX);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {
          GeneralDialogCreation.showCloudDialog(
              mainActivity, mainActivity.getAppTheme(), OpenMode.GDRIVE);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {
          GeneralDialogCreation.showCloudDialog(
              mainActivity, mainActivity.getAppTheme(), OpenMode.BOX);
        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {
          GeneralDialogCreation.showCloudDialog(
              mainActivity, mainActivity.getAppTheme(), OpenMode.ONEDRIVE);
        }
    }
  }

  public int getPhoneStorageCount() {
    return phoneStorageCount;
  }

  public void setDrawerHeaderBackground() {
    String path1 =
        mainActivity.getPrefs().getString(PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH, null);
    if (path1 == null) {
      return;
    }
    try {
      final ImageView headerImageView = new ImageView(mainActivity);
      headerImageView.setImageDrawable(drawerHeaderParent.getBackground());
      mImageLoader.get(
          path1,
          new ImageLoader.ImageListener() {
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

  public void selectCorrectDrawerItemForPath(final String path) {
    Integer id = dataUtils.findLongestContainingDrawerItem(path);

    if (id == null) deselectEverything();
    else {
      selectCorrectDrawerItem(id);
    }
  }

  /**
   * Select given item id in navigation drawer
   *
   * @param id given item id from menu
   */
  public void selectCorrectDrawerItem(int id) {
    if (id < 0) {
      deselectEverything();
    } else {
      MenuItem item = navView.getMenu().findItem(id);
      navView.setCheckedItem(item);
      actionViewStateManager.selectActionView(item);
    }
  }

  /**
   * Get selected item id
   *
   * @return item id from menu
   */
  public int getDrawerSelectedItem() {
    if (navView.getSelected() == null) {
      return -1;
    }
    return navView.getSelected().getItemId();
  }

  public void setBackgroundColor(@ColorInt int color) {
    mDrawerLayout.setStatusBarBackgroundColor(color);
    drawerHeaderParent.setBackgroundColor(color);
  }

  public void resetPendingPath() {
    pendingPath = null;
  }

  public void syncState() {
    if (mDrawerToggle != null) {
      mDrawerToggle.syncState();
    }
  }

  public void onConfigurationChanged(Configuration newConfig) {
    if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item);
  }

  public void setDrawerIndicatorEnabled() {
    if (mDrawerToggle != null) {
      mDrawerToggle.setDrawerIndicatorEnabled(true);
      mDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_drawer_l);
    }
  }

  public void deselectEverything() {
    actionViewStateManager
        .deselectCurrentActionView(); // If you set the item as checked the listener doesn't trigger
    if (navView.getSelected() == null) {
      return;
    }

    navView.deselectItems();

    for (int i = 0; i < navView.getMenu().size(); i++) {
      navView.getMenu().getItem(i).setChecked(false);
    }
  }

  /**
   * @param mode {@link DrawerLayout#LOCK_MODE_LOCKED_CLOSED}, {@link
   *     DrawerLayout#LOCK_MODE_LOCKED_OPEN} or {@link DrawerLayout#LOCK_MODE_UNDEFINED}
   * @throws IllegalArgumentException if you try to {{@link DrawerLayout#LOCK_MODE_LOCKED_OPEN} or
   *     {@link DrawerLayout#LOCK_MODE_UNDEFINED} on a tablet
   */
  private void lock(int mode) {
    if (isOnTablet && mode != DrawerLayout.LOCK_MODE_LOCKED_OPEN) {
      throw new IllegalArgumentException("You can't lock closed or unlock drawer in tablet!");
    }

    mDrawerLayout.setDrawerLockMode(mode, navView);
    isDrawerLocked = true;
  }

  /**
   * Does nothing on tablets {@link #isOnTablet}
   *
   * @param mode {@link DrawerLayout#LOCK_MODE_LOCKED_CLOSED}, {@link
   *     DrawerLayout#LOCK_MODE_LOCKED_OPEN} or {@link DrawerLayout#LOCK_MODE_UNDEFINED}
   */
  public void lockIfNotOnTablet(int mode) {
    if (isOnTablet) {
      return;
    }

    mDrawerLayout.setDrawerLockMode(mode, navView);
    isDrawerLocked = true;
  }

  public void unlockIfNotOnTablet() {
    if (isOnTablet) {
      return;
    }

    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, navView);
    isDrawerLocked = false;
  }

  public String getFirstPath() {
    return firstPath;
  }

  public String getSecondPath() {
    return secondPath;
  }

  public Billing getBilling() {
    return this.billing;
  }
}
