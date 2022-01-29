/*
 * Copyright (C) 2014-2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.file_operations.filesystem.filetypes

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.annotation.Native

abstract class AmazeFilesystem {
    /* -- Normalization and construction -- */
    /** filesystem prefix  */
    abstract val prefix: String

    /** Is the path of this filesystem?  */
    open fun isPathOfThisFilesystem(path: String): Boolean {
        return path.startsWith(prefix)
    }

    open fun getSeparator(): Char = STANDARD_SEPARATOR

    /**
     * Convert the given pathname string to normal form. If the string is already in normal form then
     * it is simply returned.
     */
    abstract fun normalize(path: String): String

    /**
     * Compute the length of this pathname string's prefix. The pathname string must be in normal
     * form.
     */
    open fun prefixLength(path: String): Int {
        return prefix.length
    }

    /**
     * Resolve the child pathname string against the parent. Both strings must be in normal form, and
     * the result will be in normal form.
     */
    abstract fun resolve(parent: String, child: String): String

    /**
     * Return the parent pathname string to be used when the parent-directory argument in one of the
     * two-argument File constructors is the empty pathname.
     */
    abstract val defaultParent: String
    /* -- Path operations -- */
    /** Tell whether or not the given abstract pathname is absolute.  */
    abstract fun isAbsolute(f: AmazeFile): Boolean

    /**
     * Resolve the given abstract pathname into absolute form. Invoked by the getAbsolutePath and
     * getCanonicalPath methods in the [AmazeFile] class.
     */
    abstract fun resolve(f: AmazeFile): String

    @Throws(IOException::class)
    abstract fun canonicalize(path: String?): String

    /**
     * Return the simple boolean attributes for the file or directory denoted by the given abstract
     * pathname, or zero if it does not exist or some other I/O error occurs.
     */
    fun getBooleanAttributes(f: AmazeFile, contextProvider: ContextProvider): Int {
        val file = File(f.path)
        var r = 0
        if (exists(f, contextProvider)) {
            r = r or BA_EXISTS
            if (isFile(f, contextProvider)) {
                r = r or BA_REGULAR
            }
            if (isDirectory(f, contextProvider)) {
                r = r or BA_DIRECTORY
            }
            if (isHidden(f)) {
                r = r or BA_HIDDEN
            }
        }
        return r
    }

    abstract fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean
    abstract fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean
    abstract fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean
    abstract fun isHidden(f: AmazeFile): Boolean

    /**
     * Check whether the file or directory denoted by the given abstract pathname may be accessed by
     * this process. Return false if access is denied or an I/O error occurs
     */
    abstract fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean

    /**
     * Check whether the file or directory denoted by the given abstract pathname may be accessed by
     * this process. Return false if access is denied or an I/O error occurs
     */
    abstract fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean

    /**
     * Check whether the file or directory denoted by the given abstract pathname may be accessed by
     * this process. Return false if access is denied or an I/O error occurs
     */
    abstract fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean

    /**
     * Check whether the file or directory denoted by the given abstract pathname may be accessed by
     * this process. Return false if access is denied or an I/O error occurs
     *
     * Android-added: b/25878034, to support F.exists() reimplementation.
     */
    abstract fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean

    /**
     * Set on or off the access permission (to owner only or to all) to the file or directory denoted
     * by the given abstract pathname, based on the parameters enable, access and oweronly.
     */
    abstract fun setExecutable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean

    /**
     * Set on or off the access permission (to owner only or to all) to the file or directory denoted
     * by the given abstract pathname, based on the parameters enable, access and oweronly.
     */
    abstract fun setWritable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean

    /**
     * Set on or off the access permission (to owner only or to all) to the file or directory denoted
     * by the given abstract pathname, based on the parameters enable, access and oweronly.
     */
    abstract fun setReadable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean

    /**
     * Return the time at which the file or directory denoted by the given abstract pathname was last
     * modified, or zero if it does not exist or some other I/O error occurs.
     */
    abstract fun getLastModifiedTime(f: AmazeFile): Long

    /**
     * Return the length in bytes of the file denoted by the given abstract pathname, or zero if it
     * does not exist, or some other I/O error occurs.
     *
     *
     * Note: for directories, this *could* return the size
     */
    @Throws(IOException::class)
    abstract fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long
    /* -- File operations -- */
    /**
     * Create a new empty file with the given pathname. Return `true` if the file was
     * created and `false` if a file or directory with the given pathname already exists.
     * Throw an IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    abstract fun createFileExclusively(pathname: String): Boolean

    /**
     * Delete the file or directory denoted by the given abstract pathname, returning `true
     ` *  if and only if the operation succeeds.
     */
    abstract fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean

    /**
     * List the elements of the directory denoted by the given abstract pathname. Return an array of
     * strings naming the elements of the directory if successful; otherwise, return `null`
     * .
     */
    abstract fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String>?
    abstract fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream?
    abstract fun getOutputStream(
        f: AmazeFile,
        contextProvider: ContextProvider
    ): OutputStream?

    /**
     * Create a new directory denoted by the given abstract pathname, returning `true` if
     * and only if the operation succeeds.
     */
    abstract fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean

    /**
     * Rename the file or directory denoted by the first abstract pathname to the second abstract
     * pathname, returning `true` if and only if the operation succeeds.
     */
    abstract fun rename(
        file1: AmazeFile,
        file2: AmazeFile,
        contextProvider: ContextProvider
    ): Boolean

    /**
     * Set the last-modified time of the file or directory denoted by the given abstract pathname,
     * returning `true` if and only if the operation succeeds.
     */
    abstract fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean

    /**
     * Mark the file or directory denoted by the given abstract pathname as read-only, returning
     * `true` if and only if the operation succeeds.
     */
    abstract fun setReadOnly(f: AmazeFile): Boolean
    protected fun removePrefix(path: String): String {
        return path.substring(prefixLength(path))
    }
    /* -- Filesystem interface -- */
    abstract fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long
    abstract fun getFreeSpace(f: AmazeFile): Long
    abstract fun getUsableSpace(f: AmazeFile): Long
    /* -- Basic infrastructure -- */
    /** Compare two abstract pathnames lexicographically.  */
    open fun compare(f1: AmazeFile, f2: AmazeFile): Int {
        return f1.path.compareTo(f2.path)
    }

    /** Compute the hash code of an abstract pathname.  */
    open fun hashCode(f: AmazeFile): Int {
        return basicUnixHashCode(f.path)
    }

    companion object {
        /** Return the local filesystem's name-separator character.  */
        const val STANDARD_SEPARATOR = '/'

        /* -- Attribute accessors -- */ /* Constants for simple boolean attributes */
        @JvmField
        @Native
        val BA_EXISTS = 0x01

        @JvmField
        @Native
        val BA_REGULAR = 0x02

        @JvmField
        @Native
        val BA_DIRECTORY = 0x04

        @JvmField
        @Native
        val BA_HIDDEN = 0x08

        // Flags for enabling/disabling performance optimizations for file
        // name canonicalization
        // Android-changed: Disabled caches for security reasons (b/62301183)
        // static boolean useCanonCaches      = true;
        // static boolean useCanonPrefixCache = true;
        var useCanonCaches = false
        var useCanonPrefixCache = false
        private fun getBooleanProperty(prop: String, defaultVal: Boolean): Boolean {
            val `val` = System.getProperty(prop) ?: return defaultVal
            return `val`.equals("true", ignoreCase = true)
        }

        /*
   * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
   * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
   *
   * This code is free software; you can redistribute it and/or modify it
   * under the terms of the GNU General Public License version 2 only, as
   * published by the Free Software Foundation.  Oracle designates this
   * particular file as subject to the "Classpath" exception as provided
   * by Oracle in the LICENSE file that accompanied this code.
   *
   * This code is distributed in the hope that it will be useful, but WITHOUT
   * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
   * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
   * version 2 for more details (a copy is included in the LICENSE file that
   * accompanied this code).
   *
   * You should have received a copy of the GNU General Public License version
   * 2 along with this work; if not, write to the Free Software Foundation,
   * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
   *
   * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
   * or visit www.oracle.com if you need additional information or have any
   * questions.
   *
   * A normal Unix pathname does not contain consecutive slashes and does not end
   * with a slash. The empty string and "/" are special cases that are also
   * considered normal.
   */
        @JvmStatic
        fun simpleUnixNormalize(pathname: String): String {
            val n = pathname.length
            val normalized = pathname.toCharArray()
            var index = 0
            var prevChar = 0.toChar()
            for (i in 0 until n) {
                val current = normalized[i]
                // Remove duplicate slashes.
                if (!(current == '/' && prevChar == '/')) {
                    normalized[index++] = current
                }
                prevChar = current
            }

            // Omit the trailing slash, except when pathname == "/".
            if (prevChar == '/' && n > 1) {
                index--
            }
            return if (index != n) String(normalized, 0, index) else pathname
        }

        /*
   * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
   * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
   *
   * This code is free software; you can redistribute it and/or modify it
   * under the terms of the GNU General Public License version 2 only, as
   * published by the Free Software Foundation.  Oracle designates this
   * particular file as subject to the "Classpath" exception as provided
   * by Oracle in the LICENSE file that accompanied this code.
   *
   * This code is distributed in the hope that it will be useful, but WITHOUT
   * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
   * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
   * version 2 for more details (a copy is included in the LICENSE file that
   * accompanied this code).
   *
   * You should have received a copy of the GNU General Public License version
   * 2 along with this work; if not, write to the Free Software Foundation,
   * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
   *
   * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
   * or visit www.oracle.com if you need additional information or have any
   * questions.
   */
        // Invariant: Both |parent| and |child| are normalized paths.
        @JvmStatic
        fun basicUnixResolve(parent: String, child: String): String {
            if (child.isEmpty() || child == "/") {
                return parent
            }
            if (child[0] == '/') {
                return if (parent == "/") {
                    child
                } else parent + child
            }
            return if (parent == "/") {
                parent + child
            } else "$parent/$child"
        }

        fun basicUnixHashCode(path: String): Int {
            return path.hashCode() xor 1234321
        }

        init {
            useCanonCaches =
                getBooleanProperty("sun.io.useCanonCaches", useCanonCaches)
            useCanonPrefixCache =
                getBooleanProperty("sun.io.useCanonPrefixCache", useCanonPrefixCache)
        }
    }
}
