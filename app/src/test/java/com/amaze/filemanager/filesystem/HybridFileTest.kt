/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.net.URLDecoder
import kotlin.random.Random

/* ktlint-disable max-line-length */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [KITKAT, P, Build.VERSION_CODES.R])
@Suppress("StringLiteralDuplication")
class HybridFileTest {

    /**
     * Test [HybridFile.getParent]
     */
    @Test
    fun testGetParentGeneric() {
        val r = Random(123)
        for (i in 0..50) {
            val path = RandomPathGenerator.generateRandomPath(r, 50)
            val file = HybridFile(OpenMode.UNKNOWN, path)
            file.getParent(ApplicationProvider.getApplicationContext())
        }

        for (i in 0..50) {
            val path = RandomPathGenerator.generateRandomPath(r, 50, 0)
            val file = HybridFile(OpenMode.UNKNOWN, path)
            file.getParent(ApplicationProvider.getApplicationContext())
        }
    }

    /**
     * Test [HybridFile.getParent] for SSH paths with trailing slash.
     */
    @Test
    fun testSshGetParentWithTrailingSlash() {
        val file = HybridFile(OpenMode.SFTP, "ssh://user@127.0.0.1/home/user/next/project/")
        assertEquals(
            "ssh://user@127.0.0.1/home/user/next/",
            file.getParent(ApplicationProvider.getApplicationContext())
        )
    }

    /**
     * Test [HybridFile.getParent] for SSH paths without trailing slash.
     */
    @Test
    fun testSshGetParentWithoutTrailingSlash() {
        val file = HybridFile(OpenMode.SFTP, "ssh://user@127.0.0.1/home/user/next/project")
        assertEquals(
            "ssh://user@127.0.0.1/home/user/next/",
            file.getParent(ApplicationProvider.getApplicationContext())
        )
    }

    /**
     * Test [HybridFile.getParent] for Android/data path.
     */
    @Test
    fun testDocumentFileAndroidDataGetParent1() {
        var file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            "content://com.android.externalstorage.documents/tree/primary:Android/data/com.amaze.filemanager.debug/cache"
        )
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary:Android/data/com.amaze.filemanager.debug/",
            file.getParent(AppConfig.getInstance())
        )
        file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            file.getParent(AppConfig.getInstance())
        )
        assertEquals(
            File(Environment.getExternalStorageDirectory(), "Android/data").absolutePath,
            file.getParent(AppConfig.getInstance())
        )
    }

    /**
     * Test [HybridFile.getParent] for Android/data path (again).
     */
    @Test
    fun testDocumentFileAndroidDataGetParent2() {
        var file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            "content://com.android.externalstorage.documents/tree/primary:Android/data/com.amaze.filemanager.debug/files/external"
        )
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary:Android/data/com.amaze.filemanager.debug/files/",
            file.getParent(AppConfig.getInstance())
        )
        file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            file.getParent(AppConfig.getInstance())
        )
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary:Android/data/com.amaze.filemanager.debug/",
            file.getParent(AppConfig.getInstance())
        )
        file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            file.getParent(AppConfig.getInstance())
        )
        assertEquals(
            File(Environment.getExternalStorageDirectory(), "Android/data").absolutePath,
            file.getParent(AppConfig.getInstance())
        )
    }

    /**
     * Test [HybridFile.getParent] for Android/obb path.
     */
    @Test
    fun testDocumentFileAndroidObbGetParent3() {
        var file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            "content://com.android.externalstorage.documents/tree/primary:Android/obb/com.amaze.filemanager.debug/cache"
        )
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary:Android/obb/com.amaze.filemanager.debug/",
            file.getParent(AppConfig.getInstance())
        )
        file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            file.getParent(AppConfig.getInstance())
        )
        assertEquals(
            File(Environment.getExternalStorageDirectory(), "Android/obb").absolutePath,
            file.getParent(AppConfig.getInstance())
        )
    }

    /**
     * Test [HybridFile.getParent] for Android/obb path (again).
     */
    @Test
    fun testDocumentFileAndroidObbGetParent2() {
        var file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            "content://com.android.externalstorage.documents/tree/primary:Android/obb/com.amaze.filemanager.debug/files/external"
        )
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary:Android/obb/com.amaze.filemanager.debug/files/",
            file.getParent(AppConfig.getInstance())
        )
        file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            file.getParent(AppConfig.getInstance())
        )
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary:Android/obb/com.amaze.filemanager.debug/",
            file.getParent(AppConfig.getInstance())
        )
        file = HybridFile(
            OpenMode.DOCUMENT_FILE,
            file.getParent(AppConfig.getInstance())
        )
        assertEquals(
            File(Environment.getExternalStorageDirectory(), "Android/obb").absolutePath,
            file.getParent(AppConfig.getInstance())
        )
    }

    /**
     * Test [HybridFile.getName]
     */
    @Test
    fun testGetName() {
        for (
            name: String in arrayOf(
                "newfolder",
                "new folder 2",
                "new%20folder%203",
                "あいうえお"
            )
        ) {
            val file = HybridFile(OpenMode.FTP, "ftp://user:password@127.0.0.1/$name")
            assertEquals(
                URLDecoder.decode(name, Charsets.UTF_8.name()),
                file.getName(AppConfig.getInstance())
            )
        }
    }

    /**
     * Test [HybridFile.getName] for files having space in name
     */
    @Test
    fun testGetName2() {
        val file = HybridFile(
            OpenMode.FTP,
            "ftp://user:password@127.0.0.1:22222/multiple/levels/down the pipe"
        )
        assertEquals("down the pipe", file.getName(AppConfig.getInstance()))
    }

    /**
     * Test [HybridFile.sanitizePathAsNecessary].
     */
    @Test
    fun testSanitizePathAsNecessary() {
        assertEquals(
            "ftp://user:password@127.0.0.1:22222/multiple/levels/down/the/pipe",
            HybridFile(
                OpenMode.FTP,
                "ftp://user:password@127.0.0.1:22222//multiple///levels////down////the/pipe"
            ).path
        )
        assertEquals(
            "ssh://user@127.0.0.1/multiple/levels/down/the/pipe",
            HybridFile(
                OpenMode.SFTP,
                "ssh://user@127.0.0.1//multiple///levels////down////the/pipe"
            ).path
        )
        assertEquals(
            "ssh://user@127.0.0.1/multiple/levels/down/the/pipe",
            HybridFile(
                OpenMode.SFTP,
                "ssh://user@127.0.0.1/multiple/levels/down/the/pipe"
            ).path
        )
        assertEquals(
            "smb://127.0.0.1/legacy?disableIpcSigningCheck=true",
            HybridFile(
                OpenMode.SMB,
                "smb://127.0.0.1/legacy?disableIpcSigningCheck=true"
            ).path
        )
        assertEquals(
            "smb://127.0.0.1/legacy/again/try/duplicate/folder?disableIpcSigningCheck=true",
            HybridFile(
                OpenMode.SMB,
                "smb://127.0.0.1/legacy//again/try/duplicate/////folder?disableIpcSigningCheck=true"
            ).path
        )
    }
}
/* ktlint-enable max-line-length */
