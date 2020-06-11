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

import com.android.volley.toolbox.ImageLoader;

import android.graphics.Bitmap;
import android.util.LruCache;

/** Created by vishal on 7/6/16. */
public class LruBitmapCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {
  /**
   * @param maxSize for caches that do not override {@link #sizeOf}, this is the maximum number of
   *     entries in the cache. For all other caches, this is the maximum sum of the sizes of the
   *     entries in this cache.
   */
  public LruBitmapCache(int maxSize) {
    super(maxSize);
  }

  public LruBitmapCache() {
    this(getDefaultCacheSize());
  }

  private static int getDefaultCacheSize() {
    int memory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    return memory / 8;
  }

  @Override
  public Bitmap getBitmap(String url) {
    return get(url);
  }

  @Override
  public void putBitmap(String url, Bitmap bitmap) {
    put(url, bitmap);
  }

  @Override
  protected int sizeOf(String key, Bitmap value) {

    return value.getByteCount();
  }
}
