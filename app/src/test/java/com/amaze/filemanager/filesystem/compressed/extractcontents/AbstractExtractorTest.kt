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

import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [JELLY_BEAN, KITKAT, P])
abstract class AbstractExtractorTest {

    protected abstract fun extractorClass(): Class<out Extractor>
    protected abstract val archiveType: String

    private lateinit var systemTz: TimeZone

    /**
     * Test setup, copy archives to storage space
     */
    @Before
    @Throws(Exception::class)
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
    @Throws(Exception::class)
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
    @Throws(Exception::class)
    open fun testExtractFiles() {
        doTestExtractFiles()
    }

    protected abstract fun doTestExtractFiles()

    private fun copyArchivesToStorage() {
        File("src/test/resources").listFiles()?.filter {
            it.isFile
        }?.forEach {
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
