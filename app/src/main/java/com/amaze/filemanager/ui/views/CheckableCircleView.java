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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/** This is a circle taht can have a check (√) in the middle */
public class CheckableCircleView extends View {

  private static final float CHECK_MARGIN_PERCENTUAL = 0.15f;

  private Drawable check;
  private Paint paint = new Paint();
  private boolean checked;

  public CheckableCircleView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    check = context.getResources().getDrawable(R.drawable.ic_check_white_24dp);
  }

  public void setColor(@ColorInt int color) {
    paint.setColor(color);
    paint.setAntiAlias(true);
    invalidate();
  }

  public void setChecked(boolean checked) {
    this.checked = checked;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    float min = Math.min(getHeight(), getWidth());

    canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, min / 2f, paint);

    if (checked) {
      float checkMargin = min * CHECK_MARGIN_PERCENTUAL;
      check.setBounds(
          (int) checkMargin,
          (int) checkMargin,
          (int) (getWidth() - checkMargin),
          (int) (getHeight() - checkMargin));
      check.draw(canvas);
    }
  }
}
