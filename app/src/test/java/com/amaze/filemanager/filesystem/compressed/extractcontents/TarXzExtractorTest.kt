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
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.TarXzExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Tests for [TarXzExtractor].
 */
class TarXzExtractorTest : AbstractArchiveExtractorTest() {
    override val archiveType: String = "tar.xz"

    override fun extractorClass(): Class<out Extractor?> = TarXzExtractor::class.java

    /**
     * Test extracting a malicious tar.xz archive does not allow path traversal.
     */
    @Test
    fun testExtractMaliciousTarXz() {
        val maliciousArchive = File(Environment.getExternalStorageDirectory(), "malicious.tar.xz")
        val outputDir = Environment.getExternalStorageDirectory()
        val extractor =
            TarXzExtractor(
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
            assertFalse(
                "Exception must not be a BadArchiveNotice",
                e is Extractor.BadArchiveNotice,
            )
        }

        assertFalse(
            "Malicious file must not escape the output directory",
            File(outputDir.parentFile, "POC_ZIPSLIP_PROOF.txt").exists(),
        )
    }
}
