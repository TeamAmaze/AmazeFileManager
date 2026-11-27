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

import static android.graphics.Color.RED;
import static android.graphics.Color.TRANSPARENT;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.R;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {P, R})
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CircularDrawableTransformationTest {

  @Test
  public void transformPreservesAnimationAndClipsToCircle() {
    Context context = ApplicationProvider.getApplicationContext();
    TestAnimatedDrawable source = new TestAnimatedDrawable();
    Resource<Drawable> sourceResource = new SimpleResource<>(source);

    Resource<Drawable> transformedResource =
        new CircularDrawableTransformation().transform(context, sourceResource, 64, 64);
    Drawable transformed = transformedResource.get();

    // Regression: supported animated thumbnails must remain animated and circular.
    assertNotSame(source, transformed);
    assertTrue(transformed instanceof Animatable);

    Animatable animation = (Animatable) transformed;
    animation.start();
    assertTrue(source.isRunning());
    animation.stop();
    assertFalse(source.isRunning());

    transformed.setBounds(0, 0, 64, 64);
    Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
    transformed.draw(new Canvas(bitmap));

    assertEquals(RED, bitmap.getPixel(32, 32));
    assertEquals(TRANSPARENT, bitmap.getPixel(0, 0));
    assertEquals(TRANSPARENT, bitmap.getPixel(63, 63));
  }

  private static final class TestAnimatedDrawable extends Drawable implements Animatable {

    private final Paint paint = new Paint();
    private boolean running;

    private TestAnimatedDrawable() {
      paint.setColor(RED);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
      canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int alpha) {
      paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
      paint.setColorFilter(colorFilter);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
      return 64;
    }

    @Override
    public int getIntrinsicHeight() {
      return 64;
    }

    @Override
    public void start() {
      running = true;
    }

    @Override
    public void stop() {
      running = false;
    }

    @Override
    public boolean isRunning() {
      return running;
    }
  }
}
