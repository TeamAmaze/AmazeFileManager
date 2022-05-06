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

package com.amaze.filemanager.ui.dialogs

import android.Manifest.permission.USE_FINGERPRINT
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.services.DecryptService.TAG_DECRYPT_PATH
import com.amaze.filemanager.asynchronous.services.DecryptService.TAG_OPEN_MODE
import com.amaze.filemanager.asynchronous.services.DecryptService.TAG_SOURCE
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RandomPathGenerator
import com.amaze.filemanager.filesystem.files.CryptUtil.CRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.utils.security.SecretKeygen
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowFingerprintManager
import java.io.File
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Unit test for [DecryptFingerprintDialog].
 */
@Config(
    shadows = [ShadowMultiDex::class, ShadowTabHandler::class, ShadowFingerprintManager::class],
    sdk = [P]
)
class DecryptFingerprintDialogTest : AbstractEncryptDialogTests() {

    private lateinit var file: File

    /**
     * MainActivity setup.
     */
    @Before
    override fun setUp() {
        super.setUp()
        file = File(
            Environment.getExternalStorageDirectory(),
            RandomPathGenerator.generateRandomPath(
                randomizer,
                16
            ) + CRYPT_EXTENSION
        )
        initMockSecretKeygen()
        shadowOf(AppConfig.getInstance()).grantPermissions(USE_FINGERPRINT)
    }

    /**
     * Test fingerprint authentication success scenario.
     */
    @Test
    @RequiresApi(M)
    fun testDecryptFingerprintDialogSuccess() {
        performTest(
            testContent = {
                shadowOf(
                    AppConfig.getInstance().getSystemService(FingerprintManager::class.java)
                ).run {
                    setDefaultFingerprints(1)
                    setIsHardwareDetected(true)
                    authenticationSucceeds()
                }
            },
            callback = object : EncryptDecryptUtils.DecryptButtonCallbackInterface {
                override fun confirm(intent: Intent) = assertTrue(true)
                override fun failed() = fail("Should never called")
            }
        )
    }

    /**
     * Test fingerprint authentication failure scenario.
     */
    @Test
    @RequiresApi(M)
    fun testDecryptFingerprintDialogFailed() {
        performTest(
            testContent = {
                shadowOf(
                    AppConfig.getInstance().getSystemService(FingerprintManager::class.java)
                ).run {
                    setDefaultFingerprints(1)
                    setIsHardwareDetected(true)
                    authenticationFails()
                }
            },
            callback = object : EncryptDecryptUtils.DecryptButtonCallbackInterface {
                override fun confirm(intent: Intent) = fail("Should never called")
                override fun failed() = assertTrue(true)
            }
        )
    }

    private fun performTest(
        testContent: () -> Unit,
        callback: EncryptDecryptUtils.DecryptButtonCallbackInterface =
            object : EncryptDecryptUtils.DecryptButtonCallbackInterface {}
    ) {
        scenario.onActivity { activity ->
            DecryptFingerprintDialog.show(
                activity,
                activity,
                Intent().putExtra(TAG_SOURCE, HybridFileParcelable(file.absolutePath))
                    .putExtra(TAG_OPEN_MODE, OpenMode.FILE)
                    .putExtra(
                        TAG_DECRYPT_PATH,
                        Environment.getExternalStorageDirectory().absolutePath
                    ),
                activity.appTheme,
                callback
            )
            ShadowDialog.getLatestDialog()?.run {
                assertTrue(this is MaterialDialog)
                (this as MaterialDialog).let {
                    testContent.invoke()
                }
            }
        }
    }

    companion object {

        private val randomizer = Random(System.currentTimeMillis())
        private val key = SecretKeySpec(randomizer.nextBytes(16), "AES")

        /**
         * Mock [SecretKeygen] since Robolectric does not have AndroidKeyStore.
         */
        fun initMockSecretKeygen() {
            mockkObject(SecretKeygen)
            every { SecretKeygen.getSecretKey() } returns key
        }
    }
}
