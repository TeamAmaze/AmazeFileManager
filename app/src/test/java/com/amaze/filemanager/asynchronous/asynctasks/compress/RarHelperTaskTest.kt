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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import android.os.Environment
import com.github.junrar.exception.UnsupportedRarV5Exception
import org.apache.commons.compress.archivers.ArchiveException
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RarHelperTaskTest : AbstractCompressedHelperTaskTest() {

    /**
     * Test multi volume RAR (v4).
     */
    @Test
    fun testMultiVolumeRar() {
        val task: CompressedHelperTask = RarHelperTask(
            File(
                Environment.getExternalStorageDirectory(),
                "test-multipart-archive-v4.part1.rar"
            )
                .absolutePath,
            "",
            false,
            emptyCallback
        )
        val result = task.doInBackground()
        assertNotNull(result)
        assertNotNull(result.result)
        assertEquals(1, result.result.size.toLong())
        assertEquals("test.bin", result.result[0].name)
        assertEquals((1024 * 128).toLong(), result.result[0].size)
    }

    /**
     * Test RAR v5.
     */
    @Test
    fun testMultiVolumeRarV5() {
        val task: CompressedHelperTask = RarHelperTask(
            File(
                Environment.getExternalStorageDirectory(),
                "test-multipart-archive-v5.part1.rar"
            )
                .absolutePath,
            "",
            false,
            emptyCallback
        )
        val result = task.doInBackground()
        assertNotNull(result)
        assertNull(result.result)
        assertNotNull(result.exception)
        assertEquals(ArchiveException::class.java, result.exception.javaClass)
        assertEquals(UnsupportedRarV5Exception::class.java, result.exception.cause!!.javaClass)
    }

    override fun createTask(relativePath: String): CompressedHelperTask = RarHelperTask(
        File(Environment.getExternalStorageDirectory(), "test-archive.rar").absolutePath,
        relativePath,
        false,
        emptyCallback
    )
}
