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

package com.amaze.filemanager.activities;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowEnvironment;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.utils.application.AppConfig;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;

@RunWith(RobolectricTestRunner.class)
@Config(
    constants = BuildConfig.class,
    shadows = {ShadowMultiDex.class},
    minSdk = 24,
    maxSdk = 27)
/*
 Restrict minSdk to 24 since it'd fail at SDK 21-23.
 This may only be fixed by upgrading to Robolectric 4.
 See https://github.com/robolectric/robolectric/issues/3947
*/
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

    assertThat(text.getText().toString(), is(fileContents + "\n"));
  }

  @Test
  public void testOpenContentUri() throws Exception {
    Uri uri = Uri.parse("content://foo.bar.test.streamprovider/temp/thisisatest.txt");

    ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
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
    assertThat(file.canWrite(), is(true));

    PrintWriter out = new PrintWriter(file);
    out.write(fileContents);
    out.flush();
    out.close();

    return file;
  }

  private Uri getFileContentUri(Context context, File file) {
    fail("Cannot create content URI");
    return null;
  }
}
