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

package com.amaze.filemanager.filesystem.ftpserver

import android.content.ContentResolver
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
@Suppress("TooManyFunctions") // Don't ask me. Ask Apache why.
class AndroidFtpFile(
    context: Context,
    private val parentDocument: DocumentFile,
    private val backingDocument: DocumentFile?,
    private val path: String
) : FtpFile {

    private val _context: WeakReference<Context> = WeakReference(context)
    private val context: Context
        get() = _context.get()!!

    override fun getAbsolutePath(): String {
        return path
    }

    /**
     * @see FtpFile.getName
     * @see DocumentFile.getName
     */
    override fun getName(): String = backingDocument?.name ?: path.substringAfterLast('/')

    /**
     * @see FtpFile.isHidden
     */
    override fun isHidden(): Boolean = name.startsWith(".") && name != "."

    /**
     * @see FtpFile.isDirectory
     * @see DocumentFile.isDirectory
     */
    override fun isDirectory(): Boolean = backingDocument?.isDirectory ?: false

    /**
     * @see FtpFile.isFile
     * @see DocumentFile.isFile
     */
    override fun isFile(): Boolean = backingDocument?.isFile ?: false

    /**
     * @see FtpFile.doesExist
     * @see DocumentFile.exists
     */
    override fun doesExist(): Boolean = backingDocument?.exists() ?: false

    /**
     * @see FtpFile.isReadable
     * @see DocumentFile.canRead
     */
    override fun isReadable(): Boolean = backingDocument?.canRead() ?: false

    /**
     * @see FtpFile.isWritable
     * @see DocumentFile.canWrite
     */
    override fun isWritable(): Boolean = backingDocument?.canWrite() ?: true

    /**
     * @see FtpFile.isRemovable
     * @see DocumentFile.canWrite
     */
    override fun isRemovable(): Boolean = backingDocument?.canWrite() ?: true

    /**
     * @see FtpFile.getOwnerName
     */
    override fun getOwnerName(): String = "user"

    /**
     * @see FtpFile.getGroupName
     */
    override fun getGroupName(): String = "user"

    /**
     * @see FtpFile.getLinkCount
     */
    override fun getLinkCount(): Int = 0

    /**
     * @see FtpFile.getLastModified
     * @see DocumentFile.lastModified
     */
    override fun getLastModified(): Long = backingDocument?.lastModified() ?: 0L

    /**
     * @see FtpFile.setLastModified
     * @see DocumentsContract.Document.COLUMN_LAST_MODIFIED
     * @see ContentResolver.update
     */
    override fun setLastModified(time: Long): Boolean {
        return if (doesExist()) {
            val updateValues = ContentValues().also {
                it.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, time)
            }
            val docUri: Uri = backingDocument!!.uri
            val updated: Int = context.contentResolver.update(
                docUri,
                updateValues,
                null,
                null
            )
            return updated == 1
        } else {
            false
        }
    }

    /**
     * @see FtpFile.getSize
     * @see DocumentFile.length
     */
    override fun getSize(): Long = backingDocument?.length() ?: 0L

    /**
     * @see FtpFile.getPhysicalFile
     */
    override fun getPhysicalFile(): Any = backingDocument!!

    /**
     * @see FtpFile.mkdir
     * @see DocumentFile.createDirectory
     */
    override fun mkdir(): Boolean = parentDocument.createDirectory(name) != null

    /**
     * @see FtpFile.delete
     * @see DocumentFile.delete
     */
    override fun delete(): Boolean = backingDocument?.delete() ?: false

    /**
     * @see FtpFile.move
     * @see DocumentFile.renameTo
     */
    override fun move(destination: FtpFile): Boolean =
        backingDocument?.renameTo(destination.name) ?: false

    /**
     * @see FtpFile.listFiles
     * @see DocumentFile.listFiles
     */
    override fun listFiles(): MutableList<out FtpFile> = if (doesExist()) {
        backingDocument!!.listFiles().map {
            AndroidFtpFile(context, backingDocument, it, it.name!!)
        }.toMutableList()
    } else {
        mutableListOf()
    }

    /**
     * @see FtpFile.createOutputStream
     * @see ContentResolver.openOutputStream
     */
    override fun createOutputStream(offset: Long): OutputStream? = runCatching {
        val uri = if (doesExist()) {
            backingDocument!!.uri
        } else {
            val newFile = parentDocument.createFile("", name)
            newFile?.uri ?: throw IOException("Cannot create file at $path")
        }
        context.contentResolver.openOutputStream(uri)
    }.getOrThrow()

    /**
     * @see FtpFile.createInputStream
     * @see ContentResolver.openInputStream
     */
    override fun createInputStream(offset: Long): InputStream? = runCatching {
        if (doesExist()) {
            context.contentResolver.openInputStream(backingDocument!!.uri).also {
                it?.skip(offset)
            }
        } else {
            throw FileNotFoundException(path)
        }
    }.getOrThrow()
}
