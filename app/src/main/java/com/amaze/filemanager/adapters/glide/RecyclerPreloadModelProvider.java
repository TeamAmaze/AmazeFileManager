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

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.GlideRequest;
import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 6/12/2017, at 15:15. */
public class RecyclerPreloadModelProvider
    implements ListPreloader.PreloadModelProvider<IconDataParcelable> {

  private final List<IconDataParcelable> urisToLoad;
  private final GlideRequest<Drawable> request;

  public RecyclerPreloadModelProvider(
      @NonNull Fragment fragment, @NonNull List<IconDataParcelable> uris, boolean isCircled) {
    urisToLoad = uris;
    GlideRequest<Drawable> incompleteRequest = GlideApp.with(fragment).asDrawable();

    if (isCircled) {
      request = incompleteRequest.circleCrop();
    } else {
      request = incompleteRequest.centerCrop();
    }
  }

  @Override
  @NonNull
  public List<IconDataParcelable> getPreloadItems(int position) {
    IconDataParcelable iconData = position < urisToLoad.size() ? urisToLoad.get(position) : null;
    if (iconData == null) return Collections.emptyList();
    return Collections.singletonList(iconData);
  }

  @Override
  @Nullable
  public RequestBuilder<Drawable> getPreloadRequestBuilder(IconDataParcelable iconData) {
    RequestBuilder<Drawable> requestBuilder;
    if (iconData.type == IconDataParcelable.IMAGE_FROMFILE) {
      requestBuilder = request.load(iconData.path);
    } else if (iconData.type == IconDataParcelable.IMAGE_FROMCLOUD) {
      requestBuilder = request.load(iconData.path).diskCacheStrategy(DiskCacheStrategy.NONE);
    } else {
      requestBuilder = request.load(iconData.image);
    }
    return requestBuilder;
  }
}
