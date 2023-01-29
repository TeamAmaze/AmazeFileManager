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

package com.amaze.filemanager.utils

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.utils.X509CertificateUtil.FINGERPRINT
import com.amaze.filemanager.utils.X509CertificateUtil.ISSUER
import com.amaze.filemanager.utils.X509CertificateUtil.SERIAL
import com.amaze.filemanager.utils.X509CertificateUtil.SUBJECT
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.openssl.PEMParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.StringReader
import javax.security.cert.X509Certificate

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class X509CertificateUtilTest {

    private lateinit var cert: X509Certificate

    /**
     * Read and parse PEM as setup.
     */
    @Before
    fun setUp() {
        val parser = PEMParser(
            StringReader(
                String(
                    javaClass.getResourceAsStream("/test.pem").readBytes()
                )
            )
        )
        val a = parser.readObject()
        cert = X509Certificate.getInstance((a as X509CertificateHolder).encoded)
    }

    /**
     * Test [X509CertificateUtil.parse]
     */
    @Test
    fun testParseCert() {
        val verify = X509CertificateUtil.parse(cert)

        assertTrue(verify.containsKey(SUBJECT))
        assertEquals(
            "C=in,O=Team Amaze,CN=test.ftpsd.local",
            verify[SUBJECT]
        )
        assertTrue(verify.containsKey(ISSUER))
        assertEquals(
            "C=in,O=Team Amaze,CN=test.ftpsd.local",
            verify[ISSUER]
        )
        assertTrue(verify.containsKey(SERIAL))
        assertEquals(
            "11:f5:7b:bf:1e:4f:da:f6:b9:e8:0c:e3:49:67:5e:f1:5f:b7:0a:1f",
            verify[SERIAL]
        )
        assertTrue(verify.containsKey(FINGERPRINT))
        assertEquals(
            "a9:ab:de:6f:67:3a:f8:db:41:e0:30:81:f9:b7:36:cb:7a:2b:42:fc:cd:a9:af:a2:bc:" +
                "64:55:95:f2:c7:9a:74",
            verify[FINGERPRINT]
        )
    }
}
