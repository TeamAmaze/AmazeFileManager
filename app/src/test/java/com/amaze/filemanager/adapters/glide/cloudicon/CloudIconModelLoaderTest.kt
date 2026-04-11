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

package com.amaze.filemanager.adapters.glide.cloudicon

import com.amaze.filemanager.database.CloudHandler
import com.bumptech.glide.load.Options
import com.bumptech.glide.signature.ObjectKey
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CloudIconModelLoader].
 *
 * Covers:
 *  - [CloudIconModelLoader.handles] — only cloud/SMB/SFTP paths should return `true`.
 *  - [CloudIconModelLoader.buildLoadData] — the cache [com.bumptech.glide.load.Key] must be
 *    stable (same path → same key; different paths → different keys).
 *    Before the fix the key was `ObjectKey(System.currentTimeMillis())` making every
 *    request unique and defeating Glide's memory/disk cache.
 */
class CloudIconModelLoaderTest {
    private val options = mockk<Options>(relaxed = true)
    private val context = mockk<android.content.Context>(relaxed = true)
    private val loader = CloudIconModelLoader(context)

    /**
     * Only paths that look like cloud storage URLs should return `true`
     */
    @Test
    fun `Handles cloud storage paths should return true`() {
        assertTrue(loader.handles("smb://192.168.1.1/share/photo.jpg"))
        assertTrue(loader.handles("ssh://user@host/home/user/photo.jpg"))
        assertTrue(loader.handles("${CloudHandler.CLOUD_PREFIX_DROPBOX}photo.jpg"))
        assertTrue(loader.handles("${CloudHandler.CLOUD_PREFIX_BOX}photo.jpg"))
        assertTrue(loader.handles("${CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE}photo.jpg"))
        assertTrue(loader.handles("${CloudHandler.CLOUD_PREFIX_ONE_DRIVE}photo.jpg"))
    }

    /**
     * Local file paths and other non-cloud URLs should return `false`
     */
    @Test
    fun `Local absolute path should return false`() {
        assertFalse(loader.handles("/storage/emulated/0/DCIM/photo.jpg"))
    }

    /**
     * Relative paths (without a leading slash) should also return `false` since they don't match
     * the expected cloud URL patterns.
     */
    @Test
    fun `Local relative path should return false`() {
        assertFalse(loader.handles("DCIM/photo.jpg"))
    }

    /**
     * An empty string is not a valid path and should return `false`.
     */
    @Test
    fun `Empty string should return false`() {
        assertFalse(loader.handles(""))
    }

    /**
     * FTP paths should also return `false`.
     */
    @Test
    fun `FTP path should return false`() {
        assertFalse(loader.handles("ftp://host/file.jpg"))
    }

    // -------------------------------------------------------------------------
    // buildLoadData() — stable cache key contract
    // -------------------------------------------------------------------------

    /**
     * The same path presented twice must produce the same cache [com.bumptech.glide.load.Key].
     *
     * Before the fix, `ObjectKey(System.currentTimeMillis())` was used, guaranteeing a unique key
     * on every call.  This test would have failed with the old code.
     */
    @Test
    fun `test buildLoadData twice with the same key`() {
        val path = "smb://192.168.1.1/share/photo.jpg"
        val key1 = loader.buildLoadData(path, 200, 200, options).sourceKey
        val key2 = loader.buildLoadData(path, 200, 200, options).sourceKey
        assertEquals(
            "Cache key must be stable across identical calls to buildLoadData()",
            key1,
            key2,
        )
    }

    /**
     * Different paths must produce different cache keys so distinct files are stored separately.
     */
    @Test
    fun `test buildLoadData with different paths and keys`() {
        val key1 = loader.buildLoadData("smb://host/fileA.jpg", 200, 200, options).sourceKey
        val key2 = loader.buildLoadData("smb://host/fileB.jpg", 200, 200, options).sourceKey
        assertNotEquals(
            "Different paths must produce different cache keys",
            key1,
            key2,
        )
    }

    /**
     * The fetcher embedded in [com.bumptech.glide.load.model.ModelLoader.LoadData] must be a
     * [CloudIconDataFetcher] — not null and of the correct type.
     */
    @Test
    fun testBuildLoadData_fetcherIsCloudIconDataFetcher() {
        val loadData = loader.buildLoadData("smb://host/photo.jpg", 200, 200, options)
        assertNotNull(loadData.fetcher)
        assertTrue(
            "Fetcher should be a CloudIconDataFetcher",
            loadData.fetcher is CloudIconDataFetcher,
        )
    }

    /**
     * The cache key must equal a plain [ObjectKey] built from the same path string.
     * This pins the exact key type so an accidental regression would be caught.
     */
    @Test
    fun testBuildLoadData_keyEqualsObjectKeyOfPath() {
        val path = "ssh://user@host/home/photo.jpg"
        val actualKey = loader.buildLoadData(path, 100, 100, options).sourceKey
        val expectedKey = ObjectKey(path)
        assertEquals(
            "Key should be ObjectKey(path), not ObjectKey(currentTimeMillis())",
            expectedKey,
            actualKey,
        )
    }
}
