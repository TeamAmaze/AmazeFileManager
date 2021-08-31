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

package com.amaze.filemanager.adapters.glide.cloudicon;

import java.io.IOException;
import java.io.InputStream;

import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;

public class CloudIconDataFetcher implements DataFetcher<Bitmap> {

  private static final String TAG = CloudIconDataFetcher.class.getSimpleName();

  private final String path;
  private final Context context;
  private InputStream inputStream;
  private final int width;
  private final int height;

  public CloudIconDataFetcher(Context context, String path, int width, int height) {
    this.context = context;
    this.path = path;
    this.width = width;
    this.height = height;
  }

  @Override
  public void loadData(@NonNull Priority priority, DataCallback<? super Bitmap> callback) {
    inputStream = CloudUtil.getThumbnailInputStreamForCloud(context, path);
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.outWidth = width;
    options.outHeight = height;
    Bitmap drawable = BitmapFactory.decodeStream(inputStream, null, options);
    callback.onDataReady(drawable);
  }

  @Override
  public void cleanup() {
    try {
      if (inputStream != null) {
        inputStream.close();
      }
    } catch (IOException e) {
      Log.e(TAG, "Error cleaning up cloud icon fetch", e);
    }
  }

  @Override
  public void cancel() {
    // do nothing
  }

  @NonNull
  @Override
  public Class<Bitmap> getDataClass() {
    return Bitmap.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.REMOTE;
  }
}
