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

package com.amaze.filemanager.adapters.glide.apkimage;

import com.amaze.filemanager.R;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 16:12.
 */
public class ApkImageDataFetcher implements DataFetcher<Drawable> {

  private Context context;
  private String model;

  public ApkImageDataFetcher(Context context, String model) {
    this.context = context;
    this.model = model;
  }

  @Override
  public void loadData(Priority priority, DataCallback<? super Drawable> callback) {
    PackageInfo pi = context.getPackageManager().getPackageArchiveInfo(model, 0);
    Drawable apkIcon;
    if (pi != null) {
      pi.applicationInfo.sourceDir = model;
      pi.applicationInfo.publicSourceDir = model;
      apkIcon = pi.applicationInfo.loadIcon(context.getPackageManager());
    } else {
      apkIcon = ContextCompat.getDrawable(context, R.drawable.ic_android_white_24dp);
    }
    callback.onDataReady(apkIcon);
  }

  @Override
  public void cleanup() {
    // Intentionally empty only because we're not opening an InputStream or another I/O resource!
  }

  @Override
  public void cancel() {
    // No cancelation procedure
  }

  @NonNull
  @Override
  public Class<Drawable> getDataClass() {
    return Drawable.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.LOCAL;
  }
}
