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

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;

import com.afollestad.materialdialogs.Theme;

/** Created by yuhalyn on 2018-04-02. */
public class AppThemeTest {

  @Test
  public void getThemeLightTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
    assertEquals(AppTheme.LIGHT, apptheme);
  }

  @Test
  public void getThemeDARKTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
    assertEquals(AppTheme.DARK, apptheme);
  }

  @Test
  public void getThemeTIMEDTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
    assertEquals(AppTheme.TIMED, apptheme);
  }

  @Test
  public void getThemeBLACKTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
    assertEquals(AppTheme.BLACK, apptheme);
  }

  @Test
  public void getMaterialDialogThemeLIGHTTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
    assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme());
  }

  @Test
  public void getMaterialDialogThemeDARKTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
    assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
  }

  @Test
  public void getMaterialDialogThemeTIMEDTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if (hour <= 6 || hour >= 18) {
      assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
    } else assertEquals(Theme.LIGHT, apptheme.getMaterialDialogTheme());
  }

  @Test
  public void getMaterialDialogThemeBLACKTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
    assertEquals(Theme.DARK, apptheme.getMaterialDialogTheme());
  }

  @Test
  public void getSimpleThemeLIGHTTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.LIGHT_INDEX);
    assertEquals(AppTheme.LIGHT, apptheme.getSimpleTheme());
  }

  @Test
  public void getSimpleThemeDARKTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.DARK_INDEX);
    assertEquals(AppTheme.DARK, apptheme.getSimpleTheme());
  }

  @Test
  public void getSimpleThemeTIMEDTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.TIME_INDEX);
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if (hour <= 6 || hour >= 18) {
      assertEquals(AppTheme.DARK, apptheme.getSimpleTheme());
    } else assertEquals(AppTheme.LIGHT, apptheme.getSimpleTheme());
  }

  @Test
  public void getSimpleThemeBLACKTest() {
    AppTheme apptheme = AppTheme.getTheme(AppTheme.BLACK_INDEX);
    assertEquals(AppTheme.BLACK, apptheme.getSimpleTheme());
  }

  @Test
  public void getIdLIGHTTest() {
    int index = 0;
    AppTheme apptheme = AppTheme.getTheme(index);
    assertEquals(index, apptheme.getId());
  }

  @Test
  public void getIdDARKTest() {
    int index = 1;
    AppTheme apptheme = AppTheme.getTheme(index);
    assertEquals(index, apptheme.getId());
  }

  @Test
  public void getIdTIMEDTest() {
    int index = 2;
    AppTheme apptheme = AppTheme.getTheme(index);
    assertEquals(index, apptheme.getId());
  }

  @Test
  public void getIdBLACKTest() {
    int index = 3;
    AppTheme apptheme = AppTheme.getTheme(index);
    assertEquals(index, apptheme.getId());
  }
}
