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

import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor
import com.github.junrar.Archive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.io.File

class RarExtractorTest : AbstractArchiveExtractorTest() {
    override val archiveType: String = "rar"

    override fun extractorClass(): Class<out Extractor?> = RarExtractor::class.java

    /**
     * Test [RarExtractor.tryExtractSmallestFileInArchive].
     *
     * test-archive-sizes.rar contains files of different sizes,
     * with 1 being the smallest = 2 bytes. tryExtractSmallestFileInArchive should extract this
     * file.
     */
    @Test
    fun testTryExtractSmallestFileInArchive() {
        val archiveFile = File(Environment.getExternalStorageDirectory(), "test-archive-sizes.rar")
        val extractor =
            RarExtractor(
                ApplicationProvider.getApplicationContext(),
                archiveFile.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
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
        val verify =
            RarExtractor::class.java.getDeclaredMethod(
                "tryExtractSmallestFileInArchive",
                Context::class.java,
                Archive::class.java,
            ).run {
                isAccessible = true
                invoke(
                    extractor,
                    ApplicationProvider.getApplicationContext(),
                    Archive(archiveFile).also {
                        it.password = "123456"
                    },
                )
            }
        File(
            ApplicationProvider.getApplicationContext<Context>().externalCacheDir!!,
            "test-archive/1",
        ).run {
            assertEquals(this.absolutePath, verify)
            assertEquals(2, this.length())
        }
    }

    /**
     * Verify that a RAR4 archive carrying a path-traversal entry
     * (foo//../../POC_RAR_PROOF.txt/POC_RAR_PROOF.txt) is handled safely:
     *  - extraction completes without an exception
     *  - the offending entry is recorded in invalidArchiveEntries
     *  - no file is written outside the designated output directory
     */
    @Test
    fun testExtractMaliciousRar() {
        val maliciousArchive = File(Environment.getExternalStorageDirectory(), "malicious.rar")
        val outputDir = Environment.getExternalStorageDirectory()
        val extractor =
            RarExtractor(
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
            extractor.invalidArchiveEntries.any { "POC_RAR_PROOF" in it },
        )
        // … and must NOT have been written outside the output directory
        assertFalse(
            "Malicious file must not escape the output directory",
            File(outputDir.parentFile, "POC_RAR_PROOF.txt").exists(),
        )
    }

    @Test @Ignore
    override fun testExtractBadArchive() = Unit
}
