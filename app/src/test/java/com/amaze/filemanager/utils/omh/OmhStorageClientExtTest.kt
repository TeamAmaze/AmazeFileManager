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
package com.amaze.filemanager.utils.omh

import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.model.OmhStorageEntity
import com.openmobilehub.android.storage.core.model.OmhStorageMetadata
import com.openmobilehub.android.storage.core.utils.folderSize
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date

/**
 * Unit tests for [OmhStorageClientExt] blocking wrapper functions.
 *
 * Each wrapper is a one-liner `runBlocking { underlyingFunction(...) }`. These tests verify that
 * the blocking wrapper correctly delegates to the underlying suspend function and forwards the
 * result (including null) unchanged.
 */
@Suppress("TooManyFunctions", "StringLiteralDuplication")
class OmhStorageClientExtTest {
    private lateinit var mockClient: OmhStorageClient

    /**
     * Setup before tests.
     */
    @Before
    fun setUp() {
        mockClient = mockk()
        // Required to mock the folderSize suspend extension function from the omh-storage library
        mockkStatic("com.openmobilehub.android.storage.core.utils.OmhStorageClientExtensionsKt")
    }

    /**
     * Cleanup after tests.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun makeFile(
        id: String = "file_id",
        name: String = "file.txt",
    ): OmhStorageEntity.OmhFile = OmhStorageEntity.OmhFile(id, name, Date(), Date(), "parent_id", "text/plain", "txt", 1024)

    private fun makeFolder(
        id: String = "folder_id",
        name: String = "folder",
    ): OmhStorageEntity.OmhFolder = OmhStorageEntity.OmhFolder(id, name, Date(), Date(), "parent_id")

    /**
     * Test searchBlocking delegates to search and returns results
     */
    @Test
    fun `searchBlocking delegates to search and returns results`() {
        val expected = listOf(makeFile())
        coEvery { mockClient.search(any()) } returns expected

        val result = mockClient.searchBlocking("test query")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.search("test query") }
    }

    /**
     * Test searchBlocking returns empty list when no results
     */
    @Test
    fun `searchBlocking returns empty list when no results`() {
        coEvery { mockClient.search(any()) } returns emptyList()

        val result = mockClient.searchBlocking("no results")

        assertEquals(emptyList<OmhStorageEntity>(), result)
    }

    /**
     * Test createFileWithExtensionBlocking delegates to createFileWithExtension
     */
    @Test
    fun `createFileWithExtensionBlocking delegates to createFileWithExtension`() {
        val expected = makeFile()
        coEvery { mockClient.createFileWithExtension(any(), any(), any()) } returns expected

        val result = mockClient.createFileWithExtensionBlocking("report", "pdf", "parent_id")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.createFileWithExtension("report", "pdf", "parent_id") }
    }

    /**
     * Test createFileWithExtensionBlocking returns null when underlying returns null
     */
    @Test
    fun `createFileWithExtensionBlocking returns null when underlying returns null`() {
        coEvery { mockClient.createFileWithExtension(any(), any(), any()) } returns null

        assertNull(mockClient.createFileWithExtensionBlocking("file", "txt", "parent_id"))
    }

    /**
     * Test createFolderBlocking delegates to createFolder
     */
    @Test
    fun `createFolderBlocking delegates to createFolder`() {
        val expected = makeFolder()
        coEvery { mockClient.createFolder(any(), any()) } returns expected

        val result = mockClient.createFolderBlocking("docs", "root_id")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.createFolder("docs", "root_id") }
    }

    /**
     * Test createFolderBlocking returns null when underlying returns null
     */
    @Test
    fun `createFolderBlocking returns null when underlying returns null`() {
        coEvery { mockClient.createFolder(any(), any()) } returns null

        assertNull(mockClient.createFolderBlocking("empty", "root_id"))
    }

    /**
     * Test deleteFileBlocking delegates to deleteFile
     */
    @Test
    fun `deleteFileBlocking delegates to deleteFile`() {
        coEvery { mockClient.deleteFile(any()) } returns Unit

        mockClient.deleteFileBlocking("file_to_delete")

        coVerify(exactly = 1) { mockClient.deleteFile("file_to_delete") }
    }

    /**
     * Test permanentlyDeleteFileBlocking delegates to permanentlyDeleteFile
     */
    @Test
    fun `permanentlyDeleteFileBlocking delegates to permanentlyDeleteFile`() {
        coEvery { mockClient.permanentlyDeleteFile(any()) } returns Unit

        mockClient.permanentlyDeleteFileBlocking("permanent_delete_id")

        coVerify(exactly = 1) { mockClient.permanentlyDeleteFile("permanent_delete_id") }
    }

    /**
     * Test downloadFileBlocking delegates to downloadFile and returns stream
     */
    @Test
    fun `downloadFileBlocking delegates to downloadFile and returns stream`() {
        val expected = ByteArrayOutputStream().also { it.write("hello cloud".toByteArray()) }
        coEvery { mockClient.downloadFile(any()) } returns expected

        val result = mockClient.downloadFileBlocking("download_id")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.downloadFile("download_id") }
    }

    /**
     * Test downloadFileVersionBlocking delegates to downloadFileVersion
     */
    @Test
    fun `downloadFileVersionBlocking delegates to downloadFileVersion`() {
        val expected = ByteArrayOutputStream().also { it.write("v2 content".toByteArray()) }
        coEvery { mockClient.downloadFileVersion(any(), any()) } returns expected

        val result = mockClient.downloadFileVersionBlocking("file_id", "v2")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.downloadFileVersion("file_id", "v2") }
    }

    /**
     * Test getFileMetadataBlocking delegates to getFileMetadata and returns metadata
     */
    @Test
    fun `getFileMetadataBlocking delegates to getFileMetadata and returns metadata`() {
        val entity = makeFile()
        val expected = OmhStorageMetadata(entity, Unit)
        coEvery { mockClient.getFileMetadata(any()) } returns expected

        val result = mockClient.getFileMetadataBlocking("meta_id")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.getFileMetadata("meta_id") }
    }

    /**
     * Test getFileMetadataBlocking returns null when file not found
     */
    @Test
    fun `getFileMetadataBlocking returns null when file not found`() {
        coEvery { mockClient.getFileMetadata(any()) } returns null

        assertNull(mockClient.getFileMetadataBlocking("unknown_id"))
    }

    /**
     * Test resolvePathBlocking delegates to resolvePath and returns entity
     */
    @Test
    fun `resolvePathBlocking delegates to resolvePath and returns entity`() {
        val expected = makeFolder()
        coEvery { mockClient.resolvePath(any()) } returns expected

        val result = mockClient.resolvePathBlocking("/documents/reports")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.resolvePath("/documents/reports") }
    }

    /**
     * Test resolvePathBlocking returns null when path not found
     */
    @Test
    fun `resolvePathBlocking returns null when path not found`() {
        coEvery { mockClient.resolvePath(any()) } returns null

        assertNull(mockClient.resolvePathBlocking("/nonexistent/path"))
    }

    /**
     * Test folderSizeBlocking delegates to folderSize extension function
     */
    @Test
    fun `folderSizeBlocking delegates to folderSize extension function`() {
        coEvery { mockClient.folderSize(any()) } returns 8192L

        val result = mockClient.folderSizeBlocking("folder_123")

        assertEquals(8192L, result)
        coVerify(exactly = 1) { mockClient.folderSize("folder_123") }
    }

    /**
     * Test folderSizeBlocking returns zero for empty folder
     */
    @Test
    fun `folderSizeBlocking returns zero for empty folder`() {
        coEvery { mockClient.folderSize(any()) } returns 0L

        assertEquals(0L, mockClient.folderSizeBlocking("empty_folder"))
    }

    /**
     * Test getStorageUsageBlocking delegates to getStorageUsage
     */
    @Test
    fun `getStorageUsageBlocking delegates to getStorageUsage`() {
        coEvery { mockClient.getStorageUsage() } returns 3_500_000_000L

        val result = mockClient.getStorageUsageBlocking()

        assertEquals(3_500_000_000L, result)
        coVerify(exactly = 1) { mockClient.getStorageUsage() }
    }

    /**
     * Test getStorageQuotaBlocking delegates to getStorageQuota
     */
    @Test
    fun `getStorageQuotaBlocking delegates to getStorageQuota`() {
        coEvery { mockClient.getStorageQuota() } returns 15_000_000_000L

        val result = mockClient.getStorageQuotaBlocking()

        assertEquals(15_000_000_000L, result)
        coVerify(exactly = 1) { mockClient.getStorageQuota() }
    }

    /**
     * Test uploadFileBlocking delegates to uploadFile and returns uploaded entity
     */
    @Test
    fun `uploadFileBlocking delegates to uploadFile and returns uploaded entity`() {
        val localFile = mockk<File>()
        val expected = makeFile(id = "uploaded_id", name = "photo.jpg")
        coEvery { mockClient.uploadFile(any(), any()) } returns expected

        val result = mockClient.uploadFileBlocking(localFile, "parent_id")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.uploadFile(localFile, "parent_id") }
    }

    /**
     * Test uploadFileBlocking returns null when upload fails
     */
    @Test
    fun `uploadFileBlocking returns null when upload fails`() {
        val localFile = mockk<File>()
        coEvery { mockClient.uploadFile(any(), any()) } returns null

        assertNull(mockClient.uploadFileBlocking(localFile, "parent_id"))
    }

    /**
     * Test renameBlocking delegates to rename and returns renamed entity
     */
    @Test
    fun `renameBlocking delegates to rename and returns renamed entity`() {
        val expected = makeFile(id = "file_id", name = "renamed.txt")
        coEvery { mockClient.rename(any(), any()) } returns expected

        val result = mockClient.renameBlocking("file_id", "renamed.txt")

        assertEquals(expected, result)
        coVerify(exactly = 1) { mockClient.rename("file_id", "renamed.txt") }
    }

    /**
     * Test renameBlocking returns null when rename returns null
     */
    @Test
    fun `renameBlocking returns null when rename returns null`() {
        coEvery { mockClient.rename(any(), any()) } returns null

        assertNull(mockClient.renameBlocking("file_id", "new_name.txt"))
    }
}
