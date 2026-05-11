package com.amaze.filemanager.filesystem

import android.content.Context
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException
import com.amaze.filemanager.filesystem.HybridFile.LOG
import com.amaze.filemanager.filesystem.cloud.CloudUtil
import com.amaze.filemanager.utils.omh.OMHClientHelper
import com.amaze.filemanager.utils.omh.createFolderBlocking
import com.amaze.filemanager.utils.omh.deleteFileBlocking
import com.amaze.filemanager.utils.omh.downloadFileBlocking
import com.amaze.filemanager.utils.omh.folderSizeBlocking
import com.amaze.filemanager.utils.omh.getFileMetadataBlocking
import com.amaze.filemanager.utils.omh.getStorageQuotaBlocking
import com.amaze.filemanager.utils.omh.getStorageUsageBlocking
import com.amaze.filemanager.utils.omh.retryOnUnauthorizedBlocking
import com.openmobilehub.android.storage.core.OmhStorageClient
import com.openmobilehub.android.storage.core.model.OmhStorageEntity
import com.openmobilehub.android.storage.core.model.OmhStorageMetadata
import java.io.InputStream

private fun <T> HybridFile.withStorageClient(
    onFailure: (Throwable) -> Unit = {
        LOG.error("Error in OmhStorageClient action", it)
        throw CloudPluginException(it)
    },
    action: (storageClient: OmhStorageClient) -> T,
): T? {
    val storageClient = OMHClientHelper.getStorageClient(mode)
    if (storageClient == null) {
        LOG.error("Storage client is null for mode: $mode")
        return null
    }
    return runCatching {
        retryOnUnauthorizedBlocking(mode, AppConfig.getInstance().cloudAuthTrigger) {
            action(storageClient)
        }
    }.onFailure(onFailure).getOrNull()
}

/**
 * Extension function to download a cloud file for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.downloadCloudFile(): InputStream? =
    withStorageClient { storageClient ->
        val tmpFilename = "$name.amaze_tmp"
        storageClient.downloadFileBlocking(cloudFileId).writeTo(
            AppConfig.getInstance().openFileOutput(tmpFilename, Context.MODE_PRIVATE),
        )
        AppConfig.getInstance().openFileInput(tmpFilename)
    }

/**
 * Extension function to delete a cloud file for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.deleteCloudFile() {
    withStorageClient { storageClient ->
        storageClient.deleteFileBlocking(cloudFileId)
    }
}

/**
 * Extension function to fetch the last modified time of a cloud file for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.getCloudLastModified(): Long = getCloudFileMetadata()?.entity?.modifiedTime?.time ?: 0L

/**
 * Extension function to fetch the size of a cloud file for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.getCloudFileSize(): Long =
    when (val entity = getCloudFileMetadata()?.entity) {
        is OmhStorageEntity.OmhFile -> entity.size?.toLong() ?: 0L
        else -> 0L
    }

/**
 * Extension function to fetch the size of a cloud folder for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.getCloudFolderSize(): Long =
    when (getCloudFileMetadata()?.entity) {
        is OmhStorageEntity.OmhFolder -> {
            withStorageClient { storageClient ->
                storageClient.folderSizeBlocking(cloudFileId)
            } ?: 0L
        }
        else -> 0L
    }

/**
 * Extension function to fetch the total space of the cloud storage for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.getCloudTotalSpace(): Long =
    withStorageClient { storageClient ->
        storageClient.getStorageQuotaBlocking()
    } ?: 0L

/**
 * Extension function to fetch the usable space of the cloud storage for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.getCloudUsableSpace(): Long =
    withStorageClient { storageClient ->
        val quota = storageClient.getStorageQuotaBlocking()
        val used = storageClient.getStorageUsageBlocking()
        quota - used
    } ?: 0L

/**
 * Extension function to fetch cloud file metadata for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
@Throws(CloudPluginException::class)
fun HybridFile.getCloudFileMetadata(): OmhStorageMetadata? {
    return withStorageClient { storageClient ->
        storageClient.getFileMetadataBlocking(cloudFileId)
    }
}

/**
 * Extension function to create a cloud folder for a [HybridFile] instance.
 *
 * Method runs blockingly.
 */
fun HybridFile.createCloudFolder() {
    withStorageClient { storageClient ->
        storageClient.createFolderBlocking(
            CloudUtil.stripCloudPath(mode, path),
            cloudFileId,
        )
    }
}
