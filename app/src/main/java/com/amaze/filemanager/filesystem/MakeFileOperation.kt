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

import android.content.Context
import android.os.Build
import android.util.Log
import com.amaze.filemanager.ui.icons.MimeTypes
import com.amaze.filemanager.utils.AppConstants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

// This object is here to not polute the global namespace
// All functions must be static
object MakeFileOperation {
    val LOG = "MakeFileOperation"

    /**
     * Get a temp file.
     *
     * @param file The base file for which to create a temp file.
     * @return The temp file.
     */
    @JvmStatic
    fun getTempFile(file: File, context: Context): File {
        val extDir = context.getExternalFilesDir(null)
        return File(extDir, file.name)
    }

    @JvmStatic
    fun mkfile(file: File?, context: Context): Boolean {
        if (file == null) return false
        if (file.exists()) {
            // nothing to create.
            return !file.isDirectory
        }

        // Try the normal way
        try {
            if (file.createNewFile()) {
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            ExternalSdCardOperation.isOnExtSdCard(file, context)
        ) {
            val document = ExternalSdCardOperation.getDocumentFile(file.parentFile, true, context)
            // getDocumentFile implicitly creates the directory.
            return try {
                (
                    document?.createFile(
                        MimeTypes.getMimeType(file.path, file.isDirectory), file.name
                    )
                        != null
                    )
            } catch (e: UnsupportedOperationException) {
                e.printStackTrace()
                false
            }
        }
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            MediaStoreHack.mkfile(context, file)
        } else false
    }

    @JvmStatic
    fun mktextfile(data: String?, path: String?, fileName: String): Boolean {
        val f = File(
            path,
            "$fileName${AppConstants.NEW_FILE_DELIMITER}${AppConstants.NEW_FILE_EXTENSION_TXT}"
        )
        var out: FileOutputStream? = null
        var outputWriter: OutputStreamWriter? = null
        return try {
            if (f.createNewFile()) {
                out = FileOutputStream(f, false)
                outputWriter = OutputStreamWriter(out)
                outputWriter.write(data)
                true
            } else {
                false
            }
        } catch (io: IOException) {
            Log.e(LOG, "Error writing file contents", io)
            false
        } finally {
            try {
                if (outputWriter != null) {
                    outputWriter.flush()
                    outputWriter.close()
                }
                if (out != null) {
                    out.flush()
                    out.close()
                }
            } catch (e: IOException) {
                Log.e(LOG, "Error closing file output stream", e)
            }
        }
    }
}
