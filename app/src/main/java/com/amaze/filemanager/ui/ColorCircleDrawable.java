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

package com.amaze.filemanager.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Created by yaroslav on 26.01.16. */
public class ColorCircleDrawable extends Drawable {
  private final Paint mPaint;
  private int mRadius = 0;

  public ColorCircleDrawable(final int color) {
    this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    this.mPaint.setColor(color);
    this.mPaint.setStyle(Paint.Style.FILL);
  }

  @Override
  public void draw(final Canvas canvas) {
    final Rect bounds = getBounds();
    canvas.drawCircle(bounds.centerX(), bounds.centerY(), mRadius, mPaint);
  }

  @Override
  protected void onBoundsChange(final Rect bounds) {
    super.onBoundsChange(bounds);
    mRadius = Math.min(bounds.width(), bounds.height()) / 2;
  }

  @Override
  public void setAlpha(final int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(final ColorFilter cf) {
    mPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
