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

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.GrantPermissionRule
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

/**
 * Test for [CryptUtil] against real devices.
 *
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@Suppress("StringLiteralDuplication")
class CryptUtilEspressoTest {

    @Rule @JvmField
    val storagePermissionRule: GrantPermissionRule = GrantPermissionRule
        .grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * Sanity test of CryptUtil legacy method, to ensure refactoring won't break
     * on physical devices
     */
    @Test
    fun testEncryptDecryptLegacyMethod() {
        val source = Random(System.currentTimeMillis()).nextBytes(117)
        val sourceFile = File(Environment.getExternalStorageDirectory(), "test.bin")
        ByteArrayInputStream(source).copyTo(FileOutputStream(sourceFile))
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(sourceFile.absolutePath),
            ProgressHandler(),
            ArrayList(),
            "test.bin${CryptUtil.CRYPT_EXTENSION}",
            false,
            null
        )
        val targetFile = File(
            Environment.getExternalStorageDirectory(),
            "test.bin${CryptUtil.CRYPT_EXTENSION}"
        )
        assertTrue(targetFile.exists())
        if (SDK_INT < JELLY_BEAN_MR2) {
            // Quirks for SDK < 18. File is not encrypted at all.
            assertTrue(
                "Source and target file size should be the same = ${source.size}",
                source.size.toLong() == targetFile.length()
            )
        } else {
            assertTrue(
                "Source size = ${source.size} target file size = ${targetFile.length()}",
                targetFile.length() > source.size
            )
        }
        sourceFile.delete()
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(targetFile.absolutePath).also {
                it.size = targetFile.length()
            },
            Environment.getExternalStorageDirectory().absolutePath,
            ProgressHandler(),
            ArrayList(),
            null
        )
        File(Environment.getExternalStorageDirectory(), "test.bin").run {
            assertTrue(this.exists())
            assertArrayEquals(source, this.readBytes())
        }
    }

    /**
     * Test encrypt and decrypt routine with AESCrypt format.
     */
    @Test
    fun testEncryptDecryptAescrypt() {
        val source = Random(System.currentTimeMillis()).nextBytes(117)
        val sourceFile = File(Environment.getExternalStorageDirectory(), "test.bin")
        ByteArrayInputStream(source).copyTo(FileOutputStream(sourceFile))
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(sourceFile.absolutePath),
            ProgressHandler(),
            ArrayList(),
            "test.bin${CryptUtil.AESCRYPT_EXTENSION}",
            true,
            "12345678"
        )
        val targetFile = File(
            Environment.getExternalStorageDirectory(),
            "test.bin${CryptUtil.AESCRYPT_EXTENSION}"
        )
        assertTrue(targetFile.exists())
        assertTrue(
            "Source size = ${source.size} target file size = ${targetFile.length()}",
            targetFile.length() > source.size
        )
        sourceFile.delete()
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(targetFile.absolutePath).also {
                it.size = targetFile.length()
            },
            Environment.getExternalStorageDirectory().absolutePath,
            ProgressHandler(),
            ArrayList(),
            "12345678"
        )
        File(Environment.getExternalStorageDirectory(), "test.bin").run {
            assertTrue(this.exists())
            assertArrayEquals(source, this.readBytes())
        }
    }
}
