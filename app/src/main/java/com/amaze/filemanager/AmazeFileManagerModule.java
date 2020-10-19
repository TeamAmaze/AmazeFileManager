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

package com.amaze.filemanager;

import com.amaze.filemanager.adapters.glide.apkimage.ApkImageModelLoaderFactory;
import com.amaze.filemanager.adapters.glide.cloudicon.CloudIconModelFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/** Ensures that Glide's generated API is created for the Gallery sample. */
@GlideModule
public class AmazeFileManagerModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.prepend(
        String.class, Drawable.class, new ApkImageModelLoaderFactory(context.getPackageManager()));
    registry.prepend(String.class, Bitmap.class, new CloudIconModelFactory(context));
  }
}
