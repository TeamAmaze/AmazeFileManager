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
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.utils.security.SecretKeygen
import io.mockk.every
import io.mockk.mockkObject
import org.awaitility.Awaitility.await
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class CryptUtilTest {

    /**
     * Test AESCrypt function under CryptUtil.
     *
     * @see CryptUtil
     * @see AESCrypt
     */
    @Test
    fun testAescrypt() {
        performTest(
            useAescrypt = true,
            password = "abcdefgh1234567890",
            targetExtension = "aes"
        )
        performFolderTest(
            useAescrypt = true,
            password = "abcdefgh1234567890",
            targetExtension = "aes"
        )
    }

    /**
     * Test legacy encryption/decryption under CryptUtil.
     *
     * @see CryptUtil
     */
    @Test
    fun testLegacyEncrypt() {
        initMockSecretKeygen()
        performTest(useAescrypt = false, targetExtension = "aze")
        performFolderTest(useAescrypt = false, targetExtension = "aze")
    }

    private fun performTest(
        useAescrypt: Boolean,
        password: String? = null,
        targetExtension: String
    ) {
        val source = randomizer.nextBytes(117)
        val sourceFile = File(Environment.getExternalStorageDirectory(), "test.bin")
        ByteArrayInputStream(source).copyTo(FileOutputStream(sourceFile))
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(sourceFile.absolutePath),
            ProgressHandler(),
            ArrayList(),
            "test.bin.$targetExtension",
            useAescrypt,
            password
        )
        val targetFile = File(
            Environment.getExternalStorageDirectory(),
            "test.bin.$targetExtension"
        )
        assertTrue(targetFile.length() > source.size)
        sourceFile.delete()
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(targetFile.absolutePath).also {
                it.setSize(targetFile.length())
            },
            Environment.getExternalStorageDirectory().absolutePath,
            ProgressHandler(),
            ArrayList(),
            password
        )
        File(Environment.getExternalStorageDirectory(), "test.bin").run {
            assertTrue(this.exists())
            assertArrayEquals(source, this.readBytes())
        }
    }

    private fun performFolderTest(
        useAescrypt: Boolean,
        password: String? = null,
        targetExtension: String
    ) {
        val filesSize = randomizer.nextInt(10, 20)
        val sourceData = Array(filesSize) {
            randomizer.nextBytes(117)
        }
        val sourceFolder = File(Environment.getExternalStorageDirectory(), "test")
        sourceFolder.mkdirs()
        repeat(filesSize) {
            val file = File(sourceFolder, "test$it.bin")
            ByteArrayInputStream(sourceData[it]).copyTo(FileOutputStream(file))
        }
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(sourceFolder.absolutePath).also {
                it.isDirectory = true
            },
            ProgressHandler(),
            ArrayList(),
            "test.$targetExtension",
            useAescrypt,
            password
        )
        val targetFolder = File(
            Environment.getExternalStorageDirectory(),
            "test.$targetExtension"
        )
        assertTrue(targetFolder.isDirectory)
        assertEquals(sourceData.size, targetFolder.listFiles()?.size)
        sourceFolder.deleteRecursively()
        CryptUtil(
            AppConfig.getInstance(),
            HybridFileParcelable(targetFolder.absolutePath).also {
                it.isDirectory = true
            },
            Environment.getExternalStorageDirectory().absolutePath,
            ProgressHandler(),
            ArrayList(),
            password
        )
        File(Environment.getExternalStorageDirectory(), "test").run {
            assertTrue(this.exists())
            assertTrue(this.isDirectory)
            await().atMost(10, TimeUnit.SECONDS).until {
                sourceData.size == this.listFiles()?.size
            }
            this.listFiles()?.forEach { file: File? ->
                file?.run {
                    val index = file.name.substring(
                        "test".length,
                        file.name.indexOf('.')
                    ).toInt()
                    assertArrayEquals(
                        "Comparison broken at ${file.absolutePath}",
                        sourceData[index],
                        file.readBytes()
                    )
                } ?: fail("File not found")
            } ?: fail("No files found")
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
