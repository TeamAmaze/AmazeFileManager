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

import android.app.ActivityManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
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
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.fragments.DbViewerFragment;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.RootUtils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.util.ArrayList;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Created by Vishal on 02-02-2015.
 */
public class DbViewer extends BaseActivity {

    private String path;
    private ListView listView;
    private ArrayList<String> arrayList;
    private ArrayAdapter arrayAdapter;
    private Cursor c;

    // the copy of db file which is to be opened, in the app cache
    private File pathFile;
    boolean delete = false;
    public Toolbar toolbar;
    public SQLiteDatabase sqLiteDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.checkStorage = false;
        super.onCreate(savedInstanceState);


        if (getAppTheme().equals(AppTheme.DARK)) {
            setTheme(R.style.appCompatDark);
            getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        setContentView(R.layout.activity_db_viewer);
        toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (SDK_INT >= 21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription
                    ("Amaze", ((BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap
                            .ic_launcher))
                            .getBitmap(),
                            getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            setTaskDescription(taskDescription);
        }
        getSupportActionBar()
                .setBackgroundDrawable(getColorPreference().getDrawable(ColorUsage.getPrimary(MainActivity.currentTab)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (SDK_INT == 20 || SDK_INT == 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.parentdb).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (SDK_INT >= 21) {
            boolean colourednavigation = sharedPref.getBoolean("colorednavigation", true);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));
            if (colourednavigation)
                window.setNavigationBarColor(PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));

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
        ArrayList<String> result = new ArrayList<>();
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
                File file1 = getExternalCacheDir();

                // if the db can't be read, and we have root enabled, try reading it by
                // first copying it in cache dir
                if (!file.canRead() && BaseActivity.rootMode) {

                    try {
                        RootUtils.copy(pathFile.getPath(),
                                new File(file1.getPath(), file.getName()).getPath());
                        pathFile = new File(file1.getPath(), file.getName());
                    } catch (RootNotPermittedException e) {
                        e.printStackTrace();
                    }
                    delete = true;
                }
                try {
                    sqLiteDatabase = SQLiteDatabase.openDatabase(pathFile.getPath(), null,
                            SQLiteDatabase.OPEN_READONLY);

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
        if (sqLiteDatabase != null) sqLiteDatabase.close();
        if (c != null) c.close();
        if (delete) pathFile.delete();
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
    public void onBackPressed() {
        super.onBackPressed();
        toolbar.setTitle(pathFile.getName());
    }

}
