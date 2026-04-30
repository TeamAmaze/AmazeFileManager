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
package com.amaze.filemanager.filesystem.cloud

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.arch.core.util.Function
import androidx.core.net.toUri
import com.amaze.filemanager.R
import com.amaze.filemanager.database.CloudContract
import com.amaze.filemanager.fileoperations.exceptions.CloudPluginException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.fileoperations.filesystem.cloud.CloudStreamer
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.cloud.CloudUtil.getCloudFiles
import com.amaze.filemanager.ui.icons.MimeTypes
import com.amaze.filemanager.utils.OTGUtil.getDocumentFile
import com.amaze.filemanager.utils.omh.AuthTrigger
import com.amaze.filemanager.utils.omh.OMHClientHelper.getStorageClient
import com.amaze.filemanager.utils.omh.retryOnUnauthorized
import com.openmobilehub.android.storage.core.ThumbnailSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.ProtocolException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by vishal on 19/4/17.
 *
 *
 * Class provides helper methods for cloud utilities
 */
object CloudUtil {
    @JvmStatic
    private val LOG: Logger = LoggerFactory.getLogger(CloudUtil::class.java)

    /**
     * Blocking version of [getCloudFiles].
     * Collects all files first, then passes them to the callback to avoid
     * ConcurrentModificationException.
     */
    @JvmStatic
    @Suppress("TooGenericExceptionCaught")
    fun getCloudFilesBlocking(
        folderId: String,
        path: String,
        openMode: OpenMode,
        authTrigger: AuthTrigger,
        fileFoundCallback: Function<HybridFileParcelable, Unit>,
    ) {
        val collectedFiles = CopyOnWriteArrayList<HybridFileParcelable>()

        runBlocking(Dispatchers.IO.limitedParallelism(5)) {
            try {
                getCloudFiles(folderId, path, openMode, authTrigger) { file ->
                    collectedFiles.add(file)
                }
            } catch (e: Exception) {
                throw CloudPluginException(e)
            }
        }

        // Now safely pass all collected files to the callback on the calling thread
        for (file in collectedFiles) {
            fileFoundCallback.apply(file)
        }
    }

    /**
     * Helper method to get list of files from cloud storage.
     */
    @JvmStatic
    @Suppress("TooGenericExceptionCaught")
    suspend fun getCloudFiles(
        folderId: String,
        path: String,
        openMode: OpenMode,
        authTrigger: AuthTrigger,
        fileFoundCallback: Function<HybridFileParcelable, Unit>,
    ) {
        retryOnUnauthorized<Unit>(
            openMode = openMode,
            trigger = authTrigger,
        ) {
            try {
                getStorageClient(openMode)?.let { storageClient ->
                    for (omhStorageEntity in storageClient.listFiles(folderId.ifEmpty { storageClient.rootFolder })) {
                        val baseFile = HybridFileParcelable(path, openMode, omhStorageEntity)
                        baseFile.cloudFileId = omhStorageEntity.id
                        fileFoundCallback.apply(baseFile)
                    }
                }
            } catch (e: ProtocolException) {
                LOG.warn("Protocol exception while getting cloud files: ", e)
                throw CloudPluginException(e)
            } catch (e: Exception) {
                LOG.warn("failed to get cloud files", e)
                throw CloudPluginException(e)
            }
        }
    }

    /** Strips down the cloud path to remove any prefix  */
    @JvmStatic
    fun stripCloudPath(
        openMode: OpenMode,
        path: String,
    ): String {
        val prefix =
            when (openMode) {
                OpenMode.DROPBOX -> CloudContract.CLOUD_PREFIX_DROPBOX
                OpenMode.BOX -> CloudContract.CLOUD_PREFIX_BOX
                OpenMode.ONEDRIVE -> CloudContract.CLOUD_PREFIX_ONE_DRIVE
                OpenMode.GDRIVE -> CloudContract.CLOUD_PREFIX_GOOGLE_DRIVE
                else -> return path
            }
        if (path == "$prefix/") {
            // we're at root, just replace the prefix
            return ""
        } else {
            // we're not at root, replace prefix + /
            // handle when paths are in format gdrive:/Documents // TODO: normalize drive paths
            val pathReplaced = path.replace("$prefix/", "")
            if (pathReplaced == path) {
                // we convert gdrive:/Documents to /Documents
                return path.replace(prefix.substring(0, prefix.length - 1), "")
            }
            return pathReplaced
        }
    }

    /**
     * Attempt to launch a cloud file using [CloudStreamer] and an implicit intent
     */
    @JvmStatic
    @Suppress("TooGenericExceptionCaught")
    fun launchCloud(
        baseFile: HybridFile,
        serviceType: OpenMode,
        activity: Activity,
    ) {
        val streamer = CloudStreamer.getInstance()

        Thread {
            try {
                streamer.setStreamSrc(
                    baseFile.getInputStream(activity),
                    baseFile.getName(activity),
                    baseFile.length(activity),
                )
                activity.runOnUiThread {
                    try {
                        val file =
                            File(
                                stripCloudPath(serviceType, baseFile.path).toUri()
                                    .path,
                            )
                        val uri =
                            (CloudStreamer.URL + Uri.fromFile(file).encodedPath).toUri()
                        val i = Intent(Intent.ACTION_VIEW)
                        i.setDataAndType(
                            uri,
                            MimeTypes.getMimeType(
                                baseFile.path,
                                baseFile.isDirectory(activity),
                            ),
                        )
                        val packageManager = activity.packageManager
                        val resInfos = packageManager.queryIntentActivities(i, 0)
                        if (resInfos.isNotEmpty()) {
                            activity.startActivity(i)
                        } else {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.smb_launch_error),
                                Toast.LENGTH_SHORT,
                            )
                                .show()
                        }
                    } catch (e: ActivityNotFoundException) {
                        LOG.warn("failed to launch cloud file in activity", e)
                    }
                }
            } catch (e: Exception) {
                LOG.warn("failed to launch cloud file", e)
            }
        }
            .start()
    }

    /**
     * Get an input stream for thumbnail for a given path.
     *
     * @param cloudFileId If non-null (OMH cloud storage), the file ID is used directly to fetch
     *   the thumbnail, avoiding an extra [OmhStorageClient.resolvePath] network call.
     */
    @Suppress("LabeledExpression")
    fun getThumbnailInputStreamForCloud(
        context: Context,
        path: String?,
        cloudFileId: String? = null,
    ): InputStream? {
        var inputStream: InputStream?
        val hybridFile = HybridFile(OpenMode.UNKNOWN, path)
        hybridFile.generateMode(context)

        when (hybridFile.mode) {
            OpenMode.SFTP -> inputStream = hybridFile.getInputStream(context)
            OpenMode.FTP -> // Until we find a way to properly handle threading issues with thread unsafe FTPClient,
                // we refrain from loading any files via FTP as file thumbnail. - TranceLove
                inputStream = null

            OpenMode.SMB ->
                try {
                    inputStream = hybridFile.smbFile.inputStream
                } catch (e: IOException) {
                    inputStream = null
                    LOG.warn("failed to get inputstream for smb file for thumbnail", e)
                }

            OpenMode.OTG -> {
                val contentResolver = context.contentResolver
                val documentSourceFile =
                    getDocumentFile(hybridFile.path, context, false)
                try {
                    inputStream = contentResolver.openInputStream(documentSourceFile!!.uri)
                } catch (e: FileNotFoundException) {
                    LOG.warn("failed to get input stream for otg for thumbnail", e)
                    inputStream = null
                }
            }

            OpenMode.DROPBOX, OpenMode.BOX, OpenMode.GDRIVE, OpenMode.ONEDRIVE -> {
                val storageClient = getStorageClient(openMode = hybridFile.mode)
                if (storageClient == null) {
                    LOG.warn("failed to get input stream for cloud files for thumbnail - no storage client")
                    inputStream = null
                } else {
                    inputStream =
                        ByteArrayInputStream(
                            runBlocking {
                                // Use the cloudFileId directly if it was provided (avoids an
                                // extra resolvePath() network round-trip per thumbnail).
                                val resolvedFileId =
                                    if (!cloudFileId.isNullOrEmpty()) {
                                        cloudFileId
                                    } else {
                                        val strippedPath = stripCloudPath(hybridFile.mode, path!!)
                                        val storageEntity =
                                            storageClient.resolvePath(
                                                if (strippedPath.startsWith("/")) strippedPath else "/$strippedPath",
                                            ) ?: return@runBlocking ByteArray(0)
                                        storageEntity.id
                                    }
                                storageClient.getFileThumbnail(
                                    resolvedFileId,
                                    ThumbnailSize.MEDIUM,
                                ).toByteArray()
                            },
                        )
                }
            }

            else ->
                try {
                    inputStream = FileInputStream(hybridFile.path)
                } catch (e: FileNotFoundException) {
                    inputStream = null
                    LOG.warn("failed to get inputstream for cloud files for thumbnail", e)
                }
        }
        return inputStream
    }
}
