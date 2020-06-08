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

import com.amaze.filemanager.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.core.view.ViewCompat;

/*
 * A layout that draws something in the insets passed to {@link #fitSystemWindows(Rect)}, i.e. the area above UI chrome
 * (status and navigation bars, overlay action bars).
 */
public class ScrimInsetsRelativeLayout extends RelativeLayout {
  private Drawable mInsetForeground;

  private Rect mInsets;
  private Rect mTempRect = new Rect();
  private OnInsetsCallback mOnInsetsCallback;

  public ScrimInsetsRelativeLayout(Context context) {
    super(context);
    init(context, null, 0);
  }

  public ScrimInsetsRelativeLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs, 0);
  }

  public ScrimInsetsRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs, defStyle);
  }

  private void init(Context context, AttributeSet attrs, int defStyle) {
    final TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.ScrimInsetsFrameLayout, defStyle, 0);
    if (a == null) {
      return;
    }
    mInsetForeground = a.getDrawable(R.styleable.ScrimInsetsFrameLayout_insetForeground);
    a.recycle();

    setWillNotDraw(true);
  }

  @Override
  protected boolean fitSystemWindows(Rect insets) {
    mInsets = new Rect(insets);
    setWillNotDraw(mInsetForeground == null);
    ViewCompat.postInvalidateOnAnimation(this);
    if (mOnInsetsCallback != null) {
      mOnInsetsCallback.onInsetsChanged(insets);
    }
    return true; // consume insets
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    int width = getWidth();
    int height = getHeight();
    if (mInsets != null && mInsetForeground != null) {
      int sc = canvas.save();
      canvas.translate(getScrollX(), getScrollY());

      // Top
      mTempRect.set(0, 0, width, mInsets.top);
      mInsetForeground.setBounds(mTempRect);
      mInsetForeground.draw(canvas);

      // Bottom
      mTempRect.set(0, height - mInsets.bottom, width, height);
      mInsetForeground.setBounds(mTempRect);
      mInsetForeground.draw(canvas);

      // Left
      mTempRect.set(0, mInsets.top, mInsets.left, height - mInsets.bottom);
      mInsetForeground.setBounds(mTempRect);
      mInsetForeground.draw(canvas);

      // Right
      mTempRect.set(width - mInsets.right, mInsets.top, width, height - mInsets.bottom);
      mInsetForeground.setBounds(mTempRect);
      mInsetForeground.draw(canvas);

      canvas.restoreToCount(sc);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (mInsetForeground != null) {
      mInsetForeground.setCallback(this);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (mInsetForeground != null) {
      mInsetForeground.setCallback(null);
    }
  }

  /**
   * Allows the calling container to specify a callback for custom processing when insets change
   * (i.e. when {@link #fitSystemWindows(Rect)} is called. This is useful for setting padding on UI
   * elements based on UI chrome insets (e.g. a Google Map or a ListView). When using with ListView
   * or GridView, remember to set clipToPadding to false.
   */
  public void setOnInsetsCallback(OnInsetsCallback onInsetsCallback) {
    mOnInsetsCallback = onInsetsCallback;
  }

  public interface OnInsetsCallback {
    void onInsetsChanged(Rect insets);
  }
}
