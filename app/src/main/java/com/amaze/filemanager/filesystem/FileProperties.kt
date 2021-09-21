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

package com.amaze.filemanager.filesystem

import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.CloudHandler
import com.amaze.filemanager.filesystem.DeleteOperation.deleteFile
import com.amaze.filemanager.filesystem.ExternalSdCardOperation.isOnExtSdCard
import com.amaze.filemanager.filesystem.smb.CifsContexts
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool
import com.amaze.filemanager.utils.OTGUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern

// TODO check if these can be done with just File methods
// TODO make all of these methods File extensions
object FileProperties {

    private const val STORAGE_PRIMARY = "primary"
    private const val COM_ANDROID_EXTERNALSTORAGE_DOCUMENTS =
        "com.android.externalstorage.documents"

    val EXCLUDED_DIRS = arrayOf(
        File(Environment.getExternalStorageDirectory(), "Android/data").absolutePath,
        File(Environment.getExternalStorageDirectory(), "Android/obb").absolutePath
    )

    /**
     * Check if a file is readable.
     *
     * @param file The file
     * @return true if the file is reabable.
     */
    @JvmStatic
    fun isReadable(file: File?): Boolean {
        if (file == null) return false
        if (!file.exists()) return false
        return try {
            file.canRead()
        } catch (e: SecurityException) {
            return false
        }
    }

    /**
     * Check if a file is writable. Detects write issues on external SD card.
     *
     * @param file The file
     * @return true if the file is writable.
     */
    @JvmStatic
    fun isWritable(file: File?): Boolean {
        if (file == null) return false
        val isExisting = file.exists()
        try {
            val output = FileOutputStream(file, true)
            try {
                output.close()
            } catch (e: IOException) {
                e.printStackTrace()
                // do nothing.
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        }
        val result = file.canWrite()

        // Ensure that file is not created during this process.
        if (!isExisting) {
            file.delete()
        }
        return result
    }

    /**
     * Check for a directory if it is possible to create files within this directory, either via
     * normal writing or via Storage Access Framework.
     *
     * @param folder The directory
     * @return true if it is possible to write in this directory.
     */
    @JvmStatic
    fun isWritableNormalOrSaf(folder: File?, c: Context): Boolean {
        if (folder == null) {
            return false
        }

        // Verify that this is a directory.
        if (!folder.exists() || !folder.isDirectory) {
            return false
        }

        // Find a non-existing file in this directory.
        var i = 0
        var file: File
        do {
            val fileName = "AugendiagnoseDummyFile" + ++i
            file = File(folder, fileName)
        } while (file.exists())

        // First check regular writability
        if (isWritable(file)) {
            return true
        }

        // Next check SAF writability.
        val document = ExternalSdCardOperation.getDocumentFile(file, false, c)
        document ?: return false

        // This should have created the file - otherwise something is wrong with access URL.
        val result = document.canWrite() && file.exists()

        // Ensure that the dummy file is not remaining.
        deleteFile(file, c)
        return result
    }

    // Utility methods for Kitkat
    /**
     * Checks whether the target path exists or is writable
     *
     * @param f the target path
     * @return 1 if exists or writable, 0 if not writable
     */
    @JvmStatic
    fun checkFolder(f: String?, context: Context): Int {
        if (f == null) return 0
        if (f.startsWith(CifsContexts.SMB_URI_PREFIX) ||
            f.startsWith(SshConnectionPool.SSH_URI_PREFIX) ||
            f.startsWith(OTGUtil.PREFIX_OTG) ||
            f.startsWith(CloudHandler.CLOUD_PREFIX_BOX) ||
            f.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE) ||
            f.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX) ||
            f.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE) ||
            f.startsWith("content://")
        ) return 1
        val folder = File(f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            isOnExtSdCard(folder, context)
        ) {
            if (!folder.exists() || !folder.isDirectory) {
                return 0
            }

            // On Android 5, trigger storage access framework.
            if (isWritableNormalOrSaf(folder, context)) {
                return 1
            }
        } else return if (Build.VERSION.SDK_INT == 19 &&
            isOnExtSdCard(folder, context)
        ) {
            // Assume that Kitkat workaround works
            1
        } else if (folder.canWrite()) {
            1
        } else {
            0
        }
        return 0
    }

    /**
     * Validate given text is a valid filename.
     *
     * @param text
     * @return true if given text is a valid filename
     */
    @JvmStatic
    fun isValidFilename(text: String): Boolean {
        val filenameRegex =
            Pattern.compile("[\\\\\\/:\\*\\?\"<>\\|\\x01-\\x1F\\x7F]", Pattern.CASE_INSENSITIVE)

        // It's not easy to use regex to detect single/double dot while leaving valid values
        // (filename.zip) behind...
        // So we simply use equality to check them
        return !filenameRegex.matcher(text).find() && "." != text && ".." != text
    }

    @JvmStatic
    fun unmapPathForApi30OrAbove(uriPath: String): String? {
        val uri = Uri.parse(uriPath)
        return uri.path?.let { p ->
            File(
                Environment.getExternalStorageDirectory(),
                p.substringAfter("tree/primary:")
            ).absolutePath
        }
    }

    @JvmStatic
    fun remapPathForApi30OrAbove(path: String, openDocumentTree: Boolean = false): String {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && EXCLUDED_DIRS.contains(path)) {
            val suffix =
                path.substringAfter(Environment.getExternalStorageDirectory().absolutePath)
            val documentId = "$STORAGE_PRIMARY:${suffix.substring(1)}"
            SafRootHolder.volumeLabel = STORAGE_PRIMARY
            if (openDocumentTree) {
                DocumentsContract.buildDocumentUri(
                    COM_ANDROID_EXTERNALSTORAGE_DOCUMENTS,
                    documentId
                ).toString()
            } else {
                DocumentsContract.buildTreeDocumentUri(
                    COM_ANDROID_EXTERNALSTORAGE_DOCUMENTS,
                    documentId
                ).toString()
            }
        } else {
            path
        }
    }

    @JvmStatic
    fun getDeviceStorageRemainingSpace(volume: String = STORAGE_PRIMARY): Long {
        return if (STORAGE_PRIMARY.equals(volume)) {
            if (Build.VERSION.SDK_INT < O) {
                Environment.getExternalStorageDirectory().freeSpace
            } else {
                AppConfig.getInstance().getSystemService(StorageStatsManager::class.java)
                    .getFreeBytes(StorageManager.UUID_DEFAULT)
            }
        } else 0L
    }
}
