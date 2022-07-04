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
import com.amaze.filemanager.fileoperations.utils.UpdatePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import java.util.concurrent.CountDownLatch

abstract class AbstractCompressedFileExtractorTest : AbstractExtractorTest() {

    @Throws(Exception::class)
    override fun doTestExtractFiles() {
        val latch = CountDownLatch(1)
        val extractor = extractorClass()
            .getConstructor(
                Context::class.java,
                String::class.java,
                String::class.java,
                Extractor.OnUpdate::class.java,
                UpdatePosition::class.java
            )
            .newInstance(
                ApplicationProvider.getApplicationContext(),
                archiveFile.absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                object : Extractor.OnUpdate {
                    override fun onStart(totalBytes: Long, firstEntryName: String) = Unit
                    override fun onUpdate(entryPath: String) = Unit
                    override fun isCancelled(): Boolean = false
                    override fun onFinish() {
                        latch.countDown()
                    }
                },
                ServiceWatcherUtil.UPDATE_POSITION
            )
        extractor.extractEverything()
        latch.await()
        verifyExtractedArchiveContents()
    }

    private fun verifyExtractedArchiveContents() {
        Environment.getExternalStorageDirectory().run {
            File(this, "test.txt").let {
                assertTrue(it.exists())
                assertEquals("abcdefghijklmnopqrstuvwxyz1234567890", it.readText().trim())
            }
        }
    }

    override val archiveFile: File
        get() = File(Environment.getExternalStorageDirectory(), "test.txt.$archiveType")
}
