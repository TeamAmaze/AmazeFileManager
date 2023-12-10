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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import android.os.Build.VERSION_CODES.*
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.randomBytes
import com.amaze.filemanager.test.supportedArchiveExtensions
import org.apache.commons.compress.archivers.ArchiveException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Test behaviour of CompressedHelpers in handling corrupt archives.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [KITKAT, P])
class CompressedHelperForBadArchiveTest {

    /**
     * Test handling of corrupt archive with random junk.
     */
    @Test
    fun testCorruptArchive() {
        doTestBadArchive(randomBytes())
    }

    /**
     * Test handling of zero byte archive.
     */
    @Test
    fun testZeroByteArchive() {
        doTestBadArchive(ByteArray(0))
    }

    private fun doTestBadArchive(data: ByteArray) {
        for (
            archiveType in supportedArchiveExtensions().subtract(excludedArchiveTypes)
        ) {
            val badArchive = File(
                Environment.getExternalStorageDirectory(),
                "bad-archive.$archiveType"
            )
            ByteArrayInputStream(data).copyTo(FileOutputStream(badArchive))
            val task = CompressedHelper.getCompressorInstance(
                ApplicationProvider.getApplicationContext(),
                badArchive
            )
            Assert.assertNotNull(task)
            task!!
            try {
                val result = task.changePath("", false).call()
                Assert.assertNull("Thrown from ${task.javaClass}", result)
            } catch (exception: ArchiveException) {
                Assert.assertNotNull(exception)
                Assert.assertTrue(
                    "Thrown from ${task.javaClass}: ${exception.javaClass} was thrown",
                    ArchiveException::class.java.isAssignableFrom(exception.javaClass)
                )
            }
        }
    }

    companion object {

        /*
         * tar and rar currently will produce empty list.
         * bz2, lzma, gz, xz by default must have one entry
         * = filename without compressed extension. They won't throw exceptions, so excluded from
         * list
         */
        private val excludedArchiveTypes = listOf("tar", "rar", "bz2", "lzma", "gz", "xz")
    }
}
