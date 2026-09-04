/*
 * Copyright (C) 2014-2025 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * Instrumentation test for [FtpServerSslKeyStoreProvider].
 *
 * Robolectric does not support AndroidKeyStore well enough for this code path, so this test uses
 * a real device/emulator-backed keystore.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class FtpServerSslKeyStoreProviderEspressoTest {
    @Before
    fun setUp() {
        assumeTrue(SDK_INT >= M)
        clearAliasFromAndroidKeyStore()
        FtpServerSslKeyStoreProvider.reset()
    }

    @After
    fun tearDown() {
        runCatching {
            clearAliasFromAndroidKeyStore()
        }
        FtpServerSslKeyStoreProvider.reset()
    }

    @Test
    fun testGeneratesValidCertificateInAndroidKeyStore() {
        val keyStore = FtpServerSslKeyStoreProvider.getKeyStore()

        assertEquals("AndroidKeyStore", keyStore.type)
        assertTrue(keyStore.containsAlias(FtpServerSslKeyStoreProvider.FTPS_CERT_ALIAS))

        val entry =
            keyStore.getEntry(
                FtpServerSslKeyStoreProvider.FTPS_CERT_ALIAS,
                null,
            ) as KeyStore.PrivateKeyEntry
        val certificate = entry.certificate as X509Certificate

        assertNotNull(certificate)
        certificate.checkValidity()
        assertEquals("RSA", entry.privateKey.algorithm)
        assertTrue(certificate.subjectX500Principal.name.contains("CN=ftpserver"))
        assertTrue(certificate.subjectX500Principal.name.contains("OU=Amaze File Manager"))
        assertTrue(certificate.subjectX500Principal.name.contains("O=Team Amaze"))
    }

    @Test
    fun testClearRemovesGeneratedCertificateFromAndroidKeyStore() {
        FtpServerSslKeyStoreProvider.getKeyStore()
        assertTrue(realAndroidKeyStore().containsAlias(FtpServerSslKeyStoreProvider.FTPS_CERT_ALIAS))

        FtpServerSslKeyStoreProvider.clear()

        assertFalse(realAndroidKeyStore().containsAlias(FtpServerSslKeyStoreProvider.FTPS_CERT_ALIAS))
    }

    private fun clearAliasFromAndroidKeyStore() {
        val keyStore = realAndroidKeyStore()
        if (keyStore.containsAlias(FtpServerSslKeyStoreProvider.FTPS_CERT_ALIAS)) {
            keyStore.deleteEntry(FtpServerSslKeyStoreProvider.FTPS_CERT_ALIAS)
        }
    }

    private fun realAndroidKeyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
}
