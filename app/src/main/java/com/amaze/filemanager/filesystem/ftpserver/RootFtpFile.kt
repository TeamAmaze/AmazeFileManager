/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.User
import org.apache.ftpserver.usermanager.impl.WriteRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream

class RootFtpFile(
    private val fileName: String,
    private val backingFile: SuFile,
    private val user: User
) : FtpFile {

    companion object {
        @JvmStatic
        private val logger: Logger = LoggerFactory.getLogger(RootFtpFile::class.java)
    }

    override fun getAbsolutePath(): String = backingFile.absolutePath

    override fun getName(): String = backingFile.name

    override fun isHidden(): Boolean = backingFile.isHidden

    override fun isDirectory(): Boolean = backingFile.isDirectory

    override fun isFile(): Boolean = backingFile.isFile

    override fun doesExist(): Boolean = backingFile.exists()

    override fun isReadable(): Boolean = backingFile.canRead()

    override fun isWritable(): Boolean {
        logger.debug("Checking authorization for $absolutePath")
        if (user.authorize(WriteRequest(absolutePath)) == null) {
            logger.debug("Not authorized")
            return false
        }

        logger.debug("Checking if file exists")
        if (backingFile.exists()) {
            logger.debug("Checking can write: " + backingFile.canWrite())
            return backingFile.canWrite()
        }

        logger.debug("Authorized")
        return true
    }

    override fun isRemovable(): Boolean {
        // root cannot be deleted
        if ("/" == fileName) {
            return false
        }

        val fullName = absolutePath
        // we check FTPServer's write permission for this file.
        if (user.authorize(WriteRequest(fullName)) == null) {
            return false
        }
        // In order to maintain consistency, when possible we delete the last '/' character in the String
        val indexOfSlash = fullName.lastIndexOf('/')
        val parentFullName: String = if (indexOfSlash == 0) {
            "/"
        } else {
            fullName.substring(0, indexOfSlash)
        }

        // we check if the parent FileObject is writable.
        return backingFile.absoluteFile.parentFile?.run {
            RootFtpFile(
                parentFullName,
                this,
                user
            ).isWritable
        } ?: false
    }

    override fun getOwnerName(): String = "user"

    override fun getGroupName(): String = "user"

    override fun getLinkCount(): Int = if (backingFile.isDirectory) 3 else 1

    override fun getLastModified(): Long = backingFile.lastModified()

    override fun setLastModified(time: Long): Boolean = backingFile.setLastModified(time)

    override fun getSize(): Long = backingFile.length()

    override fun getPhysicalFile(): Any = backingFile

    override fun mkdir(): Boolean = backingFile.mkdirs()

    override fun delete(): Boolean = backingFile.delete()

    override fun move(destination: FtpFile): Boolean =
        backingFile.renameTo(destination.physicalFile as SuFile)

    override fun listFiles(): MutableList<out FtpFile> = backingFile.listFiles()?.map {
        RootFtpFile(it.name, it, user)
    }?.toMutableList() ?: emptyList<FtpFile>().toMutableList()

    override fun createOutputStream(offset: Long): OutputStream =
        SuFileOutputStream.open(backingFile.absolutePath)

    override fun createInputStream(offset: Long): InputStream =
        SuFileInputStream.open(backingFile.absolutePath)
}
