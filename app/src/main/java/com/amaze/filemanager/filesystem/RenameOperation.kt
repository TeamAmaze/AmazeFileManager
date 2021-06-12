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
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.MakeDirectoryOperation.mkdir
import com.amaze.filemanager.filesystem.root.RenameFileCommand.renameFile
import java.io.*
import java.nio.channels.FileChannel

object RenameOperation {
    private val LOG = "RenameOperation"

    /**
     * Copy a file. The target file may even be on external SD card for Kitkat.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    @JvmStatic
    private fun copyFile(source: File, target: File, context: Context): Boolean {
        var inStream: FileInputStream? = null
        var outStream: OutputStream? = null
        var inChannel: FileChannel? = null
        var outChannel: FileChannel? = null
        try {
            inStream = FileInputStream(source)

            // First try the normal way
            if (FileProperties.isWritable(target)) {
                // standard way
                outStream = FileOutputStream(target)
                inChannel = inStream.channel
                outChannel = outStream.channel
                inChannel.transferTo(0, inChannel.size(), outChannel)
            } else {
                outStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    val targetDocument =
                        ExternalSdCardOperation.getDocumentFile(target, false, context)
                    targetDocument ?: throw IOException("Couldn't get DocumentFile")
                    context.contentResolver.openOutputStream(targetDocument.uri)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    val uri = MediaStoreHack.getUriFromFile(target.absolutePath, context)
                    uri ?: return false
                    context.contentResolver.openOutputStream(uri)
                } else {
                    return false
                }
                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    val buffer = ByteArray(16384) // MAGIC_NUMBER
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(
                LOG,
                "Error when copying file from ${source.absolutePath} to ${target.absolutePath}",
                e
            )
            return false
        } finally {
            try {
                inStream?.close()
            } catch (e: IOException) {
                // ignore exception
            }
            try {
                outStream?.close()
            } catch (e: IOException) {
                // ignore exception
            }
            try {
                inChannel?.close()
            } catch (e: IOException) {
                // ignore exception
            }
            try {
                outChannel?.close()
            } catch (e: IOException) {
                // ignore exception
            }
        }
        return true
    }

    @JvmStatic
    @Throws(ShellNotRunningException::class)
    private fun rename(f: File, name: String, root: Boolean): Boolean {
        val parentName = f.parent ?: return false
        val parentFile = f.parentFile ?: return false

        val newPath = "$parentName/$name"
        if (parentFile.canWrite()) {
            return f.renameTo(File(newPath))
        } else if (root) {
            renameFile(f.path, newPath)
            return true
        }
        return false
    }

    /**
     * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are
     * moved.
     *
     * @param source The source folder.
     * @param target The target folder.
     * @return true if the renaming was successful.
     */
    @JvmStatic
    @Throws(ShellNotRunningException::class)
    fun renameFolder(
        source: File,
        target: File,
        context: Context
    ): Boolean {
        // First try the normal rename.
        if (rename(source, target.name, false)) {
            return true
        }
        if (target.exists()) {
            return false
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            source.parent == target.parent &&
            ExternalSdCardOperation.isOnExtSdCard(source, context)
        ) {
            val document = ExternalSdCardOperation.getDocumentFile(source, true, context)
            document ?: return false
            if (document.renameTo(target.name)) {
                return true
            }
        }

        // Try the manual way, moving files individually.
        if (!mkdir(target, context)) {
            return false
        }
        val sourceFiles = source.listFiles() as Array<File>?
        sourceFiles ?: return true
        for (sourceFile in sourceFiles) {
            val fileName = sourceFile.name
            val targetFile = File(target, fileName)
            if (!copyFile(sourceFile, targetFile, context)) {
                // stop on first error
                return false
            }
        }
        // Only after successfully copying all files, delete files on source folder.
        for (sourceFile in sourceFiles) {
            if (!DeleteOperation.deleteFile(sourceFile, context)) {
                // stop on first error
                return false
            }
        }
        return true
    }
}
