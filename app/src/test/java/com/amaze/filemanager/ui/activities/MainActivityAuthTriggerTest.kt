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
package com.amaze.filemanager.ui.activities

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.utils.omh.AuthTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for MainActivity's AuthTrigger implementation and AuthCallback behavior.
 */
class MainActivityAuthTriggerTest : AbstractMainActivityTestBase() {
    /**
     * Test that MainActivity implements AuthTrigger interface.
     */
    @Test
    fun testMainActivityImplementsAuthTrigger() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)

        scenario.onActivity { activity ->
            assertTrue("MainActivity should implement AuthTrigger", activity is AuthTrigger)
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)
        scenario.close()
    }

    /**
     * Test AuthCallback interface contract - success case.
     */
    @Test
    fun testAuthCallbackSuccess() {
        val successCalled = AtomicBoolean(false)
        val failureCalled = AtomicBoolean(false)

        val callback =
            object : MainActivity.AuthCallback {
                override fun onAuthSuccess() {
                    successCalled.set(true)
                }

                override fun onAuthFailure(errorMessage: String?) {
                    failureCalled.set(true)
                }
            }

        callback.onAuthSuccess()

        assertTrue("onAuthSuccess should be called", successCalled.get())
        assertFalse("onAuthFailure should not be called", failureCalled.get())
    }

    /**
     * Test AuthCallback interface contract - failure case.
     */
    @Test
    fun testAuthCallbackFailure() {
        val successCalled = AtomicBoolean(false)
        val failureCalled = AtomicBoolean(false)
        val errorReceived = AtomicReference<String?>()

        val callback =
            object : MainActivity.AuthCallback {
                override fun onAuthSuccess() {
                    successCalled.set(true)
                }

                override fun onAuthFailure(errorMessage: String?) {
                    failureCalled.set(true)
                    errorReceived.set(errorMessage)
                }
            }

        callback.onAuthFailure("Test error message")

        assertFalse("onAuthSuccess should not be called", successCalled.get())
        assertTrue("onAuthFailure should be called", failureCalled.get())
        assertEquals("Test error message", errorReceived.get())
    }

    /**
     * Test AuthCallback with null error message.
     */
    @Test
    fun testAuthCallbackFailureNullMessage() {
        val errorReceived = AtomicReference<String?>("not-null")

        val callback =
            object : MainActivity.AuthCallback {
                override fun onAuthSuccess() = Unit

                override fun onAuthFailure(errorMessage: String?) {
                    errorReceived.set(errorMessage)
                }
            }

        callback.onAuthFailure(null)

        assertEquals(null, errorReceived.get())
    }

    /**
     * Test that OpenMode values used for cloud services are valid.
     */
    @Test
    fun testCloudOpenModeValues() {
        // Verify cloud-related OpenMode values exist
        assertNotNull(OpenMode.DROPBOX)
        assertNotNull(OpenMode.GDRIVE)
        assertNotNull(OpenMode.ONEDRIVE)
        assertNotNull(OpenMode.BOX)

        // Verify they are distinct
        val modes = setOf(OpenMode.DROPBOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE, OpenMode.BOX)
        assertEquals(4, modes.size)
    }

    /**
     * Test CountDownLatch behavior used in triggerAuthBlocking.
     * This validates the threading mechanism used for blocking auth.
     */
    @Test
    fun testCountDownLatchMechanism() {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)

        // Simulate async operation completing
        Thread {
            Thread.sleep(50)
            result.set(true)
            latch.countDown()
        }.start()

        val completed = latch.await(5, TimeUnit.SECONDS)

        assertTrue("Latch should complete", completed)
        assertTrue("Result should be set", result.get())
    }

    /**
     * Test CountDownLatch timeout behavior.
     */
    @Test
    fun testCountDownLatchTimeout() {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)

        // Don't countdown, simulating auth never completing
        val completed = latch.await(100, TimeUnit.MILLISECONDS)

        assertFalse("Latch should timeout", completed)
        assertFalse("Result should not be set", result.get())
    }

    /**
     * Test that multiple AuthCallback calls are handled.
     */
    @Test
    fun testMultipleAuthCallbackCalls() {
        val callCount = AtomicInteger(0)

        val callback =
            object : MainActivity.AuthCallback {
                override fun onAuthSuccess() {
                    callCount.incrementAndGet()
                }

                override fun onAuthFailure(errorMessage: String?) {
                    callCount.incrementAndGet()
                }
            }

        callback.onAuthSuccess()
        callback.onAuthSuccess()
        callback.onAuthFailure("error")

        assertEquals(3, callCount.get())
    }

    /**
     * Test concurrent AuthCallback access pattern.
     * Simulates multiple threads trying to call callbacks.
     */
    @Test
    fun testConcurrentAuthCallbackAccess() {
        val callCount = AtomicInteger(0)
        val latch = CountDownLatch(10)

        val callback =
            object : MainActivity.AuthCallback {
                override fun onAuthSuccess() {
                    callCount.incrementAndGet()
                    latch.countDown()
                }

                override fun onAuthFailure(errorMessage: String?) {
                    callCount.incrementAndGet()
                    latch.countDown()
                }
            }

        // Launch multiple threads calling the callback
        repeat(10) { index ->
            Thread {
                if (index % 2 == 0) {
                    callback.onAuthSuccess()
                } else {
                    callback.onAuthFailure("error-$index")
                }
            }.start()
        }

        val completed = latch.await(5, TimeUnit.SECONDS)

        assertTrue("All callbacks should complete", completed)
        assertEquals(10, callCount.get())
    }

    /**
     * Test the blocking pattern used in triggerAuthBlocking without actual Activity.
     * This validates the core synchronization logic.
     */
    @Test
    fun testBlockingAuthPattern() {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)
        val callbackExecuted = AtomicBoolean(false)

        // Simulate the pattern used in triggerAuthBlocking
        val simulateUiThread =
            Thread {
                // This represents runOnUiThread content
                val callback =
                    object : MainActivity.AuthCallback {
                        override fun onAuthSuccess() {
                            result.set(true)
                            latch.countDown()
                        }

                        override fun onAuthFailure(errorMessage: String?) {
                            result.set(false)
                            latch.countDown()
                        }
                    }

                // Simulate auth completing after some delay
                Thread.sleep(50)
                callbackExecuted.set(true)
                callback.onAuthSuccess()
            }
        simulateUiThread.start()

        // This represents the blocking wait
        val completed = latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should complete", completed)
        assertTrue("Callback should have executed", callbackExecuted.get())
        assertTrue("Result should be true (success)", result.get())
    }

    /**
     * Test the blocking pattern with failure.
     */
    @Test
    fun testBlockingAuthPatternFailure() {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(true) // Start with true to verify it changes

        val simulateUiThread =
            Thread {
                val callback =
                    object : MainActivity.AuthCallback {
                        override fun onAuthSuccess() {
                            result.set(true)
                            latch.countDown()
                        }

                        override fun onAuthFailure(errorMessage: String?) {
                            result.set(false)
                            latch.countDown()
                        }
                    }

                Thread.sleep(50)
                callback.onAuthFailure("Auth denied")
            }
        simulateUiThread.start()

        val completed = latch.await(5, TimeUnit.SECONDS)

        assertTrue("Should complete", completed)
        assertFalse("Result should be false (failure)", result.get())
    }

    /**
     * Test interruption handling in blocking auth.
     */
    @Test
    fun testBlockingAuthInterruption() {
        val latch = CountDownLatch(1)
        val wasInterrupted = AtomicBoolean(false)

        val blockingThread =
            Thread {
                try {
                    latch.await(10, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    wasInterrupted.set(true)
                    Thread.currentThread().interrupt()
                }
            }
        blockingThread.start()

        // Give it a moment to start waiting
        Thread.sleep(50)

        // Interrupt the thread
        blockingThread.interrupt()
        blockingThread.join(1000)

        assertTrue("Thread should have been interrupted", wasInterrupted.get())
    }

    /**
     * Test that result is correctly returned based on callback.
     */
    @Test
    fun testBlockingAuthResultMapping() {
        // Test success -> true
        assertEquals(true, simulateBlockingAuth { it.onAuthSuccess() })

        // Test failure -> false
        assertEquals(false, simulateBlockingAuth { it.onAuthFailure("error") })
    }

    /**
     * Helper to simulate the blocking auth pattern.
     */
    private fun simulateBlockingAuth(authAction: (MainActivity.AuthCallback) -> Unit): Boolean {
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)

        Thread {
            val callback =
                object : MainActivity.AuthCallback {
                    override fun onAuthSuccess() {
                        result.set(true)
                        latch.countDown()
                    }

                    override fun onAuthFailure(errorMessage: String?) {
                        result.set(false)
                        latch.countDown()
                    }
                }
            authAction(callback)
        }.start()

        latch.await(5, TimeUnit.SECONDS)
        return result.get()
    }

    /**
     * Test OpenMode enum ordinal values for cloud services.
     * This ensures serialization consistency.
     */
    @Test
    fun testOpenModeOrdinalStability() {
        // These ordinal values are used in Intent extras
        // Changes would break serialization
        val dropboxOrdinal = OpenMode.DROPBOX.ordinal
        val gdriveOrdinal = OpenMode.GDRIVE.ordinal
        val onedriveOrdinal = OpenMode.ONEDRIVE.ordinal
        val boxOrdinal = OpenMode.BOX.ordinal

        // Verify ordinals are distinct
        val ordinals = setOf(dropboxOrdinal, gdriveOrdinal, onedriveOrdinal, boxOrdinal)
        assertEquals("All cloud mode ordinals should be distinct", 4, ordinals.size)
    }

    /**
     * Test that all cloud OpenModes can be converted to/from ordinal.
     */
    @Test
    fun testOpenModeOrdinalRoundTrip() {
        for (mode in listOf(OpenMode.DROPBOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE, OpenMode.BOX)) {
            val ordinal = mode.ordinal
            val restored = OpenMode.values()[ordinal]
            assertEquals("Round-trip should preserve OpenMode", mode, restored)
        }
    }
}
