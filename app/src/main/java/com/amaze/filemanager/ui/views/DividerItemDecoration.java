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

package com.amaze.filemanager.ui.views;

import com.amaze.filemanager.adapters.RecyclerAdapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/** Created by Arpit on 23-04-2015. */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

  private static final int[] ATTRS = new int[] {android.R.attr.listDivider};

  private Drawable mDivider;

  private boolean show;
  private int leftPaddingPx = 0, rightPaddingPx = 0;
  private boolean showtopbottomdividers;

  public DividerItemDecoration(Context context, boolean showtopbottomdividers, boolean show) {
    final TypedArray typedArray = context.obtainStyledAttributes(ATTRS);
    mDivider = typedArray.getDrawable(0);
    typedArray.recycle();
    this.show = show;
    this.showtopbottomdividers = showtopbottomdividers;
    leftPaddingPx = (int) (72 * (context.getResources().getDisplayMetrics().densityDpi / 160f));
    rightPaddingPx = (int) (16 * (context.getResources().getDisplayMetrics().densityDpi / 160f));
  }

  @Override
  public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
    super.onDraw(c, parent, state);

    if (!show) return;
    if (mDivider != null) drawVertical(c, parent);
  }

  /**
   * Draws the divider on the canvas provided by RecyclerView Be advised - divider will be drawn
   * before the views, hence it'll be below the views of adapter
   */
  private void drawVertical(Canvas c, RecyclerView parent) {
    final int left = parent.getPaddingLeft() + leftPaddingPx;
    final int right = parent.getWidth() - parent.getPaddingRight() - rightPaddingPx;

    final int childCount = parent.getChildCount();
    for (int i = showtopbottomdividers ? 0 : 1; i < childCount - 1; i++) {

      final View child = parent.getChildAt(i);

      int viewType = parent.getChildViewHolder(child).getItemViewType();
      if (viewType == RecyclerAdapter.TYPE_HEADER_FILES
          || viewType == RecyclerAdapter.TYPE_HEADER_FOLDERS) {
        // no need to decorate header views
        continue;
      }

      final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
      final int top = child.getBottom() + params.bottomMargin;
      final int bottom = top + mDivider.getIntrinsicHeight();
      mDivider.setBounds(left, top, right, bottom);
      mDivider.draw(c);
    }
  }

  @Override
  public void getItemOffsets(
      Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
    super.getItemOffsets(outRect, view, parent, state);

    if (parent.getChildAdapterPosition(view) == 0) {

      // not to draw an offset at the top of recycler view
      return;
    }

    outRect.top = mDivider.getIntrinsicHeight();
  }
}
