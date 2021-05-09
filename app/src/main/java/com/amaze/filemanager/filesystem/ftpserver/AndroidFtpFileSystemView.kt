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

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import org.apache.ftpserver.ftplet.FileSystemView
import org.apache.ftpserver.ftplet.FtpFile
import java.io.File
import java.net.URI

@RequiresApi(KITKAT)
class AndroidFtpFileSystemView(private var context: Context, root: String) : FileSystemView {

    private val rootPath = root
    private val rootDocumentFile = createDocumentFileFrom(rootPath)
    private var currentPath: String? = "/"

    override fun getHomeDirectory(): FtpFile =
        AndroidFtpFile(context, rootDocumentFile, resolveDocumentFileFromRoot("/"), "/")

    override fun getWorkingDirectory(): FtpFile {
        return AndroidFtpFile(
            context, rootDocumentFile,
            resolveDocumentFileFromRoot(currentPath!!), currentPath!!
        )
    }

    override fun changeWorkingDirectory(dir: String?): Boolean {
        return when {
            dir.isNullOrBlank() -> false
            dir == "/" -> {
                currentPath = "/"
                true
            }
            dir.startsWith("..") -> {
                if (currentPath.isNullOrEmpty() || currentPath == "/")
                    false
                else {
                    currentPath = normalizePath("$currentPath/$dir")
                    resolveDocumentFileFromRoot(currentPath) != null
                }
            }
            else -> {
                currentPath = when {
                    currentPath.isNullOrEmpty() || currentPath == "/" -> dir
                    !dir.startsWith("/") -> normalizePath("$currentPath/$dir")
                    else -> normalizePath(dir)
                }
                resolveDocumentFileFromRoot(currentPath) != null
            }
        }
    }

    override fun getFile(file: String): FtpFile {
        val path = if (currentPath.isNullOrEmpty() || currentPath == "/") {
            "/$file"
        } else if (file.startsWith('/')) {
            file
        } else {
            "$currentPath/$file"
        }
        return normalizePath(path).let { normalizedPath ->
            AndroidFtpFile(
                context,
                resolveDocumentFileFromRoot(getParentFrom(normalizedPath))!!, // rootDocumentFile,
                resolveDocumentFileFromRoot(normalizedPath), normalizedPath
            )
        }
    }

    override fun isRandomAccessible(): Boolean = false

    override fun dispose() {
        // context = null!!
    }

    private fun normalizePath(path: String): String {
        return when {
            path == "\\" || path == "/" -> {
                "/"
            }
            path.length <= 1 -> {
                path
            }
            else -> {
                Uri.decode(
                    URI(Uri.encode(path, "/"))
                        .normalize()
                        .toString()
                ).replace("//", "/")
            }
        }
    }

    private fun getParentFrom(normalizedPath: String): String {
        return if (normalizedPath.length <= 1) {
            normalizedPath
        } else {
            normalizedPath.substringBeforeLast('/')
        }
    }

    private fun createDocumentFileFrom(path: String): DocumentFile {
        return if (Build.VERSION.SDK_INT in KITKAT until M) {
            DocumentFile.fromFile(File(path))
        } else {
            DocumentFile.fromTreeUri(context, Uri.parse(path))!!
        }
    }

    private fun resolveDocumentFileFromRoot(path: String?): DocumentFile? {
        return if (path.isNullOrBlank() or ("/" == path) or ("./" == path))
            rootDocumentFile
        else {
            val pathElements = path!!.split('/')
            if (pathElements.isEmpty()) {
                rootDocumentFile
            } else {
                var retval: DocumentFile? = rootDocumentFile
                pathElements.forEach { pathElement ->
                    if (pathElement.isNotBlank())
                        retval = retval?.findFile(pathElement)
                }
                retval
            }
        }
    }
}
