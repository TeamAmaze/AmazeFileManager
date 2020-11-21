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

package com.amaze.filemanager.asynchronous.asynctasks;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Looper.getMainLooper;
import static android.view.View.VISIBLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.ui.activities.DatabaseViewerActivity;
import com.amaze.filemanager.ui.fragments.DbViewerFragment;
import com.amaze.filemanager.ui.theme.AppTheme;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class DbViewerTaskTest {

  private WebView webView;

  @Before
  public void setUp() {
    webView = new WebView(ApplicationProvider.getApplicationContext());
  }

  @After
  public void tearDown() {
    webView.destroy();
  }

  @Test
  public void testOnPreExecute() {
    DbViewerFragment mock = mock(DbViewerFragment.class);
    TextView loadingText = new TextView(ApplicationProvider.getApplicationContext());
    mock.loadingText = loadingText;
    mock.databaseViewerActivity = mock(DatabaseViewerActivity.class);
    mock.loadingText.setVisibility(View.GONE);
    when(mock.databaseViewerActivity.getAppTheme()).thenReturn(AppTheme.DARK);

    DbViewerTask task = new DbViewerTask(null, null, webView, mock);
    task.onPreExecute();
    assertEquals(VISIBLE, mock.loadingText.getVisibility());
    assertTrue(task.htmlInit.contains("color:#ffffff"));
    assertEquals("utf-8", webView.getSettings().getDefaultTextEncodingName());

    when(mock.databaseViewerActivity.getAppTheme()).thenReturn(AppTheme.BLACK);
    task = new DbViewerTask(null, null, webView, mock);
    task.onPreExecute();
    assertEquals(VISIBLE, mock.loadingText.getVisibility());
    assertTrue(task.htmlInit.contains("color:#ffffff"));
    assertEquals("utf-8", webView.getSettings().getDefaultTextEncodingName());

    when(mock.databaseViewerActivity.getAppTheme()).thenReturn(AppTheme.LIGHT);
    task = new DbViewerTask(null, null, webView, mock);
    task.onPreExecute();
    assertEquals(VISIBLE, mock.loadingText.getVisibility());
    assertTrue(task.htmlInit.contains("color:#000000"));
    assertEquals("utf-8", webView.getSettings().getDefaultTextEncodingName());
  }

  @Test
  public void testExecute() {
    SQLiteDatabase sqLiteDatabase =
        SQLiteDatabase.openDatabase(
            "src/test/resources/test.db", null, SQLiteDatabase.OPEN_READONLY);
    assertNotNull(sqLiteDatabase);

    DbViewerFragment mock = mock(DbViewerFragment.class);
    TextView loadingText = new TextView(ApplicationProvider.getApplicationContext());
    mock.loadingText = loadingText;
    Cursor schemaCursor = sqLiteDatabase.rawQuery("PRAGMA table_info('users');", null);
    Cursor contentCursor = sqLiteDatabase.rawQuery("SELECT * FROM users", null);

    DbViewerTask task = new DbViewerTask(schemaCursor, contentCursor, webView, mock);
    task.doInBackground();

    shadowOf(getMainLooper()).idle();

    assertNotNull(task.schemaList);
    assertNotNull(task.contentList);

    // 3 columns
    assertEquals(3, task.schemaList.size());
    // 4 records
    assertEquals(4, task.contentList.size());
    assertEquals("4 records loaded", loadingText.getText().toString());

    sqLiteDatabase.close();
  }

  @Test
  public void testCompleteTask() {
    SQLiteDatabase sqLiteDatabase =
        SQLiteDatabase.openDatabase(
            "src/test/resources/test.db", null, SQLiteDatabase.OPEN_READONLY);
    assertNotNull(sqLiteDatabase);

    DbViewerFragment mock = mock(DbViewerFragment.class);
    TextView loadingText = new TextView(ApplicationProvider.getApplicationContext());
    mock.loadingText = loadingText;
    mock.databaseViewerActivity = mock(DatabaseViewerActivity.class);
    mock.loadingText.setVisibility(View.GONE);
    when(mock.databaseViewerActivity.getAppTheme()).thenReturn(AppTheme.DARK);
    Cursor schemaCursor = sqLiteDatabase.rawQuery("PRAGMA table_info('users');", null);
    Cursor contentCursor = sqLiteDatabase.rawQuery("SELECT * FROM users", null);

    DbViewerTask task = new DbViewerTask(schemaCursor, contentCursor, webView, mock);
    task.onPreExecute();
    task.doInBackground();
    task.onPostExecute(null);

    assertNotNull(task.stringBuilder.toString());

    Document html = Jsoup.parse(task.stringBuilder.toString());
    assertNotNull(html);
    Elements elements = html.getElementsByTag("table");
    assertEquals(1, elements.size());
    elements = elements.get(0).getElementsByTag("tr");
    assertEquals(5, elements.size());
    Elements headerRow = elements.get(0).getElementsByTag("th");
    assertEquals(3, headerRow.size());

    sqLiteDatabase.close();
  }
}
