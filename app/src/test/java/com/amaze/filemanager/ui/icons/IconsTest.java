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

package com.amaze.filemanager.ui.icons;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMimeTypeMap;

import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.webkit.MimeTypeMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
    shadows = {ShadowMultiDex.class},
    sdk = {JELLY_BEAN, KITKAT, P})
public class IconsTest {

  @Before
  public void setUp() {
    // By default Robolectric's MimeTypeMap is empty, we need to populate them
    ShadowMimeTypeMap mimeTypeMap = Shadows.shadowOf(MimeTypeMap.getSingleton());
    mimeTypeMap.addExtensionMimeTypMapping("zip", "application/zip");
    mimeTypeMap.addExtensionMimeTypMapping("rar", "application/x-rar-compressed");
    mimeTypeMap.addExtensionMimeTypMapping("tar", "application/x-tar");
  }

  @Test
  public void testReturnArchiveTypes() {
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.zip", false));
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.rar", false));
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar", false));
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.gz", false));
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.lzma", false));
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.xz", false));
    assertEquals(Icons.COMPRESSED, Icons.getTypeOfFile("archive.tar.bz2", false));
  }
}
