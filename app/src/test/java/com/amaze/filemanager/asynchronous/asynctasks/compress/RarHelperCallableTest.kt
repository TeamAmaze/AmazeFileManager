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

class RarHelperCallableTest : AbstractCompressedHelperCallableArchiveTest() {

    override val archiveFileName: String
        get() = "test-archive.rar"

    /**
     * Test multi volume RAR (v4).
     */
    @Test
    fun testMultiVolumeRar() {
        val callable: CompressedHelperCallable = RarHelperCallable(
            File(
                Environment.getExternalStorageDirectory(),
                "test-multipart-archive-v4.part1.rar"
            )
                .absolutePath,
            "",
            false
        )
        val result = callable.call()
        assertNotNull(result)
        assertNotNull(result)
        assertEquals(1, result.size.toLong())
        assertEquals("test.bin", result[0].name)
        assertEquals((1024 * 128).toLong(), result[0].size)
    }

    /**
     * Test RAR v5.
     */
    @Test
    @Suppress("Detekt.TooGenericExceptionCaught")
    fun testMultiVolumeRarV5() {
        val callable: CompressedHelperCallable = RarHelperCallable(
            File(
                Environment.getExternalStorageDirectory(),
                "test-multipart-archive-v5.part1.rar"
            )
                .absolutePath,
            "",
            false
        )
        val result = try {
            callable.call()
        } catch (exception: Throwable) {
            assertEquals(ArchiveException::class.java, exception.javaClass)
            assertEquals(UnsupportedRarV5Exception::class.java, exception.cause!!.javaClass)
            null
        }
        assertNull(result)
    }

    override fun doCreateCallable(archive: File, relativePath: String): CompressedHelperCallable =
        RarHelperCallable(
            archive.absolutePath,
            relativePath,
            false
        )
}
