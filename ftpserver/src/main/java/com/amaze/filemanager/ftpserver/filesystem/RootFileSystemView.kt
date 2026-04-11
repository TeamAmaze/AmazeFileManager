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

import android.util.Log
import com.topjohnwu.superuser.io.SuFile
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.User
import java.io.File
import java.net.URI
import java.util.StringTokenizer

class RootFileSystemView(
    private val user: User,
    private val fileFactory: SuFileFactory,
) : FileSystemView {
    private var currDir: String
    private var rootDir: String

    companion object {
        private const val TAG = "RootFileSystemView"
    }

    init {
        requireNotNull(user.homeDirectory) { "User home directory can not be null" }

        var rootDir = user.homeDirectory
        rootDir = normalizeSeparateChar(rootDir)
        rootDir = appendSlash(rootDir)

        Log.d(
            TAG,
            "Native filesystem view created for user \"${user.name}\" with root \"${rootDir}\"",
        )

        this.rootDir = rootDir
        currDir = "/"
    }

    override fun getHomeDirectory(): FtpFile {
        return RootFtpFile("/", fileFactory.create(rootDir), user)
    }

    override fun getWorkingDirectory(): FtpFile {
        return if (currDir == "/") {
            RootFtpFile("/", fileFactory.create(rootDir), user)
        } else {
            val file = fileFactory.create(rootDir, currDir.substring(1))
            RootFtpFile(currDir, file, user)
        }
    }

    override fun changeWorkingDirectory(dirArg: String): Boolean {
        var dir = dirArg

        dir = getPhysicalName(rootDir, currDir, dir)
        val dirObj = fileFactory.create(dir)
        if (!dirObj.isDirectory) {
            return false
        }

        dir = dir.substring(rootDir.length - 1)
        if (dir[dir.length - 1] != '/') {
            dir = "$dir/"
        }

        currDir = dir
        return true
    }

    override fun getFile(file: String): FtpFile {
        val physicalName = getPhysicalName(rootDir, currDir, file)
        val fileObj = fileFactory.create(physicalName)

        val userFileName = physicalName.substring(rootDir.length - 1)
        return RootFtpFile(userFileName, fileObj, user)
    }

    override fun isRandomAccessible(): Boolean = false

    override fun dispose() = Unit

    private fun getPhysicalName(
        rootDir: String,
        currDir: String,
        fileName: String,
    ): String {
        var normalizedRootDir: String = normalizeSeparateChar(rootDir)
        normalizedRootDir = appendSlash(normalizedRootDir)

        val normalizedFileName = normalizeSeparateChar(fileName)
        var result: String?

        result =
            if (normalizedFileName[0] != '/') {
                val normalizedCurrDir = normalize(currDir)
                normalizedRootDir + normalizedCurrDir.substring(1)
            } else {
                normalizedRootDir
            }

        result = trimTrailingSlash(result)

        val st = StringTokenizer(normalizedFileName, "/")
        while (st.hasMoreTokens()) {
            val tok = st.nextToken()

            if (tok == ".") {
                // ignore
            } else if (tok == "..") {
                if (result!!.startsWith(normalizedRootDir)) {
                    val slashIndex = result.lastIndexOf('/')
                    if (slashIndex != -1) {
                        result = result.substring(0, slashIndex)
                    }
                }
            } else if (tok == "~") {
                result = trimTrailingSlash(normalizedRootDir)
                continue
            } else {
                result = "$result/$tok"
            }
        }

        if (result!!.length + 1 == normalizedRootDir.length) {
            result += '/'
        }

        if (!result.startsWith(normalizedRootDir)) {
            result = normalizedRootDir
        }
        return result
    }

    private fun appendSlash(path: String): String {
        return if (!path.endsWith("/")) {
            "$path/"
        } else {
            path
        }
    }

    private fun prependSlash(path: String): String {
        return if (!path.startsWith("/")) {
            "/$path"
        } else {
            path
        }
    }

    private fun trimTrailingSlash(path: String?): String {
        return if (path!![path.length - 1] == '/') {
            path.substring(0, path.length - 1)
        } else {
            path
        }
    }

    private fun normalizeSeparateChar(pathName: String): String {
        return pathName
            .replace(File.separatorChar, '/')
            .replace('\\', '/')
    }

    private fun normalize(pathArg: String?): String {
        var path: String? = pathArg
        if (path == null || path.trim { it <= ' ' }.isEmpty()) {
            path = "/"
        }
        path = normalizeSeparateChar(path)
        path = prependSlash(appendSlash(path))
        return path
    }

    /**
     * Factory for creating SuFile instances.
     */
    interface SuFileFactory {
        /**
         * Create a SuFile instance for the given pathname.
         */
        fun create(pathname: String): SuFile = SuFile(pathname)

        /**
         * Create a SuFile instance for the given parent and child paths.
         */
        fun create(
            parent: String,
            child: String,
        ): SuFile = SuFile(parent, child)

        /**
         * Create a SuFile instance for the given parent File and child path.
         */
        fun create(
            parent: File,
            child: String,
        ): SuFile = SuFile(parent, child)

        /**
         * Create a SuFile instance for the given URI.
         */
        fun create(uri: URI): SuFile = SuFile(uri)
    }

    /**
     * Default implementation of SuFileFactory that creates SuFile instances using the default
     * constructors.
     */
    class DefaultSuFileFactory : SuFileFactory
}
