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

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.TwilightCalculator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Calendar;

/** Created by rpiotaix on 17/10/16. */
public class BasicActivity extends AppCompatActivity {

  private static final int REQUEST_PERMISSION_LOCATION_STATE = 1;

  protected AppConfig getAppConfig() {
    return (AppConfig) getApplication();
  }

  public ColorPreferenceHelper getColorPreference() {
    return getAppConfig().getUtilsProvider().getColorPreference();
  }

  public AppTheme getAppTheme() {
    AppTheme appTheme = getAppConfig().getUtilsProvider().getAppTheme();

    if (appTheme == AppTheme.SYSTEM) {
      appTheme = getSystemTheme();
    } else if (appTheme == AppTheme.TIMED) {
      appTheme = getDaytimeTheme();
    }
    return appTheme;
  }

  private AppTheme getSystemTheme() {
    AppTheme appTheme = AppTheme.LIGHT;
    int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
      appTheme = AppTheme.DARK;
    }

    return appTheme;
  }

  private AppTheme getDaytimeTheme() {
    AppTheme appTheme = AppTheme.LIGHT;

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
              , REQUEST_PERMISSION_LOCATION_STATE);
    } else if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
      ActivityCompat.requestPermissions(this,
              new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}
              , REQUEST_PERMISSION_LOCATION_STATE);
    } else {
      LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

      if (location != null) {
        long time = Calendar.getInstance().getTime().getTime();
        int state = new TwilightCalculator().calculateTwilight(time, location.getLatitude(), location.getLongitude());
        if (state == TwilightCalculator.NIGHT) {
          appTheme = AppTheme.DARK;
        }
      } else {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 6 || hour >= 18) {
          appTheme = AppTheme.DARK;
        }
      }
    }


    return appTheme;
  }

  public UtilitiesProvider getUtilsProvider() {
    return getAppConfig().getUtilsProvider();
  }


}
