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

package com.amaze.filemanager.asynchronous.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.services.EncryptService.NOTIFICATION_SERVICE
import com.amaze.filemanager.asynchronous.services.EncryptService.START_NOT_STICKY
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_AESCRYPT
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_ENCRYPT_TARGET
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_PASSWORD
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_SOURCE
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.ui.notifications.NotificationConstants
import com.amaze.filemanager.utils.AESCrypt
import com.amaze.filemanager.utils.CryptUtilTest.Companion.initMockSecretKeygen
import com.amaze.filemanager.utils.ProgressHandler
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [KITKAT, P, Build.VERSION_CODES.R])
class EncryptServiceTest {

    private lateinit var service: EncryptService
    private lateinit var notificationManager: ShadowNotificationManager
    private lateinit var source: ByteArray
    private lateinit var sourceFile: File
    private lateinit var targetFilename: String
    private lateinit var targetFile: File

    /**
     * Test setup
     */
    @Before
    fun setUp() {
        service = Robolectric.setupService(EncryptService::class.java)
        notificationManager = shadowOf(
            ApplicationProvider.getApplicationContext<Context?>()
                .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        )
        source = Random(System.currentTimeMillis()).nextBytes(73)
        sourceFile = File(Environment.getExternalStorageDirectory(), "test.bin")
        ByteArrayInputStream(source).copyTo(FileOutputStream(sourceFile))
        initMockSecretKeygen()
    }

    /**
     * Post test cleanup
     */
    @After
    fun tearDown() {
        service.stopSelf()
        service.onDestroy()
        if (sourceFile.exists()) sourceFile.delete()
    }

    /**
     * Test [EncryptService] to encrypt files using legacy method.
     *
     * No password guarding here - just the logic of [EncryptService] and [CryptUtil] itself.
     */
    @Test
    fun testLegacyEncryptWorkflow() {
        targetFilename = "test.bin${CryptUtil.CRYPT_EXTENSION}"
        targetFile = File(Environment.getExternalStorageDirectory(), targetFilename)
        Intent(ApplicationProvider.getApplicationContext(), EncryptService::class.java)
            .putExtra(TAG_SOURCE, HybridFileParcelable(sourceFile.absolutePath))
            .putExtra(TAG_ENCRYPT_TARGET, targetFilename)
            .putExtra(TAG_AESCRYPT, false).run {
                assertEquals(START_NOT_STICKY, service.onStartCommand(this, 0, 0))
            }
        if (SDK_INT < M) {
            assertTrue(notificationManager.allNotifications.isNotEmpty())
            await().atMost(100, TimeUnit.SECONDS).until {
                targetFile.length() > 0 && notificationManager.allNotifications.isEmpty()
            }
        } else {
            assertTrue(notificationManager.activeNotifications.isNotEmpty())
            notificationManager.activeNotifications.first().let {
                assertEquals(NotificationConstants.ENCRYPT_ID, it.id)
                assertEquals(NotificationConstants.CHANNEL_NORMAL_ID, it.notification.channelId)
            }
            await().atMost(10, TimeUnit.SECONDS).until {
                targetFile.length() > 0 && notificationManager.activeNotifications.isEmpty()
            }
        }
        sourceFile.delete()
        CryptUtil(
            ApplicationProvider.getApplicationContext(),
            HybridFileParcelable(targetFile.absolutePath),
            Environment.getExternalStorageDirectory().absolutePath,
            ProgressHandler(),
            ArrayList<HybridFile>(),
            null
        )
        val verifyFile = File(Environment.getExternalStorageDirectory(), "test.bin")
        await().atMost(10, TimeUnit.SECONDS).until {
            verifyFile.exists() && verifyFile.length() > 0
        }
        assertArrayEquals(source, verifyFile.readBytes())
        targetFile.delete()
    }

    /**
     * Test [EncryptService] on encrypting files using AESCrypt format.
     *
     * @see AESCrypt.encrypt
     */
    @Test
    fun testAescryptWorkflow() {
        if (SDK_INT >= M) {
            targetFilename = "test.bin${CryptUtil.AESCRYPT_EXTENSION}"
            targetFile = File(Environment.getExternalStorageDirectory(), targetFilename)
            Intent(ApplicationProvider.getApplicationContext(), EncryptService::class.java)
                .putExtra(TAG_SOURCE, HybridFileParcelable(sourceFile.absolutePath))
                .putExtra(TAG_ENCRYPT_TARGET, targetFilename)
                .putExtra(TAG_AESCRYPT, true)
                .putExtra(TAG_PASSWORD, "passW0rD").run {
                    assertEquals(START_NOT_STICKY, service.onStartCommand(this, 0, 0))
                }
            assertTrue(notificationManager.activeNotifications.isNotEmpty())
            notificationManager.activeNotifications.first().let {
                assertEquals(NotificationConstants.ENCRYPT_ID, it.id)
                assertEquals(NotificationConstants.CHANNEL_NORMAL_ID, it.notification.channelId)
            }
            await().atMost(10, TimeUnit.SECONDS).until {
                targetFile.length() > 0 && notificationManager.activeNotifications.isEmpty()
            }
            assertTrue(targetFile.length() > sourceFile.length())
            assertFalse(
                source.contentEquals(
                    File(
                        Environment.getExternalStorageDirectory(),
                        targetFilename
                    ).readBytes()
                )
            )
            val verify = ByteArrayOutputStream()
            AESCrypt("passW0rD").decrypt(targetFile.length(), FileInputStream(targetFile), verify)
            assertEquals(source.size, verify.size())
            assertArrayEquals(source, verify.toByteArray())
            targetFile.delete()
        } else {
            Log.w(javaClass.simpleName, "Test skipped for $SDK_INT")
        }
    }
}
