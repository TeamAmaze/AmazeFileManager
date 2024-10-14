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

package com.amaze.filemanager.filesystem.files

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File

/**
 * Tries to find the path of the file that is identified with [uri].
 * If the path cannot be found, returns null.
 *
 * Adapted from: https://github.com/saparkhid/AndroidFileNamePicker/blob/main/javautil/FileUtils.java
 */
fun fromUri(
    uri: Uri,
    context: Context,
): String? {
    // ExternalStorageProvider
    if (isExternalStorageDocument(uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val fullPath = getPathFromExtSD(split)
        return if (fullPath !== "") {
            fullPath
        } else {
            null
        }
    }

    // DownloadsProvider
    if (isDownloadsDocument(uri)) {
        return getPathFromDownloads(uri, context)
    }

    // MediaProvider
    if (isMediaDocument(uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val contentUri =
            when (split[0]) {
                "image" -> {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                "video" -> {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                "audio" -> {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                "document" -> {
                    MediaStore.Files.getContentUri("external")
                }
                else -> return getDataColumn(context, uri, null, null)
            }
        val selection = "_id=?"
        val selectionArgs =
            arrayOf(
                split[1],
            )
        return getDataColumn(context, contentUri, selection, selectionArgs)
    }
    if ("content".equals(uri.scheme, ignoreCase = true)) {
        if (isGooglePhotosUri(uri)) {
            return uri.lastPathSegment
        }
        val path = getDataColumn(context, uri, null, null)
        if (path != null) {
            return path
        } else if (fileExists(uri.path)) {
            // Check if the full path is the uri path
            return uri.path
        } else {
            // Check if the full path is contained in the uri path
            return getPathInUri(uri)
        }
    }
    if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }
    return null
}

private fun fileExists(filePath: String?): Boolean {
    if (filePath == null) return false

    val file = File(filePath)
    return file.exists()
}

private fun getPathFromExtSD(pathData: List<String>): String? {
    val type = pathData[0]
    val relativePath = File.separator + pathData[1]
    var fullPath: String? = null
    // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
    // something like "71F8-2C0A", some kind of unique id per storage
    // don't know any API that can get the root path of that storage based on its id.
    //
    // so no "primary" type, but let the check here for other devices
    if ("primary".equals(type, ignoreCase = true)) {
        fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
        if (fileExists(fullPath)) {
            return fullPath
        }
    }
    if ("home".equals(type, ignoreCase = true)) {
        fullPath = "/storage/emulated/0/Documents$relativePath"
        if (fileExists(fullPath)) {
            return fullPath
        }
    }

    // Adapted from: https://stackoverflow.com/questions/42110882/get-real-path-from-uri-of-file-in-sdcard-marshmallow
    fullPath = "/storage/$type$relativePath"
    return if (fileExists(fullPath)) {
        fullPath
    } else {
        null
    }
}

private fun getPathFromDownloads(
    uri: Uri,
    context: Context,
): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Try to use ContentResolver to get the file name
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val fileName =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME),
                    )
                val path =
                    Environment.getExternalStorageDirectory()
                        .toString() + "/Download/" + fileName
                if (!TextUtils.isEmpty(path)) {
                    return path
                }
            }
        }
        val id = DocumentsContract.getDocumentId(uri)
        if (!TextUtils.isEmpty(id)) {
            if (id.startsWith("raw:")) {
                return id.replaceFirst("raw:", "")
            }
            val contentUriPrefixesToTry =
                arrayOf(
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads",
                )
            // Try to guess full path with frequently used download paths
            for (contentUriPrefix in contentUriPrefixesToTry) {
                return try {
                    val contentUri =
                        ContentUris.withAppendedId(
                            Uri.parse(contentUriPrefix),
                            java.lang.Long.valueOf(id),
                        )
                    getDataColumn(context, contentUri, null, null)
                } catch (e: NumberFormatException) {
                    // In Android 8 and Android P the id is not a number
                    uri.path!!.replaceFirst("^/document/raw:", "")
                        .replaceFirst("^raw:", "")
                }
            }
        }
    } else {
        val id = DocumentsContract.getDocumentId(uri)
        if (id.startsWith("raw:")) {
            return id.replaceFirst("raw:", "")
        }
        return try {
            val contentUri =
                ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id),
                )
            getDataColumn(context, contentUri, null, null)
        } catch (e: NumberFormatException) {
            null
        }
    }
    return null
}

private fun getDataColumn(
    context: Context,
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>?,
): String? {
    val column = MediaStore.Files.FileColumns.DATA
    val projection = arrayOf(column)

    context.contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        null,
    ).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val index: Int = cursor.getColumnIndex(column)
            return if (index >= 0) {
                cursor.getString(index)
            } else {
                null
            }
        }
    }
    return null
}

private fun getPathInUri(uri: Uri): String? {
    // As last resort, check if the full path is somehow contained in the uri path
    val uriPath = uri.path ?: return null
    // Some common path prefixes
    val pathPrefixes = listOf("/storage", "/external_files")
    for (prefix in pathPrefixes) {
        if (uriPath.contains(prefix)) {
            // make sure path starts with storage
            val pathInUri = "/storage${uriPath.substring(
                uriPath.indexOf(prefix) + prefix.length,
            )}"
            if (fileExists(pathInUri)) {
                return pathInUri
            }
        }
    }
    return null
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

private fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}
