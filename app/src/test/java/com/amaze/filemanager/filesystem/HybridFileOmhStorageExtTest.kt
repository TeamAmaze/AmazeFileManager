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
package com.amaze.filemanager.filesystem

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.cloud.CloudUtil
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.utils.omh.AuthTrigger
import com.amaze.filemanager.utils.omh.OMHClientHelper
import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.model.OmhStorageEntity
import com.openmobilehub.android.storage.core.model.OmhStorageMetadata
import com.openmobilehub.android.storage.core.utils.folderSize
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Unit tests for [HybridFileOmhStorageExt] extension functions.
 *
 * Uses Robolectric so that [AppConfig.getInstance()] is available.
 *
 * Rather than using `mockkObject(OMHClientHelper)` — which triggers MockK's JvmAutoHinter and
 * therefore calls the real `getStorageClient(null)` on certain Robolectric SDK sandboxes causing
 * a [com.amaze.filemanager.fileoperations.exceptions.CloudPluginException] — we inject a mock
 * [OmhStorageClient] directly into `OMHClientHelper.storageClients` via reflection. This is safe
 * because `storageClients` is a plain [java.util.EnumMap] with no side effects on write.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class],
    sdk = [Build.VERSION_CODES.O, Build.VERSION_CODES.P, Build.VERSION_CODES.R],
)
@Suppress("TooManyFunctions", "LargeClass", "StringLiteralDuplication")
class HybridFileOmhStorageExtTest {
    private lateinit var mockStorageClient: OmhStorageClient

    /**
     * Setup before tests.
     */
    @Before
    fun setUp() {
        mockStorageClient = mockk()
        // Required to mock the folderSize suspend extension function from omh-storage library
        mockkStatic("com.openmobilehub.android.storage.core.utils.OmhStorageClientExtensionsKt")
        // Inject mock into OMHClientHelper's private storageClients map for all cloud modes.
        // This avoids using mockkObject, which triggers JvmAutoHinter and calls the real
        // getStorageClient(null) — causing CloudPluginException on some Robolectric sandboxes.
        injectStorageClient(mockStorageClient)
        // Provide a no-op auth trigger so retryOnUnauthorizedBlocking does not NPE
        AppConfig.getInstance().setCloudAuthTrigger(
            object : AuthTrigger {
                override fun triggerAuthBlocking(openMode: OpenMode): Boolean = true
            },
        )
    }

    /**
     * Cleanup after tests.
     */
    @After
    fun tearDown() {
        clearInjectedStorageClients()
        unmockkAll()
    }

    @Suppress("UNCHECKED_CAST")
    private fun storageClientsMap(): MutableMap<OpenMode, OmhStorageClient> {
        val field = OMHClientHelper.javaClass.getDeclaredField("storageClients")
        field.isAccessible = true
        return field.get(OMHClientHelper) as MutableMap<OpenMode, OmhStorageClient>
    }

    private fun injectStorageClient(client: OmhStorageClient) {
        val map = storageClientsMap()
        for (mode in listOf(OpenMode.DROPBOX, OpenMode.BOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE)) {
            map[mode] = client
        }
    }

    private fun clearInjectedStorageClients() {
        storageClientsMap().clear()
    }

    /**
     * Build a [HybridFile] for cloud use, setting [cloudFileId] directly (same-package access).
     *
     * When [name] is non-null the 4-arg constructor is used, which appends `name` to [path].
     * Pass `name = null` (or use the 2-arg constructor path below) when the full cloud path is
     * already encoded in [path] and must not be modified — e.g. for folder-creation tests.
     */
    private fun makeHybridFile(
        mode: OpenMode = OpenMode.DROPBOX,
        path: String = "dropbox:/test/file.txt",
        cloudFileId: String = "cloud_file_123",
        name: String? = "file.txt",
    ): HybridFile {
        val hf =
            if (name != null) {
                HybridFile(mode, path, name, false)
            } else {
                HybridFile(mode, path)
            }
        hf.cloudFileId = cloudFileId
        return hf
    }

    private fun makeFileEntity(
        id: String = "cloud_file_123",
        name: String = "file.txt",
        modifiedTime: Date = Date(1_700_000_000_000L),
        size: Int = 2048,
    ): OmhStorageEntity.OmhFile = OmhStorageEntity.OmhFile(id, name, Date(), modifiedTime, "parent_id", "text/plain", "txt", size)

    private fun makeFolderEntity(
        id: String = "cloud_folder_456",
        name: String = "folder",
    ): OmhStorageEntity.OmhFolder = OmhStorageEntity.OmhFolder(id, name, Date(), Date(), "parent_id")

    /**
     * Test deleteCloudFile calls deleteFile with the correct cloudFileId
     */
    @Test
    fun `deleteCloudFile calls deleteFile with the correct cloudFileId`() {
        coEvery { mockStorageClient.deleteFile(any()) } returns Unit

        val hf = makeHybridFile(cloudFileId = "my_file_id")
        hf.deleteCloudFile()

        coVerify(exactly = 1) { mockStorageClient.deleteFile("my_file_id") }
    }

    /**
     * Test getCloudFileMetadata returns metadata from storage client
     */
    @Test
    fun `getCloudFileMetadata returns metadata from storage client`() {
        val entity = makeFileEntity()
        val metadata = OmhStorageMetadata(entity, Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns metadata

        val hf = makeHybridFile(cloudFileId = "cloud_file_123")
        val result = hf.getCloudFileMetadata()

        assertEquals(metadata, result)
        coVerify(exactly = 1) { mockStorageClient.getFileMetadata("cloud_file_123") }
    }

    /**
     * Test getCloudFileMetadata returns null when getFileMetadata returns null
     */
    @Test
    fun `getCloudFileMetadata returns null when getFileMetadata returns null`() {
        coEvery { mockStorageClient.getFileMetadata(any()) } returns null

        assertNull(makeHybridFile().getCloudFileMetadata())
    }

    /**
     * Test getCloudLastModified returns modifiedTime from file entity
     */
    @Test
    fun `getCloudLastModified returns modifiedTime from file entity`() {
        val modifiedDate = Date(1_700_000_000_000L)
        val metadata = OmhStorageMetadata(makeFileEntity(modifiedTime = modifiedDate), Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns metadata

        assertEquals(1_700_000_000_000L, makeHybridFile().getCloudLastModified())
    }

    /**
     * Test getCloudLastModified returns 0 when metadata is null
     */
    @Test
    fun `getCloudLastModified returns 0 when metadata is null`() {
        coEvery { mockStorageClient.getFileMetadata(any()) } returns null

        assertEquals(0L, makeHybridFile().getCloudLastModified())
    }

    /**
     * Test getCloudFileSize returns file size for OmhFile entity
     */
    @Test
    fun `getCloudFileSize returns file size for OmhFile entity`() {
        val metadata = OmhStorageMetadata(makeFileEntity(size = 4096), Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns metadata

        assertEquals(4096L, makeHybridFile().getCloudFileSize())
    }

    /**
     * Test getCloudFileSize returns 0 for OmhFolder entity
     */
    @Test
    fun `getCloudFileSize returns 0 for OmhFolder entity`() {
        val metadata = OmhStorageMetadata(makeFolderEntity(), Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns metadata

        assertEquals(0L, makeHybridFile().getCloudFileSize())
    }

    /**
     * Test getCloudFileSize returns 0 when metadata is null
     */
    @Test
    fun `getCloudFileSize returns 0 when metadata is null`() {
        coEvery { mockStorageClient.getFileMetadata(any()) } returns null

        assertEquals(0L, makeHybridFile().getCloudFileSize())
    }

    /**
     * Test getCloudFileSize returns 0 when OmhFile size is null
     */
    @Test
    fun `getCloudFileSize returns 0 when OmhFile size is null`() {
        // Use a mock to return null from the nullable size property
        val entity = mockk<OmhStorageEntity.OmhFile>()
        every { entity.size } returns null
        val metadata = OmhStorageMetadata(entity, Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns metadata

        assertEquals(0L, makeHybridFile().getCloudFileSize())
    }

    /**
     * Test getCloudFolderSize returns size for OmhFolder entity
     */
    @Test
    fun `getCloudFolderSize returns size for OmhFolder entity`() {
        val folderId = "folder_abc"
        val folderMetadata = OmhStorageMetadata(makeFolderEntity(id = folderId), Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns folderMetadata
        coEvery { mockStorageClient.folderSize(any()) } returns 16_384L

        val hf = makeHybridFile(cloudFileId = folderId)
        assertEquals(16_384L, hf.getCloudFolderSize())
        coVerify { mockStorageClient.folderSize(folderId) }
    }

    /**
     * Test getCloudFolderSize returns 0 for OmhFile entity
     */
    @Test
    fun `getCloudFolderSize returns 0 for OmhFile entity`() {
        val fileMetadata = OmhStorageMetadata(makeFileEntity(), Unit)
        coEvery { mockStorageClient.getFileMetadata(any()) } returns fileMetadata

        // folderSize should NOT be called for a file
        assertEquals(0L, makeHybridFile().getCloudFolderSize())
        coVerify(exactly = 0) { mockStorageClient.folderSize(any()) }
    }

    /**
     * Test getCloudFolderSize returns 0 when metadata is null
     */
    @Test
    fun `getCloudFolderSize returns 0 when metadata is null`() {
        coEvery { mockStorageClient.getFileMetadata(any()) } returns null

        assertEquals(0L, makeHybridFile().getCloudFolderSize())
    }

    /**
     * Test getCloudTotalSpace returns storage quota
     */
    @Test
    fun `getCloudTotalSpace returns storage quota`() {
        coEvery { mockStorageClient.getStorageQuota() } returns 15_000_000_000L

        assertEquals(15_000_000_000L, makeHybridFile().getCloudTotalSpace())
        coVerify(exactly = 1) { mockStorageClient.getStorageQuota() }
    }

    /**
     * Test getCloudUsableSpace returns quota minus usage
     */
    @Test
    fun `getCloudUsableSpace returns quota minus usage`() {
        coEvery { mockStorageClient.getStorageQuota() } returns 10_000L
        coEvery { mockStorageClient.getStorageUsage() } returns 3_000L

        assertEquals(7_000L, makeHybridFile().getCloudUsableSpace())
    }

    /**
     * Test createCloudFolder calls createFolder with stripped path and cloudFileId as parentId
     */
    @Test
    fun `createCloudFolder calls createFolder with stripped path and cloudFileId as parentId`() {
        val mode = OpenMode.DROPBOX
        // "dropbox:/docs/reports" is the full folder path; pass name=null so the 2-arg
        // HybridFile constructor is used and nothing extra is appended to the path.
        val path = "dropbox:/docs/reports"
        val parentId = "parent_folder_id"
        coEvery { mockStorageClient.createFolder(any(), any()) } returns makeFolderEntity()

        val hf = makeHybridFile(mode = mode, path = path, cloudFileId = parentId, name = null)
        hf.createCloudFolder()

        val expectedStrippedPath = CloudUtil.stripCloudPath(mode, path)
        coVerify(exactly = 1) { mockStorageClient.createFolder(expectedStrippedPath, parentId) }
    }

    /**
     * Test deleteCloudFile works for all cloud provider modes
     */
    @Test
    fun `deleteCloudFile works for all cloud provider modes`() {
        coEvery { mockStorageClient.deleteFile(any()) } returns Unit

        for (mode in listOf(OpenMode.DROPBOX, OpenMode.BOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE)) {
            val prefix =
                when (mode) {
                    OpenMode.DROPBOX -> "dropbox:/"
                    OpenMode.BOX -> "box:/"
                    OpenMode.GDRIVE -> "gdrive:/"
                    OpenMode.ONEDRIVE -> "onedrive:/"
                    else -> continue
                }
            val hf = makeHybridFile(mode = mode, path = "${prefix}test/file.txt")
            hf.deleteCloudFile()
        }

        // Each mode should have triggered exactly one deleteFile call
        coVerify(exactly = 4) { mockStorageClient.deleteFile(any()) }
    }
}
