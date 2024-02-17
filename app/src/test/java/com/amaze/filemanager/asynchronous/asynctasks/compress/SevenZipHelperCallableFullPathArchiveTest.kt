/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.io.File

class SevenZipHelperCallableFullPathArchiveTest : AbstractCompressedHelperCallableArchiveTest() {

    override val archiveFileName: String
        get() = "test-direct-paths.7z"

    override fun testRoot() {
        val task = createCallable("")
        val result = task.call()
        assertEquals(3, result.size)
        assertNotNull(
            result.find {
                it.name == "1" && it.directory
            }
        )
        assertNotNull(
            result.find {
                it.name == "testdir" && it.directory
            }
        )
        assertNotNull(
            result.find {
                it.name == "test.jpg" && !it.directory
            }
        )
    }

    override fun testSublevels() {
        var task = createCallable("")
        var result = task.call()
        assertEquals(3, result.size)
        assertNotNull(
            result.find {
                it.name == "1" && it.directory
            }
        )
        assertNotNull(
            result.find {
                it.name == "testdir" && it.directory
            }
        )
        assertNotNull(
            result.find {
                it.name == "test.jpg" && !it.directory
            }
        )
        task = createCallable("1")
        result = task.call()
        assertEquals(1, result.size)
        assertEquals("2", result[0].name)
        assertTrue(result[0].directory)
    }

    override fun doCreateCallable(archive: File, relativePath: String): CompressedHelperCallable =
        SevenZipHelperCallable(
            archive.absolutePath,
            relativePath,
            false
        )
}
