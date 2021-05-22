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
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {JELLY_BEAN, KITKAT, P})
public class TinyDBTest {

  private SharedPreferences prefs;

  @Before
  public void setUp() {
    prefs =
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
  }

  @Test
  public void testSaveLoadBooleanArray() {
    Boolean[] value = new Boolean[] {true, false, true, false, true, true, false};
    TinyDB.putBooleanArray(prefs, "foobar", value);
    Boolean[] result =
        TinyDB.getBooleanArray(
            prefs, "foobar", new Boolean[] {false, false, false, false, false, false, false});
    assertArrayEquals(value, result);
  }

  @Test
  public void testLoadBooleanArrayShouldReturnDefaultValue() {
    Boolean[] expected =
        TinyDB.getBooleanArray(
            prefs, "foobaz", new Boolean[] {true, false, true, false, false, false});
    Boolean[] result = TinyDB.getBooleanArray(prefs, "foobaz", expected);
    assertArrayEquals(expected, result);
  }

  @Test
  public void testLoadBooleanArrayShouldReturnDefaultValue2() {
    assertNull(TinyDB.getBooleanArray(prefs, "foobam", null));
  }
}
