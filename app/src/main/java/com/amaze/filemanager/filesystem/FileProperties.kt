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
import com.amaze.filemanager.filesystem.DeleteOperation.deleteFile
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

// TODO check if these can be done with just File methods
// TODO make all of these methods File extensions
object FileProperties {
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
        val document = FileUtil.getDocumentFile(file, false, c)
        document ?: return false

        // This should have created the file - otherwise something is wrong with access URL.
        val result = document.canWrite() && file.exists()

        // Ensure that the dummy file is not remaining.
        deleteFile(file, c)
        return result
    }
}
