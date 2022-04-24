package com.amaze.filemanager.utils

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
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
        val targetFile = File(Environment.getExternalStorageDirectory(), "test.bin${CryptUtil.CRYPT_EXTENSION}")
        assertTrue(targetFile.exists())
        assertTrue("Source size = ${source.size} target file size = ${targetFile.length()}", targetFile.length() > source.size)
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
        val targetFile = File(Environment.getExternalStorageDirectory(), "test.bin${CryptUtil.AESCRYPT_EXTENSION}")
        assertTrue(targetFile.exists())
        assertTrue("Source size = ${source.size} target file size = ${targetFile.length()}", targetFile.length() > source.size)
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