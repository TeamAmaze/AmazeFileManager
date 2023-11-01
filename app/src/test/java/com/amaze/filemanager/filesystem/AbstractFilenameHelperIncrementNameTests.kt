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

package com.amaze.filemanager.filesystem

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
@Suppress("StringLiteralDuplication")
abstract class AbstractFilenameHelperIncrementNameTests {

    protected abstract val formatFlag: FilenameFormatFlag

    protected lateinit var file: HybridFile

    private val existingFiles = arrayOf(
        "/test/afile",
        "/test/abc (2) - Copy - Copy.txt",
        "/test/abc (2) - Copy.txt",
        "/test/abc.txt",
        "/test/bar.txt",
        "/test/foo (2).txt",
        "/test/foo 2 2.txt",
        "/test/foo 2.txt",
        "/test/foo 22.txt",
        "/test/foo 3 copy.txt",
        "/test/foo copy 2.txt",
        "/test/foo copy 3.txt",
        "/test/foo copy 4.txt",
        "/test/foo copy 5.txt",
        "/test/foo copy 6.txt",
        "/test/foo copy.txt",
        "/test/foo.txt",
        "/test/one (copy).txt",
        "/test/one.txt",
        "/test/qux (2).txt",
        "/test/qux 2.txt",
        "/test/qux.txt",
        "/test/sub/foo.txt",
        "/test/sub/bar.txt",
        "/test/sub/nested/bar.txt",
        "/test/sub/nested/foo.txt",
        "/test/sub/nested/foo copy.txt",
        "/test/sub/nested/qux.txt",
        "/test/sub/nested/qux 2.txt",
        "/test/sub/nested/qux (2).txt"
    )

    /**
     * Sanity check.
     */
    @Test
    fun testSanityCheck() {
        mockkConstructor(HybridFile::class)
        file = HybridFile(OpenMode.UNKNOWN, "/test/file1.txt")
        every { file.exists(AppConfig.getInstance()) } answers {
            file.path == "/test/file1.txt"
        }
        assertEquals(OpenMode.UNKNOWN, file.mode)
        assertEquals("/test/file1.txt", file.path)
        assertTrue("file.path is ${file.path}", file.exists(AppConfig.getInstance()))
        file = HybridFile(OpenMode.UNKNOWN, "/test/file2.txt")
        assertEquals(OpenMode.UNKNOWN, file.mode)
        assertEquals("/test/file2.txt", file.path)
        assertFalse("file.path is ${file.path}", file.exists(AppConfig.getInstance()))
        unmockkConstructor(HybridFile::class)
    }

    /**
     * Ensure [FilenameHelper.increment] will have no effect when [HybridFile.exists] is false.
     */
    @Test
    fun testIncrementShouldNotHaveEffectIfNotExist() {
        mockkConstructor(HybridFile::class)
        file = HybridFile(OpenMode.UNKNOWN, "/test/file1.txt")
        every { file.exists(AppConfig.getInstance()) } answers {
            false
        }
        assertEquals(OpenMode.UNKNOWN, file.mode)
        assertEquals("/test/file1.txt", file.path)
        assertFalse("file.path is ${file.path}", file.exists(AppConfig.getInstance()))
        val retval = FilenameHelper.increment(file = file, formatFlag)
        assertEquals("file1.txt", retval.getName(AppConfig.getInstance()))
        unmockkConstructor(HybridFile::class)
    }

    protected fun performTest(
        pairs: Array<Pair<String, String>>,
        strip: Boolean = false,
        removeRawNumbers: Boolean = false,
        start: Int = 1
    ) {
        for (pair in pairs) performTest(pair, strip, removeRawNumbers, start)
    }

    protected fun performTest(
        pair: Pair<String, String>,
        strip: Boolean = false,
        removeRawNumbers: Boolean = false,
        start: Int = 1
    ) {
        Mockito.mockConstruction(HybridFile::class.java) { file, context ->
            file.mode = context.arguments()[0] as OpenMode
            file.path = context.arguments()[1] as String
            if (context.arguments().size == 4) {
                file.name = context.arguments()[2] as String
                file.path += "/${context.arguments()[2] as String}"
            }
            `when`(file.exists(AppConfig.getInstance())).thenAnswer {
                val c = existingFiles
                file.path == pair.first || c.contains(file.path)
            }
            `when`(file.getName(AppConfig.getInstance())).thenAnswer {
                file.path.pathBasename()
            }
        }.run {
            file = HybridFile(OpenMode.UNKNOWN, pair.first)
            assertEquals(OpenMode.UNKNOWN, file.mode)
            assertEquals(pair.first, file.path)
            assertTrue("file.path is ${file.path}", file.exists(AppConfig.getInstance()))
            val retval = FilenameHelper.increment(file, formatFlag, strip, removeRawNumbers, start)
            assertEquals(pair.second, retval.path)
            this.close()
        }
    }
}
