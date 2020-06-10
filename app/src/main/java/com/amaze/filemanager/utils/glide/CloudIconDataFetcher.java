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

package com.amaze.filemanager.utils.glide;

import java.io.IOException;
import java.io.InputStream;

import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

/** Created by Vishal Nehra on 3/27/2018. */
public class CloudIconDataFetcher implements DataFetcher<Bitmap> {

  private String path;
  private Context context;
  private InputStream inputStream;
  private int width, height;

  public CloudIconDataFetcher(Context context, String path, int width, int height) {
    this.context = context;
    this.path = path;
    this.width = width;
    this.height = height;
  }

  @Override
  public void loadData(Priority priority, DataCallback<? super Bitmap> callback) {
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
      if (inputStream != null) inputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
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
