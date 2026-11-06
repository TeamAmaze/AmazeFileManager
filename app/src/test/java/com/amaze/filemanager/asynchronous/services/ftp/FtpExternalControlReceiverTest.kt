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

package com.amaze.filemanager.asynchronous.services.ftp

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.shadows.ShadowMultiDex
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [Build.VERSION_CODES.P])
@Suppress("StringLiteralDuplication")
class FtpExternalControlReceiverTest {
    private lateinit var receiver: FtpExternalControlReceiver

    /**
     * Pre-test setup.
     */
    @Before
    fun setUp() {
        mockkObject(FtpService)
        receiver = FtpExternalControlReceiver()
    }

    /**
     * Post test teardown.
     */
    @After
    fun tearDown() {
        unmockkObject(FtpService)
    }

    /**
     * Start action starts the foreground service when not running.
     */
    @Test
    fun testStartActionStartsService() {
        every { FtpService.isRunning() } returns false
        val spy = spyk(AppConfig.getInstance())
        every { spy.startForegroundService(any()) } returns null
        receiver.onReceive(spy, Intent(FtpService.ACTION_EXTERNAL_START_FTP))
        verify(exactly = 1) { spy.startForegroundService(any()) }
    }

    /**
     * Start action is ignored when the server is already running.
     */
    @Test
    fun testStartActionIgnoredWhenAlreadyRunning() {
        every { FtpService.isRunning() } returns true
        val spy = spyk(AppConfig.getInstance())
        receiver.onReceive(spy, Intent(FtpService.ACTION_EXTERNAL_START_FTP))
        verify(exactly = 0) { spy.startForegroundService(any()) }
        verify(exactly = 0) { spy.startService(any()) }
    }

    /**
     * Stop action stops [FtpService].
     */
    @Test
    fun testStopActionStopsService() {
        val spy = spyk(AppConfig.getInstance())
        val captured = slot<Intent>()
        every { spy.stopService(capture(captured)) } returns true
        receiver.onReceive(spy, Intent(FtpService.ACTION_EXTERNAL_STOP_FTP))
        verify(exactly = 1) { spy.stopService(any()) }
        assertEquals(FtpService::class.java.name, captured.captured.component?.className)
    }

    /**
     * Null and unknown actions do nothing.
     */
    @Test
    fun testNullOrUnknownActionIgnored() {
        every { FtpService.isRunning() } returns false
        val spy = spyk(AppConfig.getInstance())
        receiver.onReceive(spy, Intent())
        receiver.onReceive(spy, Intent("foo"))
        verify(exactly = 0) { spy.startForegroundService(any()) }
        verify(exactly = 0) { spy.startService(any()) }
        verify(exactly = 0) { spy.stopService(any()) }
    }

    /**
     * Extras sent by external callers must never reach [FtpService].
     */
    @Test
    fun testExtrasAreNotForwarded() {
        every { FtpService.isRunning() } returns false
        val spy = spyk(AppConfig.getInstance())
        val captured = slot<Intent>()
        every { spy.startForegroundService(capture(captured)) } returns null
        val intent =
            Intent(FtpService.ACTION_EXTERNAL_START_FTP)
                .putExtra(FtpService.TAG_STARTED_BY_TILE, true)
                .putExtra("ftp_path", "/")
                .putExtra("ftp_password", "x")
        receiver.onReceive(spy, intent)
        assertNull(captured.captured.extras)
    }

    /**
     * Legacy actions are not handled by this receiver.
     */
    @Test
    fun testOldActionsIgnored() {
        every { FtpService.isRunning() } returns false
        val spy = spyk(AppConfig.getInstance())
        receiver.onReceive(spy, Intent(FtpService.ACTION_START_FTPSERVER))
        receiver.onReceive(spy, Intent(FtpService.ACTION_STOP_FTPSERVER))
        verify(exactly = 0) { spy.startForegroundService(any()) }
        verify(exactly = 0) { spy.stopService(any()) }
    }

    /**
     * Verify permission declarations and receiver protection in the manifest.
     */
    @Test
    fun testManifestDeclarations() {
        val ctx = AppConfig.getInstance()
        val pm = ctx.packageManager
        val signature = pm.getPermissionInfo("${ctx.packageName}.permission.CONTROL_FTP_SERVER", 0)
        val simple =
            pm.getPermissionInfo("${ctx.packageName}.permission.CONTROL_FTP_SERVER_SIMPLE", 0)
        assertEquals(
            PermissionInfo.PROTECTION_SIGNATURE,
            signature.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE,
        )
        assertEquals(
            PermissionInfo.PROTECTION_DANGEROUS,
            simple.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE,
        )

        val receivers =
            pm.getPackageInfo(ctx.packageName, PackageManager.GET_RECEIVERS).receivers.orEmpty()
        val external = receivers.firstOrNull { it.name == FtpExternalControlReceiver::class.java.name }
        val legacy = receivers.firstOrNull { it.name == FtpReceiver::class.java.name }
        assertNotNull(external)
        assertNotNull(legacy)
        assertTrue(external!!.exported)
        assertEquals("${ctx.packageName}.permission.CONTROL_FTP_SERVER_SIMPLE", external.permission)
        assertEquals("${ctx.packageName}.permission.CONTROL_FTP_SERVER", legacy!!.permission)
    }
}
