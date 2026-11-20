/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import java.security.MessageDigest;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class CircularDrawableTransformation implements Transformation<Drawable> {

  private static final String ID =
      "com.amaze.filemanager.adapters.glide.CircularDrawableTransformation.1";
  private static final byte[] ID_BYTES = ID.getBytes(Key.CHARSET);

  private final Transformation<Drawable> staticDrawableTransformation =
      new DrawableTransformation(new CircleCrop(), false);

  @NonNull
  @Override
  public Resource<Drawable> transform(
      @NonNull Context context, @NonNull Resource<Drawable> resource, int outWidth, int outHeight) {
    Drawable drawable = resource.get();
    if (!(drawable instanceof Animatable)) {
      return staticDrawableTransformation.transform(context, resource, outWidth, outHeight);
    }

    return new CircularAnimatableDrawableResource(
        new CircularAnimatableDrawable(drawable), resource.getSize());
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof CircularDrawableTransformation;
  }

  @Override
  public int hashCode() {
    return ID.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }

  private static final class CircularAnimatableDrawableResource implements Resource<Drawable> {

    private final CircularAnimatableDrawable drawable;
    private final int size;

    private CircularAnimatableDrawableResource(CircularAnimatableDrawable drawable, int size) {
      this.drawable = drawable;
      this.size = size;
    }

    @NonNull
    @Override
    public Class<Drawable> getResourceClass() {
      return Drawable.class;
    }

    @NonNull
    @Override
    public Drawable get() {
      return drawable;
    }

    @Override
    public int getSize() {
      return size;
    }

    @Override
    public void recycle() {
      drawable.stop();
      drawable.setCallback(null);
    }
  }

  private static final class CircularAnimatableDrawable extends Drawable
      implements Animatable, Drawable.Callback {

    private final Drawable delegate;
    private final Path clipPath = new Path();

    private CircularAnimatableDrawable(Drawable delegate) {
      this.delegate = delegate;
      delegate.setCallback(this);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
      int diameter = Math.min(bounds.width(), bounds.height());
      float left = bounds.exactCenterX() - diameter / 2f;
      float top = bounds.exactCenterY() - diameter / 2f;

      clipPath.reset();
      clipPath.addCircle(
          bounds.exactCenterX(), bounds.exactCenterY(), diameter / 2f, Path.Direction.CW);

      int intrinsicWidth = delegate.getIntrinsicWidth();
      int intrinsicHeight = delegate.getIntrinsicHeight();
      if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
        delegate.setBounds(bounds);
        return;
      }

      float scale = Math.max((float) diameter / intrinsicWidth, (float) diameter / intrinsicHeight);
      int scaledWidth = Math.round(intrinsicWidth * scale);
      int scaledHeight = Math.round(intrinsicHeight * scale);
      int delegateLeft = Math.round(left + (diameter - scaledWidth) / 2f);
      int delegateTop = Math.round(top + (diameter - scaledHeight) / 2f);
      delegate.setBounds(
          delegateLeft, delegateTop, delegateLeft + scaledWidth, delegateTop + scaledHeight);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
      int saveCount = canvas.save();
      canvas.clipPath(clipPath);
      delegate.draw(canvas);
      canvas.restoreToCount(saveCount);
    }

    @Override
    public void setAlpha(int alpha) {
      delegate.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
      delegate.setColorFilter(colorFilter);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
      return delegate.getOpacity();
    }

    @Override
    public int getIntrinsicWidth() {
      return delegate.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
      return delegate.getIntrinsicHeight();
    }

    @Override
    public boolean isStateful() {
      return delegate.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
      return delegate.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
      return delegate.setLevel(level);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
      boolean changed = super.setVisible(visible, restart);
      return delegate.setVisible(visible, restart) || changed;
    }

    @Override
    public void start() {
      ((Animatable) delegate).start();
    }

    @Override
    public void stop() {
      ((Animatable) delegate).stop();
    }

    @Override
    public boolean isRunning() {
      return ((Animatable) delegate).isRunning();
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
      invalidateSelf();
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
      scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
      unscheduleSelf(what);
    }
  }
}
