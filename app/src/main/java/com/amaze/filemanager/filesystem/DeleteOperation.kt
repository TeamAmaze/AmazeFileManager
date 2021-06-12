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

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File

object DeleteOperation {
    private val LOG = "DeleteFileOperation"

    /**
     * Delete a folder.
     *
     * @param file The folder name.
     * @return true if successful.
     */
    @JvmStatic
    private fun rmdir(file: File, context: Context): Boolean {
        if (!file.exists()) return true
        val files = file.listFiles()
        if (files != null && files.size > 0) {
            for (child in files) {
                rmdir(child, context)
            }
        }

        // Try the normal way
        if (file.delete()) {
            return true
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val document = ExternalSdCardOperation.getDocumentFile(file, true, context)
            if (document != null && document.delete()) {
                return true
            }
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            val resolver = context.contentResolver
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DATA, file.absolutePath)
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            // Delete the created entry, such that content provider will delete the file.
            resolver.delete(
                MediaStore.Files.getContentUri("external"),
                MediaStore.MediaColumns.DATA + "=?", arrayOf(file.absolutePath)
            )
        }
        return !file.exists()
    }

    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    @JvmStatic
    fun deleteFile(file: File, context: Context): Boolean {
        // First try the normal deletion.
        val fileDelete = rmdir(file, context)
        if (file.delete() || fileDelete) return true

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            ExternalSdCardOperation.isOnExtSdCard(file, context)
        ) {
            val document = ExternalSdCardOperation.getDocumentFile(file, false, context)
            document ?: return true
            return document.delete()
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            val resolver = context.contentResolver
            return try {
                val uri = MediaStoreHack.getUriFromFile(file.absolutePath, context)
                if (uri == null) {
                    false
                } else {
                    resolver.delete(uri, null, null)
                    !file.exists()
                }
            } catch (e: SecurityException) {
                Log.e(LOG, "Security exception when checking for file " + file.absolutePath, e)
                false
            }
        }
        return !file.exists()
    }
}
