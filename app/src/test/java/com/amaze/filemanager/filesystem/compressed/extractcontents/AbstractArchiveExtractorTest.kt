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

package com.amaze.filemanager.filesystem.compressed.extractcontents

import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.fileoperations.utils.UpdatePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch

abstract class AbstractArchiveExtractorTest : AbstractExtractorTest() {

    companion object {
        @JvmStatic
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
    }

    /**
     * run assertion on file timestamp correctness.
     */
    protected open fun assertFileTimestampCorrect(file: File) =
        assertEquals(EXPECTED_TIMESTAMP, file.lastModified())

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
                Extractor.OnUpdate::class.java,
                UpdatePosition::class.java
            )
            .newInstance(
                ApplicationProvider.getApplicationContext(),
                archiveFile.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                object : Extractor.OnUpdate {
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

    @Throws(Exception::class)
    override fun doTestExtractFiles() {
        val latch = CountDownLatch(1)
        val extractor = extractorClass()
            .getConstructor(
                Context::class.java,
                String::class.java,
                String::class.java,
                Extractor.OnUpdate::class.java,
                UpdatePosition::class.java
            )
            .newInstance(
                ApplicationProvider.getApplicationContext(),
                archiveFile.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                object : Extractor.OnUpdate {
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
                assertFileTimestampCorrect(this)
            }

            File(File(this, "2"), "7").run {
                assertTrue(FileInputStream(this).readBytes().size == 3)
                assertFileTimestampCorrect(this)
            }

            File(File(this, "3"), "6").run {
                assertTrue(FileInputStream(this).readBytes().size == 4)
                assertFileTimestampCorrect(this)
            }

            File(File(this, "4"), "5").run {
                assertTrue(FileInputStream(this).readBytes().size == 5)
                assertFileTimestampCorrect(this)
            }

            File(File(this, "a/b/c/d"), "lipsum.bin").run {
                assertTrue(FileInputStream(this).readBytes().size == 512)
                assertFileTimestampCorrect(this)
            }
        }
    }
}
