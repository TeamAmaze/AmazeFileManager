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

package com.amaze.filemanager.application;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Looper.getMainLooper;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

import android.os.StrictMode;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@Config(sdk = {JELLY_BEAN, KITKAT, P})
public class AppConfigTest {

  @After
  public void tearDown() {
    ShadowToast.reset();
  }

  @Test
  public void testSetVmPolicyOnAppCreateHasNoFlags() throws Exception {
    Field maskField = StrictMode.VmPolicy.class.getDeclaredField("mask");
    maskField.setAccessible(true);
    assertEquals(0, maskField.get(StrictMode.getVmPolicy()));
  }

  @Test
  public void testToastWithNullContext() {
    AppConfig.toast(null, R.string.ok);
    assertNull(ShadowToast.getLatestToast());
  }

  @Test
  public void testToastWithStringRes() {
    AppConfig.toast(ApplicationProvider.getApplicationContext(), R.string.ok);
    shadowOf(getMainLooper()).idle();
    await().atMost(5, TimeUnit.SECONDS).until(() -> ShadowToast.getLatestToast() != null);
    assertEquals(
        ApplicationProvider.getApplicationContext().getString(R.string.ok),
        ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testToastWithString() {
    AppConfig.toast(ApplicationProvider.getApplicationContext(), "Hello world");
    shadowOf(getMainLooper()).idle();
    await().atMost(5, TimeUnit.SECONDS).until(() -> ShadowToast.getLatestToast() != null);
    assertEquals("Hello world", ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testGetImageLoader() throws Exception {
    Field requestQueue = AppConfig.class.getDeclaredField("requestQueue");
    Field imageLoader = AppConfig.class.getDeclaredField("imageLoader");
    requestQueue.setAccessible(true);
    imageLoader.setAccessible(true);

    assertNull(requestQueue.get(AppConfig.getInstance()));
    assertNull(imageLoader.get(AppConfig.getInstance()));

    assertNotNull(AppConfig.getInstance().getImageLoader());
  }

  @Test
  public void testGlideMemoryCategorySetToHigh() throws Exception {
    Field memoryCategory = Glide.class.getDeclaredField("memoryCategory");
    memoryCategory.setAccessible(true);
    assertEquals(
        MemoryCategory.HIGH,
        memoryCategory.get(Glide.get(ApplicationProvider.getApplicationContext())));
  }

  @Test
  public void testGetScreenUtils() {
    assertNull(AppConfig.getInstance().getScreenUtils());

    MainActivity mock = mock(MainActivity.class);
    AppConfig.getInstance().setMainActivityContext(mock);
    assertNotNull(AppConfig.getInstance().getScreenUtils());
  }
}
