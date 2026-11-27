/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowEnvironment;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.ui.activities.texteditor.SearchResultIndex;
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivity;
import com.amaze.filemanager.ui.activities.texteditor.TextEditorActivityViewModel;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.widget.ScrollView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {M, P, Build.VERSION_CODES.R})
public class TextEditorActivityTest {

  private final String fileContents = "fsdfsdfs";
  private AppCompatEditText text;

  @After
  public void tearDown() {
    AppConfig.getInstance().onTerminate();
  }

  @Test
  public void testOpenFileUri() throws IOException {
    File file = simulateFile();

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.fromFile(file));
    generateActivity(intent);

    assertEquals(fileContents, text.getText().toString());
  }

  @Test
  public void testOpenContentUri() throws Exception {
    Uri uri = Uri.parse("content://foo.bar.test.streamprovider/temp/thisisatest.txt");

    ContentResolver contentResolver =
        ApplicationProvider.getApplicationContext().getContentResolver();
    ShadowContentResolver shadowContentResolver = Shadows.shadowOf(contentResolver);
    shadowContentResolver.registerInputStream(
        uri, new ByteArrayInputStream(fileContents.getBytes("UTF-8")));

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.setType("text/plain");
    intent.setData(uri);

    generateActivity(intent);
    assertEquals(fileContents, text.getText().toString().trim());
  }

  /** Next/previous search buttons must scroll to the result (issue #4562). */
  @Test
  public void testSearchNavigationScrollsToResult() throws IOException {
    File file = simulateFile();
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.fromFile(file));
    ActivityController<TextEditorActivity> controller =
        Robolectric.buildActivity(TextEditorActivity.class, intent).create().start().visible();
    TextEditorActivity activity = controller.get();
    shadowOf(Looper.getMainLooper()).idle();

    StringBuilder longText = new StringBuilder();
    for (int i = 0; i < 300; i++) {
      longText.append("line ").append(i).append('\n');
    }
    longText.append("needle\n");
    AppCompatEditText editText = activity.findViewById(R.id.textEditorMainEditText);
    editText.setText(longText.toString());
    shadowOf(Looper.getMainLooper()).idle();

    int start = longText.indexOf("needle");
    TextEditorActivityViewModel viewModel =
        new ViewModelProvider(activity).get(TextEditorActivityViewModel.class);
    viewModel.setSearchResultIndices(
        Collections.singletonList(new SearchResultIndex(start, start + 6, 300)));
    viewModel.setCurrent(-1);

    ScrollView scrollView = activity.findViewById(R.id.textEditorScrollView);
    assertEquals(0, scrollView.getScrollY());

    activity.onClick(activity.findViewById(R.id.textEditorSearchNextButton));
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
    // Robolectric doesn't draw, so advance the smooth scroll by hand.
    scrollView.computeScroll();

    int lineTop =
        editText.getTop()
            + editText.getTotalPaddingTop()
            + editText.getLayout().getLineTop(editText.getLayout().getLineForOffset(start));
    assertTrue(
        "expected the result line to be on screen, scrollY="
            + scrollView.getScrollY()
            + " lineTop="
            + lineTop,
        lineTop >= scrollView.getScrollY()
            && lineTop < scrollView.getScrollY() + scrollView.getHeight());
    activity.onDestroy();
  }

  private void generateActivity(Intent intent) {
    ActivityController<TextEditorActivity> controller =
        Robolectric.buildActivity(TextEditorActivity.class, intent).create().start().visible();

    TextEditorActivity activity = controller.get();
    text = activity.findViewById(R.id.textEditorMainEditText);
    activity.onDestroy();
  }

  private File simulateFile() throws IOException {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    File file = new File(Environment.getExternalStorageDirectory(), "text.txt");

    file.createNewFile();

    if (!file.canWrite()) file.setWritable(true);
    assertTrue(file.canWrite());

    PrintWriter out = new PrintWriter(file);
    out.write(fileContents);
    out.flush();
    out.close();

    return file;
  }
}
