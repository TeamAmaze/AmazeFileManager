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

package com.amaze.filemanager.ftpserver.filesystem

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION_CODES.KITKAT
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import org.apache.ftpserver.ftplet.FtpFile
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

@RequiresApi(KITKAT)
@Suppress("TooManyFunctions")
class AndroidFtpFile(
    context: Context,
    private val parentDocument: DocumentFile,
    private val backingDocument: DocumentFile?,
    private val path: String,
) : FtpFile {
    private val _context: WeakReference<Context> = WeakReference(context)
    private val context: Context
        get() = _context.get()!!

    override fun getAbsolutePath(): String {
        return path
    }

    override fun getName(): String = backingDocument?.name ?: path.substringAfterLast('/')

    override fun isHidden(): Boolean = name.startsWith(".") && name != "."

    override fun isDirectory(): Boolean = backingDocument?.isDirectory ?: false

    override fun isFile(): Boolean = backingDocument?.isFile ?: false

    override fun doesExist(): Boolean = backingDocument?.exists() ?: false

    override fun isReadable(): Boolean = backingDocument?.canRead() ?: false

    override fun isWritable(): Boolean = backingDocument?.canWrite() ?: true

    override fun isRemovable(): Boolean = backingDocument?.canWrite() ?: true

    override fun getOwnerName(): String = "user"

    override fun getGroupName(): String = "user"

    override fun getLinkCount(): Int = 0

    override fun getLastModified(): Long = backingDocument?.lastModified() ?: 0L

    override fun setLastModified(time: Long): Boolean {
        return if (doesExist()) {
            val updateValues =
                ContentValues().also {
                    it.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, time)
                }
            val docUri: Uri = backingDocument!!.uri
            val updated: Int =
                context.contentResolver.update(
                    docUri,
                    updateValues,
                    null,
                    null,
                )
            return updated == 1
        } else {
            false
        }
    }

    override fun getSize(): Long = backingDocument?.length() ?: 0L

    override fun getPhysicalFile(): Any = backingDocument!!

    override fun mkdir(): Boolean = parentDocument.createDirectory(name) != null

    override fun delete(): Boolean = backingDocument?.delete() ?: false

    override fun move(destination: FtpFile): Boolean = backingDocument?.renameTo(destination.name) ?: false

    override fun listFiles(): MutableList<out FtpFile> =
        if (doesExist()) {
            backingDocument!!.listFiles().map {
                AndroidFtpFile(context, backingDocument, it, it.name!!)
            }.toMutableList()
        } else {
            mutableListOf()
        }

    override fun createOutputStream(offset: Long): OutputStream? =
        runCatching {
            val uri =
                if (doesExist()) {
                    backingDocument!!.uri
                } else {
                    val newFile = parentDocument.createFile("", name)
                    newFile?.uri ?: throw IOException("Cannot create file at $path")
                }
            context.contentResolver.openOutputStream(uri)
        }.getOrThrow()

    override fun createInputStream(offset: Long): InputStream? =
        runCatching {
            if (doesExist()) {
                context.contentResolver.openInputStream(backingDocument!!.uri).also {
                    it?.skip(offset)
                }
            } else {
                throw FileNotFoundException(path)
            }
        }.getOrThrow()
}
