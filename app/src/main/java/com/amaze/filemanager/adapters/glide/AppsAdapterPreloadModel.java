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

package com.amaze.filemanager.adapters.glide;

import java.util.Collections;
import java.util.List;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 15:38. */
public class AppsAdapterPreloadModel implements ListPreloader.PreloadModelProvider<String> {

  private RequestBuilder<Drawable> request;
  private List<String> items;

  public AppsAdapterPreloadModel(Fragment f) {
    request = Glide.with(f).asDrawable().fitCenter();
  }

  public void setItemList(List<String> items) {
    this.items = items;
  }

  @NonNull
  @Override
  public List<String> getPreloadItems(int position) {
    if (items == null) return Collections.emptyList();
    else return Collections.singletonList(items.get(position));
  }

  @Nullable
  @Override
  public RequestBuilder getPreloadRequestBuilder(String item) {
    return request.clone().load(item);
  }

  public void loadApkImage(String item, ImageView v) {
    request.load(item).into(v);
  }
}
