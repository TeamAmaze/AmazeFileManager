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

package com.amaze.filemanager.utils;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;
import static com.amaze.filemanager.utils.Utils.formatTimer;
import static com.amaze.filemanager.utils.Utils.sanitizeInput;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;

import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageVolume;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {JELLY_BEAN, KITKAT, P})
public class UtilsTest {

  @Test
  public void testSanitizeInput() {
    // This function is sanitize the string. It removes ";","|","&&","..."
    // from string.
    assertEquals("a", sanitizeInput("|a|")); // test the removing of pipe sign from string.
    assertEquals("a", sanitizeInput("...a...")); // test the removing of dots from string.
    assertEquals("a", sanitizeInput(";a;")); // test the removing of semicolon sign from string.
    assertEquals("a", sanitizeInput("&&a&&")); // test the removing of AMP sign from string.
    assertEquals(
        "a",
        sanitizeInput("|a...")); // test the removing of pipe sign and semicolon sign from string.
    assertEquals(
        "an apple",
        sanitizeInput("an &&apple")); // test the removing of AMP sign which are between two words.
    assertEquals(
        "an apple",
        sanitizeInput("an ...apple")); // test the removing of dots which are between two words.
    assertEquals(
        "an apple.",
        sanitizeInput(
            ";an |apple....")); // test the removing of pipe sign and dots which are between two
    // words. And test the fourth dot is not removed.
  }

  @Test
  public void testFormatTimer() {
    assertEquals("10:00", formatTimer(600));
    assertEquals("00:00", formatTimer(0));
    assertEquals("00:45", formatTimer(45));
    assertEquals("02:45", formatTimer(165));
    assertEquals("30:33", formatTimer(1833));
  }

  @Test
  public void testDifferenceStrings() {
    assertNull(Utils.differenceStrings(null, null));
    assertEquals("abc", Utils.differenceStrings("abc", null));
    assertEquals("abc", Utils.differenceStrings(null, "abc"));
    assertEquals("", Utils.differenceStrings("abc12345", "abc"));
    assertEquals("", Utils.differenceStrings("pqrstuv345", "pqrstuv"));
    assertEquals("12345", Utils.differenceStrings("abc", "abc12345"));
  }

  @Test
  public void testGetUriForBaseFile() {
    HybridFileParcelable file = new HybridFileParcelable("/storage/emulated/0/test.txt");
    for (OpenMode m : new OpenMode[] {OpenMode.FILE, OpenMode.ROOT}) {
      file.setMode(m);
      Uri uri = Utils.getUriForBaseFile(ApplicationProvider.getApplicationContext(), file);
      if (Build.VERSION.SDK_INT < N) {
        assertEquals("file:///storage/emulated/0/test.txt", uri.toString());
      } else {
        assertEquals(
            "content://"
                + ApplicationProvider.getApplicationContext().getPackageName()
                + "/storage_root/storage/emulated/0/test.txt",
            uri.toString());
      }
    }

    for (OpenMode m :
        new OpenMode[] {
          OpenMode.DROPBOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE, OpenMode.SMB, OpenMode.BOX
        }) {
      file = new HybridFileParcelable("/foo/bar/test.txt");
      file.setMode(m);
      assertNull(Utils.getUriForBaseFile(ApplicationProvider.getApplicationContext(), file));
      assertEquals(
          ApplicationProvider.getApplicationContext().getString(R.string.smb_launch_error),
          ShadowToast.getTextOfLatestToast());
    }

    file.setMode(OpenMode.CUSTOM);
    assertNull(Utils.getUriForBaseFile(ApplicationProvider.getApplicationContext(), file));
  }

  @Test
  public void testIsNullOrEmptyCollection() {
    assertTrue(Utils.isNullOrEmpty((Collection<Void>) null));
    assertTrue(Utils.isNullOrEmpty(Collections.EMPTY_SET));
    assertFalse(Utils.isNullOrEmpty(Collections.singletonList(null)));
    assertFalse(Utils.isNullOrEmpty(Arrays.asList(new Object())));
    Collection<Object> l = new ArrayList<Object>();
    l.add(new Object());
    assertFalse(Utils.isNullOrEmpty(l));
  }

  @Test
  public void testIsNullOrEmptyString() {
    assertTrue(Utils.isNullOrEmpty((String) null));
    assertTrue(Utils.isNullOrEmpty(""));
    assertFalse(Utils.isNullOrEmpty("null"));
    assertFalse(Utils.isNullOrEmpty("empty"));
    assertFalse(Utils.isNullOrEmpty("this is a string"));
  }

  @Test
  @Config(sdk = {P}) // min sdk is N
  public void testGetVolumeDirectory() throws Exception {
    StorageVolume mock = mock(StorageVolume.class);
    Field f = StorageVolume.class.getDeclaredField("mPath");
    f.setAccessible(true);
    f.set(mock, new File("/storage/emulated/0"));

    File result = Utils.getVolumeDirectory(mock);
    assertNotNull(result);
    assertEquals(new File("/storage/emulated/0"), result);
  }
}
