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

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 16:21. */
public class ApkImageModelLoaderFactory implements ModelLoaderFactory<String, Drawable> {

  private PackageManager packageManager;

  public ApkImageModelLoaderFactory(PackageManager packageManager) {
    this.packageManager = packageManager;
  }

  @Override
  public ModelLoader<String, Drawable> build(MultiModelLoaderFactory multiFactory) {
    return new ApkImageModelLoader(packageManager);
  }

  @Override
  public void teardown() {}
}
