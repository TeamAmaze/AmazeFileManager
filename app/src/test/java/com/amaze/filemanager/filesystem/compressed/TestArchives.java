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

package com.amaze.filemanager.filesystem.compressed;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.utils.IOUtils;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentResolver;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

public abstract class TestArchives {

  private static final String[] ARCHIVE_TYPES = {"tar.gz", "zip", "tar", "rar"};

  private static final ClassLoader classLoader = TestArchives.class.getClassLoader();

  public static void init(Context context) {
    for (String type : ARCHIVE_TYPES) {
      readArchive(context, type);
    }
  }

  public static byte[] readArchive(String type) throws IOException {
    return IOUtils.toByteArray(classLoader.getResourceAsStream("test-archive." + type));
  }

  private static void readArchive(Context context, String type) {
    try {
      Uri uri = Uri.parse("content://foo.bar.test.streamprovider/temp/test-archive." + type);

      ContentResolver contentResolver = context.getContentResolver();
      ShadowContentResolver shadowContentResolver = Shadows.shadowOf(contentResolver);
      shadowContentResolver.registerInputStream(uri, new ByteArrayInputStream(readArchive(type)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
