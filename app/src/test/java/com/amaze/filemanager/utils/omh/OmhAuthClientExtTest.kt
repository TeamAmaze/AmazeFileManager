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
package com.amaze.filemanager.utils.omh

import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ProtocolException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [retryOnUnauthorized] function.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress(
    "StringLiteralDuplication",
    "ComplexMethod",
    "LongMethod",
    "LargeClass",
    "TooGenericExceptionCaught",
    "TooGenericExceptionThrown",
)
class OmhAuthClientExtTest {
    private val testOpenMode = OpenMode.DROPBOX

    /**
     * Test that action succeeds on first try without any retries.
     */
    @Test
    fun testSuccessOnFirstTry() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { fail("refreshAction should not be called") },
                    action = {
                        actionCallCount.incrementAndGet()
                        "success"
                    },
                )

            assertEquals("success", result)
            assertEquals(1, actionCallCount.get())
        }

    /**
     * Test that action is retried when CloudPluginException with ProtocolException cause is thrown.
     */
    @Test
    fun testRetryOnProtocolException() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val refreshCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 3,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { refreshCallCount.incrementAndGet() },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count < 3) {
                            throw createProtocolException()
                        }
                        "success after retry"
                    },
                )

            assertEquals("success after retry", result)
            assertEquals(3, actionCallCount.get())
            assertEquals(2, refreshCallCount.get()) // Refresh called on first 2 failures
        }

    /**
     * Test that auth trigger is called when max retries are exhausted with ProtocolException.
     */
    @Test
    fun testAuthTriggerCalledOnMaxRetries() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val refreshCallCount = AtomicInteger(0)
            val authTriggerCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authTriggerCallCount.incrementAndGet()
                                assertEquals(testOpenMode, openMode)
                                return true
                            }
                        },
                    refreshAction = { refreshCallCount.incrementAndGet() },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count <= 2) {
                            throw createProtocolException()
                        }
                        "success after auth"
                    },
                )

            assertEquals("success after auth", result)
            assertEquals(3, actionCallCount.get()) // 2 initial attempts + 1 after auth
            assertEquals(1, refreshCallCount.get()) // Refresh called once before maxRetries
            assertEquals(1, authTriggerCallCount.get()) // Auth triggered once at maxRetries
        }

    /**
     * Test that exception is thrown when auth trigger fails.
     */
    @Test
    fun testExceptionThrownWhenAuthTriggerFails() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(shouldSucceed = false),
                    refreshAction = { /* do nothing */ },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw createProtocolException()
                    },
                )
                fail("Expected CloudPluginException to be thrown")
            } catch (e: CloudPluginException) {
                // Expected
                assertTrue(e.cause is ProtocolException)
            }

            assertEquals(2, actionCallCount.get())
        }

    /**
     * Test that non-ProtocolException CloudPluginException triggers retry with backoff.
     */
    @Test
    fun testRetryWithBackoffOnNetworkError() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 3,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { fail("refreshAction should not be called for network error") },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count < 3) {
                            // CloudPluginException without ProtocolException cause
                            throw CloudPluginException(RuntimeException("Network error"))
                        }
                        "success"
                    },
                )

            assertEquals("success", result)
            assertEquals(3, actionCallCount.get())
        }

    /**
     * Test that non-ProtocolException CloudPluginException is thrown after max retries.
     */
    @Test
    fun testNetworkErrorThrownAfterMaxRetries() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { fail("refreshAction should not be called for network error") },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw CloudPluginException(RuntimeException("Network error"))
                    },
                )
                fail("Expected CloudPluginException to be thrown")
            } catch (e: CloudPluginException) {
                // Expected
                assertTrue(e.cause is RuntimeException)
                assertEquals("Network error", e.cause?.message)
            }

            assertEquals(2, actionCallCount.get())
        }

    /**
     * Test that CancellationException is propagated immediately without retry.
     */
    @Test
    fun testCancellationExceptionPropagated() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 3,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { fail("refreshAction should not be called") },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw CancellationException("Cancelled")
                    },
                )
                fail("Expected CancellationException to be thrown")
            } catch (e: CancellationException) {
                // Expected
                assertEquals("Cancelled", e.message)
            }

            assertEquals(1, actionCallCount.get())
        }

    /**
     * Test that unexpected exceptions are propagated immediately.
     */
    @Test
    fun testUnexpectedExceptionPropagated() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 3,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { fail("refreshAction should not be called") },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw IllegalStateException("Unexpected error")
                    },
                )
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                // Expected
                assertEquals("Unexpected error", e.message)
            }

            assertEquals(1, actionCallCount.get())
        }

    /**
     * Test that refresh action exception is propagated.
     */
    @Test
    fun testRefreshActionExceptionPropagated() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(shouldSucceed = true),
                    refreshAction = { throw RuntimeException("Refresh failed") },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw createProtocolException()
                    },
                )
                fail("Expected exception from refresh action")
            } catch (e: Exception) {
                // The exception from CompletableFuture.get() wraps the original exception
                assertTrue(
                    e.message?.contains("Refresh failed") == true ||
                        e.cause?.message?.contains("Refresh failed") == true,
                )
            }

            assertEquals(1, actionCallCount.get())
        }

    /**
     * Test with maxRetries = 1, should trigger auth on first failure with ProtocolException.
     */
    @Test
    fun testSingleRetryTriggersAuth() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val authTriggerCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authTriggerCallCount.incrementAndGet()
                                return true
                            }
                        },
                    refreshAction = { fail("refreshAction should not be called with maxRetries=1") },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count == 1) {
                            throw createProtocolException()
                        }
                        "success"
                    },
                )

            assertEquals("success", result)
            assertEquals(2, actionCallCount.get())
            assertEquals(1, authTriggerCallCount.get())
        }

    /**
     * Test calling retryOnUnauthorized from runBlocking context (simulating Java interop).
     * This simulates the actual usage in CloudUtil.getCloudFilesBlocking.
     */
    @Test
    fun testFromRunBlockingContext() {
        val actionCallCount = AtomicInteger(0)
        val authTriggerCallCount = AtomicInteger(0)

        val result =
            runBlocking(Dispatchers.IO.limitedParallelism(1)) {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authTriggerCallCount.incrementAndGet()
                                return true
                            }
                        },
                    refreshAction = { /* do nothing */ },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count <= 2) {
                            throw createProtocolException()
                        }
                        "success after auth"
                    },
                )
            }

        assertEquals("success after auth", result)
        assertEquals(3, actionCallCount.get())
        assertEquals(1, authTriggerCallCount.get())
    }

    /**
     * Test concurrent calls to retryOnUnauthorized.
     * This tests thread-safety of the retry mechanism.
     */
    @Test
    fun testConcurrentCalls() =
        runTest {
            val totalCalls = 5
            val successCount = AtomicInteger(0)

            val results =
                (1..totalCalls).map { callIndex ->
                    async(Dispatchers.IO) {
                        retryOnUnauthorized(
                            openMode = testOpenMode,
                            maxRetries = 2,
                            trigger = createMockAuthTrigger(shouldSucceed = true),
                            refreshAction = { /* do nothing */ },
                            action = {
                                successCount.incrementAndGet()
                                "result-$callIndex"
                            },
                        )
                    }
                }.awaitAll()

            assertEquals(totalCalls, results.size)
            assertEquals(totalCalls, successCount.get())
        }

    /**
     * Test that auth trigger blocks correctly and completes.
     * Simulates a slow auth process.
     */
    @Test
    fun testAuthTriggerBlocksAndCompletes() =
        runTest {
            val authStarted = CountDownLatch(1)
            val authCompleted = CountDownLatch(1)
            val actionCallCount = AtomicInteger(0)

            val result =
                withContext(Dispatchers.IO) {
                    retryOnUnauthorized(
                        openMode = testOpenMode,
                        maxRetries = 1,
                        trigger =
                            object : AuthTrigger {
                                override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                    authStarted.countDown()
                                    // Simulate slow auth (100ms)
                                    Thread.sleep(100)
                                    authCompleted.countDown()
                                    return true
                                }
                            },
                        refreshAction = { fail("Should not refresh with maxRetries=1") },
                        action = {
                            val count = actionCallCount.incrementAndGet()
                            if (count == 1) {
                                throw createProtocolException()
                            }
                            "success"
                        },
                    )
                }

            assertTrue("Auth should have started", authStarted.await(5, TimeUnit.SECONDS))
            assertTrue("Auth should have completed", authCompleted.await(5, TimeUnit.SECONDS))
            assertEquals("success", result)
            assertEquals(2, actionCallCount.get())
        }

    /**
     * Test nested ProtocolException causes are detected correctly.
     */
    @Test
    fun testNestedProtocolExceptionDetection() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 1,
                    trigger = createMockAuthTrigger(shouldSucceed = false),
                    refreshAction = { fail("Should not be called") },
                    action = {
                        actionCallCount.incrementAndGet()
                        // Nested ProtocolException: RuntimeException -> ProtocolException
                        val protocolEx = ProtocolException("401 Unauthorized")
                        val wrapperEx = RuntimeException("Wrapper", protocolEx)
                        throw CloudPluginException(wrapperEx)
                    },
                )
                fail("Expected exception")
            } catch (e: CloudPluginException) {
                // Expected - should detect nested ProtocolException and trigger auth
            }

            assertEquals(1, actionCallCount.get())
        }

    // ========== Additional Tests for Different OpenMode Types ==========

    /**
     * Test with OpenMode.GDRIVE
     */
    @Test
    fun testWithGDriveOpenMode() =
        runTest {
            val openModeUsed = AtomicInteger(-1)

            val result =
                retryOnUnauthorized(
                    openMode = OpenMode.GDRIVE,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                openModeUsed.set(openMode.ordinal)
                                return true
                            }
                        },
                    refreshAction = { },
                    action = {
                        if (openModeUsed.get() == -1) {
                            throw createProtocolException()
                        }
                        "gdrive-success"
                    },
                )

            assertEquals("gdrive-success", result)
            assertEquals(OpenMode.GDRIVE.ordinal, openModeUsed.get())
        }

    /**
     * Test with OpenMode.ONEDRIVE
     */
    @Test
    fun testWithOneDriveOpenMode() =
        runTest {
            val openModeUsed = AtomicInteger(-1)

            val result =
                retryOnUnauthorized(
                    openMode = OpenMode.ONEDRIVE,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                openModeUsed.set(openMode.ordinal)
                                return true
                            }
                        },
                    refreshAction = { },
                    action = {
                        if (openModeUsed.get() == -1) {
                            throw createProtocolException()
                        }
                        "onedrive-success"
                    },
                )

            assertEquals("onedrive-success", result)
            assertEquals(OpenMode.ONEDRIVE.ordinal, openModeUsed.get())
        }

    /**
     * Test with OpenMode.BOX
     */
    @Test
    fun testWithBoxOpenMode() =
        runTest {
            val openModeUsed = AtomicInteger(-1)

            val result =
                retryOnUnauthorized(
                    openMode = OpenMode.BOX,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                openModeUsed.set(openMode.ordinal)
                                return true
                            }
                        },
                    refreshAction = { },
                    action = {
                        if (openModeUsed.get() == -1) {
                            throw createProtocolException()
                        }
                        "box-success"
                    },
                )

            assertEquals("box-success", result)
            assertEquals(OpenMode.BOX.ordinal, openModeUsed.get())
        }

    // ========== Tests for Auth Trigger Exception Scenarios ==========

    /**
     * Test when auth trigger throws an exception.
     */
    @Test
    fun testAuthTriggerThrowsException() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                throw RuntimeException("Auth system error")
                            }
                        },
                    refreshAction = { },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw createProtocolException()
                    },
                )
                fail("Expected CloudPluginException")
            } catch (e: CloudPluginException) {
                // Auth trigger exception is caught, authSuccess becomes false, original exception thrown
                assertTrue(e.cause is ProtocolException)
            }

            assertEquals(1, actionCallCount.get())
        }

    /**
     * Test when auth trigger throws RuntimeException after successful refresh attempts.
     */
    @Test
    fun testAuthTriggerExceptionAfterRefreshAttempts() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val refreshCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 3,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                throw IllegalStateException("Auth unavailable")
                            }
                        },
                    refreshAction = { refreshCallCount.incrementAndGet() },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw createProtocolException()
                    },
                )
                fail("Expected CloudPluginException")
            } catch (e: CloudPluginException) {
                assertTrue(e.cause is ProtocolException)
            }

            assertEquals(3, actionCallCount.get())
            assertEquals(2, refreshCallCount.get()) // Called for attempts 1 and 2
        }

    // ========== Tests for Multiple Failures and Recovery ==========

    /**
     * Test multiple protocol exceptions followed by success on final action after auth.
     */
    @Test
    fun testMultipleFailuresThenSuccessAfterAuth() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val refreshCallCount = AtomicInteger(0)
            val authCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 5,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authCallCount.incrementAndGet()
                                return true
                            }
                        },
                    refreshAction = { refreshCallCount.incrementAndGet() },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count <= 5) {
                            throw createProtocolException()
                        }
                        "finally success"
                    },
                )

            assertEquals("finally success", result)
            assertEquals(6, actionCallCount.get()) // 5 failures + 1 success after auth
            assertEquals(4, refreshCallCount.get()) // Refresh for attempts 1-4
            assertEquals(1, authCallCount.get()) // Auth triggered at attempt 5
        }

    /**
     * Test that refresh is called correct number of times before auth trigger.
     */
    @Test
    fun testRefreshCalledCorrectTimes() =
        runTest {
            val refreshCallCount = AtomicInteger(0)
            val authCallCount = AtomicInteger(0)
            val actionCallCount = AtomicInteger(0)

            retryOnUnauthorized(
                openMode = testOpenMode,
                maxRetries = 4,
                trigger =
                    object : AuthTrigger {
                        override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                            authCallCount.incrementAndGet()
                            return true
                        }
                    },
                refreshAction = { refreshCallCount.incrementAndGet() },
                action = {
                    val count = actionCallCount.incrementAndGet()
                    if (count <= 4) {
                        throw createProtocolException()
                    }
                    "done"
                },
            )

            assertEquals(5, actionCallCount.get())
            assertEquals(3, refreshCallCount.get()) // Refresh for attempts 1, 2, 3 (not 4, which triggers auth)
            assertEquals(1, authCallCount.get())
        }

    // ========== Tests for Deeply Nested Exception Chains ==========

    /**
     * Test detection of ProtocolException deeply nested in exception chain.
     */
    @Test
    fun testDeeplyNestedProtocolException() =
        runTest {
            val authTriggerCalled = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authTriggerCalled.incrementAndGet()
                                return true
                            }
                        },
                    refreshAction = { },
                    action = {
                        if (authTriggerCalled.get() == 0) {
                            // Create deeply nested exception: CloudPluginException -> RuntimeException -> IOException -> ProtocolException
                            val protocolEx = ProtocolException("401")
                            val ioEx = java.io.IOException("IO error", protocolEx)
                            val runtimeEx = RuntimeException("Runtime wrapper", ioEx)
                            throw CloudPluginException(runtimeEx)
                        }
                        "success"
                    },
                )

            assertEquals("success", result)
            assertEquals(1, authTriggerCalled.get())
        }

    /**
     * Test that non-ProtocolException nested chain does not trigger auth.
     */
    @Test
    fun testDeeplyNestedNonProtocolException() =
        runTest {
            val authTriggerCalled = AtomicInteger(0)
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authTriggerCalled.incrementAndGet()
                                return true
                            }
                        },
                    refreshAction = { fail("Should not refresh for non-protocol exception") },
                    action = {
                        actionCallCount.incrementAndGet()
                        // Deeply nested but no ProtocolException
                        val innerEx = IllegalArgumentException("Invalid arg")
                        val ioEx = java.io.IOException("IO error", innerEx)
                        val runtimeEx = RuntimeException("Runtime wrapper", ioEx)
                        throw CloudPluginException(runtimeEx)
                    },
                )
                fail("Expected exception")
            } catch (e: CloudPluginException) {
                // Expected - network error path, no auth trigger
            }

            assertEquals(2, actionCallCount.get())
            assertEquals(0, authTriggerCalled.get()) // Auth should NOT be called
        }

    // ========== Tests for Different Return Types ==========

    /**
     * Test returning Int type.
     */
    @Test
    fun testReturnTypeInt() =
        runTest {
            val result: Int =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = { 42 },
                )

            assertEquals(42, result)
        }

    /**
     * Test returning List type.
     */
    @Test
    fun testReturnTypeList() =
        runTest {
            val result: List<String> =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = { listOf("a", "b", "c") },
                )

            assertEquals(listOf("a", "b", "c"), result)
        }

    /**
     * Test returning nullable type with null value.
     */
    @Test
    fun testReturnTypeNullable() =
        runTest {
            val result: String? =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = { null },
                )

            assertEquals(null, result)
        }

    /**
     * Test returning Unit type.
     */
    @Test
    fun testReturnTypeUnit() =
        runTest {
            var sideEffect = false

            val result: Unit =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = {
                        sideEffect = true
                    },
                )

            assertEquals(Unit, result)
            assertTrue(sideEffect)
        }

    /**
     * Test returning data class.
     */
    @Test
    fun testReturnTypeDataClass() =
        runTest {
            data class TestData(val id: Int, val name: String)

            val result: TestData =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = { TestData(1, "test") },
                )

            assertEquals(TestData(1, "test"), result)
        }

    // ========== Tests for Sequential Calls ==========

    /**
     * Test sequential calls where first fails and second succeeds.
     */
    @Test
    fun testSequentialCalls() =
        runTest {
            val globalCallCount = AtomicInteger(0)

            // First call - fails on first attempt, succeeds after retry
            val result1 =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = {
                        val count = globalCallCount.incrementAndGet()
                        if (count == 1) {
                            throw createProtocolException()
                        }
                        "result1"
                    },
                )

            // Second call - immediate success
            val result2 =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = {
                        globalCallCount.incrementAndGet()
                        "result2"
                    },
                )

            assertEquals("result1", result1)
            assertEquals("result2", result2)
            assertEquals(3, globalCallCount.get()) // 2 for first call, 1 for second
        }

    /**
     * Test sequential calls with different open modes.
     */
    @Test
    fun testSequentialCallsDifferentModes() =
        runTest {
            val modesUsed = mutableListOf<OpenMode>()

            for (mode in listOf(OpenMode.DROPBOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE, OpenMode.BOX)) {
                retryOnUnauthorized(
                    openMode = mode,
                    maxRetries = 1,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                modesUsed.add(openMode)
                                return true
                            }
                        },
                    refreshAction = { },
                    action = {
                        if (modesUsed.lastOrNull() != mode) {
                            throw createProtocolException()
                        }
                        "success-$mode"
                    },
                )
            }

            assertEquals(listOf(OpenMode.DROPBOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE, OpenMode.BOX), modesUsed)
        }

    // ========== Tests for Mixed Exception Types ==========

    /**
     * Test mixed exception sequence: network error then protocol error.
     */
    @Test
    fun testMixedExceptionSequence() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val refreshCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 4,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { refreshCallCount.incrementAndGet() },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        when (count) {
                            1 -> throw CloudPluginException(RuntimeException("Network error")) // Network error
                            2 -> throw createProtocolException() // Protocol error - triggers refresh
                            3 -> throw CloudPluginException(java.io.IOException("Timeout")) // Network error
                            4 -> throw createProtocolException() // Protocol error at maxRetries - triggers auth
                            else -> "success"
                        }
                    },
                )

            assertEquals("success", result)
            assertEquals(5, actionCallCount.get())
            // Refresh called for attempt 2 (protocol exception before max retries)
            assertEquals(1, refreshCallCount.get())
        }

    // ========== Tests for Action Success After Failures ==========

    /**
     * Test that action succeeds immediately after token refresh (not at maxRetries).
     */
    @Test
    fun testSuccessAfterRefresh() =
        runTest {
            val actionCallCount = AtomicInteger(0)
            val refreshCallCount = AtomicInteger(0)
            val authCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 5,
                    trigger =
                        object : AuthTrigger {
                            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                                authCallCount.incrementAndGet()
                                return true
                            }
                        },
                    refreshAction = { refreshCallCount.incrementAndGet() },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        // Fail on first attempt, succeed on second (after refresh)
                        if (count == 1) {
                            throw createProtocolException()
                        }
                        "success after refresh"
                    },
                )

            assertEquals("success after refresh", result)
            assertEquals(2, actionCallCount.get())
            assertEquals(1, refreshCallCount.get())
            assertEquals(0, authCallCount.get()) // Auth never called since we succeeded before maxRetries
        }

    // ========== Tests for Concurrent Failures ==========

    /**
     * Test concurrent calls all experiencing failures then success.
     */
    @Test
    fun testConcurrentCallsWithFailures() =
        runTest {
            val totalCalls = 3
            val successCount = AtomicInteger(0)
            val callAttempts = (1..totalCalls).map { AtomicInteger(0) }

            val results =
                (0 until totalCalls).map { index ->
                    async(Dispatchers.IO) {
                        retryOnUnauthorized(
                            openMode = testOpenMode,
                            maxRetries = 2,
                            trigger = createMockAuthTrigger(true),
                            refreshAction = { },
                            action = {
                                val attempt = callAttempts[index].incrementAndGet()
                                if (attempt == 1) {
                                    throw createProtocolException()
                                }
                                successCount.incrementAndGet()
                                "result-$index"
                            },
                        )
                    }
                }.awaitAll()

            assertEquals(totalCalls, results.size)
            assertEquals(totalCalls, successCount.get())
            callAttempts.forEach { assertEquals(2, it.get()) }
        }

    // ========== Tests for runBlocking Scenarios ==========

    /**
     * Test nested runBlocking calls (simulating complex Java interop).
     */
    @Test
    fun testNestedRunBlocking() {
        val outerResult =
            runBlocking {
                val innerResult =
                    withContext(Dispatchers.IO) {
                        retryOnUnauthorized(
                            openMode = testOpenMode,
                            maxRetries = 2,
                            trigger = createMockAuthTrigger(true),
                            refreshAction = { },
                            action = { "inner" },
                        )
                    }
                "outer-$innerResult"
            }

        assertEquals("outer-inner", outerResult)
    }

    /**
     * Test from different dispatcher contexts.
     */
    @Test
    fun testDifferentDispatcherContexts() =
        runTest {
            // Default dispatcher
            val result1 =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = { "default" },
                )

            // IO dispatcher
            val result2 =
                withContext(Dispatchers.IO) {
                    retryOnUnauthorized(
                        openMode = testOpenMode,
                        maxRetries = 2,
                        trigger = createMockAuthTrigger(true),
                        refreshAction = { },
                        action = { "io" },
                    )
                }

            // Limited parallelism
            val result3 =
                withContext(Dispatchers.IO.limitedParallelism(1)) {
                    retryOnUnauthorized(
                        openMode = testOpenMode,
                        maxRetries = 2,
                        trigger = createMockAuthTrigger(true),
                        refreshAction = { },
                        action = { "limited" },
                    )
                }

            assertEquals("default", result1)
            assertEquals("io", result2)
            assertEquals("limited", result3)
        }

    // ========== Tests for Edge Cases ==========

    /**
     * Test with very high maxRetries value.
     */
    @Test
    fun testHighMaxRetries() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 100,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = {
                        val count = actionCallCount.incrementAndGet()
                        if (count < 50) {
                            throw createProtocolException()
                        }
                        "success at 50"
                    },
                )

            assertEquals("success at 50", result)
            assertEquals(50, actionCallCount.get())
        }

    /**
     * Test action that returns after some computation.
     */
    @Test
    fun testActionWithComputation() =
        runTest {
            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = {
                        // Simulate some computation
                        var sum = 0
                        for (i in 1..100) {
                            sum += i
                        }
                        sum
                    },
                )

            assertEquals(5050, result) // Sum of 1 to 100
        }

    /**
     * Test action that suspends.
     */
    @Test
    fun testActionWithSuspension() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            val result =
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 2,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { },
                    action = {
                        actionCallCount.incrementAndGet()
                        kotlinx.coroutines.delay(10) // Suspend
                        "suspended result"
                    },
                )

            assertEquals("suspended result", result)
            assertEquals(1, actionCallCount.get())
        }

    /**
     * Test rapid sequential retries with exponential backoff.
     * Note: runTest uses virtual time, so we just verify the action count.
     */
    @Test
    fun testNetworkErrorRetriesWithBackoff() =
        runTest {
            val actionCallCount = AtomicInteger(0)

            try {
                retryOnUnauthorized(
                    openMode = testOpenMode,
                    maxRetries = 3,
                    trigger = createMockAuthTrigger(true),
                    refreshAction = { fail("Should not refresh for network error") },
                    action = {
                        actionCallCount.incrementAndGet()
                        throw CloudPluginException(RuntimeException("Quick failure"))
                    },
                )
                fail("Expected exception")
            } catch (e: CloudPluginException) {
                // Expected - verify the exception message
                assertEquals("Quick failure", e.cause?.message)
            }

            assertEquals(3, actionCallCount.get())
        }

    // Helper methods

    // ...existing code...

    private fun createMockAuthTrigger(shouldSucceed: Boolean): AuthTrigger {
        return object : AuthTrigger {
            override fun triggerAuthBlocking(openMode: OpenMode): Boolean {
                return shouldSucceed
            }
        }
    }

    private fun createProtocolException(): CloudPluginException {
        val protocolException = ProtocolException("401 Unauthorized")
        return CloudPluginException(protocolException)
    }
}
