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

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

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

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class TextEditorActivityTest {

  private final String fileContents = "fsdfsdfs";
  private TextView text;

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

    assertEquals(fileContents + "\n", text.getText().toString());
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

  private void generateActivity(Intent intent) {
    ActivityController<TextEditorActivity> controller =
        Robolectric.buildActivity(TextEditorActivity.class, intent).create().start().visible();

    TextEditorActivity activity = controller.get();
    text = activity.findViewById(R.id.fname);
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
