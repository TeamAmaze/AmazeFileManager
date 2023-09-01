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

import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.utils.NetworkUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.InetAddress

/**
 * Base class for [SmbDeviceScannerObservable.DiscoverDeviceStrategy] tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [KITKAT, P, VERSION_CODES.R])
abstract class AbstractSubnetDiscoverDevicesStrategyTests {

    /**
     * Post test cleanup.
     */
    @After
    open fun tearDown() {
        unmockkStatic(NetworkUtil::class)
    }

    protected fun deviceOffline() {
        mockkStatic(NetworkUtil::class)
        every { NetworkUtil.isConnectedToWifi(any()) } returns false
        every { NetworkUtil.isConnectedToLocalNetwork(any()) } returns false
        every { NetworkUtil.getLocalInetAddress(any()) } returns null
    }

    protected fun deviceOnline() {
        mockkStatic(NetworkUtil::class)
        every { NetworkUtil.isConnectedToWifi(any()) } returns true
        every { NetworkUtil.isConnectedToLocalNetwork(any()) } returns true
        every { NetworkUtil.getLocalInetAddress(any()) } returns mockk<InetAddress>().also {
            every { it.hostName } returns "192.168.233.240"
        }
    }

    protected fun mockInetAddress(hostName: String, hostAddress: String): InetAddress {
        val upHost = mockk<InetAddress>()
        every { upHost.hostName } returns hostName
        every { upHost.hostAddress } returns hostAddress
        every { InetAddress.getByName(hostAddress) } returns upHost
        return upHost
    }
}
