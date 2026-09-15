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

package com.amaze.filemanager.utils

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Tests for [NetworkUtil].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [M, P, VERSION_CODES.R])
@Suppress("StringLiteralDuplication")
class NetworkUtilTest {
    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiInfo: WifiInfo

    /**
     * Set up the mocks.
     */
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        wifiManager = mockk(relaxed = true)
        wifiInfo = mockk(relaxed = true)

        every { context.applicationContext } returns context
        every {
            context.getSystemService(Service.CONNECTIVITY_SERVICE)
        } returns connectivityManager
        every {
            context.getSystemService(Service.WIFI_SERVICE)
        } returns wifiManager
        every { wifiManager.connectionInfo } returns wifiInfo
    }

    /**
     * Test [NetworkUtil.isConnectedToLocalNetwork] when device is not connected.
     */
    @Test
    fun testGetLocalInetAddressWhenNotConnected() {
        mockNetworkNotConnected()
        assertNull(NetworkUtil.getLocalInetAddress(context))
    }

    /**
     * Test [NetworkUtil.getLocalInetAddress] when device is connected to Wifi.
     */
    @Test
    fun testGetLocalInetAddressOnWifi() {
        mockWifiConnected()
        every { wifiInfo.ipAddress } returns 0x0F02000A // 10.0.2.15

        val result = NetworkUtil.getLocalInetAddress(context)
        assertNotNull(result)
        assertEquals("10.0.2.15", result?.hostAddress)
    }

    /**
     * Test [NetworkUtil.getLocalInetAddress] when device is connected to Ethernet.
     */
    @Test
    fun testGetLocalInetAddressOnEthernet() {
        mockEthernetConnected()
        mockkStatic(NetworkInterface::class)

        val inetAddress = mockk<Inet4Address>()
        every { inetAddress.isLoopbackAddress } returns false
        every { inetAddress.isLinkLocalAddress } returns false
        every { inetAddress.hostAddress } returns "192.168.1.100"

        val networkInterface = mockk<NetworkInterface>()
        every { networkInterface.inetAddresses } returns
            Collections.enumeration(listOf(inetAddress))
        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(networkInterface))

        val result = NetworkUtil.getLocalInetAddress(context)
        assertNotNull(result)
        assertEquals("192.168.1.100", result?.hostAddress)
    }

    /**
     * Test [NetworkUtil.getLocalInetAddress] for network interface that supports multicast.
     */
    @Test
    fun testGetLocalInetAddressWithMulticastSupport() {
        mockEthernetConnected()
        mockkStatic(NetworkInterface::class)

        val inetAddress = mockk<Inet4Address>()
        every { inetAddress.isLoopbackAddress } returns false
        every { inetAddress.isLinkLocalAddress } returns false
        every { inetAddress.hostAddress } returns "192.168.1.1"

        val networkInterface = mockk<NetworkInterface>()
        every { networkInterface.supportsMulticast() } returns true
        every { networkInterface.inetAddresses } returns
            Collections.enumeration(listOf(inetAddress))
        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(networkInterface))

        val result = NetworkUtil.getLocalInetAddress(context, requestMulticast = true)
        assertNotNull(result)
        assertEquals("192.168.1.1", result?.hostAddress)
    }

    /**
     *
     */
    @Test
    fun testGetLocalInetAddressWithoutMulticastSupport() {
        mockEthernetConnected()

        val inetAddress = mockk<Inet4Address>()
        every { inetAddress.isLoopbackAddress } returns false
        every { inetAddress.isLinkLocalAddress } returns false
        every { inetAddress.hostAddress } returns "192.168.1.100"

        val networkInterface = mockk<NetworkInterface>()
        every { networkInterface.supportsMulticast() } returns false
        every { networkInterface.displayName } returns "eth0"
        every { networkInterface.inetAddresses } returns
            Collections.enumeration(listOf(inetAddress))

        mockkStatic(NetworkInterface::class)
        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(networkInterface))

        val result = NetworkUtil.getLocalInetAddress(context, requestMulticast = true)
        assertNull(result)
    }

    /**
     * Test [NetworkUtil.getLocalInetAddress] when there are multiple network interfaces, and we
     * need to pick one that supports multicast.
     */
    @Test
    fun testGetLocalInetAddressWithMultipleInterfacesOneSupportsMulticast() {
        mockEthernetConnected()
        mockkStatic(NetworkInterface::class)

        // Create a non-multicast interface
        val nonMulticastAddress = mockk<Inet4Address>()
        every { nonMulticastAddress.isLoopbackAddress } returns false
        every { nonMulticastAddress.isLinkLocalAddress } returns false
        every { nonMulticastAddress.hostAddress } returns "192.168.1.100"

        val nonMulticastInterface = mockk<NetworkInterface>()
        every { nonMulticastInterface.supportsMulticast() } returns false
        every { nonMulticastInterface.displayName } returns "eth0"
        every { nonMulticastInterface.inetAddresses } returns
            Collections.enumeration(listOf(nonMulticastAddress))

        // Create a multicast-capable interface
        val multicastAddress = mockk<Inet4Address>()
        every { multicastAddress.isLoopbackAddress } returns false
        every { multicastAddress.isLinkLocalAddress } returns false
        every { multicastAddress.hostAddress } returns "192.168.2.100"

        val multicastInterface = mockk<NetworkInterface>()
        every { multicastInterface.supportsMulticast() } returns true
        every { multicastInterface.displayName } returns "wlan0"
        every { multicastInterface.inetAddresses } returns
            Collections.enumeration(listOf(multicastAddress))

        // Mock NetworkInterface.getNetworkInterfaces() to return both interfaces
        every { NetworkInterface.getNetworkInterfaces() } returns
            Collections.enumeration(listOf(nonMulticastInterface, multicastInterface))

        val result = NetworkUtil.getLocalInetAddress(context, requestMulticast = true)
        assertNotNull(result)
        assertEquals("192.168.2.100", result?.hostAddress)
    }

    private fun mockNetworkNotConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = mockk<NetworkCapabilities>()
            every { networkCapabilities.hasTransport(any()) } returns false
            every { connectivityManager.activeNetwork } returns null
            every {
                connectivityManager.getNetworkCapabilities(any())
            } returns networkCapabilities
        } else {
            every { connectivityManager.activeNetworkInfo } returns null
        }
    }

    private fun mockWifiConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = mockk<Network>()
            val networkCapabilities = mockk<NetworkCapabilities>()
            every {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } returns true
            every {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } returns false
            every { connectivityManager.activeNetwork } returns network
            every {
                connectivityManager.getNetworkCapabilities(network)
            } returns networkCapabilities
        } else {
            val networkInfo = mockk<NetworkInfo>()
            every { networkInfo.isConnected } returns true
            every { networkInfo.type } returns ConnectivityManager.TYPE_WIFI
            every { connectivityManager.activeNetworkInfo } returns networkInfo
        }
    }

    private fun mockEthernetConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = mockk<Network>()
            val networkCapabilities = mockk<NetworkCapabilities>()
            every {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } returns false
            every {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } returns true
            every { connectivityManager.activeNetwork } returns network
            every {
                connectivityManager.getNetworkCapabilities(network)
            } returns networkCapabilities
        } else {
            val networkInfo = mockk<NetworkInfo>()
            every { networkInfo.isConnected } returns true
            every { networkInfo.type } returns ConnectivityManager.TYPE_ETHERNET
            every { connectivityManager.activeNetworkInfo } returns networkInfo
        }
    }

    /**
     * Clean up the mocks.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }
}
