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

import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
@Config(sdk = {P}) // Min sdk is N
public class ColorUtilsTest {

  @Test
  public void testSetColorizeIcons() {
    assertTrue(doTest(R.color.video_item, Icons.VIDEO));
    assertTrue(doTest(R.color.audio_item, Icons.AUDIO));
    assertTrue(doTest(R.color.pdf_item, Icons.PDF));
    assertTrue(doTest(R.color.code_item, Icons.CODE));
    assertTrue(doTest(R.color.text_item, Icons.TEXT));
    assertTrue(doTest(R.color.archive_item, Icons.COMPRESSED));
    assertTrue(doTest(R.color.apk_item, Icons.APK));
    assertTrue(doTest(R.color.generic_item, Icons.NOT_KNOWN));
  }

  @Test
  public void testSetColorizeIconsGeneric() {
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.CERTIFICATE));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.CONTACT));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.EVENTS));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.FONT));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.PRESENTATION));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.SPREADSHEETS));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.DOCUMENTS));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.ENCRYPTED));
    assertTrue(doTestGeneric(R.color.primary_indigo, Icons.GIF));
  }

  private boolean doTest(@ColorInt int expected, int icon) {
    GradientDrawable drawable = new GradientDrawable();
    ColorUtils.colorizeIcons(
        ApplicationProvider.getApplicationContext(), icon, drawable, R.color.primary_indigo);
    doCompare(
        ColorStateList.valueOf(
            Utils.getColor(ApplicationProvider.getApplicationContext(), expected)),
        drawable);
    drawable = null;
    return true;
  }

  private boolean doTestGeneric(@ColorInt int expected, int icon) {
    GradientDrawable drawable = new GradientDrawable();
    ColorUtils.colorizeIcons(
        ApplicationProvider.getApplicationContext(), icon, drawable, R.color.primary_indigo);
    doCompare(ColorStateList.valueOf(expected), drawable);
    drawable = null;
    return true;
  }

  private void doCompare(ColorStateList expected, GradientDrawable drawable) {
    assertEquals(expected, drawable.getColor());
  }
}
