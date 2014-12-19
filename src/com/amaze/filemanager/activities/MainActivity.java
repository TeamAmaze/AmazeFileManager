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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.BookmarksManager;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.ProcessViewer;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.asynctasks.MoveFiles;
import com.amaze.filemanager.services.asynctasks.SearchTask;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.MediaFile;
import com.amaze.filemanager.utils.RootHelper;
import com.amaze.filemanager.utils.Shortcuts;
import com.melnykov.fab.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.stericson.RootTools.RootTools;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;


public class MainActivity extends android.support.v4.app.FragmentActivity {
    public Integer select;
    TextView title;

    Futils utils;
    private boolean backPressedToExitOnce = false;
    private Toast toast = null;
    private DrawerLayout mDrawerLayout;
    public ListView mDrawerList;
    SharedPreferences Sp;
    private ActionBarDrawerToggle mDrawerToggle;
    ImageButton paste;
    public List<String> val;
    ProgressWheel progress;
    DrawerAdapter adapter;
    IconUtils util;
    RelativeLayout mDrawerLinear;
    Shortcuts s;
    int tab = 0;
    public String skin,path;
    public int theme;
    public ArrayList<String> COPY_PATH = null, MOVE_PATH = null;
    Context con = this;
    public FrameLayout frameLayout;
    public boolean mReturnIntent = false;
    private Intent intent;
    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");
    public ArrayList<String> list;
    public int theme1;
    boolean rootmode,aBoolean;
    public Spinner tabsSpinner;
    public boolean mRingtonePickerIntent = false,restart=false;
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
        getActionBar().hide();
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        utils = new Futils();
        s=new Shortcuts(this);
        path=getIntent().getStringExtra("path");
        restart=getIntent().getBooleanExtra("restart",false);
        val = getStorageDirectories();
        rootmode = Sp.getBoolean("rootmode", false);
        theme = Integer.parseInt(Sp.getString("theme", "0"));
        util = new IconUtils(Sp, this);

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int th = Integer.parseInt(Sp.getString("theme", "0"));
        theme1 = th;
        if (th == 2) {
            Sp.edit().putString("uimode", "0").commit();
            if(hour<=6 || hour>=18) {
                theme1 = 1;
            } else
                theme1 = 0;
        }

        if (theme1 == 1) {
            setTheme(R.style.DarkTheme);
        }
        setContentView(R.layout.main);
        aBoolean = Sp.getBoolean("view", true);
        ImageView overflow = ((ImageView)findViewById(R.id.action_overflow));

       showPopup(overflow);
        tabsSpinner = (Spinner) findViewById(R.id.tab_spinner);
        title = (TextView) findViewById(R.id.title);
        frameLayout = (FrameLayout) findViewById(R.id.content_frame);
        paste = (ImageButton) findViewById(R.id.paste);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Main ma = ((TabFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
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

                invalidatePasteButton();

            }
        });

        intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_GET_CONTENT) || intent.getAction().equals(RingtoneManager.ACTION_RINGTONE_PICKER)) {
            mReturnIntent = true;
            if (intent.getAction().equals(RingtoneManager.ACTION_RINGTONE_PICKER)) {
                mRingtonePickerIntent = true;
            }
            Toast.makeText(this, utils.getString(con,R.string.pick_a_file), Toast.LENGTH_LONG).show();
        }

        skin = PreferenceManager.getDefaultSharedPreferences(this).getString("skin_color", "#5677fc");
        RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.action_bar);
        linearLayout.setBackgroundColor(Color.parseColor(skin));
        LinearLayout linearLayout1 = (LinearLayout) findViewById(R.id.pathbar);
        linearLayout1.setBackgroundColor(Color.parseColor(skin));
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*if(tab==0){
                    replaceFragment(1);
                }else{replaceFragment(0);}*/
            }
        });
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.scroll1);
        horizontalScrollView.setBackgroundColor(Color.parseColor(skin));
        HorizontalScrollView horizontalScrollView1 = (HorizontalScrollView) findViewById(R.id.scroll);
        horizontalScrollView1.setBackgroundColor(Color.parseColor(skin));
        LinearLayout linearLayout3 = (LinearLayout) findViewById(R.id.settings_bg);
        linearLayout3.setBackgroundColor(Color.parseColor(skin));

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(con);
                builder.items(new String[]{
                        getResources().getString(R.string.folder),
                        getResources().getString(R.string.file),
                        getResources().getString(R.string.tab)
                });
                builder.itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
                        add(i);
                    }
                });
                builder.title(getResources().getString(R.string.new_string));
                if(theme1==1)
                    builder.theme(Theme.DARK);
                builder.build().show();
            }
        });
        if (Sp.getBoolean("firstrun", true)) {
            try {
                s.makeS();
            } catch (Exception e) {
            }

            File file = new File(val.get(0));
            Sp.edit().putString("home", file.getPath()).apply();
            Sp.edit().putBoolean("firstrun", false).commit();
        }
        mDrawerLinear = (RelativeLayout) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.menu_drawer);
        View v=getLayoutInflater().inflate(R.layout.drawerheader,null);
        v.setBackgroundColor(Color.parseColor(skin));
        mDrawerList.addHeaderView(v);
        (findViewById(R.id.search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search();
            }
        });
        list = new ArrayList<String>();
        for (int i = 0; i < val.size(); i++) {
            File file = new File(val.get(i));
            if(!file.isDirectory())
                list.add(val.get(i));
            else if(file.canExecute())
                list.add(val.get(i));
        }
        list.add(utils.getString(this, R.string.apps));
        list.add(utils.getString(this, R.string.bookmanag));
        adapter = new DrawerAdapter(this, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);
        initiatebbar();
        if (savedInstanceState == null) {
            if(!restart)
            selectItem(0);
            else goToMain();
        } else {
            select = savedInstanceState.getInt("selectitem", 0);

            adapter.toggleChecked(select);

        }
        if (Build.VERSION.SDK_INT >= 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            DrawerLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mDrawerLayout.getLayoutParams();
            p.setMargins(0, config.getPixelInsetTop(false), 0, 0);
        }final Activity activity=this;
        ((ImageButton) findViewById(R.id.settingsbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(MainActivity.this, Preferences.class);

                final int enter_anim = android.R.anim.fade_in;
                final int exit_anim = android.R.anim.fade_out;

                activity.overridePendingTransition(enter_anim, exit_anim);
                activity.finish();
                activity.overridePendingTransition(enter_anim, exit_anim);
                activity.startActivity(in);
            }
        });
        if (theme1 == 1) {
            mDrawerList.setBackgroundResource(android.R.drawable.screen_background_dark);
        }
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setDivider(null);
        progress = (ProgressWheel) findViewById(R.id.progressBar);
        progress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (select != 102) {
                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.content_frame, new ProcessViewer());
                    //   transaction.addToBackStack(null);
                    select = 102;

                    title.setText(utils.getString(con, R.string.process_viewer));
                    //Commit the transaction
                    transaction.commit();
                } else {
                    selectItem(0);
                }
            }
        });
        if (select == 0) {

            title.setVisibility(View.GONE);
            tabsSpinner.setVisibility(View.VISIBLE);
        } else {

            title.setVisibility(View.VISIBLE);
            tabsSpinner.setVisibility(View.GONE);
        }
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                util.getDrawerDrawable(),  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                if(select==102)title.setText(R.string.process_viewer);
              // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //title.setText("Amaze File Manager");
                // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        ((ImageButton) findViewById(R.id.drawer_buttton)).setOnClickListener(new ImageView.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDrawerLayout.isDrawerOpen(mDrawerLinear)) {
                    mDrawerLayout.closeDrawer(mDrawerLinear);
                } else mDrawerLayout.openDrawer(mDrawerLinear);
            }
        });

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
        try {
            for(File file: s.readS()){
                rv.add(file.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return rv;
    }


    @Override
    public void onBackPressed() {
if(mDrawerLayout.isDrawerOpen(mDrawerLinear))
   mDrawerLayout.closeDrawer(mDrawerLinear); else{
    if (select < list.size() - 2) {
            try {

                TabFragment tabFragment = ((TabFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame));
                Fragment fragment=tabFragment.getTab();
                String name=fragment.getClass().getName();
                if(name.contains("Main")){
                Main main=(Main)fragment;
                if (main.results == true) {
                main.results = false;
                main.loadlist(new File(main.current), true);
            } else {
                    if (!main.current.equals(main.home) && !main.current.equals("/")) {
                        main.goBack();

                    } else {
                        exit();
                    }
                }
            }else if(name.contains("ZipViewer")){
                    ZipViewer zipViewer=(ZipViewer)fragment;
                    if (zipViewer.cangoBack()) {

                        zipViewer.goBack();
                    } else tabFragment.removeTab();
                }}catch (ClassCastException e){goToMain();}
        }
        else {
            goToMain();
        }
    }}

    public void invalidatePasteButton() {
        if (MOVE_PATH != null || COPY_PATH != null) {
            paste.setVisibility(View.VISIBLE);
        } else
            paste.setVisibility(View.INVISIBLE);
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
        list.add(utils.getString(this, R.string.apps));
        list.add(utils.getString(this, R.string.bookmanag));
        adapter = new DrawerAdapter(this, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);
    }
    public void updateDrawer(String path){

        if(list.contains(path))
        {select= list.indexOf(path);
            adapter.toggleChecked(select);
        }}
    public void goToMain(){
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        title.setText(R.string.app_name);
        transaction.replace(R.id.content_frame, new TabFragment());
        // Commit the transaction
        select=0;
        transaction.addToBackStack("tabt" + 1);
        transaction.commit();

    }
    public void selectItem(final int i) {

        if (i < list.size() - 2) {

                if (select == null || select >= list.size() - 2) {
                    TabFragment tabFragment=new TabFragment();
                    if (path != null) {
                        Bundle a = new Bundle();
                        a.putString("path", path);
                        tabFragment.setArguments(a);
                    }
                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                    transaction.replace(R.id.content_frame, tabFragment);
                    select = i;
                    // Commit the transaction
                    transaction.addToBackStack("tab1" + 1);
                    transaction.commit();


                }else{
                    try {
                        TabFragment m=((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame));
                    if(new File(list.get(i)).isDirectory())   m.addTab1(list.get(i));
                        else utils.openFile(new File(list.get(i)),this);

                    } catch (ClassCastException e) {
                        select=null;selectItem(0);
                    }
            }
            title.setText(R.string.app_name);}else {
            if (i == list.size() - 2) {

                android.support.v4.app.FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                transaction2.replace(R.id.content_frame, new AppsList());
                // transaction2.addToBackStack(null);
                select = i;
                // Commit the transaction
                transaction2.commit();
                title.setText(utils.getString(this, R.string.apps));
                title.setVisibility(View.VISIBLE);
            } else if (i == list.size() - 1) {

                android.support.v4.app.FragmentTransaction transaction3 = getSupportFragmentManager().beginTransaction();
                transaction3.replace(R.id.content_frame, new BookmarksManager());
                // transaction3.addToBackStack(null);
                select = i;
                // Commit the transaction
                transaction3.commit();
                title.setText(utils.getString(this, R.string.bookmanag));
                title.setVisibility(View.VISIBLE);
                }
        }
        adapter.toggleChecked(select);
        mDrawerLayout.closeDrawer(mDrawerLinear);
    }

    public void showPopup(View v) {
        final PopupMenu popup = new PopupMenu(this, v);
        if(Build.VERSION.SDK_INT>=19)
            v.setOnTouchListener(popup.getDragToOpenListener());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.show();
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.activity_extra, popup.getMenu());

        // Getting option for listView and gridView
        MenuItem s = popup.getMenu().findItem(R.id.view);

        if (aBoolean) {
            s.setTitle(getResources().getString(R.string.gridview));
        } else {
            s.setTitle(getResources().getString(R.string.listview));
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
            Main ma=((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
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
                    case R.id.item4:
                        ma.ic.cleanup();
                        ma.loadlist(new File(ma.current), false);
                        break;
                    case R.id.view:
                        // Save the changes, but don't show a disruptive Toast:
                        Sp.edit().putBoolean("view", !ma.aBoolean).commit();
                        ma.restartPC(ma.getActivity());
                        break;
                }
                return false;
            }
        });

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
        final Main ma=((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        switch (pos) {

            case 0:
                final String path = ma.current;
                final MaterialDialog.Builder ba1 = new MaterialDialog.Builder(this);
                ba1.title(R.string.newfolder);
                View v = getLayoutInflater().inflate(R.layout.dialog, null);
                final EditText edir = (EditText) v.findViewById(R.id.newname);
                edir.setHint(utils.getString(this, R.string.entername));
                ba1.customView(v);
                if(theme1==1)ba1.theme(Theme.DARK);
                ba1.positiveText(R.string.create);
                ba1.negativeText(R.string.cancel);
                ba1.positiveColor(Color.parseColor(skin));
                ba1.negativeColor(Color.parseColor(skin));
                ba1.callback(new MaterialDialog.Callback() {
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
                ba2.customView(v1);
                if(theme1==1)ba2.theme(Theme.DARK);
                ba2.negativeText(R.string.cancel);
                ba2.positiveText(R.string.create);
                ba2.positiveColor(Color.parseColor(skin));
                ba2.negativeColor(Color.parseColor(skin));
                ba2.callback(new MaterialDialog.Callback() {
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
            case 2:
                addTab();
                break;
        }
    }


    public void search() {
        final Main ma=((TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        final String fpath = ma.current;
        final MainActivity mainActivity=this;
        //Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.searchpath) + fpath, Toast.LENGTH_LONG).show();
        final MaterialDialog.Builder a = new MaterialDialog.Builder(this);
        a.title(R.string.search);
        View v =getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText e = (EditText) v.findViewById(R.id.newname);
        e.setHint(utils.getString(this, R.string.enterfile));
        a.customView(v);
        if(theme1==1)a.theme(Theme.DARK);
        a.negativeText(R.string.cancel);
        a.positiveText(R.string.search);
        a.positiveColor(Color.parseColor(skin));
        a.negativeColor(Color.parseColor(skin));
        a.callback(new MaterialDialog.Callback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                String a = e.getText().toString();
                Bundle b = new Bundle();
                b.putString("FILENAME", a);
                b.putString("FILEPATH", fpath);
                new SearchTask(mainActivity, ma).execute(b);

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
//

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);

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
        unregisterReceiver(RECIEVER);
        progress.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(RECIEVER, new IntentFilter("run"));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            ImageView ib = (ImageView) findViewById(R.id.action_overflow);
            if (ib.getVisibility() == View.VISIBLE) {
                ib.performClick();
            }
            // perform your desired action here

            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    private BroadcastReceiver RECIEVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            if (b != null) {
                progress.setVisibility(b.getBoolean("run", false) ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };

    private void random() {

        String[] colors = new String[]{
                "#e51c23",
                "#e51c23",
                "#e91e63",
                "#9c27b0",
                "#673ab7",
                "#3f51b5",
                "#5677fc",
                "#0288d1",
                "#0097a7",
                "#009688",
                "#259b24",
                "#8bc34a",
                "#ffa000",
                "#f57c00",
                "#e64a19",
                "#795548",
                "#212121",
                "#607d8b"
        };

        Random random = new Random();
        int pos = random.nextInt(colors.length - 1);
        Sp.edit().putString("skin_color", colors[pos]).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        boolean check = Sp.getBoolean("random_checkbox", false);
        if (check) {
            random();
        }
        Sp.edit().putBoolean("remember", true).apply();
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

                for (String k1[] : RootHelper.getFilesList(f.getPath(),rootmode,true)) {
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
                x.customView(view);
                // textView
                TextView textView = (TextView) view.findViewById(R.id.textView);
                textView.setText(utils.getString(con,R.string.fileexist) + new File(a.get(counter)).getName());
                // checkBox
                final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
                if(theme1==1)x.theme(Theme.DARK);
                x.title(utils.getString(con, R.string.paste));
                x.positiveText(R.string.skip);
                x.negativeText(R.string.overwrite);
                x.neutralText(R.string.cancel);
                x.positiveColor(Color.parseColor(skin));
                x.negativeColor(Color.parseColor(skin));
                x.neutralColor(Color.parseColor(skin));
                x.callback(new MaterialDialog.Callback() {
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
        TabFragment tabFragment = (TabFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        tabFragment.mSectionsPagerAdapter.notifyDataSetChanged();
    }
    public boolean shouldbbar(String path){
        TabFragment tabFragment = (TabFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if(tabFragment.getTab().current.equals(path))
            return true;
        return false;
    }
    public void addZipViewTab(String text){
        TabFragment tabFragment = (TabFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        tabFragment.addZipViewerTab(text);
    }
    public void addTab(){
        TabFragment tabFragment=(TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame);
        tabFragment.addTab1("");
    }
    public TabFragment getFragment(){
        TabFragment tabFragment=(TabFragment)getSupportFragmentManager().findFragmentById(R.id.content_frame);
        return tabFragment;
    }
    public void updateActionButtons(){
        TabFragment tabFragment=getFragment();
        String name=tabFragment.getTab1().getClass().getName();
        if(name.contains("Main")) {
            invalidatePasteButton();
            findViewById(R.id.search).setVisibility(View.VISIBLE);
            findViewById(R.id.action_overflow).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.search).setVisibility(View.GONE);
            findViewById(R.id.action_overflow).setVisibility(View.GONE);
            paste.setVisibility(View.GONE);
        }
    }
    public void initiatebbar() {
        LinearLayout pathbar = (LinearLayout) findViewById(R.id.pathbar);
        TextView textView = (TextView) findViewById(R.id.fullpath);

        pathbar.setBackgroundColor(Color.parseColor(skin));

        pathbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment fragment=getFragment().getTab1();
                if(fragment.getClass().getName().contains("Main")){
                Main main=(Main)fragment;
                    main.bbar(main.current);
                    main.crossfade();
                main.timer.cancel();
                main.timer.start();}
            }
        });
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment fragment=getFragment().getTab1();
                if(fragment.getClass().getName().contains("Main")){
                    Main main=(Main)fragment;
                    main.bbar(main.current);
                    main.crossfade();
                    main.timer.cancel();
                    main.timer.start();
                }
             }
        });

    }
    public void updatespinner(){
        getFragment().updateSpinner();
    }
}
