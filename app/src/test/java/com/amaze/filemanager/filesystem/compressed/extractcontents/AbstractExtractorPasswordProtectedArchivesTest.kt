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

import android.os.Environment
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException

abstract class AbstractExtractorPasswordProtectedArchivesTest : AbstractExtractorTest() {

    /**
     * Test extract files without password.
     */
    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testExtractFilesWithoutPassword() {
        ArchivePasswordCache.getInstance().clear()
        try {
            doTestExtractFiles()
        } catch (e: IOException) {
            assertExceptionIsExpected(e)
            throw e
        }
    }

    /**
     * Test extract fils with wrong password.
     */
    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testExtractFilesWithWrongPassword() {
        ArchivePasswordCache.getInstance().clear()
        ArchivePasswordCache.getInstance()[archiveFile.absolutePath] = "abcdef"
        try {
            doTestExtractFiles()
        } catch (e: IOException) {
            e.printStackTrace()
            assertExceptionIsExpected(e)
            throw e
        }
    }

    /**
     * Test extract files with repeatedly wrong password.
     */
    @Test(expected = IOException::class)
    @Throws(Exception::class)
    fun testExtractFilesWithRepeatedWrongPassword() {
        ArchivePasswordCache.getInstance().clear()
        ArchivePasswordCache.getInstance()[archiveFile.absolutePath] = "abcdef"
        try {
            doTestExtractFiles()
        } catch (e: IOException) {
            assertExceptionIsExpected(e)
            throw e
        }
        ArchivePasswordCache.getInstance()[archiveFile.absolutePath] = "pqrstuv"
        try {
            doTestExtractFiles()
        } catch (e: IOException) {
            assertExceptionIsExpected(e)
            throw e
        }
    }

    @Test
    @Throws(Exception::class)
    override fun testExtractFiles() {
        ArchivePasswordCache.getInstance()[archiveFile.absolutePath] = "123456"
        doTestExtractFiles()
    }

    override val archiveFile: File
        get() = File(
            Environment.getExternalStorageDirectory(), "test-archive-encrypted.$archiveType"
        )

    protected abstract fun expectedRootExceptionClass(): Array<Class<*>>
    @Throws(IOException::class)
    protected fun assertExceptionIsExpected(e: IOException) {
        for (c in expectedRootExceptionClass()) {
            if (
                if (e.cause != null) {
                    c.isAssignableFrom(e.cause!!.javaClass)
                } else {
                    c.isAssignableFrom(e.javaClass)
                }
            ) return
        }
        Assert.fail("Exception verification failed.")
        throw e
    }
}
