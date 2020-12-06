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
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_BOOKMARKS_ADDED;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_CHANGEPATHS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_ROOTMODE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_VIEW;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {JELLY_BEAN, KITKAT, P})
public class PreferencesActivityTest {

  private static final String[] DEFAULT_FALSE_PREFS =
      new String[] {
        PREFERENCE_SHOW_PERMISSIONS,
        PREFERENCE_SHOW_GOBACK_BUTTON,
        PREFERENCE_SHOW_HIDDENFILES,
        PREFERENCE_BOOKMARKS_ADDED,
        PREFERENCE_ROOTMODE,
        PREFERENCE_COLORED_NAVIGATION,
        PREFERENCE_TEXTEDITOR_NEWSTACK,
        PREFERENCE_CHANGEPATHS
      };

  private static final String[] DEFAULT_TRUE_PREFS =
      new String[] {
        PREFERENCE_SHOW_FILE_SIZE,
        PREFERENCE_SHOW_DIVIDERS,
        PREFERENCE_SHOW_HEADERS,
        PREFERENCE_USE_CIRCULAR_IMAGES,
        PREFERENCE_COLORIZE_ICONS,
        PREFERENCE_SHOW_THUMB,
        PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES,
        PREFERENCE_NEED_TO_SET_HOME,
        PREFERENCE_SHOW_SIDEBAR_FOLDERS,
        PREFERENCE_VIEW,
        PREFERENCE_SHOW_LAST_MODIFIED
      };

  private ActivityScenario<PreferencesActivity> scenario;

  @Before
  public void setUp() {
    scenario = ActivityScenario.launch(PreferencesActivity.class);
    scenario.moveToState(Lifecycle.State.STARTED);
  }

  @After
  public void tearDown() {
    scenario.close();
  }

  @Test
  public void testGetBooleanWithNoSavedValue() {
    scenario.onActivity(
        activity -> {
          for (String pref : DEFAULT_FALSE_PREFS) {
            assertFalse(activity.getBoolean(pref));
          }
          for (String pref : DEFAULT_TRUE_PREFS) {
            assertTrue(activity.getBoolean(pref));
          }
          assertThrows(IllegalArgumentException.class, () -> activity.getBoolean("foobar"));
        });
  }

  @Test
  public void testGetBooleanWithSavedValue() {
    SharedPreferences preferences =
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());
    scenario.onActivity(
        activity -> {
          for (String pref : DEFAULT_FALSE_PREFS) {
            preferences.edit().putBoolean(pref, true).commit();
            assertTrue(activity.getBoolean(pref));
          }
          for (String pref : DEFAULT_TRUE_PREFS) {
            preferences.edit().putBoolean(pref, false).commit();
            assertFalse(activity.getBoolean(pref));
          }
          assertThrows(IllegalArgumentException.class, () -> activity.getBoolean("foobar"));
        });
  }
}
