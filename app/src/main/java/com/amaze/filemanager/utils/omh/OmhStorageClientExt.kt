package com.amaze.filemanager.utils.omh

import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.model.OmhStorageEntity
import com.openmobilehub.android.storage.core.model.OmhStorageMetadata
import com.openmobilehub.android.storage.core.utils.folderSize
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream

/**
 * Blocking version of [OmhStorageClient.search].
 */
fun OmhStorageClient.searchBlocking(query: String): List<OmhStorageEntity> = runBlocking { search(query) }

/**
 * Blocking version of [OmhStorageClient.createFileWithExtension].
 */
fun OmhStorageClient.createFileWithExtensionBlocking(
    name: String,
    extension: String,
    parentId: String,
): OmhStorageEntity? =
    runBlocking {
        createFileWithExtension(name, extension, parentId)
    }

/**
 * Blocking version of [OmhStorageClient.createFolder].
 */
fun OmhStorageClient.createFolderBlocking(
    name: String,
    parentId: String,
): OmhStorageEntity? =
    runBlocking {
        createFolder(name, parentId)
    }

/**
 * Blocking version of [OmhStorageClient.deleteFile].
 */
fun OmhStorageClient.deleteFileBlocking(fileId: String) =
    runBlocking {
        deleteFile(fileId)
    }

/**
 * Blocking version of [OmhStorageClient.permanentlyDeleteFile].
 */
fun OmhStorageClient.permanentlyDeleteFileBlocking(id: String) =
    runBlocking {
        permanentlyDeleteFile(id)
    }

/**
 * Blocking version of [OmhStorageClient.downloadFile].
 */
fun OmhStorageClient.downloadFileBlocking(fileId: String): ByteArrayOutputStream =
    runBlocking {
        downloadFile(fileId)
    }

/**
 * Blocking version of [OmhStorageClient.downloadFileVersion].
 */
fun OmhStorageClient.downloadFileVersionBlocking(
    fileId: String,
    versionId: String,
): ByteArrayOutputStream =
    runBlocking {
        downloadFileVersion(fileId, versionId)
    }

/**
 * Blocking version of [OmhStorageClient.getFileMetadata].
 */
fun OmhStorageClient.getFileMetadataBlocking(fileId: String): OmhStorageMetadata? =
    runBlocking {
        getFileMetadata(fileId)
    }

/**
 * Blocking version of [OmhStorageClient.resolvePath].
 */
fun OmhStorageClient.resolvePathBlocking(path: String): OmhStorageEntity? =
    runBlocking {
        resolvePath(path)
    }

/**
 * Blocking version of [OmhStorageClient.folderSize].
 */
fun OmhStorageClient.folderSizeBlocking(folderId: String): Long =
    runBlocking {
        folderSize(folderId)
    }

/**
 * Blocking version of [OmhStorageClient.getStorageUsage].
 */
fun OmhStorageClient.getStorageUsageBlocking(): Long =
    runBlocking {
        getStorageUsage()
    }

/**
 *  Blocking version of [OmhStorageClient.getStorageQuota].
 */
fun OmhStorageClient.getStorageQuotaBlocking(): Long =
    runBlocking {
        getStorageQuota()
    }
