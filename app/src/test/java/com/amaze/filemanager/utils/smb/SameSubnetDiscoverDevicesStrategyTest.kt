/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.utils.smb

import com.amaze.filemanager.utils.ComputerParcelable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SameSubnetDiscoverDevicesStrategyTest : AbstractSubnetDiscoverDevicesStrategyTests() {

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(
            SameSubnetDiscoverDevicesStrategyTest::class.java
        )
    }

    /**
     * Test if device is not connected to network.
     */
    @Test
    fun testDiscoverIfNotConnected() {
        deviceOffline()
        val latch = CountDownLatch(1)
        val result = ArrayList<ComputerParcelable>()
        SameSubnetDiscoverDeviceStrategy().discoverDevices {
            result.add(it)
            latch.countDown()
        }
        try {
            latch.await(1, TimeUnit.SECONDS)
        } catch (_: Throwable) {
            latch.countDown()
        }
        assertEquals(0, result.size)
    }
}
