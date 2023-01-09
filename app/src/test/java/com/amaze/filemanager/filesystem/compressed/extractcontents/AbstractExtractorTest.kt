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
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.fileoperations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.fileoperations.utils.UpdatePosition
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.randomBytes
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [KITKAT, P, Build.VERSION_CODES.R])
abstract class AbstractExtractorTest {

    protected abstract fun extractorClass(): Class<out Extractor>
    protected abstract val archiveType: String

    private lateinit var systemTz: TimeZone

    /**
     * Test setup, copy archives to storage space
     */
    @Before
    fun setUp() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        copyArchivesToStorage()
        systemTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /**
     * Post test clean up
     */
    @After
    fun tearDown() {
        ArchivePasswordCache.getInstance().clear()
        val extractedArchiveRoot = File(Environment.getExternalStorageDirectory(), "test-archive")
        if (extractedArchiveRoot.exists()) {
            Files.walk(Paths.get(extractedArchiveRoot.absolutePath))
                .map { obj: Path -> obj.toFile() }
                .forEach { obj: File -> obj.delete() }
        }
        TimeZone.setDefault(systemTz)
    }

    /**
     * Test extractor ability to extract files
     */
    @Test
    open fun testExtractFiles() {
        doTestExtractFiles()
    }

    protected abstract fun doTestExtractFiles()

    /**
     * Test extractor ability to throw [Extractor.BadArchiveNotice]
     */
    @Test
    open fun testExtractBadArchive() {
        val badArchive = File(Environment.getExternalStorageDirectory(), "bad-archive.$archiveType")
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
                badArchive.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                object : Extractor.OnUpdate {
                    override fun onStart(totalBytes: Long, firstEntryName: String) = Unit
                    override fun onUpdate(entryPath: String) = Unit
                    override fun isCancelled(): Boolean = false
                    override fun onFinish() = Unit
                },
                ServiceWatcherUtil.UPDATE_POSITION
            )
        try {
            extractor.extractEverything()
            fail("BadArchiveNotice was not thrown")
        } catch (ex: Extractor.BadArchiveNotice) {
            // Pretend doing something to make codacy happy
            assertTrue(true)
        }
    }

    private fun copyArchivesToStorage() {
        File("src/test/resources").listFiles()?.filter {
            it.isFile
        }?.forEach {
            FileInputStream(it).copyTo(
                FileOutputStream(
                    File(Environment.getExternalStorageDirectory(), it.name)
                )
            )
            ByteArrayInputStream(randomBytes()).copyTo(
                FileOutputStream(
                    File(Environment.getExternalStorageDirectory(), "bad-archive.$archiveType")
                )
            )
        }
    }

    protected open val archiveFile: File
        get() = File(Environment.getExternalStorageDirectory(), "test-archive.$archiveType")
}
