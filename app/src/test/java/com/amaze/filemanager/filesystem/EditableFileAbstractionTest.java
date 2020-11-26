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

package com.amaze.filemanager.filesystem;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.CONTENT;
import static com.amaze.filemanager.filesystem.EditableFileAbstraction.Scheme.FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class EditableFileAbstractionTest {

  @Test(expected = IllegalArgumentException.class)
  public void testBogeyUri() {
    new EditableFileAbstraction(
        ApplicationProvider.getApplicationContext(), Uri.parse("https://github.com/TeamAmaze"));
  }

  @Test
  public void testContentUri() {
    Uri uri = Uri.parse("content://com.amaze.filemanager.test/foobar/foobar.txt");
    String displayName = "foobar.txt";
    ContentResolver cr = ApplicationProvider.getApplicationContext().getContentResolver();
    ContentValues content = new ContentValues();
    content.put(OpenableColumns.DISPLAY_NAME, displayName);
    cr.insert(uri, content);

    EditableFileAbstraction verify =
        new EditableFileAbstraction(ApplicationProvider.getApplicationContext(), uri);
    assertEquals(CONTENT, verify.scheme);
    assertEquals(displayName, verify.name);
    assertNull(verify.hybridFileParcelable);
    assertEquals(uri, verify.uri);
  }

  @Test
  public void testNonExistentContentUri() {
    Uri uri = Uri.parse("content://foo.bar.bogey.uri/test.txt");
    EditableFileAbstraction verify =
        new EditableFileAbstraction(ApplicationProvider.getApplicationContext(), uri);
    assertEquals("test.txt", verify.name);
    assertNull(verify.hybridFileParcelable);
    assertEquals(CONTENT, verify.scheme);
    assertEquals(uri, verify.uri);
  }

  @Test
  public void testFileUri() {
    File file = new File(Environment.getExternalStorageDirectory(), "test.odt");
    Uri uri = Uri.fromFile(file);
    EditableFileAbstraction verify =
        new EditableFileAbstraction(ApplicationProvider.getApplicationContext(), uri);
    assertEquals("test.odt", verify.name);
    assertNotNull(verify.hybridFileParcelable);
    assertEquals(FILE, verify.scheme);
    assertNull(verify.uri);
  }
}
