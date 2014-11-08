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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.fragments.BookmarksManager;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.ProcessViewer;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.Shortcuts;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;


public class MainActivity extends android.support.v4.app.FragmentActivity {
    public int select;
    TextView title;

    Futils utils;
    private boolean backPressedToExitOnce = false;
    private Toast toast = null;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    SharedPreferences Sp;
    private ActionBarDrawerToggle mDrawerToggle;
    public Spinner tabsSpinner;
    private TabHandler tabHandler;
    ImageButton paste;
    public String[] val;
    ProgressBar progress;
    DrawerAdapter adapter;
    IconUtils util;
    RelativeLayout mDrawerLinear;
    Shortcuts s = new Shortcuts();
    int tab = 0;
    public String skin;
    public int theme;
    public ArrayList<String> COPY_PATH = null, MOVE_PATH = null;
    Context con = this;
    public FrameLayout frameLayout;
    public boolean mReturnIntent = false;
    private Intent intent;
    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");
    private ArrayList<String> list;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Futils();

        val = getStorageDirectories();
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        theme = Integer.parseInt(Sp.getString("theme", "0"));
        util = new IconUtils(Sp, this);
        int th = Integer.parseInt(Sp.getString("theme", "0"));
        if (th == 1) {
            setTheme(R.style.DarkTheme);
        }
        setContentView(R.layout.main);
        getActionBar().hide();
        title = (TextView) findViewById(R.id.title);
        tabsSpinner = (Spinner) findViewById(R.id.tab_spinner);
        frameLayout = (FrameLayout) findViewById(R.id.content_frame);
        paste = (ImageButton) findViewById(R.id.paste);
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Main ma = ((Main) getSupportFragmentManager().findFragmentById(R.id.content_frame));
                String path = ma.current;
                ArrayList<String> arrayList = new ArrayList<String>();
                if (COPY_PATH != null) {
                    arrayList = COPY_PATH;
                    new CheckForFiles(ma, path, false).execute(arrayList);
                } else if (MOVE_PATH != null) {
                    arrayList = MOVE_PATH;
                    new CheckForFiles(ma, path, false).execute(arrayList);
                }
                COPY_PATH = null;
                MOVE_PATH = null;

                invalidatePasteButton();

            }
        });

        intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
            mReturnIntent = true;
            Toast.makeText(this, "Pick a file", Toast.LENGTH_LONG).show();
        }

        skin = PreferenceManager.getDefaultSharedPreferences(this).getString("skin_color", "#673ab7");
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
        LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.drawerheader);
        linearLayout2.setBackgroundColor(Color.parseColor(skin));
        LinearLayout linearLayout3 = (LinearLayout) findViewById(R.id.settings_bg);
        linearLayout3.setBackgroundColor(Color.parseColor(skin));

        //tabsSpinner.setPopupBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
        tabHandler = new TabHandler(this, "", null, 1);
        if (Sp.getBoolean("firstrun", true)) {
            try {
                s.makeS();
            } catch (Exception e) {
            }

            File file = new File(val[0]);
            tabHandler.addTab(new Tab(0, file.getName(), file.getPath()));
            Sp.edit().putString("home", file.getPath()).apply();
            Sp.edit().putBoolean("firstrun", false).commit();
        }
        mDrawerLinear = (RelativeLayout) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.menu_drawer);
        list = new ArrayList<String>();
        for (int i = 0; i < val.length; i++) {
            File file = new File(val[i]);
            if (file.canExecute()) {

                list.add(val[i]);
            }
        }
        list.add(utils.getString(this, R.string.apps));
        list.add(utils.getString(this, R.string.bookmanag));
        adapter = new DrawerAdapter(this, list, MainActivity.this, Sp);
        mDrawerList.setAdapter(adapter);

        if (savedInstanceState == null) {
            selectItem(0);
        } else {
            select = savedInstanceState.getInt("selectItem", 0);

            adapter.toggleChecked(select);

            if (select == 0) {

                title.setVisibility(View.GONE);
                tabsSpinner.setVisibility(View.VISIBLE);
            } else {

                title.setVisibility(View.VISIBLE);
                tabsSpinner.setVisibility(View.GONE);
            }
        }
        if (select < 4) {
            title.setText(list.get(select));
        }
        if (Build.VERSION.SDK_INT >= 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            DrawerLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mDrawerLayout.getLayoutParams();
            p.setMargins(0, config.getPixelInsetTop(false), 0, 0);
        }
        ((ImageButton) findViewById(R.id.settingsbutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), Preferences.class);
                finish();
                startActivity(i);
            }
        });
        if (th == 1) {
            mDrawerList.setBackgroundResource(android.R.drawable.screen_background_dark);
        }
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setDivider(null);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (select != 102) {
                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.content_frame, new ProcessViewer());
                    //   transaction.addToBackStack(null);
                    select = 102;
                    //Commit the transaction
                    transaction.commit();
                } else {
                    selectItem(0);
                }
            }
        });
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                util.getDrawerDrawable(),  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                if (select <= 5) {
                    title.setText(list.get(select));
                    getActionBar().setSubtitle(list.get(select));
                }// creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //title.setText("Amaze File Manager");
                getActionBar().setSubtitle(utils.getString(MainActivity.this, R.string.select));
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
    public static String[] getStorageDirectories() {
        // Final set of paths
        final Set<String> rv = new HashSet<String>();
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
        }
        return rv.toArray(new String[rv.size()]);
    }

    private boolean backHome(Main main) {
        for (int i = 0; i < list.size() - 2; i++) {
            if (main.current.equals(list.get(i)))
                return true;
            else
                continue;
        }
        return false;
    }

    @Override
    public void onBackPressed() {

        if (select < list.size() - 2) {
            Main main = ((Main) getSupportFragmentManager().findFragmentById(R.id.content_frame));

            if (main.results == true) {
                main.results = false;
                main.loadlist(new File(main.current), true);
            } else {
                if (!backHome(main)) {
                    if (utils.canGoBack(new File(main.current))) {
                        main.goBack();

                    } else {
                        exit();
                    }
                } else exit();

            }
        } else {
            selectItem(0);
        }
    }

    public void invalidatePasteButton() {
        if (MOVE_PATH != null || COPY_PATH != null) {
            paste.setVisibility(View.VISIBLE);
        } else
            paste.setVisibility(View.GONE);
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

    public void selectItem(int i) {

        if (i < list.size() - 2) {

            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, new Main());
            select = i;
            // Commit the transaction
            transaction.addToBackStack("tab" + 1);
            transaction.commit();

            boolean remember = Sp.getBoolean("remember", false);
            if (!remember) {

                TabHandler tabHandler1 = new TabHandler(this, null, null, 1);
                int pos = Sp.getInt("spinner_selected", 0);
                File file = new File(list.get(i));
                tabHandler1.updateTab(new Tab(pos, file.getName(), file.getPath()));
            }
            Sp.edit().putBoolean("remember", false).apply();

            title.setVisibility(View.GONE);
            tabsSpinner.setVisibility(View.VISIBLE);
        } else {
            if (i == list.size() - 2) {

                android.support.v4.app.FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                transaction2.replace(R.id.content_frame, new AppsList());
                // transaction2.addToBackStack(null);
                select = i;
                // Commit the transaction
                transaction2.commit();
                title.setText(utils.getString(this, R.string.apps));
                title.setVisibility(View.VISIBLE);
                tabsSpinner.setVisibility(View.GONE);
            } else if (i == list.size() - 1) {

                android.support.v4.app.FragmentTransaction transaction3 = getSupportFragmentManager().beginTransaction();
                transaction3.replace(R.id.content_frame, new BookmarksManager());
                // transaction3.addToBackStack(null);
                select = i;
                // Commit the transaction
                transaction3.commit();
                title.setText(utils.getString(this, R.string.bookmanag));
                title.setVisibility(View.VISIBLE);
                tabsSpinner.setVisibility(View.GONE);
            }
        }
        adapter.toggleChecked(i);
        mDrawerLayout.closeDrawer(mDrawerLinear);
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
        outState.putInt("selectItem", select);
    }

    @Override
    protected void onPause() {
        super.onPause();
        killToast();
        unregisterReceiver(RECIEVER);
        progress.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(RECIEVER, new IntentFilter("run"));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            ImageView ib = (ImageView) findViewById(R.id.overflow);
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
                progress.setVisibility(b.getBoolean("run", false) ? View.VISIBLE : View.GONE);
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
            if (!move) {
                long totalBytes = 0;
                for (int i = 0; i < params[0].size(); i++) {

                    File f1 = new File(params[0].get(i));
                    if (f1.isDirectory()) {
                        totalBytes = totalBytes + new Futils().folderSize(f1, false);
                    } else {
                        totalBytes = totalBytes + f1.length();
                    }
                }
                if (new File(ma.current).getUsableSpace() > totalBytes) {
                    File f = new File(path);
                    for (File k : f.listFiles()) {
                        for (String j : params[0]) {
                            if (k.getName().equals(new File(j).getName())) {
                                a.add(j);
                            } else {
                                b.add(j);
                            }
                        }
                    }
                } else publishProgress("Insufficient space");
            } else {

                long totalBytes = 0;
                for (int i = 0; i < MOVE_PATH.size(); i++) {

                    File f1 = new File(MOVE_PATH.get(i));
                    if (f1.isDirectory()) {
                        totalBytes = totalBytes + new Futils().folderSize(f1, false);
                    } else {
                        totalBytes = totalBytes + f1.length();
                    }
                }
                if (new File(path).getUsableSpace() > totalBytes) {
                    File f = new File(path);
                    for (File k : f.listFiles()) {
                        for (String j : params[0]) {
                            if (k.getName().equals(new File(j).getName())) {
                                a.add(j);
                            } else {
                                b.add(j);
                            }
                        }
                    }
                } else publishProgress("Insufficient space");

            }
            return a;
        }

        public void showDialog() {


            if (counter == a.size()) {
                if (lol != null && lol.size() != 0) {
                    Intent intent = new Intent(con, CopyService.class);
                    intent.putExtra("FILE_PATHS", lol);
                    intent.putExtra("COPY_DIRECTORY", ma.current);
                    startService(intent);
                } else {
                    Toast.makeText(MainActivity.this, "No file was overwritten", Toast.LENGTH_SHORT).show();
                }
            } else {

                AlertDialog.Builder x = new AlertDialog.Builder(MainActivity.this);
                x.setMessage("File with same name already exists " + new File(a.get(counter)).getName());
                x.setPositiveButton("Skip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (counter < a.size()) {
                            counter++;
                            showDialog();
                        }
                        dialogInterface.cancel();
                    }
                });
                x.setNeutralButton("Overwrite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (counter < a.size()) {
                            lol.add(a.get(counter));
                            counter++;
                            showDialog();
                        }
                        dialogInterface.cancel();
                    }
                });
                x.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.cancel();
                    }
                });
                x.show();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            if (!move) {

                if (b != null && b.size() != 0) {

                    Intent intent = new Intent(con, CopyService.class);
                    intent.putExtra("FILE_PATHS", b);
                    intent.putExtra("COPY_DIRECTORY", ma.current);
                    startService(intent);
                }

                if (a != null && a.size() != 0) {
                    showDialog();
                }
            } else {
                // yet to be implemented
            }
        }
    }
}
