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
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.utils.OTGUtil
import jcifs.smb.SmbException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

// This object is here to not polute the global namespace
// All functions must be static
object MakeDirectoryOperation {

    private val log: Logger = LoggerFactory.getLogger(MakeDirectoryOperation::class.java)

    /**
     * Create a folder. The folder may even be on external SD card for Kitkat.
     *
     * @param file The folder to be created.
     * @return True if creation was successful.
     */
    @JvmStatic
    @Deprecated("use {@link #mkdirs(Context, HybridFile)}")
    fun mkdir(file: File?, context: Context): Boolean {
        if (file == null) return false
        if (file.exists()) {
            // nothing to create.
            return file.isDirectory
        }

        // Try the normal way
        if (file.mkdirs()) {
            return true
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            ExternalSdCardOperation.isOnExtSdCard(file, context)
        ) {
            val document = ExternalSdCardOperation.getDocumentFile(file, true, context)
            document ?: return false
            // getDocumentFile implicitly creates the directory.
            return document.exists()
        }

        // Try the Kitkat workaround.
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            try {
                MediaStoreHack.mkdir(context, file)
            } catch (e: IOException) {
                false
            }
        } else false
    }

    /**
     * Creates the directories on given [file] path, including nonexistent parent directories.
     * So use proper [HybridFile] constructor as per your need.
     *
     * @return true if successfully created directory, otherwise returns false.
     */
    @JvmStatic
    fun mkdirs(context: Context, file: HybridFile): Boolean {
        var isSuccessful = true
        when (file.mode) {
            OpenMode.SMB ->
                try {
                    val smbFile = file.smbFile
                    smbFile.mkdirs()
                } catch (e: SmbException) {
                    log.warn("failed to make directory in smb", e)
                    isSuccessful = false
                }
            OpenMode.OTG -> {
                val documentFile = OTGUtil.getDocumentFile(file.getPath(), context, true)
                isSuccessful = documentFile != null
            }
            OpenMode.FILE -> isSuccessful = mkdir(File(file.getPath()), context)
            // With ANDROID_DATA will not accept create directory
            OpenMode.ANDROID_DATA -> isSuccessful = false
            else -> isSuccessful = true
        }
        return isSuccessful
    }
}
