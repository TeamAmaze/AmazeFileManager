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

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants
import java.io.File
import java.io.IOException
import java.util.*

object ExternalSdCardOperation {
    val LOG = "ExternalSdCardOperation"

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If
     * the file is not existing, it is created.
     *
     * @param file The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @return The DocumentFile
     */
    @JvmStatic
    fun getDocumentFile(
        file: File,
        isDirectory: Boolean,
        context: Context
    ): DocumentFile? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) return DocumentFile.fromFile(file)
        val baseFolder = getExtSdCardFolder(file, context)
        var originalDirectory = false
        if (baseFolder == null) {
            return null
        }
        var relativePath: String? = null
        try {
            val fullPath = file.canonicalPath
            if (baseFolder != fullPath) {
                relativePath = fullPath.substring(baseFolder.length + 1)
            } else {
                originalDirectory = true
            }
        } catch (e: IOException) {
            return null
        }

        val preferenceUri = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PreferencesConstants.PREFERENCE_URI, null)
        var treeUri: Uri? = null
        if (preferenceUri != null) {
            treeUri = Uri.parse(preferenceUri)
        }
        if (treeUri == null) {
            return null
        }

        // start with root of SD card and then parse through document tree.
        var document = DocumentFile.fromTreeUri(context, treeUri)
        if (originalDirectory || relativePath == null) {
            return document
        }

        val parts = relativePath.split("/").toTypedArray()
        for (i in parts.indices) {
            if (document == null) {
                return null
            }

            var nextDocument = document.findFile(parts[i])
            if (nextDocument == null) {
                nextDocument = if (i < parts.size - 1 || isDirectory) {
                    document.createDirectory(parts[i])
                } else {
                    document.createFile("image", parts[i])
                }
            }
            document = nextDocument
        }

        return document
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun getExtSdCardPaths(context: Context): Array<String> {
        val paths: MutableList<String> = ArrayList()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null && file != context.getExternalFilesDir("external")) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) {
                    Log.w(LOG, "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }
                    paths.add(path)
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1")
        return paths.toTypedArray()
    }

    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun getExtSdCardPathsForActivity(context: Context): Array<String> {
        val paths: MutableList<String> = ArrayList()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) {
                    Log.w(LOG, "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }
                    paths.add(path)
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1")
        return paths.toTypedArray()
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD
     * card. Otherwise, null is returned.
     */
    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public fun getExtSdCardFolder(file: File, context: Context): String? {
        val extSdPaths = getExtSdCardPaths(context)
        try {
            for (i in extSdPaths.indices) {
                if (file.canonicalPath.startsWith(extSdPaths[i])) {
                    return extSdPaths[i]
                }
            }
        } catch (e: IOException) {
            return null
        }
        return null
    }

    /**
     * Determine if a file is on external sd card. (Kitkat or higher.)
     *
     * @param file The file.
     * @return true if on external sd card.
     */
    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun isOnExtSdCard(file: File, c: Context): Boolean {
        return getExtSdCardFolder(file, c) != null
    }
}
