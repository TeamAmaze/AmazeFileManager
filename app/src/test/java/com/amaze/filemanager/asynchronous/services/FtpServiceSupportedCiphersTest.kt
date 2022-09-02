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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.services.ftp.FtpService
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Test [FtpService.enabledCipherSuites].
 *
 * This test is deliberately set to run on all available SDKs to ensure no one is missing out.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class])
class FtpServiceSupportedCiphersTest {

    /**
     * Check for enabled ciphers, to ensure no unsupported ciphers on the list.
     *
     * @see FtpService.enabledCipherSuites
     * @see javax.net.ssl.SSLEngine
     */
    @Test
    fun testSupportedCiphers() {
        val verify = FtpService.enabledCipherSuites
        assertNotNull(verify)
        assertTrue(verify.isNotEmpty())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertTrue(verify.contains("TLS_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_AES_256_GCM_SHA384"))
            assertTrue(verify.contains("TLS_CHACHA20_POLY1305_SHA256"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256"))
            assertTrue(verify.contains("TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"))
            assertTrue(verify.contains("TLS_RSA_WITH_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_RSA_WITH_AES_256_GCM_SHA384"))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(verify.contains("TLS_RSA_WITH_AES_128_CBC_SHA"))
            assertTrue(verify.contains("TLS_RSA_WITH_AES_256_CBC_SHA"))
        }
    }
}
