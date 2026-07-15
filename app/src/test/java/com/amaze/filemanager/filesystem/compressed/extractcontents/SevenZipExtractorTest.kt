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
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.SevenZipExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException

open class SevenZipExtractorTest : AbstractArchiveExtractorTest() {
    override val archiveType: String = "7z"

    override fun extractorClass(): Class<out Extractor?> = SevenZipExtractor::class.java

    /**
     * Verify that a 7-Zip archive carrying a path-traversal entry
     * (../POC_7Z_PROOF.txt) is blocked by the canonical-path guard:
     *  - extractEverything() must throw IOException
     *  - no file is written outside the designated output directory
     */
    @Test
    fun testExtractMalicious7z() {
        val maliciousArchive = File(Environment.getExternalStorageDirectory(), "malicious.7z")
        val outputDir = Environment.getExternalStorageDirectory()
        val extractor =
            SevenZipExtractor(
                ApplicationProvider.getApplicationContext(),
                maliciousArchive.absolutePath,
                outputDir.absolutePath,
                object : Extractor.OnUpdate {
                    override fun onStart(
                        totalBytes: Long,
                        firstEntryName: String,
                    ) = Unit

                    override fun onUpdate(entryPath: String) = Unit

                    override fun isCancelled(): Boolean = false

                    override fun onFinish() = Unit
                },
                ServiceWatcherUtil.UPDATE_POSITION,
            )

        try {
            extractor.extractEverything()
            fail("Expected IOException: canonical-path guard must reject the traversal entry")
        } catch (e: IOException) {
            // Confirm the guard fired (not a generic bad-archive error)
            assertFalse(
                "Exception must not be a BadArchiveNotice",
                e is Extractor.BadArchiveNotice,
            )
        }

        // The malicious file must NOT have been written outside the output directory
        assertFalse(
            "Malicious file must not escape the output directory",
            File(outputDir.parentFile, "POC_7Z_PROOF.txt").exists(),
        )
    }
}
