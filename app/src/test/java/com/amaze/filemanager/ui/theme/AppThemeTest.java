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

package com.amaze.filemanager.ui.theme;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowStorageManager;

import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.shadows.ShadowMultiDex;
import com.amaze.filemanager.shadows.jcifs.smb.ShadowSmbFile;
import com.amaze.filemanager.test.ShadowCryptUtil;

import android.content.Context;
import android.content.res.Configuration;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(
        sdk = {JELLY_BEAN, KITKAT, P},
        shadows = {ShadowMultiDex.class}
        )
public class AppThemeTest {
  @Test
  public void getThemeLightTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.LIGHT_INDEX);
    assertEquals(AppTheme.LIGHT, apptheme);
  }

  @Test
  public void getThemeDARKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.DARK_INDEX);
    assertEquals(AppTheme.DARK, apptheme);
  }

  @Test
  public void getThemeTIMEDTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.TIME_INDEX);
    assertEquals(AppTheme.TIMED, apptheme);
  }

  @Test
  public void getThemeBLACKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.BLACK_INDEX);
    assertEquals(AppTheme.BLACK, apptheme);
  }

  @Test
  public void getMaterialDialogThemeLIGHTTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.LIGHT_INDEX);
    assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme(context));
  }

  @Test
  public void getMaterialDialogThemeDARKTest() {
    final Context context = ApplicationProvider.getApplicationContext();

    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.DARK_INDEX);
    assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme(context));
  }

  @Test
  public void getMaterialDialogThemeTIMEDTest() {
    final Context context = ApplicationProvider.getApplicationContext();

    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.TIME_INDEX);
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if (hour <= 6 || hour >= 18) {
      assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme(context));
    } else assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme(context));
  }

  @Test
  public void getMaterialDialogThemeBLACKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.BLACK_INDEX);
    assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme(context));
  }

  @Test
  public void getSimpleThemeLIGHTTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.LIGHT_INDEX);
    assertEquals(
        AppTheme.LIGHT,
        apptheme.getSimpleTheme(
            (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES));
  }

  @Test
  public void getSimpleThemeDARKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.DARK_INDEX);
    assertEquals(
        AppTheme.DARK,
        apptheme.getSimpleTheme(
            (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES));
  }

  @Test
  public void getSimpleThemeTIMEDTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.TIME_INDEX);
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if (hour <= 6 || hour >= 18) {
      assertEquals(
          AppTheme.DARK,
          apptheme.getSimpleTheme(
              (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                  == Configuration.UI_MODE_NIGHT_YES));
    } else
      assertEquals(
          AppTheme.LIGHT,
          apptheme.getSimpleTheme(
              (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                  == Configuration.UI_MODE_NIGHT_YES));
  }

  @Test
  public void getSimpleThemeBLACKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    AppTheme apptheme = AppTheme.getTheme(context, AppTheme.BLACK_INDEX);
    assertEquals(
        AppTheme.BLACK,
        apptheme.getSimpleTheme(
            (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES));
  }

  @Test
  public void getIdLIGHTTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    int index = 0;
    AppTheme apptheme = AppTheme.getTheme(context, index);
    assertEquals(index, apptheme.getId());
  }

  @Test
  public void getIdDARKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    int index = 1;
    AppTheme apptheme = AppTheme.getTheme(context, index);
    assertEquals(index, apptheme.getId());
  }

  @Test
  public void getIdTIMEDTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    int index = 2;
    AppTheme apptheme = AppTheme.getTheme(context, index);
    assertEquals(index, apptheme.getId());
  }

  @Test
  public void getIdBLACKTest() {
    final Context context = ApplicationProvider.getApplicationContext();
    int index = 3;
    AppTheme apptheme = AppTheme.getTheme(context, index);
    assertEquals(index, apptheme.getId());
  }
}
