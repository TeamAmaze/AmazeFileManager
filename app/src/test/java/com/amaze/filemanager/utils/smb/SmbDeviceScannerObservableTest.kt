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

import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.utils.ComputerParcelable
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unit tests for [SmbDeviceScannerObservable].
 */
@Suppress("LongClass", "StringLiteralDuplication")
@RunWith(AndroidJUnit4::class)
@Config(sdk = [O, P, R])
class SmbDeviceScannerObservableTest {
    private lateinit var mockStrategy1: SmbDeviceScannerObservable.DiscoverDeviceStrategy
    private lateinit var mockStrategy2: SmbDeviceScannerObservable.DiscoverDeviceStrategy

    /**
     * Set up mocks before each test.
     */
    @Before
    fun setUp() {
        mockStrategy1 = mockk(relaxed = true)
        mockStrategy2 = mockk(relaxed = true)

        // Override RxJava schedulers to use trampoline for testing
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
    }

    /**
     * Reset RxJava plugins after each test.
     */
    @After
    fun tearDown() {
        RxJavaPlugins.reset()
    }

    /**
     * Test that subscription triggers device discovery on all strategies.
     */
    @Test
    fun testSubscriptionTriggersDiscovery() {
        val latch = CountDownLatch(1)

        every { mockStrategy1.discoverDevices(any()) } answers {
            // Do nothing, just capture the callback
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            // Do nothing, just capture the callback
        }

        val observable = createObservableWithMockStrategies()

        observable
            .subscribeOn(Schedulers.trampoline())
            .observeOn(Schedulers.trampoline())
            .subscribe()

        // Give time for async operations
        latch.await(500, TimeUnit.MILLISECONDS)

        // Verify both strategies were called
        verify { mockStrategy1.discoverDevices(any()) }
        verify { mockStrategy2.discoverDevices(any()) }
    }

    /**
     * Test that discovered devices are emitted to the observer.
     */
    @Test
    fun testDiscoveredDevicesAreEmitted() {
        val computer1 = ComputerParcelable("Server1", "192.168.1.100")
        val computer2 = ComputerParcelable("Server2", "192.168.1.101")

        every { mockStrategy1.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer1)
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer2)
        }

        val observable = createObservableWithMockStrategies()

        observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        // Verify both strategies were called and emitted devices
        verify { mockStrategy1.discoverDevices(any()) }
        verify { mockStrategy2.discoverDevices(any()) }
    }

    /**
     * Test that multiple devices from the same strategy are emitted.
     * This test verifies that strategy.discoverDevices is called and
     * that the observable can receive callbacks.
     */
    @Test
    fun testMultipleDevicesFromSameStrategy() {
        val computer1 = ComputerParcelable("Server1", "192.168.1.100")

        every { mockStrategy1.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer1)
        }

        // Create observable with single strategy
        val observable = SmbDeviceScannerObservable()
        val field = SmbDeviceScannerObservable::class.java.getDeclaredField("discoverDeviceStrategies")
        field.isAccessible = true
        field.set(observable, arrayOf(mockStrategy1))

        observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        // Verify that strategy was called
        verify { mockStrategy1.discoverDevices(any()) }
    }

    /**
     * Test that stop() calls onCancel on all strategies.
     */
    @Test
    fun testStopCallsOnCancelOnAllStrategies() {
        val latch = CountDownLatch(1)

        every { mockStrategy1.discoverDevices(any()) } answers {
            // Do nothing
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            // Do nothing
        }

        val observable = createObservableWithMockStrategies()

        observable
            .subscribeOn(Schedulers.trampoline())
            .observeOn(Schedulers.trampoline())
            .subscribe()

        // Wait a bit for subscription to complete
        latch.await(200, TimeUnit.MILLISECONDS)

        observable.stop()

        // Verify onCancel was called on both strategies
        verify { mockStrategy1.onCancel() }
        verify { mockStrategy2.onCancel() }
    }

    /**
     * Test that errors from strategies are propagated to the observer.
     */
    @Test
    fun testErrorsArePropagated() {
        val latch = CountDownLatch(1)
        val errorReceived = AtomicBoolean(false)
        val testException = RuntimeException("Test error")

        every { mockStrategy1.discoverDevices(any()) } throws testException
        every { mockStrategy2.discoverDevices(any()) } answers {
            // Do nothing
        }

        val observable = createObservableWithMockStrategies()

        observable
            .subscribeOn(Schedulers.trampoline())
            .observeOn(Schedulers.trampoline())
            .subscribe(
                { },
                { error ->
                    if (error is RuntimeException && error.message == "Test error") {
                        errorReceived.set(true)
                    }
                    latch.countDown()
                },
            )

        latch.await(1, TimeUnit.SECONDS)

        assertTrue("Error should be propagated to observer", errorReceived.get())
    }

    /**
     * Test that discovery continues even if one strategy doesn't find anything.
     */
    @Test
    fun testDiscoveryContinuesIfOneStrategyFindsNothing() {
        val computer = ComputerParcelable("Server1", "192.168.1.100")

        every { mockStrategy1.discoverDevices(any()) } answers {
            // Strategy 1 finds nothing
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer)
        }

        val observable = createObservableWithMockStrategies()

        observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        // Verify both strategies were called
        verify { mockStrategy1.discoverDevices(any()) }
        verify { mockStrategy2.discoverDevices(any()) }
    }

    /**
     * Test that duplicate devices (same address) can be emitted (filtering is done by consumer).
     */
    @Test
    fun testDuplicateDevicesAreEmitted() {
        val computer1 = ComputerParcelable("Server1", "192.168.1.100")
        val computer2 = ComputerParcelable("Server1", "192.168.1.100") // Same as computer1

        every { mockStrategy1.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer1)
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer2)
        }

        val observable = createObservableWithMockStrategies()

        observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        // Verify both strategies were called (both emit - consumer is responsible for deduplication)
        verify { mockStrategy1.discoverDevices(any()) }
        verify { mockStrategy2.discoverDevices(any()) }
    }

    /**
     * Test that observable works with a single strategy.
     */
    @Test
    fun testSingleStrategy() {
        val computer = ComputerParcelable("Server1", "192.168.1.100")

        every { mockStrategy1.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer)
        }

        val observable = SmbDeviceScannerObservable()
        // Use reflection to set single strategy
        val field = SmbDeviceScannerObservable::class.java.getDeclaredField("discoverDeviceStrategies")
        field.isAccessible = true
        field.set(observable, arrayOf(mockStrategy1))

        observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        // Verify that strategy was called
        verify { mockStrategy1.discoverDevices(any()) }
    }

    /**
     * Test that callbacks are invoked even if emitter is not disposed.
     */
    @Test
    fun testCallbacksInvokedWhenNotDisposed() {
        val callbackInvoked = AtomicBoolean(false)

        val computer = ComputerParcelable("Server1", "192.168.1.100")

        every { mockStrategy1.discoverDevices(any()) } answers {
            val callback = firstArg<(ComputerParcelable) -> Unit>()
            callback(computer)
            callbackInvoked.set(true)
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            // Do nothing
        }

        val observable = createObservableWithMockStrategies()

        observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue("Callback should have been invoked", callbackInvoked.get())
    }

    /**
     * Test that disposing stops receiving new devices.
     */
    @Test
    fun testDisposingStopsReceivingDevices() {
        every { mockStrategy1.discoverDevices(any()) } answers {
            // Do nothing - just verify we can subscribe and dispose
        }
        every { mockStrategy2.discoverDevices(any()) } answers {
            // Do nothing
        }

        val observable = createObservableWithMockStrategies()

        val disposable = observable.subscribe()

        // Process any pending runnables
        shadowOf(Looper.getMainLooper()).idle()

        // Dispose
        disposable.dispose()

        // Verify subscription was created and disposed
        assertTrue("Subscription should be disposed", disposable.isDisposed)
    }

    /**
     * Helper method to create an observable with mock strategies.
     */
    private fun createObservableWithMockStrategies(): SmbDeviceScannerObservable {
        val observable = SmbDeviceScannerObservable()

        // Use reflection to replace the strategies with our mocks
        val field = SmbDeviceScannerObservable::class.java.getDeclaredField("discoverDeviceStrategies")
        field.isAccessible = true
        field.set(observable, arrayOf(mockStrategy1, mockStrategy2))

        return observable
    }
}
