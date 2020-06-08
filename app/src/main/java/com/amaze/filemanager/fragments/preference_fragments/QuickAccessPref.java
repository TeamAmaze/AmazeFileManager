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

package com.amaze.filemanager.fragments.preference_fragments;

import static java.lang.Boolean.TRUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.TinyDB;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

/** @author Emmanuel on 17/4/2017, at 23:17. */
public class QuickAccessPref extends PreferenceFragment
    implements Preference.OnPreferenceClickListener {

  public static final String KEY = "quick access array";
  public static final String[] KEYS = {
    "fastaccess", "recent", "image", "video", "audio", "documents", "apks"
  };
  public static final Boolean[] DEFAULT = {TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE};
  private static Map<String, Integer> prefPos = new HashMap<>();

  static {
    Map<String, Integer> mem = new HashMap<>();
    for (int i = 0; i < KEYS.length; i++) mem.put(KEYS[i], i);
    prefPos = Collections.unmodifiableMap(mem);
  }

  private SharedPreferences preferences;
  private Boolean[] currentValue;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.fastaccess_prefs);

    preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    currentValue = TinyDB.getBooleanArray(preferences, KEY, DEFAULT);

    for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
      getPreferenceScreen().getPreference(i).setOnPreferenceClickListener(this);
    }
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    currentValue[prefPos.get(preference.getKey())] = ((SwitchPreference) preference).isChecked();
    TinyDB.putBooleanArray(preferences, KEY, currentValue);
    return true;
  }
}
