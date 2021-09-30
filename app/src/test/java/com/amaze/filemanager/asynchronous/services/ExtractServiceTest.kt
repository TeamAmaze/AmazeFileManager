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

package com.amaze.filemanager.asynchronous.services

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION_CODES.*
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.file_operations.filesystem.compressed.ArchivePasswordCache
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.util.concurrent.InlineExecutorService
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPausedAsyncTask
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class], sdk = [JELLY_BEAN, KITKAT, P])
@LooperMode(LooperMode.Mode.PAUSED)
@Suppress("TooManyFunctions", "StringLiteralDuplication")
class ExtractServiceTest {

    private val zipfile1: File
    private val zipfile2: File
    private val zipfile3: File
    private val emptyZip: File
    private val rarfile: File
    private val tarfile: File
    private val emptyTar: File
    private val tarballfile: File
    private val tarLzmafile: File
    private val tarXzfile: File
    private val tarBz2file: File
    private val sevenZipfile: File
    private val passwordProtectedZipfile: File
    private val passwordProtected7Zipfile: File
    private val listPasswordProtected7Zipfile: File
    private val multiVolumeRarFilePart1: File
    private val multiVolumeRarFilePart2: File
    private val multiVolumeRarFilePart3: File
    private val multiVolumeRarFileV5Part1: File
    private val multiVolumeRarFileV5Part2: File
    private val multiVolumeRarFileV5Part3: File

    init {
        Environment.getExternalStorageDirectory().run {
            zipfile1 = File(this, "zip-slip.zip")
            zipfile2 = File(this, "zip-slip-win.zip")
            zipfile3 = File(this, "test-archive.zip")
            emptyZip = File(this, "empty.zip")
            rarfile = File(this, "test-archive.rar")
            tarfile = File(this, "test-archive.tar")
            emptyTar = File(this, "empty.tar")
            tarballfile = File(this, "test-archive.tar.gz")
            tarLzmafile = File(this, "test-archive.tar.lzma")
            tarXzfile = File(this, "test-archive.tar.xz")
            tarBz2file = File(this, "test-archive.tar.bz2")
            sevenZipfile = File(this, "test-archive.7z")
            passwordProtectedZipfile = File(this, "test-archive-encrypted.zip")
            passwordProtected7Zipfile = File(this, "test-archive-encrypted.7z")
            listPasswordProtected7Zipfile = File(this, "test-archive-encrypted-list.7z")
            multiVolumeRarFilePart1 = File(this, "test-multipart-archive-v4.part1.rar")
            multiVolumeRarFilePart2 = File(this, "test-multipart-archive-v4.part2.rar")
            multiVolumeRarFilePart3 = File(this, "test-multipart-archive-v4.part3.rar")
            multiVolumeRarFileV5Part1 = File(this, "test-multipart-archive-v5.part1.rar")
            multiVolumeRarFileV5Part2 = File(this, "test-multipart-archive-v5.part2.rar")
            multiVolumeRarFileV5Part3 = File(this, "test-multipart-archive-v5.part3.rar")
        }
    }

    private var service: ExtractService? = null

    /**
     * Copy archives to storage.
     *
     * @throws Exception
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        javaClass.classLoader!!.run {
            getResourceAsStream("zip-slip.zip").copyTo(FileOutputStream(zipfile1))
            getResourceAsStream("zip-slip-win.zip").copyTo(FileOutputStream(zipfile2))
            getResourceAsStream("test-archive.zip").copyTo(FileOutputStream(zipfile3))
            getResourceAsStream("empty.zip").copyTo(FileOutputStream(emptyZip))
            getResourceAsStream("test-archive.rar").copyTo(FileOutputStream(rarfile))
            getResourceAsStream("test-archive.tar").copyTo(FileOutputStream(tarfile))
            getResourceAsStream("empty.tar").copyTo(FileOutputStream(emptyTar))
            getResourceAsStream("test-archive.tar.gz").copyTo(FileOutputStream(tarballfile))
            getResourceAsStream("test-archive.tar.lzma").copyTo(FileOutputStream(tarLzmafile))
            getResourceAsStream("test-archive.tar.xz").copyTo(FileOutputStream(tarXzfile))
            getResourceAsStream("test-archive.tar.bz2").copyTo(FileOutputStream(tarBz2file))
            getResourceAsStream("test-archive.7z").copyTo(FileOutputStream(sevenZipfile))
            getResourceAsStream("test-archive-encrypted.zip")
                .copyTo(FileOutputStream(passwordProtectedZipfile))
            getResourceAsStream("test-archive-encrypted.7z")
                .copyTo(FileOutputStream(passwordProtected7Zipfile))
            getResourceAsStream("test-archive-encrypted-list.7z")
                .copyTo(FileOutputStream(listPasswordProtected7Zipfile))
            getResourceAsStream("test-multipart-archive-v4.part1.rar")
                .copyTo(FileOutputStream(multiVolumeRarFilePart1))
            getResourceAsStream("test-multipart-archive-v4.part2.rar")
                .copyTo(FileOutputStream(multiVolumeRarFilePart2))
            getResourceAsStream("test-multipart-archive-v4.part3.rar")
                .copyTo(FileOutputStream(multiVolumeRarFilePart3))
            getResourceAsStream("test-multipart-archive-v5.part1.rar")
                .copyTo(FileOutputStream(multiVolumeRarFileV5Part1))
            getResourceAsStream("test-multipart-archive-v5.part2.rar")
                .copyTo(FileOutputStream(multiVolumeRarFileV5Part2))
            getResourceAsStream("test-multipart-archive-v5.part3.rar")
                .copyTo(FileOutputStream(multiVolumeRarFileV5Part3))
        }
        ShadowPausedAsyncTask.overrideExecutor(InlineExecutorService())
        ShadowToast.reset()

        service = Robolectric.setupService(ExtractService::class.java)
    }

    /**
     * Post test cleanup.
     *
     * @throws Exception
     */
    @After
    @Throws(Exception::class)
    fun tearDown() {
        val extractedArchiveRoot = File(Environment.getExternalStorageDirectory(), "test-archive")
        if (extractedArchiveRoot.exists() && extractedArchiveRoot.isDirectory) {
            Files.walk(Paths.get(extractedArchiveRoot.absolutePath))
                .map { obj: Path -> obj.toFile() }
                .forEach { obj: File -> obj.delete() }
        }
        service?.stopSelf()
        service?.onDestroy()
    }

    /**
     * Test zip-slip security vulnerability.
     */
    @Test
    fun testExtractZipSlip() {
        performTest(zipfile1)
        ShadowLooper.idleMainLooper()
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until {
                ShadowToast.getLatestToast() != null
            }
        assertEquals(
            ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.multiple_invalid_archive_entries),
            ShadowToast.getTextOfLatestToast()
        )
    }

    /**
     * Test zip-slip vulnerability for zips with Windows paths.
     */
    @Test
    fun testExtractZipSlipWin() {
        performTest(zipfile2)
        ShadowLooper.idleMainLooper()
        await().atMost(10, TimeUnit.SECONDS).until { ShadowToast.getLatestToast() != null }
        assertEquals(
            ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.multiple_invalid_archive_entries),
            ShadowToast.getTextOfLatestToast()
        )
    }

    /**
     * Test normal unzip.
     */
    @Test
    fun testExtractZipNormal() {
        performTest(zipfile3)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test unrar.
     */
    @Test
    fun testExtractRar() {
        performTest(rarfile)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test tar extract.
     */
    @Test
    fun testExtractTar() {
        performTest(tarfile)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test tar.gz extract.
     */
    @Test
    fun testExtractTarGz() {
        performTest(tarballfile)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test tar.lzma extract.
     */
    @Test
    fun testExtractTarLzma() {
        performTest(tarLzmafile)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test tar.xz extract.
     */
    @Test
    fun testExtractTarXz() {
        performTest(tarXzfile)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test tar.bz2 extract.
     */
    @Test
    fun testExtractTarBz2() {
        performTest(tarBz2file)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test 7z extract.
     */
    @Test
    fun testExtract7z() {
        performTest(sevenZipfile)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test password-protected zip.
     */
    @Test
    fun testExtractPasswordProtectedZip() {
        ArchivePasswordCache.getInstance()[passwordProtectedZipfile.absolutePath] = "123456"
        performTest(passwordProtectedZipfile)
        ShadowLooper.idleMainLooper()
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test password-protected 7z.
     */
    @Test
    @Ignore("Work isn't finished yet, skipping test")
    fun testExtractPasswordProtected7Zip() {
        ArchivePasswordCache.getInstance()[passwordProtected7Zipfile.absolutePath] = "123456"
        performTest(passwordProtected7Zipfile)
        ShadowLooper.idleMainLooper()
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test password-protected 7zip with list also protected.
     */
    @Test
    fun testExtractListPasswordProtected7Zip() {
        ArchivePasswordCache.getInstance()[listPasswordProtected7Zipfile.absolutePath] = "123456"
        performTest(listPasswordProtected7Zipfile)
        ShadowLooper.idleMainLooper()
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test multi-volume rar extract.
     */
    @Test
    fun testExtractMultiVolumeRar() {
        performTest(multiVolumeRarFilePart1)
        assertNull(ShadowToast.getLatestToast())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Test multi volume rar v5 extract. Should Toast user that rar v5 is unsupported.
     */
    @Test
    fun testExtractMultiVolumeRarV5() {
        performTest(multiVolumeRarFileV5Part1)
        ShadowLooper.idleMainLooper()
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until {
                ShadowToast.getLatestToast() != null
            }
    }

    /**
     * Test for extracting zip with zero entries.
     *
     * @see [https://github.com/TeamAmaze/AmazeFileManager/issues/2659]
     */
    @Test
    fun testEmptyZip() {
        performTest(emptyZip)
        ShadowLooper.idleMainLooper()
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until {
                ShadowToast.getLatestToast() != null
                ShadowToast.getTextOfLatestToast().contains("is an empty archive")
            }
    }

    /**
     * Test for extracting tar with zero entries.
     *
     * @see [https://github.com/TeamAmaze/AmazeFileManager/issues/2659]
     */
    @Test
    fun testEmptyTar() {
        performTest(emptyTar)
        ShadowLooper.idleMainLooper()
        await()
            .atMost(10, TimeUnit.SECONDS)
            .until {
                ShadowToast.getLatestToast() != null
                ShadowToast.getTextOfLatestToast().contains("is an empty archive")
            }
    }

    private fun performTest(archiveFile: File) {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ExtractService::class.java)
            .putExtra(ExtractService.KEY_PATH_ZIP, archiveFile.absolutePath)
            .putExtra(ExtractService.KEY_ENTRIES_ZIP, arrayOfNulls<String>(0))
            .putExtra(
                ExtractService.KEY_PATH_EXTRACT,
                File(Environment.getExternalStorageDirectory(), "test-archive")
                    .absolutePath
            )
        service!!.onStartCommand(intent, 0, 0)
    }
}
