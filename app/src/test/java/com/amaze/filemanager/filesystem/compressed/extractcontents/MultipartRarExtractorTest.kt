/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.os.Build
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.filesystem.compressed.extractcontents.Extractor.OnUpdate
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.github.junrar.exception.UnsupportedRarV5Exception
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [Build.VERSION_CODES.JELLY_BEAN, Build.VERSION_CODES.KITKAT, Build.VERSION_CODES.P]
)
class MultipartRarExtractorTest {

    private val callback = object : OnUpdate {
        override fun onStart(totalBytes: Long, firstEntryName: String) = Unit
        override fun onUpdate(entryPath: String) = Unit
        override fun onFinish() = Unit
        override fun isCancelled(): Boolean = false
    }

    /**
     * Test setup.
     */
    @Before
    fun setUp() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        copyArchivesToStorage()
    }

    /**
     * Test extract a multipart RAR v4.
     */
    @Test
    fun testExtractMultiVolumeV4Rar() {
        val latch = CountDownLatch(1)
        RarExtractor(
            ApplicationProvider.getApplicationContext(),
            File(
                Environment.getExternalStorageDirectory(),
                "test-multipart-archive-v4.part1.rar"
            ).absolutePath,
            Environment.getExternalStorageDirectory().absolutePath,
            object : OnUpdate by callback {
                override fun onFinish() {
                    latch.countDown()
                    val verify = File(Environment.getExternalStorageDirectory(), "test.bin")
                    Assert.assertTrue(verify.exists())
                    Assert.assertEquals((1024 * 128).toLong(), verify.length())
                }
            },
            ServiceWatcherUtil.UPDATE_POSITION
        ).extractEverything()
        latch.await()
    }

    /**
     * Test extract a multipart RAR v5, which should fail.
     */
    @Test
    fun testExtractMultiVolumeV5Rar() {
        try {
            RarExtractor(
                ApplicationProvider.getApplicationContext(),
                File(
                    Environment.getExternalStorageDirectory(),
                    "test-multipart-archive-v5.part1.rar"
                )
                    .absolutePath,
                Environment.getExternalStorageDirectory().absolutePath,
                callback,
                ServiceWatcherUtil.UPDATE_POSITION
            ).extractEverything()
            Assert.fail("No exception was thrown")
        } catch (expected: IOException) {
            expected.printStackTrace()
            Assert.assertEquals(UnsupportedRarV5Exception::class.java, expected.cause!!.javaClass)
        }
    }

    private fun copyArchivesToStorage() {
        File("src/test/resources").listFiles()?.forEach {
            FileInputStream(it)
                .copyTo(FileOutputStream(File(Environment.getExternalStorageDirectory(), it.name)))
        }
    }
}
