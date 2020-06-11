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

package com.amaze.filemanager.filesystem.usb;

import static android.os.Build.VERSION_CODES.KITKAT;
import static com.amaze.filemanager.filesystem.usb.ReflectionHelpers.addUsbOtgDevice;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.shadows.ShadowMultiDex;

import android.net.Uri;

@RunWith(RobolectricTestRunner.class)
@Config(
    constants = BuildConfig.class,
    shadows = {ShadowMultiDex.class})
public class SingletonUsbOtgTest {
  @Test
  @Config(minSdk = KITKAT)
  public void usbConnectionTest() {
    ActivityController<MainActivity> controller =
        Robolectric.buildActivity(MainActivity.class).create();

    addUsbOtgDevice(controller.get());

    controller.resume().get();

    Uri rootBefore = Uri.parse("ssh://testuser:testpassword@127.0.0.1:22222");

    SingletonUsbOtg.getInstance().setUsbOtgRoot(rootBefore);

    controller.pause().resume().get();

    Uri rootAfter = SingletonUsbOtg.getInstance().getUsbOtgRoot();

    assertEquals(
        "Uris are different: (before:) " + rootBefore + " (after:) " + rootAfter,
        rootBefore,
        rootAfter);
  }
}
