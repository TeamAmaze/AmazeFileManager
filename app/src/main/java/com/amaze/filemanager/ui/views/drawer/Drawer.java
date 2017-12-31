package com.amaze.filemanager.ui.views.drawer;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.adapters.data.DrawerItem;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.fragments.AppsListFragment;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.fragments.FTPServerFragment;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.fragments.preference_fragments.QuickAccessPref;
import com.amaze.filemanager.ui.views.ScrimInsetsRelativeLayout;
import com.amaze.filemanager.utils.BookSorter;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.TinyDB;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.files.FileUtils;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Box;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static android.os.Build.VERSION.SDK_INT;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 26/12/2017, at 23:08.
 */

public class Drawer {

    public static final int image_selector_request_code = 31;

    /**
     * In drawer nothing is selected.
     */
    public static final int SELECTED_NONE = -1;
    /**
     * In drawer first storage is selected.
     */
    public static final int SELECTED_DEFAULT = 0;
    /**
     * In drawer {@link ProcessViewerFragment} is selected (which is a special case
     * of {@link #SELECTED_NONE} as ProcessViewer has no drawer item). //TODO might be wrong
     */
    public static final int SELECTED_PROCESSVIEWER = 102;
    /**
     * In drawer FTP or Apps list (also Settings for a brief second) are selected.
     */
    public static final int SELECTED_LASTSECTION = -2;

    private MainActivity mainActivity;
    private Resources resources;
    private DataUtils dataUtils = DataUtils.getInstance();

    /**
     * Which item in nav drawer is selected values go from 0 to the length of the nav drawer list,
     * special values are {@link #SELECTED_DEFAULT}, {@link #SELECTED_NONE},
     * {@link #SELECTED_PROCESSVIEWER} and {@link #SELECTED_LASTSECTION}.
     */
    private int selectedStorage;
    private volatile int storage_count = 0; // number of storage available (internal/external/otg etc)
    private boolean isDrawerLocked = false;
    private DrawerAdapter adapter;
    private FragmentTransaction pending_fragmentTransaction;
    private String pendingPath;
    private ImageLoader mImageLoader;
    private String firstPath, secondPath;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    private ScrimInsetsRelativeLayout mDrawerLinear;
    private RelativeLayout drawerHeaderParent;
    private View drawerHeaderLayout, drawerHeaderView;

    public Drawer(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        resources = mainActivity.getResources();

        drawerHeaderLayout = mainActivity.getLayoutInflater().inflate(R.layout.drawerheader, null);
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
            mainActivity.startActivityForResult(intent1, image_selector_request_code);
            return false;
        });

        mImageLoader = AppConfig.getInstance().getImageLoader();

        mDrawerLinear = mainActivity.findViewById(R.id.left_drawer);
        if (mainActivity.getAppTheme().equals(AppTheme.DARK)) mDrawerLinear.setBackgroundColor(Utils.getColor(mainActivity, R.color.holo_dark_background));
        else if (mainActivity.getAppTheme().equals(AppTheme.BLACK)) mDrawerLinear.setBackgroundColor(Utils.getColor(mainActivity, android.R.color.black));
        else mDrawerLinear.setBackgroundColor(Color.WHITE);
        mDrawerLayout = mainActivity.findViewById(R.id.drawer_layout);
        //mDrawerLayout.setStatusBarBackgroundColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));
        mDrawerList = mainActivity.findViewById(R.id.menu_drawer);
        drawerHeaderView.setBackgroundResource(R.drawable.amaze_header);
        //drawerHeaderParent.setBackgroundColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));
        if (mainActivity.findViewById(R.id.tab_frame) != null) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerLinear);
            mDrawerLayout.openDrawer(mDrawerLinear);
            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
            mDrawerLayout.post(() -> mDrawerLayout.openDrawer(mDrawerLinear));
            isDrawerLocked = true;
        } else if (mainActivity.findViewById(R.id.tab_frame) == null) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLinear);
            mDrawerLayout.closeDrawer(mDrawerLinear);
            mDrawerLayout.post(() -> mDrawerLayout.closeDrawer(mDrawerLinear));
            isDrawerLocked = false;
        }
        mDrawerList.addHeaderView(drawerHeaderLayout);

        if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {
            mDrawerList.setBackgroundColor(ContextCompat.getColor(mainActivity, R.color.holo_dark_background));
        } else if (mainActivity.getAppTheme().equals(AppTheme.BLACK)) {
            mDrawerList.setBackgroundColor(ContextCompat.getColor(mainActivity, android.R.color.black));
        }
        mDrawerList.setDivider(null);
        if (!isDrawerLocked) {
            mDrawerToggle = new ActionBarDrawerToggle(
                    mainActivity,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer_l,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            ) {
                public void onDrawerClosed(View view) {
                    Drawer.this.onDrawerClosed();
                }

                public void onDrawerOpened(View drawerView) {
                    //title.setText("Amaze File Manager");
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


    public void refreshDrawer() {
        ArrayList<DrawerItem> sectionDrawerItems = new ArrayList<>();
        ArrayList<String> storageDirectories = mainActivity.getStorageDirectories();
        storage_count = 0;
        for (String file : storageDirectories) {
            File f = new File(file);
            String name;
            Drawable icon1 = ContextCompat.getDrawable(mainActivity, R.drawable.ic_sd_storage_white_24dp);
            if ("/storage/emulated/legacy".equals(file) || "/storage/emulated/0".equals(file)) {
                name = resources.getString(R.string.storage);
            } else if ("/storage/sdcard1".equals(file)) {
                name = resources.getString(R.string.extstorage);
            } else if ("/".equals(file)) {
                name = resources.getString(R.string.rootdirectory);
                icon1 = ContextCompat.getDrawable(mainActivity, R.drawable.ic_drawer_root_white);
            } else if (file.contains(OTGUtil.PREFIX_OTG)) {
                name = "OTG";
                icon1 = ContextCompat.getDrawable(mainActivity, R.drawable.ic_usb_white_24dp);
            } else name = f.getName();
            if (!f.isDirectory() || f.canExecute()) {
                storage_count++;
                sectionDrawerItems.add(new DrawerItem(name, file, icon1));
            }
        }
        dataUtils.setStorages(storageDirectories);
        sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));

        firstPath = sectionDrawerItems.get(0).path;
        secondPath = sectionDrawerItems.get(1).path;

        if (dataUtils.getServers().size() > 0) {
            Collections.sort(dataUtils.getServers(), new BookSorter());
            synchronized (dataUtils.getServers()) {
                for (String[] file : dataUtils.getServers()) {
                    sectionDrawerItems.add(new DrawerItem(file[0], file[1],
                            ContextCompat.getDrawable(mainActivity, R.drawable.ic_settings_remote_white_24dp)));
                }
            }
            sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));
        }

        ArrayList<String[]> accountAuthenticationList = new ArrayList<>();

        if (CloudSheetFragment.isCloudProviderAvailable(mainActivity)) {
            for (CloudStorage cloudStorage : dataUtils.getAccounts()) {
                if (cloudStorage instanceof Dropbox) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_DROPBOX,
                            CloudHandler.CLOUD_PREFIX_DROPBOX + "/",
                            ContextCompat.getDrawable(mainActivity, R.drawable.ic_dropbox_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_DROPBOX,
                            CloudHandler.CLOUD_PREFIX_DROPBOX + "/",
                    });
                } else if (cloudStorage instanceof Box) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_BOX,
                            CloudHandler.CLOUD_PREFIX_BOX + "/",
                            ContextCompat.getDrawable(mainActivity, R.drawable.ic_box_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_BOX,
                            CloudHandler.CLOUD_PREFIX_BOX + "/",
                    });
                } else if (cloudStorage instanceof OneDrive) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_ONE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/",
                            ContextCompat.getDrawable(mainActivity, R.drawable.ic_onedrive_white_24dp)));

                    accountAuthenticationList.add(new String[] {
                            CloudHandler.CLOUD_NAME_ONE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_ONE_DRIVE + "/",
                    });
                } else if (cloudStorage instanceof GoogleDrive) {

                    sectionDrawerItems.add(new DrawerItem(CloudHandler.CLOUD_NAME_GOOGLE_DRIVE,
                            CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE + "/",
                            ContextCompat.getDrawable(mainActivity, R.drawable.ic_google_drive_white_24dp)));

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

        if (mainActivity.getPrefs().getBoolean(PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS, true)) {
            if (dataUtils.getBooks().size() > 0) {

                Collections.sort(dataUtils.getBooks(), new BookSorter());

                synchronized (dataUtils.getBooks()) {
                    for (String[] file : dataUtils.getBooks()) {
                        sectionDrawerItems.add(new DrawerItem(file[0], file[1],
                                ContextCompat.getDrawable(mainActivity, R.drawable.ic_folder_white_24dp)));
                    }
                }
                sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));
            }
        }

        Boolean[] quickAccessPref = TinyDB.getBooleanArray(mainActivity.getPrefs(), QuickAccessPref.KEY,
                QuickAccessPref.DEFAULT);

        if (mainActivity.getPrefs().getBoolean(PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES, true)) {
            if (quickAccessPref[0])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.quick), "5",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_star_white_24dp)));
            if (quickAccessPref[1])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.recent), "6",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_history_white_24dp)));
            if (quickAccessPref[2])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.images), "0",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_photo_library_white_24dp)));
            if (quickAccessPref[3])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.videos), "1",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_video_library_white_24dp)));
            if (quickAccessPref[4])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.audio), "2",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_library_music_white_24dp)));
            if (quickAccessPref[5])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.documents), "3",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_library_books_white_24dp)));
            if (quickAccessPref[6])
                sectionDrawerItems.add(new DrawerItem(resources.getString(R.string.apks), "4",
                        ContextCompat.getDrawable(mainActivity, R.drawable.ic_apk_library_white_24dp)));
        } else {
            sectionDrawerItems.remove(sectionDrawerItems.size() - 1); //Deletes last divider
        }

        sectionDrawerItems.add(new DrawerItem(DrawerItem.ITEM_SECTION));

        sectionDrawerItems.add(new DrawerItem(mainActivity.getString(R.string.ftp),
                ContextCompat.getDrawable(mainActivity, R.drawable.ic_ftp_white_24dp), () -> {
            FragmentTransaction transaction2 = mainActivity.getSupportFragmentManager().beginTransaction();
            transaction2.replace(R.id.content_frame, new FTPServerFragment());
            mainActivity.getAppbar().getAppbarLayout().animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            pending_fragmentTransaction = transaction2;
            if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
            else onDrawerClosed();
        }));
        sectionDrawerItems.add(new DrawerItem(mainActivity.getString(R.string.apps),
                ContextCompat.getDrawable(mainActivity, R.drawable.ic_android_white_24dp), () -> {
            FragmentTransaction transaction2 = mainActivity.getSupportFragmentManager().beginTransaction();
            transaction2.replace(R.id.content_frame, new AppsListFragment());
            mainActivity.getAppbar().getAppbarLayout().animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            pending_fragmentTransaction = transaction2;
            if (!isDrawerLocked) mDrawerLayout.closeDrawer(mDrawerLinear);
            else onDrawerClosed();
        }));
        sectionDrawerItems.add(new DrawerItem(mainActivity.getString(R.string.setting),
                ContextCompat.getDrawable(mainActivity, R.drawable.ic_settings_white_24dp), () -> {
            Intent in = new Intent(mainActivity, PreferencesActivity.class);
            mainActivity.startActivity(in);
        }));

        dataUtils.setDrawerItems(sectionDrawerItems);

        adapter = new DrawerAdapter(mainActivity, mainActivity.getUtilsProvider(),
                sectionDrawerItems, mainActivity);
        mDrawerList.setAdapter(adapter);
    }

    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (mainActivity.getPrefs() != null && intent != null && intent.getData() != null) {
            if (SDK_INT >= Build.VERSION_CODES.KITKAT)
                mainActivity.getContentResolver().takePersistableUriPermission(intent.getData(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mainActivity.getPrefs().edit().putString(PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH,
                    intent.getData().toString()).commit();
            setDrawerHeaderBackground();
        }
    }

    public void closeIfNotLocked() {
        if(!isLocked()) close();
    }

    public boolean isLocked() {
        return isDrawerLocked;
    }

    public boolean isOpen() {
        return mDrawerLayout.isDrawerOpen(mDrawerLinear);
    }

    public void close() {
        mDrawerLayout.closeDrawer(mDrawerLinear);
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

            MainFragment mainFrag = mainActivity.getCurrentMainFragment();
            if (mainFrag != null) {
                mainFrag.loadlist(pendingPath, false, OpenMode.UNKNOWN);
            } else {
                mainActivity.goToMain(pendingPath);
                return;
            }
            pendingPath = null;
        }
        mainActivity.supportInvalidateOptionsMenu();
    }

    public void selectItem(final int i) {
        ArrayList<DrawerItem> directoryDrawerItems = dataUtils.getDrawerItems();
        switch (directoryDrawerItems.get(i).type) {
            case DrawerItem.ITEM_ENTRY:
                if ((selectedStorage == SELECTED_NONE || selectedStorage >= directoryDrawerItems.size())) {
                    TabFragment tabFragment = new TabFragment();
                    Bundle a = new Bundle();
                    a.putString("path", directoryDrawerItems.get(i).path);

                    tabFragment.setArguments(a);

                    android.support.v4.app.FragmentTransaction transaction = mainActivity.getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.content_frame, tabFragment);

                    transaction.addToBackStack("tabt1" + 1);
                    pending_fragmentTransaction = transaction;
                    selectedStorage = i;
                    adapter.toggleChecked(selectedStorage);
                    closeIfNotLocked();
                    if(isLocked()) onDrawerClosed();
                    mainActivity.getFAB().setVisibility(View.VISIBLE);
                    mainActivity.getFAB().getMenuButton().show();
                } else {
                    pendingPath = directoryDrawerItems.get(i).path;

                    selectedStorage = i;
                    adapter.toggleChecked(selectedStorage);

                    if (directoryDrawerItems.get(i).path.contains(OTGUtil.PREFIX_OTG) &&
                            mainActivity.getPrefs().getString(MainActivity.KEY_PREF_OTG, null).equals(MainActivity.VALUE_PREF_OTG_NULL)) {
                        // we've not gotten otg path yet
                        // start system request for storage access framework
                        Toast.makeText(mainActivity, mainActivity.getString(R.string.otg_access), Toast.LENGTH_LONG).show();
                        Intent safIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        mainActivity.startActivityForResult(safIntent, mainActivity.REQUEST_CODE_SAF);
                    } else {
                        closeIfNotLocked();
                        if(isLocked()) onDrawerClosed();
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

    public int getSelectedStorage() {
        return selectedStorage;
    }

    public void setSelectedStorage(int selectedStorage) {
        this.selectedStorage = selectedStorage;
    }

    public int getStorageCount() {
        return storage_count;
    }

    public void setDrawerHeaderBackground() {
        String path1 = mainActivity.getPrefs().getString(PreferencesConstants.PREFERENCE_DRAWER_HEADER_PATH, null);
        if (path1 == null) return;
        try {
            final ImageView headerImageView = new ImageView(mainActivity);
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

    public void selectCorrectDrawerItemForPath(final String path) {
        Integer position = dataUtils.findLongestContainingDrawerItem(path);

        if (adapter != null) {
            adapter.toggleChecked(position != null? position:-1);
        }
    }

    public void setBackgroundColor(@ColorInt int color) {
        mDrawerLayout.setStatusBarBackgroundColor(color);
        drawerHeaderParent.setBackgroundColor(color);
    }

    public void resetPendingPath() {
        pendingPath = null;
    }

    public void syncState() {
        if (mDrawerToggle != null) mDrawerToggle.syncState();
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
        adapter.deselectEverything();
    }

    public void toggleCheckedSelectedStorage() {
        adapter.toggleChecked(selectedStorage);
    }

    public void lock() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, mDrawerLinear);
    }

    public void unlock() {
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLinear);
    }

    public String getFirstPath() {
        return firstPath;
    }

    public String getSecondPath() {
        return secondPath;
    }
}
