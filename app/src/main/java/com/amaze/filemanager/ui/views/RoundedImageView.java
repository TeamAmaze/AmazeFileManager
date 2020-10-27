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
import com.amaze.filemanager.utils.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

/** Created by Arpit on 23-01-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com> */
public class RoundedImageView extends androidx.appcompat.widget.AppCompatImageView {

  private static final float BACKGROUND_CIRCLE_MARGIN_PERCENTUAL = 0.015f;

  private float relativeWidth;
  private float relativeHeight;
  private boolean isImageAnIcon = false;
  private boolean forceRedraw = false;
  private Paint background = new Paint();
  private Bitmap bitmap = null;

  public RoundedImageView(Context ctx, AttributeSet attrs) {
    super(ctx, attrs);

    background.setColor(Color.TRANSPARENT);
    background.setAntiAlias(true);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) return;

    Drawable drawable = getDrawable();
    int w = getWidth(), h = getHeight();

    if (isImageAnIcon) {
      float min = Math.min(w, h);
      float backgroundCircleMargin = min * BACKGROUND_CIRCLE_MARGIN_PERCENTUAL;
      float radius = min / 2f - backgroundCircleMargin * 2;
      if (bitmap == null || forceRedraw) {
        bitmap = drawableToBitmapRelative(drawable, radius, relativeWidth, relativeHeight);
        forceRedraw = false;
      }

      canvas.drawCircle(w / 2, h / 2, radius, background);
      canvas.drawBitmap(
          bitmap, w / 2 - bitmap.getWidth() / 2, h / 2 - bitmap.getHeight() / 2, null);
    } else {
      if (bitmap == null || forceRedraw) {
        Bitmap b = drawableToBitmap(drawable);
        Bitmap tempBitmap = b.copy(Config.ARGB_8888, true);

        bitmap = getRoundedCroppedBitmap(getContext(), tempBitmap, w);
        forceRedraw = false;
      }

      canvas.drawBitmap(bitmap, 0, 0, null);
    }
  }

  @Override
  public void setImageDrawable(@Nullable Drawable drawable) {
    super.setImageDrawable(drawable);
    forceRedraw = true;
  }

  @Override
  public void setBackgroundColor(int color) {
    background.setColor(color);
  }

  /**
   * Prevents vectors from getting bigger than their intended size. You MUST NOT provide bigger
   * dimensions than the view!
   *
   * <p>The size is relative to the smallest between width and height
   */
  public void setRelativeSize(float width, float height) {
    if (width > 2 || height > 2)
      throw new UnsupportedOperationException(
          "Can't make image bigger! Accepted values are [2; 0)");
    relativeWidth = width;
    relativeHeight = height;
    isImageAnIcon = true;
    forceRedraw = true;
  }

  public static Bitmap getRoundedCroppedBitmap(Context context, Bitmap bitmap, int radius) {
    Bitmap finalBitmap;
    if (bitmap.getWidth() != radius || bitmap.getHeight() != radius) {
      finalBitmap = Bitmap.createScaledBitmap(bitmap, radius, radius, false);
    } else {
      finalBitmap = bitmap;
    }

    Bitmap output =
        Bitmap.createBitmap(finalBitmap.getWidth(), finalBitmap.getHeight(), Config.ARGB_8888);
    Canvas canvas = new Canvas(output);

    final Paint paint = new Paint();
    final Rect rect = new Rect(0, 0, finalBitmap.getWidth(), finalBitmap.getHeight());

    paint.setAntiAlias(true);
    paint.setFilterBitmap(true);
    paint.setDither(true);
    paint.setColor(Utils.getColor(context, R.color.roundedimagepaint));
    canvas.drawCircle(
        finalBitmap.getWidth() / 2 + 0.7f,
        finalBitmap.getHeight() / 2 + 0.7f,
        finalBitmap.getWidth() / 2 + 0.1f,
        paint);
    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
    canvas.drawBitmap(finalBitmap, rect, rect, paint);

    return output;
  }

  /**
   * Converts a {@link Drawable} to {@link Bitmap} A drawable can be drawn on a {@link Canvas} and a
   * Canvas can be backed by a Bitmap. Hence the conversion
   */
  public static Bitmap drawableToBitmap(Drawable drawable) {
    Bitmap bitmap;

    if (drawable instanceof BitmapDrawable) {
      BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
      if (bitmapDrawable.getBitmap() != null) {
        return bitmapDrawable.getBitmap();
      }
    }

    if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
      bitmap =
          Bitmap.createBitmap(
              1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
    } else {
      bitmap =
          Bitmap.createBitmap(
              drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    }
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }

  /**
   * Converts a {@link Drawable} to {@link Bitmap} A drawable can be drawn on a {@link Canvas} and a
   * Canvas can be backed by a Bitmap. Hence the conversion
   */
  public static Bitmap drawableToBitmapRelative(
      Drawable drawable, float radius, float relativeWidth, float relativeHeight) {
    if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
      throw new IllegalStateException(
          "Solid colors cannot be represented as images! Use RoundedImageView.setBackgroundColor()");
    }

    int sizeW = (int) (radius * relativeWidth);
    int sizeH = (int) (radius * relativeHeight);

    Bitmap bitmap = Bitmap.createBitmap(sizeW, sizeH, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, sizeW, sizeH);
    drawable.draw(canvas);
    return bitmap;
  }
}
