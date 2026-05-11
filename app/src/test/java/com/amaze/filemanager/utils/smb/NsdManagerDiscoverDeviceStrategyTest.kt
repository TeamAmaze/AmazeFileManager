/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.utils.ComputerParcelable
import com.amaze.filemanager.utils.smb.NsdManagerDiscoverDeviceStrategy.Companion.SERVICE_TYPE_SMB
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [NsdManagerDiscoverDeviceStrategy].
 */
@Suppress("LongClass", "StringLiteralDuplication")
@RunWith(AndroidJUnit4::class)
@Config(sdk = [O, P, R])
class NsdManagerDiscoverDeviceStrategyTest {
    private lateinit var context: Context
    private lateinit var mockNsdManager: NsdManager
    private lateinit var mockWifiManager: WifiManager
    private lateinit var mockMulticastLock: WifiManager.MulticastLock

    /**
     * Set up mocks before each test.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockNsdManager = mockk(relaxed = true)
        mockWifiManager = mockk(relaxed = true)
        mockMulticastLock = mockk(relaxed = true)

        every { mockWifiManager.createMulticastLock(any()) } returns mockMulticastLock
        every { mockMulticastLock.isHeld } returns true
    }

    /**
     * Clean up after each test.
     */
    @After
    fun tearDown() {
        // Clean up if necessary
    }

    /**
     * Test that onServiceFound callback properly invokes the callback with discovered device.
     */
    @Test
    fun testOnServiceFoundInvokesCallback() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        val resolveListenerSlot = slot<NsdManager.ResolveListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create a mock NsdServiceInfo for discovery
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"
        every { mockServiceInfo.serviceType } returns SERVICE_TYPE_SMB

        // Create a mock resolved NsdServiceInfo with a valid host
        val mockResolvedServiceInfo = mockk<NsdServiceInfo>()
        val mockHost = mockk<InetAddress>()
        every { mockResolvedServiceInfo.serviceName } returns "TestServer"
        every { mockResolvedServiceInfo.host } returns mockHost
        every { mockHost.hostAddress } returns "192.168.1.100"

        // Mock resolveService to capture the ResolveListener and invoke onServiceResolved
        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onServiceResolved(mockResolvedServiceInfo)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()
        val latch = CountDownLatch(1)

        strategy.discoverDevices { computer ->
            result.add(computer)
            latch.countDown()
        }

        // Trigger onServiceFound
        listenerSlot.captured.onServiceFound(mockServiceInfo)

        // Wait for callback
        latch.await(1, TimeUnit.SECONDS)

        // Verify callback was invoked with correct data
        assertEquals(1, result.size)
        assertEquals("TestServer", result[0].name)
        assertEquals("192.168.1.100", result[0].addr)
    }

    /**
     * Test that onServiceFound does not invoke callback when host is null.
     */
    @Test
    fun testOnServiceFoundDoesNotInvokeCallbackWhenHostIsNull() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        val resolveListenerSlot = slot<NsdManager.ResolveListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create a mock NsdServiceInfo for discovery
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"
        every { mockServiceInfo.serviceType } returns SERVICE_TYPE_SMB

        // Create a mock resolved NsdServiceInfo with null host
        val mockResolvedServiceInfo = mockk<NsdServiceInfo>()
        every { mockResolvedServiceInfo.serviceName } returns "TestServer"
        every { mockResolvedServiceInfo.host } returns null

        // Mock resolveService to capture the ResolveListener and invoke onServiceResolved
        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onServiceResolved(mockResolvedServiceInfo)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()

        strategy.discoverDevices { computer ->
            result.add(computer)
        }

        // Trigger onServiceFound
        listenerSlot.captured.onServiceFound(mockServiceInfo)

        // Verify callback was NOT invoked
        assertEquals(0, result.size)
    }

    /**
     * Test that onServiceFound does not invoke callback when hostAddress is empty.
     */
    @Test
    fun testOnServiceFoundDoesNotInvokeCallbackWhenHostAddressIsEmpty() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        val resolveListenerSlot = slot<NsdManager.ResolveListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create a mock NsdServiceInfo for discovery
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"
        every { mockServiceInfo.serviceType } returns SERVICE_TYPE_SMB

        // Create a mock resolved NsdServiceInfo with empty hostAddress
        val mockResolvedServiceInfo = mockk<NsdServiceInfo>()
        val mockHost = mockk<InetAddress>()
        every { mockResolvedServiceInfo.serviceName } returns "TestServer"
        every { mockResolvedServiceInfo.host } returns mockHost
        every { mockHost.hostAddress } returns ""

        // Mock resolveService to capture the ResolveListener and invoke onServiceResolved
        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onServiceResolved(mockResolvedServiceInfo)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()

        strategy.discoverDevices { computer ->
            result.add(computer)
        }

        // Trigger onServiceFound
        listenerSlot.captured.onServiceFound(mockServiceInfo)

        // Verify callback was NOT invoked
        assertEquals(0, result.size)
    }

    /**
     * Test that onServiceFound does not invoke callback when hostAddress is null.
     */
    @Test
    fun testOnServiceFoundDoesNotInvokeCallbackWhenHostAddressIsNull() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        val resolveListenerSlot = slot<NsdManager.ResolveListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create a mock NsdServiceInfo for discovery
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"
        every { mockServiceInfo.serviceType } returns SERVICE_TYPE_SMB

        // Create a mock resolved NsdServiceInfo with null hostAddress
        val mockResolvedServiceInfo = mockk<NsdServiceInfo>()
        val mockHost = mockk<InetAddress>()
        every { mockResolvedServiceInfo.serviceName } returns "TestServer"
        every { mockResolvedServiceInfo.host } returns mockHost
        every { mockHost.hostAddress } returns null

        // Mock resolveService to capture the ResolveListener and invoke onServiceResolved
        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onServiceResolved(mockResolvedServiceInfo)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()

        strategy.discoverDevices { computer ->
            result.add(computer)
        }

        // Trigger onServiceFound
        listenerSlot.captured.onServiceFound(mockServiceInfo)

        // Verify callback was NOT invoked
        assertEquals(0, result.size)
    }

    /**
     * Test that discoverDevices acquires multicast lock and starts NSD discovery.
     */
    @Test
    fun testDiscoverDevicesStartsDiscovery() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            // Simulate discovery started callback
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()
        val latch = CountDownLatch(1)

        strategy.discoverDevices { computer ->
            result.add(computer)
            latch.countDown()
        }

        // Verify that multicast lock was acquired
        verify { mockMulticastLock.setReferenceCounted(true) }
        verify { mockMulticastLock.acquire() }

        // Verify that discovery was started
        verify {
            mockNsdManager.discoverServices(
                SERVICE_TYPE_SMB,
                NsdManager.PROTOCOL_DNS_SD,
                any(),
            )
        }
    }

    /**
     * Test that onCancel stops NSD discovery and releases multicast lock.
     */
    @Test
    fun testOnCancelStopsDiscoveryAndReleasesLock() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }
        strategy.onCancel()

        // Verify that multicast lock was released
        verify { mockMulticastLock.release() }
    }

    /**
     * Test that onCancel does not release lock if not held.
     */
    @Test
    fun testOnCancelDoesNotReleaseLockIfNotHeld() {
        every { mockMulticastLock.isHeld } returns false

        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }
        strategy.onCancel()

        // Verify that release was not called since lock is not held
        verify(exactly = 0) { mockMulticastLock.release() }
    }

    /**
     * Test onStartDiscoveryFailed callback stops discovery.
     */
    @Test
    fun testOnStartDiscoveryFailedStopsDiscovery() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            // Simulate discovery failed callback
            listenerSlot.captured.onStartDiscoveryFailed(
                SERVICE_TYPE_SMB,
                NsdManager.FAILURE_INTERNAL_ERROR,
            )
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }

        // Verify that stopServiceDiscovery was called
        verify { mockNsdManager.stopServiceDiscovery(any()) }
    }

    /**
     * Test onStopDiscoveryFailed callback stops discovery.
     */
    @Test
    fun testOnStopDiscoveryFailedStopsDiscovery() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }

        // Manually trigger stop discovery failed
        listenerSlot.captured.onStopDiscoveryFailed(
            SERVICE_TYPE_SMB,
            NsdManager.FAILURE_INTERNAL_ERROR,
        )

        // Verify that stopServiceDiscovery was called
        verify { mockNsdManager.stopServiceDiscovery(any()) }
    }

    /**
     * Test discovery stopped callback logs correctly.
     */
    @Test
    fun testOnDiscoveryStoppedLogs() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }

        // Should not throw - just logs
        listenerSlot.captured.onDiscoveryStopped(SERVICE_TYPE_SMB)
    }

    /**
     * Test that onResolveFailed does not invoke callback and logs error.
     */
    @Test
    fun testOnResolveFailedDoesNotInvokeCallback() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        val resolveListenerSlot = slot<NsdManager.ResolveListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create a mock NsdServiceInfo for discovery
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"
        every { mockServiceInfo.serviceType } returns SERVICE_TYPE_SMB

        // Mock resolveService to capture the ResolveListener and invoke onResolveFailed
        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onResolveFailed(mockServiceInfo, NsdManager.FAILURE_INTERNAL_ERROR)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()

        strategy.discoverDevices { computer ->
            result.add(computer)
        }

        // Trigger onServiceFound which will trigger onResolveFailed
        listenerSlot.captured.onServiceFound(mockServiceInfo)

        // Verify callback was NOT invoked
        assertEquals(0, result.size)
    }

    /**
     * Test that onServiceLost logs and does not crash.
     */
    @Test
    fun testOnServiceLostLogs() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }

        // Create a mock NsdServiceInfo
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"

        // Should not throw - just logs
        listenerSlot.captured.onServiceLost(mockServiceInfo)
    }

    /**
     * Test that onServiceLost handles null serviceInfo.
     */
    @Test
    fun testOnServiceLostHandlesNull() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        val strategy = createStrategyWithMocks()

        strategy.discoverDevices { }

        // Should not throw - just logs
        listenerSlot.captured.onServiceLost(null)
    }

    /**
     * Test that onServiceFound does not resolve service when serviceType does not match.
     */
    @Test
    fun testOnServiceFoundDoesNotResolveWhenServiceTypeDoesNotMatch() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create a mock NsdServiceInfo with non-matching service type
        val mockServiceInfo = mockk<NsdServiceInfo>()
        every { mockServiceInfo.serviceName } returns "TestServer"
        every { mockServiceInfo.serviceType } returns "_http._tcp."

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()

        strategy.discoverDevices { computer ->
            result.add(computer)
        }

        // Trigger onServiceFound with non-matching service type
        listenerSlot.captured.onServiceFound(mockServiceInfo)

        // Verify resolveService was NOT called
        verify(exactly = 0) {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(any(), any())
        }

        // Verify callback was NOT invoked
        assertEquals(0, result.size)
    }

    /**
     * Test discovering multiple devices.
     */
    @Test
    fun testDiscoverMultipleDevices() {
        val listenerSlot = slot<NsdManager.DiscoveryListener>()
        val resolveListenerSlot = slot<NsdManager.ResolveListener>()
        every {
            mockNsdManager.discoverServices(
                eq(SERVICE_TYPE_SMB),
                eq(NsdManager.PROTOCOL_DNS_SD),
                capture(listenerSlot),
            )
        } answers {
            listenerSlot.captured.onDiscoveryStarted(SERVICE_TYPE_SMB)
        }

        // Create mock NsdServiceInfo for first device
        val mockServiceInfo1 = mockk<NsdServiceInfo>()
        every { mockServiceInfo1.serviceName } returns "Server1"
        every { mockServiceInfo1.serviceType } returns SERVICE_TYPE_SMB

        val mockResolvedServiceInfo1 = mockk<NsdServiceInfo>()
        val mockHost1 = mockk<InetAddress>()
        every { mockResolvedServiceInfo1.serviceName } returns "Server1"
        every { mockResolvedServiceInfo1.host } returns mockHost1
        every { mockHost1.hostAddress } returns "192.168.1.100"

        // Create mock NsdServiceInfo for second device
        val mockServiceInfo2 = mockk<NsdServiceInfo>()
        every { mockServiceInfo2.serviceName } returns "Server2"
        every { mockServiceInfo2.serviceType } returns SERVICE_TYPE_SMB

        val mockResolvedServiceInfo2 = mockk<NsdServiceInfo>()
        val mockHost2 = mockk<InetAddress>()
        every { mockResolvedServiceInfo2.serviceName } returns "Server2"
        every { mockResolvedServiceInfo2.host } returns mockHost2
        every { mockHost2.hostAddress } returns "192.168.1.101"

        // Mock resolveService for both devices
        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo1), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onServiceResolved(mockResolvedServiceInfo1)
        }

        every {
            @Suppress("DEPRECATION")
            mockNsdManager.resolveService(eq(mockServiceInfo2), capture(resolveListenerSlot))
        } answers {
            resolveListenerSlot.captured.onServiceResolved(mockResolvedServiceInfo2)
        }

        val strategy = createStrategyWithMocks()
        val result = ArrayList<ComputerParcelable>()
        val latch = CountDownLatch(2)

        strategy.discoverDevices { computer ->
            result.add(computer)
            latch.countDown()
        }

        // Trigger onServiceFound for both devices
        listenerSlot.captured.onServiceFound(mockServiceInfo1)
        listenerSlot.captured.onServiceFound(mockServiceInfo2)

        // Wait for callbacks
        latch.await(1, TimeUnit.SECONDS)

        // Verify both callbacks were invoked
        assertEquals(2, result.size)
        assertEquals("Server1", result[0].name)
        assertEquals("192.168.1.100", result[0].addr)
        assertEquals("Server2", result[1].name)
        assertEquals("192.168.1.101", result[1].addr)
    }

    /**
     * Helper method to create a strategy instance with mocked dependencies.
     * Uses reflection to inject mocks since the class initializes managers in constructor.
     */
    private fun createStrategyWithMocks(): NsdManagerDiscoverDeviceStrategy {
        val strategy = NsdManagerDiscoverDeviceStrategy()

        // Use reflection to replace the private fields with our mocks
        val wifiManagerField =
            NsdManagerDiscoverDeviceStrategy::class.java
                .getDeclaredField("wifiManager")
        wifiManagerField.isAccessible = true
        wifiManagerField.set(strategy, mockWifiManager)

        val nsdManagerField =
            NsdManagerDiscoverDeviceStrategy::class.java
                .getDeclaredField("nsdManager")
        nsdManagerField.isAccessible = true
        nsdManagerField.set(strategy, mockNsdManager)

        return strategy
    }
}
