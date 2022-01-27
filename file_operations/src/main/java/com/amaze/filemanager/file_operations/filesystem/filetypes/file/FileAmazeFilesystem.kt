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
package com.amaze.filemanager.file_operations.filesystem.filetypes.file

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.ExternalSdCardOperation.getDocumentFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.ExternalSdCardOperation.isOnExtSdCard
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.UriForSafPersistance.get
import java.io.*

/**
 * This is in in essence calls to UnixFilesystem, but that class is not public so all calls must go
 * through java.io.File
 */
class FileAmazeFilesystem private constructor() : AmazeFilesystem() {
    companion object {
        val INSTANCE = FileAmazeFilesystem()
        val TAG = FileAmazeFilesystem::class.java.simpleName

        init {
            AmazeFile.addFilesystem(INSTANCE)
        }
    }

    override fun isPathOfThisFilesystem(path: String): Boolean {
        return path[0] == getSeparator()
    }

    override val prefix: String
        get() = throw NotImplementedError()

    override fun getSeparator(): Char {
        return File.separatorChar
    }

    override fun normalize(path: String): String {
        return File(path).path
    }

    override fun prefixLength(path: String): Int {
        if (path.length == 0) return 0
        return if (path[0] == '/') 1 else 0
    }

    override fun resolve(parent: String?, child: String?): String {
        return File(parent, child).path
    }

    override val defaultParent: String
        get() = File(File(""), "").path

    override fun isAbsolute(f: AmazeFile): Boolean {
        return File(f.path).isAbsolute
    }

    override fun resolve(f: AmazeFile): String {
        return File(f.path).absolutePath
    }

    @Throws(IOException::class)
    override fun canonicalize(path: String?): String {
        return File(path).canonicalPath
    }

    override fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return f.exists(contextProvider)
    }

    override fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).isFile
    }

    override fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).isDirectory
    }

    override fun isHidden(f: AmazeFile): Boolean {
        return File(f.path).isHidden
    }

    override fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).canExecute()
    }

    override fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).canWrite()
    }

    override fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).canRead()
    }

    override fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).exists()
    }

    override fun setExecutable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return File(f.path).setExecutable(enable, owneronly)
    }

    override fun setWritable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return File(f.path).setWritable(enable, owneronly)
    }

    override fun setReadable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return File(f.path).setReadable(enable, owneronly)
    }

    override fun getLastModifiedTime(f: AmazeFile): Long {
        return File(f.path).lastModified()
    }

    @Throws(IOException::class)
    override fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long {
        return File(f.path).length()
    }

    @Throws(IOException::class)
    override fun createFileExclusively(pathname: String?): Boolean {
        return File(pathname).createNewFile()
    }

    override fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        if (f.isDirectory(contextProvider)) {
            val children = f.listFiles(contextProvider)
            if (children != null) {
                for (child in children) {
                    delete(child, contextProvider)
                }
            }

            // Try the normal way
            if (File(f.path).delete()) {
                return true
            }
            val context = contextProvider.getContext()

            // Try with Storage Access Framework.
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val document = getDocumentFile(
                        f, true, context, get(context))
                if (document != null && document.delete()) {
                    return true
                }
            }

            // Try the Kitkat workaround.
            if (context != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                val resolver = context.contentResolver
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DATA, f.absolutePath)
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                // Delete the created entry, such that content provider will delete the file.
                resolver.delete(
                        MediaStore.Files.getContentUri("external"),
                        MediaStore.MediaColumns.DATA + "=?", arrayOf(f.absolutePath))
            }
            return !f.exists(contextProvider)
        }
        if (File(f.path).delete()) {
            return true
        }
        val context = contextProvider.getContext()

        // Try with Storage Access Framework.
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isOnExtSdCard(f, context)) {
            val document = getDocumentFile(
                    f, false, context, get(context)) ?: return true
            if (document.delete()) {
                return true
            }
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            val resolver = context!!.contentResolver
            try {
                val uri = MediaStoreHack.getUriFromFile(f.absolutePath, context)
                        ?: return false
                resolver.delete(uri, null, null)
                return !f.exists(contextProvider)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception when checking for file " + f.absolutePath, e)
            }
        }
        return false
    }

    override fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String?>? {
        return File(f.path).list()
    }

    override fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream? {
        return try {
            FileInputStream(f.path)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot find file", e)
            null
        }
    }

    override fun getOutputStream(f: AmazeFile, contextProvider: ContextProvider): OutputStream? {
        return try {
            if (f.canWrite(contextProvider)) {
                return FileOutputStream(f.path)
            } else {
                val context = contextProvider.getContext() ?: return null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    val targetDocument = getDocumentFile(
                            f, false, context, get(context)) ?: return null
                    return context.contentResolver.openOutputStream(targetDocument.uri)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    return MediaStoreHack.getOutputStream(context, f.path)
                }
            }
            null
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot find file", e)
            null
        }
    }

    override fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        if (File(f.path).mkdir()) {
            return true
        }
        val context = contextProvider.getContext()

        // Try with Storage Access Framework.
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isOnExtSdCard(f, context)) {
            val preferenceUri = get(context)
            val document = getDocumentFile(f, true, context, preferenceUri)
                    ?: return false
            // getDocumentFile implicitly creates the directory.
            return document.exists()
        }

        // Try the Kitkat workaround.
        return if (context != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            try {
                MediaStoreHack.mkdir(context, f)
            } catch (e: IOException) {
                false
            }
        } else false
    }

    override fun rename(f1: AmazeFile, f2: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f1.path).renameTo(File(f2.path))
    }

    override fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean {
        return File(f.path).setLastModified(time)
    }

    override fun setReadOnly(f: AmazeFile): Boolean {
        return File(f.path).setReadOnly()
    }

    override fun listRoots(): Array<AmazeFile> {
        val roots = File.listRoots()
        return Array(roots.size) { i: Int ->
            AmazeFile(roots[i].path)
        }
    }

    override fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long {
        return File(f.path).totalSpace
    }

    override fun getFreeSpace(f: AmazeFile): Long {
        return File(f.path).freeSpace
    }

    override fun getUsableSpace(f: AmazeFile): Long {
        return File(f.path).usableSpace
    }

    override fun compare(f1: AmazeFile, f2: AmazeFile): Int {
        return File(f1.path).compareTo(File(f2.path))
    }

    override fun hashCode(f: AmazeFile): Int {
        return File(f.path).hashCode()
    }
}