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

import androidx.test.core.app.ApplicationProvider
import org.apache.commons.compress.archivers.ArchiveException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ZipHelperCallableTest : AbstractCompressedHelperCallableArchiveTest() {

    override val archiveFileName: String
        get() = "test-archive.zip"

    /**
     * Verification on logic in [ZipHelperCallable] assigning zip entry path.
     *
     * @see ZipHelperCallable.addElements
     */
    @Test
    fun testVariableYAssignment() {
        var a = "aaz"
        var y = a.apply {
            if (startsWith("/")) {
                substring(1, length)
            }
        }
        assertEquals("aaz", y)
        a = "/abcdefg"
        y = a.let {
            if (it.startsWith("/")) {
                it.substring(1, it.length)
            } else {
                it
            }
        }
        assertEquals("abcdefg", y)
    }

    /**
     * Test behaviour when URI that cannot be recognized as file is passed into ZipHelperTask.
     */
    @Test(expected = ArchiveException::class)
    fun testInvalidFileUriShouldThrowArchiveException() {
        ZipHelperCallable(
            ApplicationProvider.getApplicationContext(),
            "mailto:test@test.com",
            "",
            false
        ).addElements(ArrayList())
    }

    override fun doCreateCallable(archive: File, relativePath: String): CompressedHelperCallable =
        ZipHelperCallable(
            ApplicationProvider.getApplicationContext(),
            archive.absolutePath,
            relativePath,
            false
        )
}
