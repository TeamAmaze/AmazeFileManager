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
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.DbViewerFragment;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.RootHelper;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Vishal on 02-02-2015.
 */
public class DbViewer extends ActionBarActivity {

    private SharedPreferences Sp;
    private String skin, path;
    private boolean rootMode;
    private ListView listView;
    private ArrayList<String> arrayList;
    private ArrayAdapter arrayAdapter;
    private Cursor c;
    private File pathFile;
    boolean delete=false;
    public Toolbar toolbar;
    public SQLiteDatabase sqLiteDatabase;
    public int theme, theme1, skinStatusBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        theme = Integer.parseInt(Sp.getString("theme", "0"));

        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;

        if (theme1 == 1) {
            setTheme(R.style.appCompatDark);
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        setContentView(R.layout.activity_db_viewer);
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        skin = Sp.getString("skin_color", "#3f51b5");
        if (Build.VERSION.SDK_INT>=21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze", ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_launcher)).getBitmap(), Color.parseColor(skin));
            ((Activity)this).setTaskDescription(taskDescription);
        }
        String x = PreferenceUtils.getStatusColor(skin);
        skinStatusBar = Color.parseColor(x);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rootMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("rootmode", false);
        int sdk= Build.VERSION.SDK_INT;
        if(sdk==20 || sdk==19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(Color.parseColor(skin));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.parentdb).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        }else if(Build.VERSION.SDK_INT>=21){
            boolean colourednavigation=Sp.getBoolean("colorednavigation",true);
            Window window =getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.parseColor(PreferenceUtils.getStatusColor(skin)));
            if(colourednavigation)
                window.setNavigationBarColor(Color.parseColor(PreferenceUtils.getStatusColor(skin)));

        }

        path = getIntent().getStringExtra("path");
        pathFile = new File(path);
        listView = (ListView) findViewById(R.id.listView);

        load(pathFile);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                DbViewerFragment fragment = new DbViewerFragment();
                Bundle bundle = new Bundle();
                bundle.putString("table", arrayList.get(position));
                fragment.setArguments(bundle);
                fragmentTransaction.add(R.id.content_frame, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

    }

    private ArrayList<String> getDbTableNames(Cursor c) {
        ArrayList<String> result = new ArrayList<String>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            for (int i = 0; i < c.getColumnCount(); i++) {
                result.add(c.getString(i));
            }
        }
        return result;
    }
    private void load(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!file.canRead() && rootMode) {
                    File file1=getExternalCacheDir();
                    if(file1!=null)file1=getCacheDir();
                    RootTools.remount(file.getParent(), "RW");
                    RootTools.copyFile(pathFile.getPath(),new File(file1.getPath(),file.getName()).getPath(), true,false);
                    pathFile=new File(file1.getPath(),file.getName());
                    RootHelper.runAndWait("chmod 777 "+pathFile.getPath(),true);
                    delete=true;
                }
                try {
                    sqLiteDatabase = SQLiteDatabase.openDatabase(pathFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);

                    c = sqLiteDatabase.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table'", null);
                    arrayList = getDbTableNames(c);
                    arrayAdapter = new ArrayAdapter(DbViewer.this, android.R.layout.simple_list_item_1, arrayList);
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        listView.setAdapter(arrayAdapter);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       if(sqLiteDatabase!=null) sqLiteDatabase.close();
        if(c!=null) c.close();
        if(true)pathFile.delete();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        toolbar.setTitle(pathFile.getName());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            toolbar.setTitle(pathFile.getName());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
