/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters

import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.utils.AppConstants.MEGABYTE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RecyclerAdapter.shouldLoadThumbnailStatic].
 *
 * These are pure JVM tests – no Android runtime is required – because the method under test only
 * depends on primitive values and the [OpenMode] enum.  The [maxSizes] array mirrors the
 * `R.array.thumbnailDisplaySizeLimitPreference` resource: `[-1, 1, 4, 10, 100]` (MB; index 0
 * means "no limit").
 */
class RecyclerAdapterShouldLoadThumbnailTest {
    /** Matches R.array.thumbnailDisplaySizeLimitPreference: [-1, 1, 4, 10, 100] */
    private val maxSizes = intArrayOf(-1, 1, 4, 10, 100)

    // ------------------------------------------------------------------ showThumb = false

    /**
     * When the global "show thumbnails" toggle is off, [RecyclerAdapter.shouldLoadThumbnailStatic]
     * must return `false` regardless of mode or file size.
     */
    @Test
    fun testShowThumbFalse_localFile_returnsFalse() {
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                // showThumb  =
                false,
                maxSizes,
                // capIndex   =
                0,
                // longSize   =
                100L,
                OpenMode.FILE,
            ),
        )
    }

    @Test
    fun testShowThumbFalse_remoteSmallFile_returnsFalse() {
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                // showThumb  =
                false,
                maxSizes,
                // capIndex   =
                1, // 1 MB cap
                // longSize   =
                512L, // 512 bytes – well below the cap
                OpenMode.SFTP,
            ),
        )
    }

    // ------------------------------------------------------------------ FTP mode

    /**
     * FTPClient is not thread-safe; thumbnails are disabled for FTP unconditionally.
     */
    @Test
    fun testFtpMode_showThumbTrue_noCap_returnsFalse() {
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                // showThumb  =
                true,
                maxSizes,
                // capIndex   =
                0, // no cap
                // longSize   =
                1024L,
                OpenMode.FTP,
            ),
        )
    }

    @Test
    fun testFtpMode_showThumbTrue_withCap_smallFile_returnsFalse() {
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                // showThumb  =
                true,
                maxSizes,
                // capIndex   =
                1,
                // longSize   =
                100L,
                OpenMode.FTP,
            ),
        )
    }

    // ------------------------------------------------------------------ Local modes

    /**
     * Local file-system modes have no size cap – thumbnails are always loaded when showThumb=true.
     */
    @Test
    fun testLocalFileMode_returnsTrue() {
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                // showThumb  =
                true,
                maxSizes,
                // capIndex   =
                1, // cap is irrelevant for local files
                // longSize   =
                500L * MEGABYTE,
                OpenMode.FILE,
            ),
        )
    }

    @Test
    fun testRootMode_returnsTrue() {
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                0,
                1024L,
                OpenMode.ROOT,
            ),
        )
    }

    // ------------------------------------------------------------------ Remote modes: no cap

    /**
     * When capIndex == 0 ("No limit"), any file size should load a thumbnail for remote modes.
     */
    @Test
    fun testSftp_noCap_hugeFile_returnsTrue() {
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                0,
                // longSize   =
                500L * MEGABYTE,
                OpenMode.SFTP,
            ),
        )
    }

    @Test
    fun testSmb_noCap_largeFile_returnsTrue() {
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                0,
                100L * MEGABYTE,
                OpenMode.SMB,
            ),
        )
    }

    // ------------------------------------------------------------------ Remote modes: with cap

    /** File strictly below the 1 MB cap → thumbnail should load. */
    @Test
    fun testSftp_1MbCap_smallFile_returnsTrue() {
        val smallFile = 100 * 1024L // 100 KB
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                1, // maxSizes[1] == 1 → 1 MB cap
                smallFile,
                OpenMode.SFTP,
            ),
        )
    }

    /** File exactly at the 1 MB cap → thumbnail should load (inclusive <=). */
    @Test
    fun testSftp_1MbCap_exactlyAtLimit_returnsTrue() {
        val exactly1Mb = 1 * MEGABYTE.toLong()
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                1,
                exactly1Mb,
                OpenMode.SFTP,
            ),
        )
    }

    /** File 1 byte above the 1 MB cap → thumbnail must NOT load. */
    @Test
    fun testSftp_1MbCap_oneBytePastLimit_returnsFalse() {
        val justOver1Mb = 1 * MEGABYTE.toLong() + 1L
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                1,
                justOver1Mb,
                OpenMode.SFTP,
            ),
        )
    }

    /** Large file above the 4 MB cap → must not load. */
    @Test
    fun testDropbox_4MbCap_largeFile_returnsFalse() {
        val fiveMb = 5L * MEGABYTE
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                2, // maxSizes[2] == 4 → 4 MB cap
                fiveMb,
                OpenMode.DROPBOX,
            ),
        )
    }

    /** BOX file below the 100 MB cap → should load. */
    @Test
    fun testBox_100MbCap_fileWithinCap_returnsTrue() {
        val fiftyMb = 50L * MEGABYTE
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                4, // maxSizes[4] == 100 → 100 MB cap
                fiftyMb,
                OpenMode.BOX,
            ),
        )
    }

    /** GDRIVE file above the 10 MB cap → must not load. */
    @Test
    fun testGdrive_10MbCap_fileAboveCap_returnsFalse() {
        val fiftyMb = 50L * MEGABYTE
        assertFalse(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                3, // maxSizes[3] == 10 → 10 MB cap
                fiftyMb,
                OpenMode.GDRIVE,
            ),
        )
    }

    /** ONEDRIVE file within the 10 MB cap → should load. */
    @Test
    fun testOnedrive_10MbCap_fileWithinCap_returnsTrue() {
        val fiveMb = 5L * MEGABYTE
        assertTrue(
            RecyclerAdapter.shouldLoadThumbnailStatic(
                true,
                maxSizes,
                // capIndex   =
                3,
                fiveMb,
                OpenMode.ONEDRIVE,
            ),
        )
    }
}
