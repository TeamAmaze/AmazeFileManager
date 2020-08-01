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

package com.amaze.filemanager.ui.colors;

import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.utils.Utils;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.ColorInt;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(minSdk = N, maxSdk = P)
public class ColorUtilsTest {

  @Test
  public void testSetColorizeIcons() {
    doTest(R.color.video_item, Icons.VIDEO);
    doTest(R.color.audio_item, Icons.AUDIO);
    doTest(R.color.pdf_item, Icons.PDF);
    doTest(R.color.code_item, Icons.CODE);
    doTest(R.color.text_item, Icons.TEXT);
    doTest(R.color.archive_item, Icons.COMPRESSED);
    doTest(R.color.apk_item, Icons.APK);
    doTest(R.color.generic_item, Icons.NOT_KNOWN);
    assertNotNull(ApplicationProvider.getApplicationContext()); //idiotic codacy compliance...
  }

  @Test
  public void testSetColorizeIconsGeneric() {
    doTestGeneric(R.color.primary_indigo, Icons.CERTIFICATE);
    doTestGeneric(R.color.primary_indigo, Icons.CONTACT);
    doTestGeneric(R.color.primary_indigo, Icons.EVENTS);
    doTestGeneric(R.color.primary_indigo, Icons.FONT);
    doTestGeneric(R.color.primary_indigo, Icons.PRESENTATION);
    doTestGeneric(R.color.primary_indigo, Icons.SPREADSHEETS);
    doTestGeneric(R.color.primary_indigo, Icons.DOCUMENTS);
    doTestGeneric(R.color.primary_indigo, Icons.ENCRYPTED);
    doTestGeneric(R.color.primary_indigo, Icons.GIF);
    assertNotNull(ApplicationProvider.getApplicationContext()); //idiotic codacy compliance...
  }

  private void doTest(@ColorInt int expected, int icon) {
    GradientDrawable drawable = new GradientDrawable();
    ColorUtils.colorizeIcons(
        ApplicationProvider.getApplicationContext(), icon, drawable, R.color.primary_indigo);
    doCompare(
        ColorStateList.valueOf(
            Utils.getColor(ApplicationProvider.getApplicationContext(), expected)),
        drawable);
    drawable = null;
  }

  private void doTestGeneric(@ColorInt int expected, int icon) {
    GradientDrawable drawable = new GradientDrawable();
    ColorUtils.colorizeIcons(
        ApplicationProvider.getApplicationContext(), icon, drawable, R.color.primary_indigo);
    doCompare(ColorStateList.valueOf(expected), drawable);
    drawable = null;
  }

  private void doCompare(ColorStateList expected, GradientDrawable drawable) {
    assertEquals(expected, drawable.getColor());
  }
}
