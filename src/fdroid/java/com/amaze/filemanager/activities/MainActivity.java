/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.BookmarksManager;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.ProcessViewer;
import com.amaze.filemanager.fragments.RarViewer;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.SearchService;
import com.amaze.filemanager.services.asynctasks.MoveFiles;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.MediaFile;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.RootHelper;
import com.amaze.filemanager.utils.RoundedImageView;
import com.amaze.filemanager.utils.ScrimInsetsRelativeLayout;
import com.amaze.filemanager.utils.Shortcuts;
import com.melnykov.fab.FloatingActionButton;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


public class MainActivity extends ActionBarActivity {
    public Integer select;
    Futils utils;
    private boolean backPressedToExitOnce = false;
    private Toast toast = null;
    private DrawerLayout mDrawerLayout;
    public ListView mDrawerList;
    SharedPreferences Sp;
    private android.support.v7.app.ActionBarDrawerToggle mDrawerToggle;
    public List<String> val;
    MainActivity mainActivity=this;
    DrawerAdapter adapter;
    IconUtils util;
    ScrimInsetsRelativeLayout mDrawerLinear;
    Shortcuts s;
    public String skin,path="", launchPath;
    public int theme;
    public ArrayList<String> COPY_PATH = null, MOVE_PATH = null;
    Context con = this;
    public FrameLayout frameLayout;
    public boolean mReturnIntent = false;
    private Intent intent;
    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");
    public ArrayList<String> list;
    public int theme1;
    public boolean rootmode,aBoolean,openzip=false;
    String zippath;
    public Spinner tabsSpinner;
    public boolean mRingtonePickerIntent = false,restart=false,colourednavigation=false;
    public Toolbar toolbar;
    public int skinStatusBar;
    FragmentTransaction pending_fragmentTransaction;
    String pending_path;
    boolean openprocesses=false;
    public int booksize=0;
    private View drawerHeaderView;
    private RelativeLayout drawerHeaderLayout;
    private int sdk;
    public FloatingActionButton floatingActionButton;
    public String fabskin, fabSkinPressed;
    private LinearLayout buttons;
    private HorizontalScrollView scroll, scroll1;
    private CountDownTimer timer;
    private IconUtils icons;
    private TabHandler tabHandler;
    int hidemode;
    public LinearLayout pathbar;
    public Animation fabShowAnim, fabHideAnim;
    public FrameLayout buttonBarFrame;
    private boolean topfab=false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Sp = PreferenceManager.getDefaultSharedPreferences(this);

        int th = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = th==2 ? PreferenceUtils.hourOfDay() : th;

        if (theme1 == 1) {
            setTheme(R.style.appCompatDark);
        }

        setContentView(R.layout.main_toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        tabHandler=new TabHandler(this,null,null,1);

        buttonBarFrame = (FrameLayout) findViewById(R.id.buttonbarframe);
        fabskin = Sp.getString("fab_skin_color", "#e91e63");
        fabSkinPressed = PreferenceUtils.getStatusColor(fabskin);
        hidemode=Sp.getInt("hidemode", 0);
        topfab = hidemode==0 ? true:false;

        floatingActionButton = !topfab ?
                (FloatingActionButton) findViewById(R.id.fab) : (FloatingActionButton) findViewById(R.id.fab2);
        fabShowAnim = AnimationUtils.loadAnimation(this, R.anim.fab_newtab);
        fabHideAnim = AnimationUtils.loadAnimation(this, R.anim.fab_hide);
        floatingActionButton.setAnimation(fabShowAnim);
        floatingActionButton.animate();
        floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.setShadow(true);
        floatingActionButton.setColorNormal(Color.parseColor(fabskin));
        floatingActionButton.setColorPressed(Color.parseColor(fabSkinPressed));

        drawerHeaderView = getLayoutInflater().inflate(R.layout.drawerheader, null);
        drawerHeaderLayout = (RelativeLayout) drawerHeaderView.findViewById(R.id.drawer_header);
        drawerHeaderLayout.setBackgroundResource(R.drawable.amaze_header);

        utils = new Futils();
        s = new Shortcuts(this);
        path = getIntent().getStringExtra("path");
        openprocesses=getIntent().getBooleanExtra("openprocesses", false);
        restart = getIntent().getBooleanExtra("restart", false);
        val = getStorageDirectories();
        rootmode = Sp.getBoolean("rootmode", false);
        theme = Integer.parseInt(Sp.getString("theme", "0"));
        util = new IconUtils(Sp, this);
        icons = new IconUtils(Sp, this);

        boolean v20 = Sp.getBoolean("v2.0", false);
        if (!v20) {
            try {
                utils.deletedirectory(new File("/data/data/com.amaze.filemanager"));
            } catch (Exception e) {
                try {
                    utils.deletedirectory(getCacheDir());
                } catch (Exception e1) {

                }

            }
            Sp.edit().putBoolean("v2.0", true).apply();
        }

        pathbar = (LinearLayout) findViewById(R.id.pathbar);
        buttons = (LinearLayout) findViewById(R.id.buttons);
        scroll = (HorizontalScrollView) findViewById(R.id.scroll);
        scroll1 = (HorizontalScrollView) findViewById(R.id.scroll1);
        scroll.setSmoothScrollingEnabled(true);
        scroll1.setSmoothScrollingEnabled(true);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(MainActivity.this);
                builder.items(new String[]{
                        getResources().getString(R.string.folder),
                        getResources().getString(R.string.file)

                });
                builder.itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
                        add(i);
                    }
                });
                builder.title(getResources().getString(R.string.new_string));
                if (theme1 == 1)
                    builder.theme(Theme.DARK);
                builder.build().show();
            }
        });

        IntentFilter newFilter = new IntentFilter();
        newFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        newFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        newFilter.addDataScheme(ContentResolver.SCHEME_FILE);
        registerReceiver(mNotificationReceiver, newFilter);
        // Toolbar
        toolbar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        aBoolean = Sp.getBoolean("view", true);
        //ImageView overflow = ((ImageView)findViewById(R.id.action_overflow));

        //showPopup(overflow);
        tabsSpinner = (Spinner) findViewById(R.id.tab_spinner);
        //title = (TextView) findViewById(R.id.title);
        frameLayout = (FrameLayout) findViewById(R.id.content_frame);

        timer=new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                crossfadeInverse();
            }
        };

        try {
            intent = getIntent();
            if (intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {

                // file picker intent
                mReturnIntent = true;
                Toast.makeText(this, utils.getString(con, R.string.pick_a_file), Toast.LENGTH_LONG).show();
            } else if (intent.getAction().equals(RingtoneManager.ACTION_RINGTONE_PICKER)) {

                // ringtone picker intent
                mReturnIntent = true;
                mRingtonePickerIntent = true;
                Toast.makeText(this, utils.getString(con, R.string.pick_a_file), Toast.LENGTH_LONG).show();
            } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {

                // zip viewer intent
                Uri uri = intent.getData();
                openzip = true;
                zippath = uri.getPath();
            }
        } catch (Exception e) {

        }

        skin = PreferenceManager.getDefaultSharedPreferences(this).getString("skin_color", "#3f51b5");
        findViewById(R.id.buttonbarframe).setBackgroundColor(Color.parseColor(skin));

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));

        skinStatusBar = Color.parseColor(PreferenceUtils.getStatusColor(skin));

        mDrawerLinear = (ScrimInsetsRelativeLayout) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setStatusBarBackgroundColor(Color.parseColor(skin));
        mDrawerList = (ListView) findViewById(R.id.menu_drawer);

        // status bar
        sdk = Build.VERSION.SDK_INT;

        if (sdk == 20 || sdk == 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.drawer_layout).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (Build.VERSION.SDK_INT >= 21) {
            colourednavigation = Sp.getBoolean("colorednavigation", true);

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //window.setStatusBarColor(Color.parseColor(statusBarColorBuilder.toString()));
            if (colourednavigation)
                window.setNavigationBarColor(skinStatusBar);

        }

        View settingsbutton = findViewById(R.id.settingsbutton);
        if (theme1 == 1) {
            settingsbutton.setBackgroundResource(R.drawable.safr_ripple_black);
            ((ImageView) settingsbutton.findViewById(R.id.settingicon)).setImageResource(R.drawable.ic_settings_white_48dp);
            ((TextView)settingsbutton.findViewById(R.id.settingtext)).setTextColor(getResources().getColor(android.R.color.white));
        }
        settingsbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(MainActivity.this, Preferences.class);
                finish(); final int enter_anim = android.R.anim.fade_in;
                final int exit_anim = android.R.anim.fade_out;
                Activity s=MainActivity.this;
                s.overridePendingTransition( exit_anim,enter_anim);
                s.finish();
                s.overridePendingTransition(enter_anim, exit_anim);
                s.startActivity(in); }

        });
        View appbutton = findViewById(R.id.appbutton);
        if (theme1 == 1) {
            appbutton.setBackgroundResource(R.drawable.safr_ripple_black);
            ((ImageView) appbutton.findViewById(R.id.appicon)).setImageResource(R.drawable.ic_action_view_as_grid);
            ((TextView)appbutton.findViewById(R.id.apptext)).setTextColor(getResources().getColor(android.R.color.white));
        }
        appbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.support.v4.app.FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                transaction2.replace(R.id.content_frame, new AppsList());

                pending_fragmentTransaction=transaction2;
                mDrawerLayout.closeDrawer(mDrawerLinear);
                select=list.size()+1;
                adapter.toggleChecked(false);
            }
        });

        mDrawerList.addHeaderView(drawerHeaderView);
        list = new ArrayList<String>();
        for (int i = 0; i < val.size(); i++) {
            File file = new File(val.get(i));
            if(!file.isDirectory())
                list.add(val.get(i));
            else if(file.canExecute())
                list.add(val.get(i));
        }
        adapter = new DrawerAdapter(this, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);
        if (savedInstanceState == null) {

            if (openprocesses) {
                android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.content_frame, new ProcessViewer());
                //   transaction.addToBackStack(null);
                select = 102;
                openprocesses=false;
                //title.setText(utils.getString(con, R.string.process_viewer));
                //Commit the transaction
                transaction.commit();
                supportInvalidateOptionsMenu();
            } else {
                goToMain(path);
            }
        } else {
            select = savedInstanceState.getInt("selectitem", 0);
            adapter.toggleChecked(select);
        }
        if (theme1 == 1) {
            mDrawerList.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setDivider(null);
        if (select == 0) {

            //title.setVisibility(View.GONE);
            tabsSpinner.setVisibility(View.VISIBLE);
        } else {

            //title.setVisibility(View.VISIBLE);
            tabsSpinner.setVisibility(View.GONE);
        }
        mDrawerToggle = new android.support.v7.app.ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                toolbar,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                if(pending_fragmentTransaction!=null){
                    pending_fragmentTransaction.commit();
                    pending_fragmentTransaction=null;
                }
                if(pending_path!=null){
                    try {
                        TabFragment m=getFragment();
                        if(new File(pending_path).isDirectory()) {
                            ((Main) m.getTab()).loadlist(new File(pending_path), false);
                        }   else utils.openFile(new File(pending_path),mainActivity);

                    } catch (ClassCastException e) {
                        select=null;
                        goToMain("");
                    }pending_path=null;}
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                //title.setText("Amaze File Manager");
                // creates call to onPrepareOptionsMenu()
                supportInvalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();
        /*((ImageButton) findViewById(R.id.drawer_buttton)).setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDrawerLayout.isDrawerOpen(mDrawerLinear)) {
                    mDrawerLayout.closeDrawer(mDrawerLinear);
                } else mDrawerLayout.openDrawer(mDrawerLinear);
            }
        });*/
        //recents header color implementation
        if (Build.VERSION.SDK_INT>=21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze", ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_launcher)).getBitmap(), Color.parseColor(skin));
            ((Activity)this).setTaskDescription(taskDescription);
        }
    }

    /**
     * Returns all available SD-Cards in the system (include emulated)
     * <p/>
     * Warning: Hack! Based on Android source code of version 4.3 (API 18)
     * Because there is no standard way to get it.
     * TODO: Test on future Android versions 4.4+
     *
     * @return paths to all available SD-Cards in the system (include emulated)
     */
    public  List<String> getStorageDirectories() {
        // Final set of paths
        final ArrayList<String> rv=new ArrayList<String>();
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
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
        }rootmode = Sp.getBoolean("rootmode", false);
        if(rootmode)
            rv.add("/");
        File usb=getUsbDrive();
        if(usb!=null && !rv.contains(usb.getPath()))rv.add(usb.getPath());
        try {
            for(File file: s.readS()){
                rv.add(file.getPath());
            }
        } catch (Exception e) {
            try {
                s.makeS();booksize=0;
                for(File file: s.readS()){
                    rv.add(file.getPath());booksize++;
                }} catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return rv;
    }


    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLinear))
            mDrawerLayout.closeDrawer(mDrawerLinear);
        else {
            try {

                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
                String name = fragment.getClass().getName();
                if (name.contains("TabFragment")) {
                    TabFragment tabFragment = ((TabFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame));
                    Fragment fragment1 = tabFragment.getTab();
                    Main main = (Main) fragment1;
                    main.goBack();
                } else if (name.contains("ZipViewer")){
                    ZipViewer zipViewer = (ZipViewer) getSupportFragmentManager().findFragmentById(R.id.content_frame);
                    if(zipViewer.mActionMode==null) {
                        if (zipViewer.cangoBack()) {

                            zipViewer.goBack();
                        } else if(openzip) {
                            openzip=false;
                            finish();
                        } else {

                            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                            fragmentTransaction.remove(zipViewer);
                            fragmentTransaction.commit();
                            supportInvalidateOptionsMenu();

                            fabShowAnim = AnimationUtils.loadAnimation(this, R.anim.fab_newtab);
                            floatingActionButton.setAnimation(fabShowAnim);
                            floatingActionButton.animate();
                            floatingActionButton.setVisibility(View.VISIBLE);
                        }
                    } else {
                        zipViewer.mActionMode.finish();
                    }
                }else if (name.contains("RarViewer")) {

                    RarViewer zipViewer = (RarViewer) getSupportFragmentManager().findFragmentById(R.id.content_frame);
                    if(zipViewer.mActionMode==null) {
                        if (zipViewer.cangoBack()) {

                            zipViewer.elements.clear();
                            zipViewer.goBack();
                        } else if(openzip) {

                            openzip=false;finish();
                        } else {

                            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                            fragmentTransaction.remove(zipViewer);
                            fragmentTransaction.commit();
                            supportInvalidateOptionsMenu();

                            fabShowAnim = AnimationUtils.loadAnimation(this, R.anim.fab_newtab);
                            floatingActionButton.setAnimation(fabShowAnim);
                            floatingActionButton.animate();
                            floatingActionButton.setVisibility(View.VISIBLE);
                        }
                    } else {
                        zipViewer.mActionMode.finish();
                    }
                } else if(name.contains("Process")) {
                    finish();
                } else
                    goToMain("");
            } catch (ClassCastException e) {
                goToMain("");
            }
        }
    }

    public void invalidatePasteButton(MenuItem paste) {
        if (MOVE_PATH != null || COPY_PATH != null) {
            paste.setVisible(true);
        } else {
            paste.setVisible(false);
        }
    }

    public void exit() {
        if (backPressedToExitOnce) {
            finish();
        } else {
            this.backPressedToExitOnce = true;
            showToast(utils.getString(this, R.string.pressagain));
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    backPressedToExitOnce = false;
                }
            }, 2000);
        }
    }
    public void updateDrawer(){
        list.clear();
        for (String file:getStorageDirectories()) {
            File f=new File(file);
            if(!f.isDirectory())
                list.add(file);
            else if(f.canExecute())
                list.add(file);
        }
        /*list.add(utils.getString(this, R.string.apps));
        list.add(utils.getString(this, R.string.bookmanag));*/
        adapter = new DrawerAdapter(this, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);
    }
    public void updateDrawer(String path){

        if(list.contains(path))
        {
            select= list.indexOf(path);
            adapter.toggleChecked(select);
        }}
    public void goToMain(String path){
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //title.setText(R.string.app_name);
        TabFragment tabFragment=new TabFragment();
        if(path!=null && path.length()>0){
            Bundle b=new Bundle();
            b.putString("path",path);
            tabFragment.setArguments(b);
        }
        transaction.replace(R.id.content_frame, tabFragment);
        // Commit the transaction
        select=0;
        transaction.addToBackStack("tabt" + 1);
        transaction.commit();
        toolbar.setTitle(null);

        fabShowAnim = AnimationUtils.loadAnimation(this, R.anim.fab_newtab);
        tabsSpinner.setVisibility(View.VISIBLE);
        floatingActionButton.setAnimation(fabShowAnim);
        floatingActionButton.animate();
        floatingActionButton.setVisibility(View.VISIBLE);
        if(openzip && zippath!=null) {
            if(zippath.endsWith(".zip") || zippath.endsWith(".apk"))openZip(zippath);else{openRar(zippath);}zippath=null;
        }
    }
    public void selectItem(final int i, boolean removeBookmark) {
        if (select == null || select >= list.size() -2 && !removeBookmark) {

            TabFragment tabFragment=new TabFragment();
            Bundle a = new Bundle();
            a.putString("path", list.get(i));
            tabFragment.setArguments(a);

            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, tabFragment);

            transaction.addToBackStack("tabt1" + 1);
            pending_fragmentTransaction=transaction;
            select = i;
            adapter.toggleChecked(select);
            mDrawerLayout.closeDrawer(mDrawerLinear);
        }  else if (removeBookmark) {

            //adapter.notifyDataSetChanged();
            try {
                s.removeS(new File(list.get(i)), MainActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            list.remove(i);
        } else {

            pending_path=list.get(i);
            select = i;
            adapter.toggleChecked(select);
            mDrawerLayout.closeDrawer(mDrawerLinear);
        }

        adapter = new DrawerAdapter(this, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_extra, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem s = menu.findItem(R.id.view);
        MenuItem search = menu.findItem(R.id.search);
        MenuItem paste = menu.findItem(R.id.paste);
        String f= null;
        Fragment fragment;
        try { fragment=getSupportFragmentManager().findFragmentById(R.id.content_frame);
            f = fragment.getClass().getName();
        } catch (Exception e1) {
            return true;
        }
        if(f.contains("TabFragment")) {

            try {
                TabFragment tabFragment = (TabFragment) fragment;
                updatePath(((Main) tabFragment.getTab()).current, true);
            } catch (Exception e) {
            }
            tabsSpinner.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("");
            if (aBoolean) {
                s.setTitle(getResources().getString(R.string.gridview));
            } else {
                s.setTitle(getResources().getString(R.string.listview));
            }
            if (Build.VERSION.SDK_INT >= 21) toolbar.setElevation(0);
            invalidatePasteButton(paste);
            search.setVisible(true);
            menu.findItem(R.id.search).setVisible(true);
            menu.findItem(R.id.home).setVisible(true);
            menu.findItem(R.id.history).setVisible(true);
            menu.findItem(R.id.item10).setVisible(true);
            menu.findItem(R.id.hiddenitems).setVisible(true);
            menu.findItem(R.id.view).setVisible(true);
            invalidatePasteButton(menu.findItem(R.id.paste));
            findViewById(R.id.buttonbarframe).setVisibility(View.VISIBLE);
        } else if(f.contains("AppsList") || f.contains("ProcessViewer")) {
            tabsSpinner.setVisibility(View.GONE);
            findViewById(R.id.buttonbarframe).setVisibility(View.GONE);
            menu.findItem(R.id.search).setVisible(false);
            menu.findItem(R.id.home).setVisible(false);
            menu.findItem(R.id.history).setVisible(false);
            menu.findItem(R.id.item10).setVisible(false);
            menu.findItem(R.id.hiddenitems).setVisible(false);
            menu.findItem(R.id.view).setVisible(false);
            menu.findItem(R.id.paste).setVisible(false);
        } else if(f.contains("ZipViewer") || f.contains("RarViewer")) {
            tabsSpinner.setVisibility(View.GONE);
            menu.findItem(R.id.search).setVisible(false);
            menu.findItem(R.id.home).setVisible(false);
            menu.findItem(R.id.history).setVisible(false);
            menu.findItem(R.id.item10).setVisible(false);
            menu.findItem(R.id.hiddenitems).setVisible(false);
            menu.findItem(R.id.view).setVisible(false);
            menu.findItem(R.id.paste).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void showToast(String message) {
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

    private void killToast() {
        if (this.toast != null) {
            this.toast.cancel();
        }
    }

    public void back() {
        super.onBackPressed();
    }


    //// called when the user exits the action mode
//
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        Main ma=null;
        try {
            ma=(Main)((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        switch (item.getItemId()) {
            case R.id.home:
                ma.home();
                break;
            case R.id.history:
                utils.showHistoryDialog(ma);
                break;
            case R.id.item3:
                finish();
                break;
            case R.id.item10:
                utils.showSortDialog(ma);
                break;
            case R.id.hiddenitems:
                utils.showHiddenDialog(ma);
                break;
            case R.id.view:
                // Save the changes, but don't show a disruptive Toast:
                Sp.edit().putBoolean("view", !ma.islist).commit();
                ma.restartPC(ma.getActivity());
                break;
            case R.id.search:
                search();
                break;
            case R.id.paste:
                String path = ma.current;
                ArrayList<String> arrayList = new ArrayList<String>();
                if (COPY_PATH != null) {
                    arrayList = COPY_PATH;
                    new CheckForFiles(ma, path, false).execute(arrayList);
                } else if (MOVE_PATH != null) {
                    arrayList = MOVE_PATH;
                    new CheckForFiles(ma, path, true).execute(arrayList);
                }
                COPY_PATH = null;
                MOVE_PATH = null;

                invalidatePasteButton(item);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
    public void add(int pos) {
        final MainActivity mainActivity=this;
        final Main ma=(Main)((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        switch (pos) {

            case 0:
                final String path = ma.current;
                final MaterialDialog.Builder ba1 = new MaterialDialog.Builder(this);
                ba1.title(R.string.newfolder);
                View v = getLayoutInflater().inflate(R.layout.dialog, null);
                final EditText edir = (EditText) v.findViewById(R.id.newname);
                edir.setHint(utils.getString(this, R.string.entername));
                ba1.customView(v, true);
                if(theme1==1)ba1.theme(Theme.DARK);
                ba1.positiveText(R.string.create);
                ba1.negativeText(R.string.cancel);
                ba1.positiveColor(Color.parseColor(fabskin));
                ba1.negativeColor(Color.parseColor(fabskin));
                ba1.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        String a = edir.getText().toString();
                        File f = new File(path + "/" + a);
                        boolean b=false;
                        if (!f.exists()) {
                            b=f.mkdirs();
                            ma.updateList();
                            if(b)
                                Toast.makeText(mainActivity, (R.string.foldercreated), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(mainActivity, ( R.string.fileexist), Toast.LENGTH_LONG).show();
                        }
                        if(!b && rootmode){
                            RootTools.remount(f.getParent(), "rw");
                            RootHelper.runAndWait("mkdir "+f.getPath(),true);
                            ma.updateList();
                        }
                        else if(!b && !rootmode){
                            try {
                                new MediaFile(mainActivity,f).mkdir();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                    }
                });
                ba1.build().show();
                break;
            case 1:
                final String path1 = ma.current;
                final MaterialDialog.Builder ba2 = new MaterialDialog.Builder(this);
                ba2.title((R.string.newfile));
                View v1 = getLayoutInflater().inflate(R.layout.dialog, null);
                final EditText edir1 = (EditText) v1.findViewById(R.id.newname);
                edir1.setHint(utils.getString(this, R.string.entername));
                ba2.customView(v1, true);
                if(theme1==1)ba2.theme(Theme.DARK);
                ba2.negativeText(R.string.cancel);
                ba2.positiveText(R.string.create);
                ba2.positiveColor(Color.parseColor(fabskin));
                ba2.negativeColor(Color.parseColor(fabskin));
                ba2.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        String a = edir1.getText().toString();boolean b=false;
                        File f1 = new File(path1 + "/" + a);
                        if (!f1.exists()) {
                            try {
                                b = f1.createNewFile();
                                ma.updateList();
                                Toast.makeText(mainActivity, ( R.string.filecreated), Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(mainActivity,( R.string.fileexist), Toast.LENGTH_LONG).show();
                        }if(!b && rootmode)RootTools.remount(f1.getParent(),"rw");
                        RootHelper.runAndWait("touch "+f1.getPath(),true);
                        ma.updateList();
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                    }
                });
                ba2.build().show();
                break;
        }
    }


    public void search() {
        final Main ma=(Main)((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        final String fpath = ma.current;
        final MaterialDialog.Builder a = new MaterialDialog.Builder(this);
        a.title(R.string.search);
        View v =getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText e = (EditText) v.findViewById(R.id.newname);
        e.setHint(utils.getString(this, R.string.enterfile));
        a.customView(v, true);
        e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                e.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager inputMethodManager = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(e, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
        e.requestFocus();
        if(theme1==1)a.theme(Theme.DARK);
        a.negativeText(R.string.cancel);
        a.positiveText(R.string.search);
        a.positiveColor(Color.parseColor(fabskin));
        a.negativeColor(Color.parseColor(fabskin));
        a.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                String a = e.getText().toString();
                Intent i=new Intent(con, SearchService.class);
                i.putExtra("path",fpath);
                i.putExtra("text",a);
                startService(i);
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {

            }
        });
        a.build().show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position, false);

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectitem", select);
    }

    @Override
    protected void onPause() {
        super.onPause();
        killToast();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(SEARCHRECIEVER);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(LOADSEARCHRECIEVER);
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(SEARCHRECIEVER, new IntentFilter("searchresults"));
        LocalBroadcastManager.getInstance(this).registerReceiver(LOADSEARCHRECIEVER, new IntentFilter("loadsearchresults"));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            /*ImageView ib = (ImageView) findViewById(R.id.action_overflow);
            if (ib.getVisibility() == View.VISIBLE) {
                ib.performClick();
            }*/
            // perform your desired action here

            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }
    ProgressDialog p;
    private BroadcastReceiver SEARCHRECIEVER = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                int  paths=intent.getIntExtra("paths", 0);
                if(p==null ) {
                    p = new ProgressDialog(con);
                    p.setMessage("Found " +paths);
                    p.setIndeterminate(true);
                    p.setTitle(R.string.searching);
                    p.setCancelable(false);
                    p.setButton(DialogInterface.BUTTON_POSITIVE,utils.getString(con,R.string.cancel),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.out.println("Broadcast sent");
                            LocalBroadcastManager.getInstance(con).sendBroadcast(new Intent("searchcancel"));
                            dialog.cancel();
                        }
                    });
                }else{
                    p.setMessage("Found " +paths);
                }p.show();

            }
        }
    };
    private BroadcastReceiver LOADSEARCHRECIEVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle e = intent.getExtras();
            System.out.println("GOT IT");
            if(p!=null && p.isShowing())p.cancel();
            if (e != null) {
                ArrayList<String[]> arrayList=new ArrayList<String[]>();
                ArrayList<String> b=e.getStringArrayList("b");
                ArrayList<String> c=e.getStringArrayList("c");
                ArrayList<String> d=e.getStringArrayList("d");
                ArrayList<String> f=e.getStringArrayList("f");
                for(int i=0;i<b.size();i++){
                    arrayList.add(new String[]{b.get(i),c.get(i),d.get(i),f.get(i)});
                }
                Fragment fragment=getFragment().getTab();
                if(fragment.getClass().getName().contains("Main")){
                    ((Main)fragment).loadsearchlist(arrayList);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        boolean check = Sp.getBoolean("random_checkbox", false);
        if (check) {
            PreferenceUtils.random(Sp);
        }
        Sp.edit().putBoolean("remember", true).apply();
        unregisterReceiver(mNotificationReceiver);
    }

    class CheckForFiles extends AsyncTask<ArrayList<String>, String, ArrayList<String>> {
        Main ma;
        String path;
        Boolean move;
        ArrayList<String> ab, a, b, lol;
        int counter = 0;

        public CheckForFiles(Main main, String path, Boolean move) {
            this.ma = main;
            this.path = path;
            this.move = move;
            a = new ArrayList<String>();
            b = new ArrayList<String>();
            lol = new ArrayList<String>();
        }

        @Override
        public void onProgressUpdate(String... message) {
            Toast.makeText(con, message[0], Toast.LENGTH_LONG).show();
        }

        @Override
        // Actual download method, run in the task thread
        protected ArrayList<String> doInBackground(ArrayList<String>... params) {

            ab = params[0];
            long totalBytes = 0;

            for (int i = 0; i < params[0].size(); i++) {

                File f1 = new File(params[0].get(i));

                if (f1.isDirectory()) {

                    totalBytes = totalBytes + new Futils().folderSize(f1);
                } else {

                    totalBytes = totalBytes + f1.length();
                }
            }

            if (new File(path).getUsableSpace() > totalBytes) {

                File f = new File(path);

                for (String k1[] : RootHelper.getFilesList(f.getPath(),rootmode,true,false)) {
                    File k=new File(k1[0]);
                    for (String j : ab) {

                        if (k.getName().equals(new File(j).getName())) {

                            a.add(j);}
                    }
                }
            } else publishProgress(utils.getString(con,R.string.in_safe));

            return a;
        }

        public void showDialog() {

            if (counter == a.size() || a.size()==0) {

                if (ab != null && ab.size()!=0) {

                    if(!move){

                        Intent intent = new Intent(con, CopyService.class);
                        intent.putExtra("FILE_PATHS", ab);
                        intent.putExtra("COPY_DIRECTORY", path);
                        startService(intent);
                    } else{

                        new MoveFiles(utils.toFileArray(ab), ma,ma.getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path);
                    }
                } else {

                    Toast.makeText(MainActivity.this, utils.getString(con,R.string.no_file_overwrite), Toast.LENGTH_SHORT).show();
                }
            } else {

                final MaterialDialog.Builder x = new MaterialDialog.Builder(MainActivity.this);
                LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.copy_dialog, null);
                x.customView(view, true);
                // textView
                TextView textView = (TextView) view.findViewById(R.id.textView);
                textView.setText(utils.getString(con,R.string.fileexist) + "\n" + new File(a.get(counter)).getName());
                // checkBox
                final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
                if(theme1==1)x.theme(Theme.DARK);
                x.title(utils.getString(con, R.string.paste));
                x.positiveText(R.string.skip);
                x.negativeText(R.string.overwrite);
                x.neutralText(R.string.cancel);
                x.positiveColor(Color.parseColor(fabskin));
                x.negativeColor(Color.parseColor(fabskin));
                x.neutralColor(Color.parseColor(fabskin));
                x.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {

                        if (counter < a.size()) {

                            if (!checkBox.isChecked()) {

                                ab.remove(a.get(counter));
                                counter++;

                            } else {
                                for (int j = counter; j < a.size(); j++) {

                                    ab.remove(a.get(j));
                                }
                                counter = a.size();
                            }
                            showDialog();
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                        if (counter < a.size()) {

                            if (!checkBox.isChecked()) {

                                counter++;
                            } else {

                                counter = a.size();
                            }
                            showDialog();
                        }

                    }
                });
                final MaterialDialog y=x.build();
                y.show();
                if (new File(ab.get(0)).getParent().equals(path)) {
                    View negative = y.getActionButton(DialogAction.NEGATIVE);
                    negative.setEnabled(false);
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            showDialog();
        }
    }
    public void updatepager() {
        getFragment().mSectionsPagerAdapter.notifyDataSetChanged();
    }
    public void updatepaths(){
        try {
            getFragment().updatepaths();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openZip(String path) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_in_bottom);
        Fragment zipFragment = new ZipViewer();
        Bundle bundle = new Bundle();
        bundle.putString("path", path);
        zipFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.content_frame, zipFragment);
        fragmentTransaction.commit();
    }
    public void openRar(String path) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_in_bottom);
        Fragment zipFragment = new RarViewer();
        Bundle bundle = new Bundle();
        bundle.putString("path", path);
        zipFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.content_frame, zipFragment);
        fragmentTransaction.commit();
    }
    public TabFragment getFragment(){
        TabFragment tabFragment=(TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame);
        return tabFragment;
    }
    public void setPagingEnabled(boolean b){
        getFragment().mViewPager.setPagingEnabled(b);
    }

    public  File getUsbDrive() {
        File parent ;
        parent=new File("/storage");

        try{
            for(File f:parent.listFiles())
            {if(f.exists() && f.getName().toLowerCase().contains("usb") && f.canExecute()){
                return f;
            }}}catch (Exception e){}
        parent = new File("/mnt/sdcard/usbStorage");
        if (parent.exists() && parent.canExecute())
            return (parent);
        parent = new File("/mnt/sdcard/usb_storage");
        if (parent.exists() && parent.canExecute())
            return parent;

        return null;
    }

    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    Toast.makeText(con,"Media Mounted",Toast.LENGTH_SHORT).show();
                    String a=intent.getData().getPath();
                    if(a!=null && a.trim().length()!=0 && new File(a).exists() && new File(a).canExecute()){
                        list.add(0,a);
                        adapter.notifyDataSetChanged();
                    }else {refreshDrawer();
                    }
                }else if(intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)){

                    refreshDrawer();
                }
            }
        }
    };
    public void refreshDrawer(){
        val=getStorageDirectories();
        list = new ArrayList<String>();
        for (int i = 0; i < val.size(); i++) {
            File file = new File(val.get(i));
            if(!file.isDirectory())
                list.add(val.get(i));
            else if(file.canExecute())
                list.add(val.get(i));
        }


        adapter = new DrawerAdapter(con, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);

    }

    public void bbar(final Main main) {
        final String text = main.current;
        try {
            buttons.removeAllViews();
            buttons.setMinimumHeight(pathbar.getHeight());
            Drawable arrow=getResources().getDrawable(R.drawable.abc_ic_ab_back_holo_dark);
            Bundle b = utils.getPaths(text, this);
            ArrayList<String> names = b.getStringArrayList("names");
            ArrayList<String> rnames = new ArrayList<String>();

            for (int i = names.size() - 1; i >= 0; i--) {
                rnames.add(names.get(i));
            }

            ArrayList<String> paths = b.getStringArrayList("paths");
            final ArrayList<String> rpaths = new ArrayList<String>();

            for (int i = paths.size() - 1; i >= 0; i--) {
                rpaths.add(paths.get(i));
            }
            for (int i = 0; i < names.size(); i++) {
                final int k=i;
                ImageView v=new ImageView(this);
                v.setImageDrawable(arrow);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity= Gravity.CENTER_VERTICAL;
                v.setLayoutParams(params);
                final int index = i;
                if (rpaths.get(i).equals("/")) {
                    ImageButton ib = new ImageButton(this);
                    ib.setImageDrawable(icons.getRootDrawable());
                    ib.setBackgroundColor(Color.parseColor("#00ffffff"));
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            main.loadlist(new File("/"), false);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    ib.setLayoutParams(params);
                    buttons.addView(ib);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                } else if (rpaths.get(i).equals(Environment.getExternalStorageDirectory().getPath())) {
                    ImageButton ib = new ImageButton(this);
                    ib.setImageDrawable(icons.getSdDrawable());
                    ib.setBackgroundColor(Color.parseColor("#00ffffff"));
                    ib.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            main.loadlist(new File(rpaths.get(k)), false);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    ib.setLayoutParams(params);
                    buttons.addView(ib);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                } else {
                    Button button = new Button(this);
                    button.setText(rnames.get(index));
                    button.setTextColor(getResources().getColor(android.R.color.white));
                    button.setTextSize(13);
                    button.setLayoutParams(params);
                    button.setBackgroundResource(0);
                    button.setOnClickListener(new Button.OnClickListener() {

                        public void onClick(View p1) {
                            main.loadlist(new File(rpaths.get(k)), false);
                            timer.cancel();
                            timer.start();
                        }
                    });
                    button.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {

                            File file1 = new File(rpaths.get(index));
                            copyToClipboard(MainActivity.this, file1.getPath());
                            Toast.makeText(MainActivity.this, getResources().getString(R.string.pathcopied), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });

                    buttons.addView(button);
                    if(names.size()-i!=1)
                        buttons.addView(v);
                }
            }
            File f=new File(text);

            TextView textView = (TextView)pathbar.findViewById(R.id.pathname);
            String used = utils.readableFileSize(f.getTotalSpace()-f.getFreeSpace());
            String free = utils.readableFileSize(f.getFreeSpace());
            textView.setText(getResources().getString(R.string.used)+" " + used +" "+ getResources().getString(R.string.free)+" " + free);

            TextView bapath=(TextView)pathbar.findViewById(R.id.fullpath);
            bapath.setText(f.getPath());
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    sendScroll(scroll);
                    sendScroll(scroll1);}
            });

            if(buttons.getVisibility()==View.VISIBLE){timer.cancel();timer.start();}
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("button view not available");
        }
    }
    private void sendScroll(final HorizontalScrollView scrollView){
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {Thread.sleep(100);} catch (InterruptedException e) {}
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_RIGHT);
                    }
                });
            }
        }).start();
    }
    public void updatePath(final String newPath,boolean calcsize){
        File f= null;
        try {
            f = new File(newPath);
        } catch (Exception e) {
            return;
        }   TextView textView = (TextView) pathbar.findViewById(R.id.pathname);
        textView.setText("");
        if(calcsize) {
            String used = utils.readableFileSize(f.getTotalSpace() - f.getFreeSpace());
            String free = utils.readableFileSize(f.getFreeSpace());
            textView.setText(getResources().getString(R.string.used) + " " + used + " " + getResources().getString(R.string.free) + " " + free);
        }final TextView bapath=(TextView)pathbar.findViewById(R.id.fullpath);
        final TextView animPath = (TextView) pathbar.findViewById(R.id.fullpath_anim);
        final String oldPath=bapath.getText().toString();
        // implement animation while setting text
        final StringBuilder stringBuilder = new StringBuilder();
        if (newPath.length() >= oldPath.length()) {
            // navigate forward
            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
            stringBuilder.append(newPath);
            stringBuilder.delete(0, oldPath.length());
            animPath.setAnimation(slideIn);
            animPath.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animPath.setVisibility(View.GONE);
                    bapath.setText(newPath);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    animPath.setVisibility(View.VISIBLE);
                    animPath.setText(stringBuilder.toString());
                    //bapath.setText(oldPath);

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            scroll1.fullScroll(View.FOCUS_RIGHT);
                        }
                    });
                }
            }).start();
        } else if (newPath.length() <= oldPath.length()) {
            // navigate backwards
            Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
            stringBuilder.append(oldPath);
            stringBuilder.delete(0, newPath.length());
            animPath.setAnimation(slideOut);
            animPath.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    animPath.setVisibility(View.GONE);
                    bapath.setText(newPath);

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            scroll1.fullScroll(View.FOCUS_RIGHT);
                        }
                    });
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    animPath.setVisibility(View.VISIBLE);
                    animPath.setText(stringBuilder.toString());
                    bapath.setText(newPath);

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            scroll1.fullScroll(View.FOCUS_LEFT);
                        }
                    });
                }
            }).start();
        }
    }

    public void initiatebbar() {
        LinearLayout pathbar = (LinearLayout) findViewById(R.id.pathbar);
        TextView textView = (TextView) findViewById(R.id.fullpath);

        pathbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bbar(((Main)getFragment().getTab()));
                crossfade();
                timer.cancel();
                timer.start();
            }
        });
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bbar(((Main)getFragment().getTab()));
                crossfade();
                timer.cancel();
                timer.start();
            }
        });

    }

    public void crossfade() {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        buttons.setAlpha(0f);
        buttons.setVisibility(View.VISIBLE);



        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        buttons.animate()
                .alpha(1f)
                .setDuration(100)
                .setListener(null);
        pathbar.animate()
                .alpha(0f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        pathbar.setVisibility(View.GONE);
                    }
                });
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)

    }

    private void crossfadeInverse() {


        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.

        pathbar.setAlpha(0f);
        pathbar.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        pathbar.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);
        buttons.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        buttons.setVisibility(View.GONE);
                    }
                });
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
    }

    public boolean copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("Path copied to clipboard", text);
            clipboard.setPrimaryClip(clip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}