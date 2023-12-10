/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.fileoperations.filesystem.DOESNT_EXIST
import com.amaze.filemanager.fileoperations.filesystem.WRITABLE_ON_REMOTE
import com.amaze.filemanager.shadows.ShadowSmbUtil
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.smb.SmbUtil.checkFolder
import com.amaze.filemanager.utils.smb.SmbUtil.createFrom
import com.amaze.filemanager.utils.smb.SmbUtil.getSmbDecryptedPath
import com.amaze.filemanager.utils.smb.SmbUtil.getSmbEncryptedPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for [SmbUtil].
 */
@Suppress("StringLiteralDuplication")
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [KITKAT, P, Build.VERSION_CODES.R],
    shadows = [ShadowPasswordUtil::class, ShadowSmbUtil::class]
)
class SmbUtilTest {

    /**
     * Test encrypt/decrypt SMB URIs.
     */
    @Test
    fun testEncryptDecryptSmb() {
        val path = "smb://root:toor@127.0.0.1"
        val encrypted = getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path)
        assertNotEquals(path, encrypted)
        assertTrue(encrypted.startsWith("smb://root:"))
        assertTrue(encrypted.endsWith("@127.0.0.1"))
        val decrypted = getSmbDecryptedPath(ApplicationProvider.getApplicationContext(), encrypted)
        assertEquals(path, decrypted)
    }

    /**
     * Test encrypt/decrypt FTP(S) URIs.
     */
    @Test
    fun testEncryptDecryptFtps() {
        val path = "ftps://root:toor@127.0.0.1"
        val encrypted = getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path)
        assertNotEquals(path, encrypted)
        assertTrue(encrypted.startsWith("ftps://root:"))
        assertTrue(encrypted.endsWith("@127.0.0.1"))
        val decrypted = getSmbDecryptedPath(ApplicationProvider.getApplicationContext(), encrypted)
        assertEquals(path, decrypted)
    }

    /**
     * Test encrypt/decrypt URIs without username and password. It should stay the same.
     */
    @Test
    fun testEncryptWithoutCredentials() {
        val path = "smb://127.0.0.1"
        assertEquals(
            path,
            getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path)
        )
    }

    /**
     * Test encrypt/decrypt URIs without password. It should stay the same too.
     */
    @Test
    fun testEncryptWithoutPassword() {
        val path = "smb://toor@127.0.0.1"
        assertEquals(
            path,
            getSmbEncryptedPath(ApplicationProvider.getApplicationContext(), path)
        )
    }

    /**
     * Tests [SmbUtil.checkFolder].
     */
    @Test
    fun testCheckFolder() {
        assertEquals(
            DOESNT_EXIST,
            checkFolder("smb://user:password@5.6.7.8/newfolder/DummyFolder")
        )
        assertEquals(
            DOESNT_EXIST,
            checkFolder("smb://user:password@5.6.7.8/newfolder/resume.doc")
        )
        assertEquals(
            WRITABLE_ON_REMOTE,
            checkFolder("smb://user:password@5.6.7.8/newfolder/Documents")
        )
        assertEquals(
            DOESNT_EXIST,
            checkFolder("smb://user:password@5.6.7.8/newfolder/wirebroken.log")
        )
        assertEquals(
            DOESNT_EXIST,
            checkFolder("smb://user:password@5.6.7.8/newfolder/failcheck")
        )
    }

    /**
     * Test [SmbUtil.createFrom] for different username/password/domain combinations.
     */
    @Test
    fun testCreateNtlmPasswordAuthenticator() {
        var auth = createFrom(null)
        assertEquals("", auth.userDomain)
        assertEquals("", auth.username)
        assertEquals("", auth.password)
        auth = createFrom("")
        assertEquals("", auth.userDomain)
        assertEquals("", auth.username)
        assertEquals("", auth.password)
        auth = createFrom("username:password")
        assertEquals("", auth.userDomain)
        assertEquals("username", auth.username)
        assertEquals("password", auth.password)
        auth = createFrom("WORKGROUP;username:password")
        assertEquals("WORKGROUP", auth.userDomain)
        assertEquals("username", auth.username)
        assertEquals("password", auth.password)
        auth = createFrom("WORKGROUP;username")
        assertEquals("WORKGROUP", auth.userDomain)
        assertEquals("username", auth.username)
        assertEquals("", auth.password)

        // #2313 major symptom
        auth = createFrom("username:pass%w0rd")
        assertEquals("", auth.userDomain)
        assertEquals("username", auth.username)
        assertEquals("pass%w0rd", auth.password)

        // Shall not happen - we should rarely have % in username/workgroup names, but anyway.
        auth = createFrom("WORKGROUP;user%1")
        assertEquals("WORKGROUP", auth.userDomain)
        assertEquals("user%1", auth.username)
        assertEquals("", auth.password)
        auth = createFrom("WORKGROUP%2;user%1:pass%word")
        assertEquals("WORKGROUP%2", auth.userDomain)
        assertEquals("user%1", auth.username)
        assertEquals("pass%word", auth.password)
    }
}
