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

package com.amaze.filemanager.utils;

import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.ui.views.ThemedTextView;

import android.os.Looper;
import android.view.animation.Interpolator;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {KITKAT, P})
public class AnimUtilsTest {

  @Test
  public void testGetFastOutSlowInInterpolator()
      throws NoSuchFieldException, IllegalAccessException {
    Field f = AnimUtils.class.getDeclaredField("fastOutSlowIn");
    f.setAccessible(true);
    assertNull(f.get(null));
    Interpolator result =
        AnimUtils.getFastOutSlowInInterpolator(ApplicationProvider.getApplicationContext());
    assertNotNull(result);
    assertNotNull(f.get(null));
  }

  @Test
  public void testMarqueeAfterDelay() {
    ThemedTextView mock = mock(ThemedTextView.class);
    doCallRealMethod().when(mock).setSelected(anyBoolean());
    doCallRealMethod().when(mock).isSelected();
    mock.setSelected(false);

    AnimUtils.marqueeAfterDelay(150, mock);
    shadowOf(Looper.myLooper()).getScheduler().advanceToLastPostedRunnable();
    assertTrue(mock.isSelected());
  }
}
