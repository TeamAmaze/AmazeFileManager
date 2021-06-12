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

package com.amaze.filemanager.filesystem.compressed.extractcontents

import android.content.Context
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.file_operations.utils.UpdatePosition
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor.OnUpdate
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [JELLY_BEAN, KITKAT, P])
abstract class AbstractExtractorTest {

    private val EXPECTED_TIMESTAMP = ZonedDateTime.of(
        2018,
        5,
        29,
        10,
        38,
        0,
        0,
        ZoneId.of("UTC")
    ).toInstant().toEpochMilli()

    protected abstract fun extractorClass(): Class<out Extractor>
    protected abstract val archiveType: String

    /**
     * Test setup, copy archives to storage space
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        copyArchivesToStorage()
    }

    /**
     * Post test clean up
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        ArchivePasswordCache.getInstance().clear()
        val extractedArchiveRoot = File(Environment.getExternalStorageDirectory(), "test-archive")
        if (extractedArchiveRoot.exists()) {
            Files.walk(Paths.get(extractedArchiveRoot.absolutePath))
                .map { obj: Path -> obj.toFile() }
                .forEach { obj: File -> obj.delete() }
        }
    }

    /**
     * Test extractor ability to correct problematic archive entries for security
     */
    @Test
    @Suppress("StringLiteralDuplication")
    fun testFixEntryName() {
        val extractor = extractorClass()
            .getConstructor(
                Context::class.java,
                String::class.java,
                String::class.java,
                OnUpdate::class.java,
                UpdatePosition::class.java
            )
            .newInstance(
                ApplicationProvider.getApplicationContext(),
                archiveFile.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                object : OnUpdate {
                    override fun onStart(totalBytes: Long, firstEntryName: String) = Unit
                    override fun onUpdate(entryPath: String) = Unit
                    override fun isCancelled(): Boolean = false
                    override fun onFinish() = Unit
                },
                ServiceWatcherUtil.UPDATE_POSITION
            )
        assertEquals("test.txt", extractor.fixEntryName("test.txt"))
        assertEquals("test.txt", extractor.fixEntryName("/test.txt"))
        assertEquals("test.txt", extractor.fixEntryName("/////////test.txt"))
        assertEquals("test/", extractor.fixEntryName("/test/"))
        assertEquals("test/a/b/c/d/e/", extractor.fixEntryName("/test/a/b/c/d/e/"))
        assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("a/b/c/d/e/test.txt"))
        assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("/a/b/c/d/e/test.txt"))
        assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("///////a/b/c/d/e/test.txt"))

        // It is known redundant slashes inside path components are NOT tampered.
        assertEquals("a/b/c//d//e//test.txt", extractor.fixEntryName("a/b/c//d//e//test.txt"))
        assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("a/b/c/d/e/test.txt"))
        assertEquals("test.txt", extractor.fixEntryName("\\test.txt"))
        assertEquals("test.txt", extractor.fixEntryName("\\\\\\\\\\\\\\\\\\\\test.txt"))
        assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("\\a\\b\\c\\d\\e\\test.txt"))
        assertEquals("a/b/c/d/e/test.txt", extractor.fixEntryName("\\a\\b/c\\d\\e/test.txt"))
    }

    /**
     * Test extractor ability to extract files
     */
    @Test
    @Throws(Exception::class)
    open fun testExtractFiles() {
        doTestExtractFiles()
    }

    @Throws(Exception::class)
    protected fun doTestExtractFiles() {
        val latch = CountDownLatch(1)
        val extractor = extractorClass()
            .getConstructor(
                Context::class.java,
                String::class.java,
                String::class.java,
                OnUpdate::class.java,
                UpdatePosition::class.java
            )
            .newInstance(
                ApplicationProvider.getApplicationContext(),
                archiveFile.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                object : OnUpdate {
                    override fun onStart(totalBytes: Long, firstEntryName: String) = Unit
                    override fun onUpdate(entryPath: String) = Unit
                    override fun isCancelled(): Boolean = false
                    override fun onFinish() {
                        latch.countDown()
                        try {
                            verifyExtractedArchiveContents()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            fail("Error verifying extracted archive contents")
                        }
                    }
                },
                ServiceWatcherUtil.UPDATE_POSITION
            )
        extractor.extractEverything()
        latch.await()
    }

    @Throws(IOException::class)
    private fun verifyExtractedArchiveContents() {
        File(Environment.getExternalStorageDirectory(), "test-archive").run {
            assertTrue(exists())
            assertTrue(File(this, "1").exists())
            assertTrue(File(this, "2").exists())
            assertTrue(File(this, "3").exists())
            assertTrue(File(this, "4").exists())
            assertTrue(File(this, "a").exists())
            assertTrue(File(File(this, "1"), "8").exists())
            assertTrue(File(File(this, "2"), "7").exists())
            assertTrue(File(File(this, "3"), "6").exists())
            assertTrue(File(File(this, "4"), "5").exists())
            assertTrue(File(File(this, "a/b/c/d"), "lipsum.bin").exists())

            File(File(this, "1"), "8").run {
                assertTrue(FileInputStream(this).readBytes().size == 2)
                assertEquals(EXPECTED_TIMESTAMP, lastModified())
            }

            File(File(this, "2"), "7").run {
                assertTrue(FileInputStream(this).readBytes().size == 3)
                assertEquals(EXPECTED_TIMESTAMP, lastModified())
            }

            File(File(this, "3"), "6").run {
                assertTrue(FileInputStream(this).readBytes().size == 4)
                assertEquals(EXPECTED_TIMESTAMP, lastModified())
            }

            File(File(this, "4"), "5").run {
                assertTrue(FileInputStream(this).readBytes().size == 5)
                assertEquals(EXPECTED_TIMESTAMP, lastModified())
            }

            File(File(this, "a/b/c/d"), "lipsum.bin").run {
                assertTrue(FileInputStream(this).readBytes().size == 512)
                assertEquals(EXPECTED_TIMESTAMP, lastModified())
            }
        }
    }

    private fun copyArchivesToStorage() {
        File("src/test/resources").listFiles()?.forEach {
            FileInputStream(it).copyTo(
                FileOutputStream(
                    File(Environment.getExternalStorageDirectory(), it.name)
                )
            )
        }
    }

    protected open val archiveFile: File
        get() = File(Environment.getExternalStorageDirectory(), "test-archive.$archiveType")
}
