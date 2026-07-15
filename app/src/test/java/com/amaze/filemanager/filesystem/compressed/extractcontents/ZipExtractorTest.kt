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
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ZipExtractorTest : AbstractArchiveExtractorTest() {
    override val archiveType: String = "zip"

    override fun extractorClass(): Class<out Extractor?> = ZipExtractor::class.java

    /**
     * Verify that a ZIP archive carrying a path-traversal entry
     * (foo/../../POC_ZIP_PROOF.txt) is handled safely:
     *  - extraction completes without an exception
     *  - the offending entry is recorded in invalidArchiveEntries
     *  - no file is written outside the designated output directory
     */
    @Test
    fun testExtractMaliciousZip() {
        val maliciousArchive = File(Environment.getExternalStorageDirectory(), "malicious.zip")
        val outputDir = Environment.getExternalStorageDirectory()
        val extractor =
            ZipExtractor(
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

        // Extraction must succeed — path-traversal entries are quarantined, not thrown
        extractor.extractEverything()

        // The traversal entry must be recorded as invalid …
        assertTrue(
            "Malicious path-traversal entry must be captured in invalidArchiveEntries",
            extractor.invalidArchiveEntries.isNotEmpty(),
        )
        assertTrue(
            "invalidArchiveEntries must contain the POC entry",
            extractor.invalidArchiveEntries.any { "POC_ZIP_PROOF" in it },
        )
        // … and must NOT have been written outside the output directory
        val escapedFile = File(outputDir, "foo/../../POC_ZIP_PROOF.txt").canonicalFile
        assertFalse(
            "Malicious file must not escape the output directory",
            escapedFile.exists(),
        )
        assertFalse(
            "Escaped file canonical path must not reside under output directory",
            escapedFile.canonicalPath.startsWith(outputDir.canonicalPath),
        )
    }
}
