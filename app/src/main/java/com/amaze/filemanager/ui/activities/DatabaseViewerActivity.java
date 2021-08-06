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

import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;

import java.io.File;
import java.util.ArrayList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.root.CopyFilesCommand;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.fragments.DbViewerFragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

/** Created by Vishal on 02-02-2015. */
public class DatabaseViewerActivity extends ThemedActivity {

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
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_db_viewer);
    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    boolean useNewStack = getBoolean(PREFERENCE_TEXTEDITOR_NEWSTACK);
    getSupportActionBar().setDisplayHomeAsUpEnabled(!useNewStack);

    path = getIntent().getStringExtra("path");
    pathFile = new File(path);
    listView = findViewById(R.id.listView);

    load(pathFile);
    listView.setOnItemClickListener(
        (parent, view, position, id) -> {
          FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
          DbViewerFragment fragment = new DbViewerFragment();
          Bundle bundle = new Bundle();
          bundle.putString("table", arrayList.get(position));
          fragment.setArguments(bundle);
          fragmentTransaction.add(R.id.content_frame, fragment);
          fragmentTransaction.addToBackStack(null);
          fragmentTransaction.commit();
        });
    initStatusBarResources(findViewById(R.id.parentdb));
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
    new Thread(
            () -> {
              File file1 = getExternalCacheDir();

              // if the db can't be read, and we have root enabled, try reading it by
              // first copying it in cache dir
              if (!file.canRead() && isRootExplorer()) {

                try {
                  CopyFilesCommand.INSTANCE.copyFiles(
                      pathFile.getPath(), new File(file1.getPath(), file.getName()).getPath());
                  pathFile = new File(file1.getPath(), file.getName());
                } catch (ShellNotRunningException e) {
                  e.printStackTrace();
                }
                delete = true;
              }
              try {
                sqLiteDatabase =
                    SQLiteDatabase.openDatabase(
                        pathFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);

                c =
                    sqLiteDatabase.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table'", null);
                arrayList = getDbTableNames(c);
                arrayAdapter =
                    new ArrayAdapter(
                        DatabaseViewerActivity.this,
                        android.R.layout.simple_list_item_1,
                        arrayList);
              } catch (Exception e) {
                e.printStackTrace();
                finish();
              }
              runOnUiThread(
                  () -> {
                    listView.setAdapter(arrayAdapter);
                  });
            })
        .start();
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
