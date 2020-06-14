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

package com.amaze.filemanager.asynchronous.services.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * This test is separated from FtpServiceEspressoTest since it does not actually requires the FTP
 * service itself.
 *
 * <p>It is expected that you are not running all the cases in one go. <b>You have been warned</b>.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class FtpServiceStaticMethodsTest {

  /** To test {@link FtpService#getLocalInetAddress(Context)} must not return an empty string. */
  @Test
  public void testGetLocalInetAddressMustNotBeEmpty() {
    if (!FtpService.isConnectedToLocalNetwork(
        InstrumentationRegistry.getInstrumentation().getTargetContext()))
      fail("Please connect your device to network to run this test!");
    assertNotNull(
        FtpService.getLocalInetAddress(
            InstrumentationRegistry.getInstrumentation().getTargetContext()));
    assertNotNull(
        FtpService.getLocalInetAddress(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
            .getHostAddress());
  }

  /**
   * To test IP address returned by {@link FtpService#getLocalInetAddress(Context)} must be
   * 192.168.43.1.
   *
   * <p><b>Remember to turn on Wi-Fi AP when running this test on <u>real</u> devices.</b>
   */
  @Test
  public void testGetLocalInetAddressMustBeAPAddress() {
    if (!FtpService.isEnabledWifiHotspot(
        InstrumentationRegistry.getInstrumentation().getTargetContext()))
      fail("Please enable Wi-Fi hotspot on your device to run this test!");

    assertEquals(
        "192.168.43.1",
        FtpService.getLocalInetAddress(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
            .getHostAddress());
  }
}
