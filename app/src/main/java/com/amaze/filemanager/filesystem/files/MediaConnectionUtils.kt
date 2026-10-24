/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.amaze.filemanager.filesystem.HybridFile
import org.slf4j.LoggerFactory

object MediaConnectionUtils {
    private val LOG = LoggerFactory.getLogger(MediaConnectionUtils::class.java)

    /**
     * Invokes MediaScannerConnection#scanFile for the given files
     *
     * @param context the context
     * @param hybridFiles files to be scanned
     */
    @JvmStatic
    fun scanFiles(
        context: Context,
        hybridFiles: Array<HybridFile>,
    ) {
        val paths: Array<String> =
            hybridFiles.map {
                it.path
            }.toTypedArray()

        MediaScannerConnection.scanFile(
            context,
            paths,
            null,
        ) { path: String, _: Uri? ->
            LOG.info("MediaConnectionUtils#scanFile finished scanning path$path")
        }
    }

    /**
     * Invokes MediaScannerConnection#scanFile for a given file
     *
     * @param context the context
     * @param filePath the path of the file to be scanned
     */
    @JvmStatic
    fun scanFile(
        context: Context,
        filePath: String,
    ) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            null,
        ) { path: String, _: Uri? ->
            LOG.info("MediaConnectionUtils#scanFile finished scanning path$path")
        }
    }

    /**
     * Invokes MediaScannerConnection#scanFile for the given file.
     *
     * @param context the context
     * @param path the file path to be scanned
     * @param mimeType the mime type of the file. Optional.
     *
     */
    @JvmStatic
    fun scanFileByFileSystemPathAndMimeType(
        context: Context,
        path: String,
        mimeType: String? = null,
        callback: MediaScannerConnection.OnScanCompletedListener? = null,
    ) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path),
            mimeType?.let {
                arrayOf(it)
            },
        ) { scannedPath: String, uri: Uri? ->
            LOG.info("Finished scanning path $scannedPath")
            callback?.onScanCompleted(scannedPath, uri)
        }
    }
}
