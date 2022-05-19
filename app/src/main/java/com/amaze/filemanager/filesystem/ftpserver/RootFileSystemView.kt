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

import android.util.Log
import com.topjohnwu.superuser.io.SuFile
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.FtpFile
import org.apache.ftpserver.ftplet.User
import java.io.File
import java.net.URI
import java.util.*

class RootFileSystemView(
    private val user: User,
    private val fileFactory: SuFileFactory
) : FileSystemView {

    private var currDir: String
    private var rootDir: String

    companion object {
        private const val TAG = "RootFileSystemView"
    }

    init {
        requireNotNull(user.homeDirectory) { "User home directory can not be null" }

        // add last '/' if necessary
        var rootDir = user.homeDirectory
        rootDir = normalizeSeparateChar(rootDir)
        rootDir = appendSlash(rootDir)

        Log.d(
            TAG,
            "Native filesystem view created for user \"${user.name}\" with root \"${rootDir}\""
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

        // not a directory - return false
        dir = getPhysicalName(rootDir, currDir, dir)
        val dirObj = fileFactory.create(dir)
        if (!dirObj.isDirectory) {
            return false
        }

        // strip user root and add last '/' if necessary
        dir = dir.substring(rootDir.length - 1)
        if (dir[dir.length - 1] != '/') {
            dir = "$dir/"
        }

        currDir = dir
        return true
    }

    override fun getFile(file: String): FtpFile {
        // get actual file object
        val physicalName = getPhysicalName(rootDir, currDir, file)
        val fileObj = fileFactory.create(physicalName)

        // strip the root directory and return
        val userFileName = physicalName.substring(rootDir.length - 1)
        return RootFtpFile(userFileName, fileObj, user)
    }

    override fun isRandomAccessible(): Boolean = false

    override fun dispose() = Unit

    /**
     * Get the physical canonical file name. It works like
     * File.getCanonicalPath().
     *
     * @param rootDir
     * The root directory.
     * @param currDir
     * The current directory. It will always be with respect to the
     * root directory.
     * @param fileName
     * The input file name.
     * @return The return string will always begin with the root directory. It
     * will never be null.
     */
    private fun getPhysicalName(
        rootDir: String,
        currDir: String,
        fileName: String
    ): String {
        // normalize root dir
        var normalizedRootDir: String = normalizeSeparateChar(rootDir)
        normalizedRootDir = appendSlash(normalizedRootDir)

        // normalize file name
        val normalizedFileName = normalizeSeparateChar(fileName)
        var result: String?

        // if file name is relative, set resArg to root dir + curr dir
        // if file name is absolute, set resArg to root dir
        result = if (normalizedFileName[0] != '/') {
            // file name is relative
            val normalizedCurrDir = normalize(currDir)
            normalizedRootDir + normalizedCurrDir.substring(1)
        } else {
            normalizedRootDir
        }

        // strip last '/'
        result = trimTrailingSlash(result)

        // replace ., ~ and ..
        // in this loop resArg will never end with '/'
        val st = StringTokenizer(normalizedFileName, "/")
        while (st.hasMoreTokens()) {
            val tok = st.nextToken()

            // . => current directory
            if (tok == ".") {
                // ignore and move on
            } else if (tok == "..") {
                // .. => parent directory (if not root)
                if (result!!.startsWith(normalizedRootDir)) {
                    val slashIndex = result.lastIndexOf('/')
                    if (slashIndex != -1) {
                        result = result.substring(0, slashIndex)
                    }
                }
            } else if (tok == "~") {
                // ~ => home directory (in this case the root directory)
                result = trimTrailingSlash(normalizedRootDir)
                continue
            } else {
                result = "$result/$tok"
            }
        }

        // add last slash if necessary
        if (result!!.length + 1 == normalizedRootDir.length) {
            result += '/'
        }

        // make sure we did not end up above root dir
        if (!result.startsWith(normalizedRootDir)) {
            result = normalizedRootDir
        }
        return result
    }

    /**
     * Append trailing slash ('/') if missing
     */
    private fun appendSlash(path: String): String {
        return if (!path.endsWith("/")) {
            "$path/"
        } else {
            path
        }
    }

    /**
     * Prepend leading slash ('/') if missing
     */
    private fun prependSlash(path: String): String {
        return if (!path.startsWith("/")) {
            "/$path"
        } else {
            path
        }
    }

    /**
     * Trim trailing slash ('/') if existing
     */
    private fun trimTrailingSlash(path: String?): String {
        return if (path!![path.length - 1] == '/') {
            path.substring(0, path.length - 1)
        } else {
            path
        }
    }

    /**
     * Normalize separate character. Separate character should be '/' always.
     */
    private fun normalizeSeparateChar(pathName: String): String {
        return pathName
            .replace(File.separatorChar, '/')
            .replace('\\', '/')
    }

    /**
     * Normalize separator char, append and prepend slashes. Default to
     * defaultPath if null or empty
     */
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
     * Interface responsible for creating [SuFile] instances.
     *
     * Mainly for facilitating tests.
     */
    interface SuFileFactory {
        /**
         * Create SuFile.
         */
        fun create(pathname: String): SuFile = SuFile(pathname)

        /**
         * Create SuFile.
         */
        fun create(parent: String, child: String): SuFile = SuFile(parent, child)

        /**
         * Create SuFile.
         */
        fun create(parent: File, child: String): SuFile = SuFile(parent, child)

        /**
         * Create SuFile.
         */
        fun create(uri: URI): SuFile = SuFile(uri)
    }

    /**
     * Marker class as default implementation of [SuFileFactory].
     */
    class DefaultSuFileFactory : SuFileFactory
}
