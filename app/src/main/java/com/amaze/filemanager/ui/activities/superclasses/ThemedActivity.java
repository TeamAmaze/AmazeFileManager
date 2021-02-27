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

package com.amaze.filemanager.ui.activities.superclasses;

import static android.os.Build.VERSION.SDK_INT;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper;
import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.dialogs.ColorPickerDialog;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import android.app.ActivityManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

/** Created by arpitkh996 on 03-03-2016. */
public class ThemedActivity extends PreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // setting window background color instead of each item, in order to reduce pixel overdraw
    if (getAppTheme().equals(AppTheme.LIGHT)) {
      getWindow().setBackgroundDrawableResource(android.R.color.white);
    } else if (getAppTheme().equals(AppTheme.BLACK)) {
      getWindow().setBackgroundDrawableResource(android.R.color.black);
    } else {
      getWindow().setBackgroundDrawableResource(R.color.holo_dark_background);
    }

    // checking if theme should be set light/dark or automatic
    int colorPickerPref =
        getPrefs().getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, ColorPickerDialog.NO_DATA);
    if (colorPickerPref == ColorPickerDialog.RANDOM_INDEX) {
      getColorPreference().saveColorPreferences(getPrefs(), ColorPreferenceHelper.randomize(this));
    }

    if (SDK_INT >= 21) {
      ActivityManager.TaskDescription taskDescription =
          new ActivityManager.TaskDescription(
              getString(R.string.appbar_name),
              ((BitmapDrawable) ContextCompat.getDrawable(this, R.mipmap.ic_launcher)).getBitmap(),
              getPrimary());
      setTaskDescription(taskDescription);
    }

    setTheme();
  }

  /**
   * Set status bar and navigation bar colors based on sdk
   *
   * @param parentView parent view required to set margin on kitkat top
   */
  public void initStatusBarResources(View parentView) {

    if (getToolbar() != null) {
      getToolbar().setBackgroundColor(getPrimary());
    }

    Window window = getWindow();
    if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (findViewById(R.id.tab_frame) != null || findViewById(R.id.drawer_layout) == null) {
        window.setStatusBarColor(PreferenceUtils.getStatusColor(getPrimary()));
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      }
    } else if (SDK_INT == Build.VERSION_CODES.KITKAT_WATCH
        || SDK_INT == Build.VERSION_CODES.KITKAT) {
      setKitkatStatusBarMargin(parentView);
      setKitkatStatusBarTint();
    }

    if (getBoolean(PREFERENCE_COLORED_NAVIGATION) && SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.setNavigationBarColor(PreferenceUtils.getStatusColor(getPrimary()));
    }
  }

  public UserColorPreferences getCurrentColorPreference() {
    return getColorPreference().getCurrentUserColorPreferences(this, getPrefs());
  }

  public @ColorInt int getAccent() {
    return getColorPreference().getCurrentUserColorPreferences(this, getPrefs()).getAccent();
  }

  private void setKitkatStatusBarMargin(View parentView) {
    SystemBarTintManager tintManager = new SystemBarTintManager(this);
    tintManager.setStatusBarTintEnabled(true);
    // tintManager.setStatusBarTintColor(Color.parseColor((currentTab==1 ? skinTwo : skin)));
    FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) parentView.getLayoutParams();
    SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
    p.setMargins(0, config.getStatusBarHeight(), 0, 0);
  }

  private void setKitkatStatusBarTint() {
    SystemBarTintManager tintManager = new SystemBarTintManager(this);
    tintManager.setStatusBarTintEnabled(true);
    tintManager.setStatusBarTintColor(getPrimary());
  }

  public @ColorInt int getPrimary() {
    return ColorPreferenceHelper.getPrimary(getCurrentColorPreference(), getCurrentTab());
  }

  @Nullable
  private Toolbar getToolbar() {
    return findViewById(R.id.toolbar);
  }

  void setTheme() {
    AppTheme theme = getAppTheme().getSimpleTheme();
    if (Build.VERSION.SDK_INT >= 21) {

      String stringRepresentation = String.format("#%06X", (0xFFFFFF & getAccent()));

      switch (stringRepresentation.toUpperCase()) {
        case "#F44336":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_red);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_red);
          else setTheme(R.style.pref_accent_dark_red);
          break;

        case "#E91E63":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_pink);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_pink);
          else setTheme(R.style.pref_accent_dark_pink);
          break;

        case "#9C27B0":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_purple);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_purple);
          else setTheme(R.style.pref_accent_dark_purple);
          break;

        case "#673AB7":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_deep_purple);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_deep_purple);
          else setTheme(R.style.pref_accent_dark_deep_purple);
          break;

        case "#3F51B5":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_indigo);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_indigo);
          else setTheme(R.style.pref_accent_dark_indigo);
          break;

        case "#2196F3":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_blue);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_blue);
          else setTheme(R.style.pref_accent_dark_blue);
          break;

        case "#03A9F4":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_light_blue);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_light_blue);
          else setTheme(R.style.pref_accent_dark_light_blue);
          break;

        case "#00BCD4":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_cyan);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_cyan);
          else setTheme(R.style.pref_accent_dark_cyan);
          break;

        case "#009688":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_teal);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_teal);
          else setTheme(R.style.pref_accent_dark_teal);
          break;

        case "#4CAF50":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_green);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_green);
          else setTheme(R.style.pref_accent_dark_green);
          break;

        case "#8BC34A":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_light_green);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_light_green);
          else setTheme(R.style.pref_accent_dark_light_green);
          break;

        case "#FFC107":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_amber);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_amber);
          else setTheme(R.style.pref_accent_dark_amber);
          break;

        case "#FF9800":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_orange);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_orange);
          else setTheme(R.style.pref_accent_dark_orange);
          break;

        case "#FF5722":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_deep_orange);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_deep_orange);
          else setTheme(R.style.pref_accent_dark_deep_orange);
          break;

        case "#795548":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_brown);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_brown);
          else setTheme(R.style.pref_accent_dark_brown);
          break;

        case "#212121":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_black);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_black);
          else setTheme(R.style.pref_accent_dark_black);
          break;

        case "#607D8B":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_blue_grey);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_blue_grey);
          else setTheme(R.style.pref_accent_dark_blue_grey);
          break;

        case "#004D40":
          if (theme.equals(AppTheme.LIGHT)) setTheme(R.style.pref_accent_light_super_su);
          else if (theme.equals(AppTheme.BLACK)) setTheme(R.style.pref_accent_black_super_su);
          else setTheme(R.style.pref_accent_dark_super_su);
          break;
      }
    } else {
      if (theme.equals(AppTheme.LIGHT)) {
        setTheme(R.style.appCompatLight);
      } else if (theme.equals(AppTheme.BLACK)) {
        setTheme(R.style.appCompatBlack);
      } else {
        setTheme(R.style.appCompatDark);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    setTheme();
  }
}
