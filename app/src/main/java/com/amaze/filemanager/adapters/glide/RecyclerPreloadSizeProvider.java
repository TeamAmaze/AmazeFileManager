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

import com.amaze.filemanager.adapters.data.IconDataParcelable;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

import android.util.SparseArray;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * This uses a callback to know for each position what View is the one in which you're going to
 * insert the image.
 *
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 12:27.
 */
public class RecyclerPreloadSizeProvider
    implements ListPreloader.PreloadSizeProvider<IconDataParcelable> {

  private RecyclerPreloadSizeProviderCallback callback;
  private SparseArray<int[]> viewSizes = new SparseArray<>();
  private boolean isAdditionClosed = false;

  public RecyclerPreloadSizeProvider(RecyclerPreloadSizeProviderCallback c) {
    callback = c;
  }

  /**
   * Adds one of the views that can be used to put an image inside. If the id is already inserted
   * the call will be ignored, but for performance you should call {@link #closeOffAddition()} once
   * you are done.
   *
   * @param id a unique number for each view loaded to this object
   * @param v the ciew to load
   */
  public void addView(int id, View v) {
    if (!isAdditionClosed && viewSizes.get(id, null) != null) return;

    final int viewNumber = id;
    new SizeViewTarget(
        v, (width, height) -> viewSizes.append(viewNumber, new int[] {width, height}));
  }

  /** Calls to {@link #addView(int, View)} will be ignored */
  public void closeOffAddition() {
    isAdditionClosed = true;
  }

  @Nullable
  @Override
  public int[] getPreloadSize(IconDataParcelable item, int adapterPosition, int perItemPosition) {
    return viewSizes.get(callback.getCorrectView(item, adapterPosition), null);
  }

  public interface RecyclerPreloadSizeProviderCallback {

    /**
     * Get the id for the view in which the image will be loaded.
     *
     * @return the view's id
     */
    int getCorrectView(IconDataParcelable item, int adapterPosition);
  }

  private static final class SizeViewTarget extends ViewTarget<View, Object> {
    public SizeViewTarget(View view, SizeReadyCallback callback) {
      super(view);
      getSize(callback);
    }

    @Override
    public void onResourceReady(Object resource, Transition<? super Object> transition) {
      // Do nothing
    }
  }
}
