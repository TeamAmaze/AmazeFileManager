/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.shadows.ShadowMultiDex
import io.mockk.Called
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [KITKAT, P, Build.VERSION_CODES.R])
@Suppress("StringLiteralDuplication")
class FtpReceiverTest {
    private lateinit var receiver: FtpReceiver

    /**
     * Pre-test setup.
     */
    @Before
    fun setUp() {
        mockkObject(FtpService)
        receiver = FtpReceiver()
    }

    /**
     * Post test teardown.
     */
    @After
    fun tearDown() {
        unmockkObject(FtpService)
    }

    /**
     * Test when an invalid Intent is passed into the [FtpReceiver].
     */
    @Test
    fun testWhenNoActionSpecified() {
        every { FtpService.isRunning() } returns false
        assertFalse(FtpService.isRunning())
        receiver.onReceive(AppConfig.getInstance(), Intent())
        assertFalse(FtpService.isRunning())
    }

    /**
     * Test [Context.startService()] called for pre-Oreo Androids.
     */
    @Test
    @Config(minSdk = KITKAT, maxSdk = N)
    fun testStartServiceCalled() {
        val ctx = AppConfig.getInstance()
        val spy = spyk(ctx)
        val capturedIntent = slot<Intent>()
        every { spy.startService(capture(capturedIntent)) } answers { callOriginal() }
        val intent = Intent(FtpService.ACTION_START_FTPSERVER)
        receiver.onReceive(spy, intent)

        verify {
            spy.startService(capturedIntent.captured)
        }
    }

    /**
     * Test [Context.startForegroundService()] called for post-Nougat Androids.
     */
    @Test
    @Config(minSdk = O)
    fun testStartForegroundServiceCalled() {
        val ctx = AppConfig.getInstance()
        val spy = spyk(ctx)
        val capturedIntent = slot<Intent>()
        every { spy.startService(capture(capturedIntent)) } answers { callOriginal() }
        every { spy.startForegroundService(capture(capturedIntent)) } answers { callOriginal() }
        val intent = Intent(FtpService.ACTION_START_FTPSERVER)
        receiver.onReceive(spy, intent)

        verify {
            spy.startService(capturedIntent.captured)?.wasNot(Called)
            spy.startForegroundService(capturedIntent.captured)
        }
    }
}
