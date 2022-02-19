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

import android.os.Parcel
import android.util.Log
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.box.BoxAmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.dropbox.DropboxAmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.gdrive.GoogledriveAmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.cloud.onedrive.OnedriveAmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.SmbAmazeFilesystem
import kotlinx.parcelize.Parceler
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.*

// Android-added: Info about UTF-8 usage in filenames.
/**
 * An abstract representation of file and directory pathnames.
 *
 *
 * User interfaces and operating systems use system-dependent *pathname strings* to name
 * files and directories. This class presents an abstract, system-independent view of hierarchical
 * pathnames. An *abstract pathname* has two components:
 *
 *
 *  1. An optional system-dependent *prefix* string, such as a disk-drive specifier, `
 * "/"`&nbsp;for the UNIX root directory, or `"\\\\"`&nbsp;for a Microsoft
 * Windows UNC pathname, and
 *  1. A sequence of zero or more string *names*.
 *
 *
 * The first name in an abstract pathname may be a directory name or, in the case of Microsoft
 * Windows UNC pathnames, a hostname. Each subsequent name in an abstract pathname denotes a
 * directory; the last name may denote either a directory or a file. The *empty* abstract
 * pathname has no prefix and an empty name sequence.
 *
 *
 * The conversion of a pathname string to or from an abstract pathname is inherently
 * system-dependent. When an abstract pathname is converted into a pathname string, each name is
 * separated from the next by a single copy of the default *separator character*. The default
 * name-separator character is defined by the system property `file.separator`, and is
 * made available in the public static fields `[ ][.separator]` and `[.separatorChar]` of this class. When a pathname string
 * is converted into an abstract pathname, the names within it may be separated by the default
 * name-separator character or by any other name-separator character that is supported by the
 * underlying system.
 *
 *
 * A pathname, whether abstract or in string form, may be either *absolute* or
 * *relative*. An absolute pathname is complete in that no other information is required in
 * order to locate the file that it denotes. A relative pathname, in contrast, must be interpreted
 * in terms of information taken from some other pathname. By default the classes in the `
 * java.io` package always resolve relative pathnames against the current user directory. This
 * directory is named by the system property `user.dir`, and is typically the directory
 * in which the Java virtual machine was invoked.
 *
 *
 * The *parent* of an abstract pathname may be obtained by invoking the [.getParent]
 * method of this class and consists of the pathname's prefix and each name in the pathname's name
 * sequence except for the last. Each directory's absolute pathname is an ancestor of any
 * <tt>AmazeFile</tt> object with an absolute abstract pathname which begins with the directory's
 * absolute pathname. For example, the directory denoted by the abstract pathname <tt>"/usr"</tt> is
 * an ancestor of the directory denoted by the pathname <tt>"/usr/local/bin"</tt>.
 *
 *
 * The prefix concept is used to handle root directories on UNIX platforms, and drive specifiers,
 * root directories and UNC pathnames on Microsoft Windows platforms, as follows:
 *
 *
 *  * For UNIX platforms, the prefix of an absolute pathname is always `"/"`. Relative
 * pathnames have no prefix. The abstract pathname denoting the root directory has the prefix
 * `"/"` and an empty name sequence.
 *  * For Microsoft Windows platforms, the prefix of a pathname that contains a drive specifier
 * consists of the drive letter followed by `":"` and possibly followed by `
 * "\\"` if the pathname is absolute. The prefix of a UNC pathname is `"\\\\"
` * ; the hostname and the share name are the first two names in the name sequence. A
 * relative pathname that does not specify a drive has no prefix.
 *
 *
 *
 * Instances of this class may or may not denote an actual file-system object such as a file or a
 * directory. If it does denote such an object then that object resides in a *partition*. A
 * partition is an operating system-specific portion of storage for a file system. A single storage
 * device (e.g. a physical disk-drive, flash memory, CD-ROM) may contain multiple partitions. The
 * object, if any, will reside on the partition <a name="partName">named</a> by some ancestor of the
 * absolute form of this pathname.
 *
 *
 * A file system may implement restrictions to certain operations on the actual file-system
 * object, such as reading, writing, and executing. These restrictions are collectively known as
 * *access permissions*. The file system may have multiple sets of access permissions on a
 * single object. For example, one set may apply to the object's *owner*, and another may apply
 * to all other users. The access permissions on an object may cause some methods in this class to
 * fail.
 *
 *
 * On Android strings are converted to UTF-8 byte sequences when sending filenames to the
 * operating system, and byte sequences returned by the operating system (from the various `list` methods) are converted to strings by decoding them as UTF-8 byte sequences.
 *
 * @author unascribed
 */
class AmazeFile : Comparable<AmazeFile?> {
    companion object {
        @JvmStatic
        val TAG = AmazeFile::class.java.simpleName
        @JvmStatic
        private val filesystems: MutableList<AmazeFilesystem> = ArrayList()

        @JvmStatic
        fun addFilesystem(amazeFilesystem: AmazeFilesystem) {
            filesystems.add(amazeFilesystem)
        }

        init {
            BoxAmazeFilesystem
            DropboxAmazeFilesystem
            GoogledriveAmazeFilesystem
            OnedriveAmazeFilesystem

            SmbAmazeFilesystem
        }

        private fun slashify(path: String, isDirectory: Boolean): String {
            var p = path
            if (File.separatorChar != '/') p = p.replace(File.separatorChar, '/')
            if (!p.startsWith("/")) p = "/$p"
            if (!p.endsWith("/") && isDirectory) p = "$p/"
            return p
        }

        object AmazeFileParceler : Parceler<AmazeFile> {
            override fun create(parcel: Parcel): AmazeFile =
                AmazeFile(parcel.readString() ?: "")

            override fun AmazeFile.write(parcel: Parcel, flags: Int) {
                parcel.writeString(absolutePath)
            }
        }
    }

    /** The FileSystem object representing the platform's local file system.  */
    private lateinit var fs: AmazeFilesystem

    /**
     * Converts this abstract pathname into a pathname string. The resulting string uses the [separator]
     * to separate the names in the name sequence.
     *
     * This abstract pathname's normalized pathname string. A normalized pathname string uses the
     * default name-separator character and does not contain any duplicate or redundant separators.
     */
    val path: String

    /** The flag indicating whether the file path is invalid.  */
    @Transient
    private var status: PathStatus? = null

    /** Returns the length of this abstract pathname's prefix. For use by FileSystem classes.  */
    /** The length of this abstract pathname's prefix, or zero if it has no prefix.  */
    @Transient
    val prefixLength: Int

    /**
     * The system-dependent default name-separator character. This field is initialized to contain the
     * first character of the value of the system property `file.separator`. On UNIX
     * systems the value of this field is `'/'`; on Microsoft Windows systems it is `
     * '\\'`.
     *
     * @see java.lang.System.getProperty
     */
    val separatorChar: Char

    /**
     * The system-dependent default name-separator character, represented as a string for convenience.
     * This string contains a single character, namely `[.separatorChar]`.
     */
    val separator: String

    /** Enum type that indicates the status of a file path.  */
    private enum class PathStatus {
        INVALID, CHECKED
    }

    /**
     * Check if the file has an invalid path. Currently, the inspection of a file path is very
     * limited, and it only covers Nul character check. Returning true means the path is definitely
     * invalid/garbage. But returning false does not guarantee that the path is valid.
     *
     * @return true if the file path is invalid.
     */
    val isInvalid: Boolean
        get() {
            if (status == null) {
                status = if (path.indexOf('\u0000') < 0) PathStatus.CHECKED else PathStatus.INVALID
            }
            return status == PathStatus.INVALID
        }
    /* -- Constructors -- */
    /** Internal constructor for already-normalized pathname strings.  */
    private constructor(pathname: String, prefixLength: Int) {
        loadFilesystem(pathname)
        separatorChar = fs.getSeparator()
        separator = "" + separatorChar
        path = pathname
        this.prefixLength = prefixLength
    }

    /**
     * Internal constructor for already-normalized pathname strings. The parameter order is used to
     * disambiguate this method from the public(AmazeFile, String) constructor.
     */
    private constructor(child: String, parent: AmazeFile) {
        assert(parent.path != "")
        loadFilesystem(parent.path)
        separatorChar = fs.getSeparator()
        separator = "" + separatorChar
        path = fs.resolve(parent.path, child)
        prefixLength = parent.prefixLength
    }

    /**
     * Creates a new `AmazeFile` instance by converting the given pathname string into an
     * abstract pathname. If the given string is the empty string, then the result is the empty
     * abstract pathname.
     *
     * @param pathname A pathname string
     */
    constructor(pathname: String) {
        loadFilesystem(pathname)
        separatorChar = fs.getSeparator()
        separator = "" + separatorChar
        path = fs.normalize(pathname)
        prefixLength = fs.prefixLength(path)
    }

    /**
     * Note: The two-argument File constructors do not interpret an empty
     * parent abstract pathname as the current user directory.  An empty parent
     * instead causes the child to be resolved against the system-dependent
     * directory defined by the FileSystem.getDefaultParent method.  On Unix
     * this default is "/", while on Microsoft Windows it is "\\".  This is required for
     * compatibility with the original behavior of this class.
     *
     * Creates a new `AmazeFile` instance from a parent pathname string and a child
     * pathname string.
     *
     *
     * If `parent` is `null` then the new `AmazeFile` instance is
     * created as if by invoking the single-argument `AmazeFile` constructor on the given
     * `child` pathname string.
     *
     *
     * Otherwise the `parent` pathname string is taken to denote a directory, and the
     * `child` pathname string is taken to denote either a directory or a file. If the
     * `child` pathname string is absolute then it is converted into a relative pathname in
     * a system-dependent way. If `parent` is the empty string then the new `AmazeFile
     ` *  instance is created by converting `child` into an abstract pathname and
     * resolving the result against a system-dependent default directory. Otherwise each pathname
     * string is converted into an abstract pathname and the child abstract pathname is resolved
     * against the parent.
     *
     * @param parent The parent pathname string
     * @param child The child pathname string
     */
    constructor(parent: String?, child: String) {
        // BEGIN Android-changed: b/25859957, app-compat; don't substitute empty parent.
        if (parent != null && !parent.isEmpty()) {
            loadFilesystem(parent)
            separatorChar = fs.getSeparator()
            separator = "" + separatorChar
            path = fs.resolve(fs.normalize(parent), fs.normalize(child))
            // END Android-changed: b/25859957, app-compat; don't substitute empty parent.
        } else {
            loadFilesystem(child)
            separatorChar = fs.getSeparator()
            separator = "" + separatorChar
            path = fs.normalize(child)
        }
        prefixLength = fs.prefixLength(path)
    }

    /**
     * Creates a new `AmazeFile` instance from a parent abstract pathname and a child
     * pathname string.
     *
     *
     * If `parent` is `null` then the new `AmazeFile` instance is
     * created as if by invoking the single-argument `AmazeFile` constructor on the given
     * `child` pathname string.
     *
     *
     * Otherwise the `parent` abstract pathname is taken to denote a directory, and the
     * `child` pathname string is taken to denote either a directory or a file. If the
     * `child` pathname string is absolute then it is converted into a relative pathname in
     * a system-dependent way. If `parent` is the empty abstract pathname then the new
     * `AmazeFile` instance is created by converting `child` into an abstract
     * pathname and resolving the result against a system-dependent default directory. Otherwise each
     * pathname string is converted into an abstract pathname and the child abstract pathname is
     * resolved against the parent.
     *
     * @param parent The parent abstract pathname
     * @param child The child pathname string
     */
    constructor(parent: AmazeFile?, child: String) {
        if (parent != null) {
            loadFilesystem(parent.path)
            separatorChar = fs.getSeparator()
            separator = "" + separatorChar
            if (parent.path == "") {
                path = fs.resolve(fs.defaultParent, fs.normalize(child))
            } else {
                path = fs.resolve(parent.path, fs.normalize(child))
            }
        } else {
            loadFilesystem(child)
            separatorChar = fs.getSeparator()
            separator = "" + separatorChar
            path = fs.normalize(child)
        }
        prefixLength = fs.prefixLength(path)
    }

    private fun loadFilesystem(path: String) {
        var loadedAFs = false

        for (filesystem in filesystems) {
            if (filesystem.isPathOfThisFilesystem(path)) {
                fs = filesystem
                loadedAFs = true
            }
        }

        if (!loadedAFs) {
            Log.e(
                TAG,
                "Failed to load a filesystem, did you forget to add the class to the " +
                    "[AmazeFile]'s companion object initialization block?"
            )
        }
    }
    /* -- Path-component accessors -- */
    /**
     * Returns the name of the file or directory denoted by this abstract pathname. This is just the
     * last name in the pathname's name sequence. If the pathname's name sequence is empty, then the
     * empty string is returned.
     *
     * @return The name of the file or directory denoted by this abstract pathname, or the empty
     * string if this pathname's name sequence is empty
     */
    val name: String
        get() {
            val index = path.lastIndexOf(separatorChar)
            if (index < prefixLength) {
                return path.substring(prefixLength)
            }
            if (path.endsWith("/")) {
                val newIndex = path.substring(0, path.length - 2).lastIndexOf(separatorChar)
                return if (newIndex < prefixLength) {
                    path.substring(prefixLength)
                } else path.substring(newIndex + 1)
            }
            return path.substring(index + 1)
        }

    /**
     * Returns the pathname string of this abstract pathname's parent, or `null` if this
     * pathname does not name a parent directory.
     *
     *
     * The *parent* of an abstract pathname consists of the pathname's prefix, if any, and
     * each name in the pathname's name sequence except for the last. If the name sequence is empty
     * then the pathname does not name a parent directory.
     *
     * @return The pathname string of the parent directory named by this abstract pathname, or `
     * null` if this pathname does not name a parent
     */
    val parent: String?
        get() {
            val index = path.lastIndexOf(separatorChar)
            if (index < prefixLength) {
                return if (prefixLength > 0 && path.length > prefixLength) {
                    path.substring(0, prefixLength)
                } else null
            }
            if (path.endsWith("/")) {
                val newIndex = path.substring(0, path.length - 2).lastIndexOf(separatorChar)
                return if (newIndex < prefixLength) {
                    if (prefixLength > 0 && path.length > prefixLength) {
                        path.substring(0, prefixLength)
                    } else null
                } else path.substring(0, newIndex)
            }
            return path.substring(0, index)
        }

    /**
     * Returns the abstract pathname of this abstract pathname's parent, or `null` if this
     * pathname does not name a parent directory.
     *
     *
     * The *parent* of an abstract pathname consists of the pathname's prefix, if any, and
     * each name in the pathname's name sequence except for the last. If the name sequence is empty
     * then the pathname does not name a parent directory.
     *
     * @return The abstract pathname of the parent directory named by this abstract pathname, or
     * `null` if this pathname does not name a parent
     */
    val parentFile: AmazeFile?
        get() {
            val p = parent ?: return null
            return AmazeFile(p, prefixLength)
        }
    /* -- Path operations -- */ // Android-changed: Android-specific path information
    /**
     * Tests whether this abstract pathname is absolute. The definition of absolute pathname is system
     * dependent. On Android, absolute paths start with the character '/'.
     *
     * @return `true` if this abstract pathname is absolute, `false` otherwise
     */
    val isAbsolute: Boolean
        get() = fs.isAbsolute(this)
    // Android-changed: Android-specific path information
    /**
     * Returns the absolute path of this file. An absolute path is a path that starts at a root of the
     * file system. On Android, there is only one root: `/`.
     *
     *
     * A common use for absolute paths is when passing paths to a `Process` as command-line
     * arguments, to remove the requirement implied by relative paths, that the child must have the
     * same working directory as its parent.
     *
     * @return The absolute pathname string denoting the same file or directory as this abstract
     * pathname
     * @see java.io.File.isAbsolute
     */
    val absolutePath: String
        get() = fs.resolve(this)

    /**
     * Returns the absolute form of this abstract pathname. Equivalent to `
     * new&nbsp;File(this.[.getAbsolutePath])`.
     *
     * @return The absolute abstract pathname denoting the same file or directory as this abstract
     * pathname
     */
    val absoluteFile: AmazeFile
        get() {
            val absPath = absolutePath
            return AmazeFile(absPath, fs.prefixLength(absPath))
        }

    /**
     * Returns the canonical pathname string of this abstract pathname.
     *
     *
     * A canonical pathname is both absolute and unique. The precise definition of canonical form
     * is system-dependent. This method first converts this pathname to absolute form if necessary, as
     * if by invoking the [.getAbsolutePath] method, and then maps it to its unique form in a
     * system-dependent way. This typically involves removing redundant names such as <tt>"."</tt> and
     * <tt>".."</tt> from the pathname, resolving symbolic links (on UNIX platforms), and converting
     * drive letters to a standard case (on Microsoft Windows platforms).
     *
     *
     * Every pathname that denotes an existing file or directory has a unique canonical form. Every
     * pathname that denotes a nonexistent file or directory also has a unique canonical form. The
     * canonical form of the pathname of a nonexistent file or directory may be different from the
     * canonical form of the same pathname after the file or directory is created. Similarly, the
     * canonical form of the pathname of an existing file or directory may be different from the
     * canonical form of the same pathname after the file or directory is deleted.
     *
     * @return The canonical pathname string denoting the same file or directory as this abstract
     * pathname
     * @throws IOException If an I/O error occurs, which is possible because the construction of the
     * canonical pathname may require filesystem queries
     * @see Path.toRealPath
     */
    @get:Throws(IOException::class)
    val canonicalPath: String
        get() {
            if (isInvalid) {
                throw IOException("Invalid file path")
            }
            return fs.canonicalize(fs.resolve(this))
        }

    /**
     * Returns the canonical form of this abstract pathname. Equivalent to `
     * new&nbsp;File(this.[.getCanonicalPath])`.
     *
     * @return The canonical pathname string denoting the same file or directory as this abstract
     * pathname
     * @throws IOException If an I/O error occurs, which is possible because the construction of the
     * canonical pathname may require filesystem queries
     * @see Path.toRealPath
     */
    @get:Throws(IOException::class)
    val canonicalFile: AmazeFile
        get() {
            val canonPath = canonicalPath
            return AmazeFile(canonPath, fs.prefixLength(canonPath))
        }
    /* -- Attribute accessors -- */ // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on android
    /**
     * Tests whether the application can read the file denoted by this abstract pathname.
     *
     * @return `true` if and only if the file specified by this abstract pathname exists
     * *and* can be read by the application; `false` otherwise
     */
    fun canRead(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.canRead(this, contextProvider)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on android
    /**
     * Tests whether the application can modify the file denoted by this abstract pathname.
     *
     * @return `true` if and only if the file system actually contains a file denoted by
     * this abstract pathname *and* the application is allowed to write to the file; `
     * false` otherwise.
     */
    fun canWrite(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.canWrite(this, contextProvider)
    }

    /**
     * Tests whether the file or directory denoted by this abstract pathname exists.
     *
     * @return `true` if and only if the file or directory denoted by this abstract
     * pathname exists; `false` otherwise
     */
    fun exists(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.canAccess(this, contextProvider)

        // Android-changed: b/25878034 work around SELinux stat64 denial.
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a directory.
     *
     *
     * Where it is required to distinguish an I/O exception from the case that the file is not a
     * directory, or where several attributes of the same file are required at the same time, then the
     * [Files.readAttributes][java.nio.file.Files.readAttributes] method
     * may be used.
     *
     * @return `true` if and only if the file denoted by this abstract pathname exists
     * *and* is a directory; `false` otherwise
     */
    fun isDirectory(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.getBooleanAttributes(this, contextProvider) and AmazeFilesystem.BA_DIRECTORY != 0
    }

    /**
     * Tests whether the file denoted by this abstract pathname is a normal file. A file is
     * *normal* if it is not a directory and, in addition, satisfies other system-dependent
     * criteria. Any non-directory file created by a Java application is guaranteed to be a normal
     * file.
     *
     *
     * Where it is required to distinguish an I/O exception from the case that the file is not a
     * normal file, or where several attributes of the same file are required at the same time, then
     * the [Files.readAttributes][java.nio.file.Files.readAttributes]
     * method may be used.
     *
     * @return `true` if and only if the file denoted by this abstract pathname exists
     * *and* is a normal file; `false` otherwise
     */
    fun isFile(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.getBooleanAttributes(this, contextProvider) and AmazeFilesystem.BA_REGULAR != 0
    }

    /**
     * Tests whether the file named by this abstract pathname is a hidden file. The exact definition
     * of *hidden* is system-dependent. On UNIX systems, a file is considered to be hidden if
     * its name begins with a period character (`'.'`). On Microsoft Windows systems, a
     * file is considered to be hidden if it has been marked as such in the filesystem.
     *
     * @return `true` if and only if the file denoted by this abstract pathname is hidden
     * according to the conventions of the underlying platform
     */
    fun isHidden(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.getBooleanAttributes(this, contextProvider) and AmazeFilesystem.BA_HIDDEN != 0
    }

    /**
     * Returns the time that the file denoted by this abstract pathname was last modified.
     *
     *
     * Where it is required to distinguish an I/O exception from the case where `0L` is
     * returned, or where several attributes of the same file are required at the same time, or where
     * the time of last access or the creation time are required, then the [ ][java.nio.file.Files.readAttributes] method may be
     * used.
     *
     * @return A `long` value representing the time the file was last modified, measured in
     * milliseconds since the epoch (00:00:00 GMT, January 1, 1970), or `0L` if the
     * file does not exist or if an I/O error occurs
     */
    fun lastModified(): Long {
        return if (isInvalid) {
            0L
        } else fs.getLastModifiedTime(this)
    }

    /**
     * Returns the length of the file denoted by this abstract pathname. The return value is
     * unspecified if this pathname denotes a directory.
     *
     *
     * Where it is required to distinguish an I/O exception from the case that `0L` is
     * returned, or where several attributes of the same file are required at the same time, then the
     * [Files.readAttributes][java.nio.file.Files.readAttributes] method
     * may be used.
     *
     * @return The length, in bytes, of the file denoted by this abstract pathname, or `0L`
     * if the file does not exist. Some operating systems may return `0L` for pathnames
     * denoting system-dependent entities such as devices or pipes.
     */
    @Throws(IOException::class)
    fun length(contextProvider: ContextProvider): Long {
        return if (isInvalid) {
            0L
        } else fs.getLength(this, contextProvider)
    }
    /* -- File operations -- */
    /**
     * Atomically creates a new, empty file named by this abstract pathname if and only if a file with
     * this name does not yet exist. The check for the existence of the file and the creation of the
     * file if it does not exist are a single operation that is atomic with respect to all other
     * filesystem activities that might affect the file.
     *
     *
     * Note: this method should *not* be used for file-locking, as the resulting protocol
     * cannot be made to work reliably. The [FileLock][java.nio.channels.FileLock] facility
     * should be used instead.
     *
     * @return `true` if the named file does not exist and was successfully created; `
     * false` if the named file already exists
     * @throws IOException If an I/O error occurred
     */
    @Throws(IOException::class)
    fun createNewFile(): Boolean {
        if (isInvalid) {
            throw IOException("Invalid file path")
        }
        return fs.createFileExclusively(path)
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname.
     *
     *
     * Note that the [java.nio.file.Files] class defines the [ ][java.nio.file.Files.delete] method to throw an [IOException] when a file
     * cannot be deleted. This is useful for error reporting and to diagnose why a file cannot be
     * deleted.
     *
     * @return `true` if and only if the file or directory is successfully deleted; `
     * false` otherwise
     */
    fun delete(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.delete(this, contextProvider)
    }
    // Android-added: Additional information about Android behaviour.
    /**
     * Requests that the file or directory denoted by this abstract pathname be deleted when the
     * virtual machine terminates. Files (or directories) are deleted in the reverse order that they
     * are registered. Invoking this method to delete a file or directory that is already registered
     * for deletion has no effect. Deletion will be attempted only for normal termination of the
     * virtual machine, as defined by the Java Language Specification.
     *
     *
     * Once deletion has been requested, it is not possible to cancel the request. This method
     * should therefore be used with care.
     *
     *
     * Note: this method should *not* be used for file-locking, as the resulting protocol
     * cannot be made to work reliably. The [FileLock][java.nio.channels.FileLock] facility
     * should be used instead.
     *
     *
     * *Note that on Android, the application lifecycle does not include VM termination, so
     * calling this method will not ensure that files are deleted*. Instead, you should use the
     * most appropriate out of:
     *
     *
     *  * Use a `finally` clause to manually invoke [.delete].
     *  * Maintain your own set of files to delete, and process it at an appropriate point in your
     * application's lifecycle.
     *  * Use the Unix trick of deleting the file as soon as all readers and writers have opened
     * it. No new readers/writers will be able to access the file, but all existing ones will
     * still have access until the last one closes the file.
     *
     *
     * @see .delete
     */
    fun deleteOnExit() {
        if (isInvalid) {
            return
        }
        AmazeDeleteOnExitHook.add(path)
    }

    /**
     * Returns an array of strings naming the files and directories in the directory denoted by this
     * abstract pathname.
     *
     *
     * If this abstract pathname does not denote a directory, then this method returns `null`. Otherwise an array of strings is returned, one for each file or directory in the
     * directory. Names denoting the directory itself and the directory's parent directory are not
     * included in the result. Each string is a file name rather than a complete path.
     *
     *
     * There is no guarantee that the name strings in the resulting array will appear in any
     * specific order; they are not, in particular, guaranteed to appear in alphabetical order.
     *
     *
     * Note that the [java.nio.file.Files] class defines the [ ][java.nio.file.Files.newDirectoryStream] method to open a directory and
     * iterate over the names of the files in the directory. This may use less resources when working
     * with very large directories, and may be more responsive when working with remote directories.
     *
     * @return An array of strings naming the files and directories in the directory denoted by this
     * abstract pathname. The array will be empty if the directory is empty. Returns `null`
     * if this abstract pathname does not denote a directory, or if an I/O error occurs.
     */
    fun list(contextProvider: ContextProvider): Array<String>? {
        return if (isInvalid) {
            null
        } else fs.list(this, contextProvider)
    }

    /**
     * Returns an array of strings naming the files and directories in the directory denoted by this
     * abstract pathname that satisfy the specified filter. The behavior of this method is the same as
     * that of the [.list] method, except that the strings in the returned array must satisfy
     * the filter. If the given `filter` is `null` then all names are accepted. Otherwise,
     * a name satisfies the filter if and only if the value `true` results when the [ ][AmazeFilenameFilter.accept] method of the filter
     * is invoked on this abstract pathname and the name of a file or directory in the directory that
     * it denotes.
     *
     * @param filter A filename filter
     * @return An array of strings naming the files and directories in the directory denoted by this
     * abstract pathname that were accepted by the given `filter`. The array will be empty
     * if the directory is empty or if no names were accepted by the filter. Returns `null`
     * if this abstract pathname does not denote a directory, or if an I/O error occurs.
     * @see java.nio.file.Files.newDirectoryStream
     */
    fun list(filter: AmazeFilenameFilter?, contextProvider: ContextProvider): Array<String>? {
        val names = list(contextProvider)
        if (names == null || filter == null) {
            return names
        }
        val v: MutableList<String> = ArrayList()
        for (i in names.indices) {
            if (filter.accept(this, names[i])) {
                v.add(names[i])
            }
        }
        return v.toTypedArray()
    }

    /**
     * Returns an array of abstract pathnames denoting the files in the directory denoted by this
     * abstract pathname.
     *
     *
     * If this abstract pathname does not denote a directory, then this method returns `null`. Otherwise an array of `File` objects is returned, one for each file or directory
     * in the directory. Pathnames denoting the directory itself and the directory's parent directory
     * are not included in the result. Each resulting abstract pathname is constructed from this
     * abstract pathname using the [File(File,&amp;nbsp;String)][.File] constructor.
     * Therefore if this pathname is absolute then each resulting pathname is absolute; if this
     * pathname is relative then each resulting pathname will be relative to the same directory.
     *
     *
     * There is no guarantee that the name strings in the resulting array will appear in any
     * specific order; they are not, in particular, guaranteed to appear in alphabetical order.
     *
     *
     * Note that the [java.nio.file.Files] class defines the [ ][java.nio.file.Files.newDirectoryStream] method to open a directory and
     * iterate over the names of the files in the directory. This may use less resources when working
     * with very large directories.
     *
     * @return An array of abstract pathnames denoting the files and directories in the directory
     * denoted by this abstract pathname. The array will be empty if the directory is empty.
     * Returns `null` if this abstract pathname does not denote a directory, or if an I/O
     * error occurs.
     */
    fun listFiles(contextProvider: ContextProvider): Array<AmazeFile>? {
        val files = ArrayList<AmazeFile>()
        listFiles(contextProvider, files::add) ?: return null
        return files.toTypedArray()
    }

    fun listFiles(contextProvider: ContextProvider, onFileFound: (AmazeFile) -> Unit): Unit? {
        val ss = list(contextProvider) ?: return null
        for (i in ss.indices) {
            onFileFound(AmazeFile(ss[i], this))
        }
        return Unit
    }

    /**
     * Returns an array of abstract pathnames denoting the files and directories in the directory
     * denoted by this abstract pathname that satisfy the specified filter. The behavior of this
     * method is the same as that of the [.listFiles] method, except that the pathnames in the
     * returned array must satisfy the filter. If the given `filter` is `null` then all
     * pathnames are accepted. Otherwise, a pathname satisfies the filter if and only if the value
     * `true` results when the [ AmazeFilenameFilter.accept(File,&amp;nbsp;String)][AmazeFilenameFilter.accept] method of the filter is invoked on this abstract
     * pathname and the name of a file or directory in the directory that it denotes.
     *
     * @param filter A filename filter
     * @return An array of abstract pathnames denoting the files and directories in the directory
     * denoted by this abstract pathname. The array will be empty if the directory is empty.
     * Returns `null` if this abstract pathname does not denote a directory, or if an I/O
     * error occurs.
     * @see java.nio.file.Files.newDirectoryStream
     */
    fun listFiles(
        filter: AmazeFilenameFilter?,
        contextProvider: ContextProvider
    ): Array<AmazeFile>? {
        val files = ArrayList<AmazeFile>()
        listFiles(filter, contextProvider, files::add) ?: return null
        return files.toTypedArray()
    }

    fun listFiles(
        filter: AmazeFilenameFilter?,
        contextProvider: ContextProvider,
        onFileFound: (AmazeFile) -> Unit
    ): Unit? {
        val ss = list(contextProvider) ?: return null
        for (s in ss) {
            if (filter == null || filter.accept(this, s)) {
                onFileFound(AmazeFile(s, this))
            }
        }
        return Unit
    }

    /**
     * Returns an array of abstract pathnames denoting the files and directories in the directory
     * denoted by this abstract pathname that satisfy the specified filter. The behavior of this
     * method is the same as that of the [.listFiles] method, except that the pathnames in the
     * returned array must satisfy the filter. If the given `filter` is `null` then all
     * pathnames are accepted. Otherwise, a pathname satisfies the filter if and only if the value
     * `true` results when the [FileFilter.accept(File)][FileFilter.accept] method of the
     * filter is invoked on the pathname.
     *
     * @param filter A file filter
     * @return An array of abstract pathnames denoting the files and directories in the directory
     * denoted by this abstract pathname. The array will be empty if the directory is empty.
     * Returns `null` if this abstract pathname does not denote a directory, or if an I/O
     * error occurs.
     * @see java.nio.file.Files.newDirectoryStream
     */
    fun listFiles(filter: AmazeFileFilter?, contextProvider: ContextProvider): Array<AmazeFile>? {
        val files = ArrayList<AmazeFile>()
        forFiles(filter, contextProvider, files::add) ?: return null
        return files.toTypedArray()
    }

    /**
     * Calls a function on an array of abstract pathnames denoting the files and directories
     * in the directory denoted by this abstract pathname that satisfy the specified filter.
     * The behavior of this method is the same as that of the [listFiles] method, except that the
     * pathnames in the returned array must satisfy the filter. If the given `filter` is `null` then
     * all pathnames are accepted. Otherwise, a pathname satisfies the filter if and only if the value
     * `true` results when the [FileFilter.accept] method of the
     * filter is invoked on the pathname.
     *
     * @param filter A file filter
     * @param onFileFound the function called on every file and directory in the directory
     * denoted by this abstract pathname. Returns `null` if this abstract pathname does
     * not denote a directory, or if an I/O error occurs.
     * @see java.nio.file.Files.newDirectoryStream
     */
    fun forFiles(
        filter: AmazeFileFilter?,
        contextProvider: ContextProvider,
        onFileFound: (AmazeFile) -> Unit
    ): Unit? {
        val ss = list(contextProvider) ?: return null
        for (s in ss) {
            val f = AmazeFile(s, this)
            if (filter == null || filter.accept(f)) {
                onFileFound(f)
            }
        }
        return Unit
    }

    fun getInputStream(contextProvider: ContextProvider): InputStream {
        return fs.getInputStream(this, contextProvider)!!
    }

    fun getOutputStream(contextProvider: ContextProvider): OutputStream {
        return fs.getOutputStream(this, contextProvider)!!
    }

    /**
     * Creates the directory named by this abstract pathname.
     *
     * @return `true` if and only if the directory was created; `false`
     * otherwise
     */
    fun mkdir(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.createDirectory(this, contextProvider)
    }

    /**
     * Creates the directory named by this abstract pathname, including any necessary but nonexistent
     * parent directories. Note that if this operation fails it may have succeeded in creating some of
     * the necessary parent directories.
     *
     * @return `true` if and only if the directory was created, along with all necessary
     * parent directories; `false` otherwise
     */
    fun mkdirs(contextProvider: ContextProvider): Boolean {
        if (exists(contextProvider)) {
            return false
        }
        if (mkdir(contextProvider)) {
            return true
        }
        val canonFile = try {
            canonicalFile
        } catch (e: IOException) {
            return false
        }
        val parent = canonFile.parentFile
        return (
            parent != null && (parent.mkdirs(contextProvider) || parent.exists(contextProvider)) &&
                canonFile.mkdir(contextProvider)
            )
    }
    // Android-changed: Replaced generic platform info with Android specific one.
    /**
     * Renames the file denoted by this abstract pathname.
     *
     *
     * Many failures are possible. Some of the more likely failures include:
     *
     *
     *  * Write permission is required on the directories containing both the source and
     * destination paths.
     *  * Search permission is required for all parents of both paths.
     *  * Both paths be on the same mount point. On Android, applications are most likely to hit
     * this restriction when attempting to copy between internal storage and an SD card.
     *
     *
     *
     * The return value should always be checked to make sure that the rename operation was
     * successful.
     *
     *
     * Note that the [java.nio.file.Files] class defines the [ move][java.nio.file.Files.move] method to move or rename a file in a platform independent manner.
     *
     * @param dest The new abstract pathname for the named file
     * @return `true` if and only if the renaming succeeded; `false` otherwise
     */
    fun renameTo(dest: AmazeFile, contextProvider: ContextProvider): Boolean {
        return if (isInvalid || dest.isInvalid) {
            false
        } else fs.rename(this, dest, contextProvider)
    }

    /**
     * Sets the last-modified time of the file or directory named by this abstract pathname.
     *
     *
     * All platforms support file-modification times to the nearest second, but some provide more
     * precision. The argument will be truncated to fit the supported precision. If the operation
     * succeeds and no intervening operations on the file take place, then the next invocation of the
     * `[.lastModified]` method will return the (possibly truncated) `time
     ` *  argument that was passed to this method.
     *
     * @param time The new last-modified time, measured in milliseconds since the epoch (00:00:00 GMT,
     * January 1, 1970)
     * @return `true` if and only if the operation succeeded; `false` otherwise
     * @throws IllegalArgumentException If the argument is negative
     */
    fun setLastModified(time: Long): Boolean {
        require(time >= 0) { "Negative time" }
        return if (isInvalid) {
            false
        } else fs.setLastModifiedTime(this, time)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * Marks the file or directory named by this abstract pathname so that only read operations are
     * allowed. After invoking this method the file or directory will not change until it is either
     * deleted or marked to allow write access. Whether or not a read-only file or directory may be
     * deleted depends upon the underlying system.
     *
     * @return `true` if and only if the operation succeeded; `false` otherwise
     */
    fun setReadOnly(): Boolean {
        return if (isInvalid) {
            false
        } else fs.setReadOnly(this)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * Sets the owner's or everybody's write permission for this abstract pathname.
     *
     *
     * The [java.nio.file.Files] class defines methods that operate on file attributes
     * including file permissions. This may be used when finer manipulation of file permissions is
     * required.
     *
     * @param writable If `true`, sets the access permission to allow write operations; if
     * `false` to disallow write operations
     * @param ownerOnly If `true`, the write permission applies only to the owner's write
     * permission; otherwise, it applies to everybody. If the underlying file system can not
     * distinguish the owner's write permission from that of others, then the permission will
     * apply to everybody, regardless of this value.
     * @return `true` if and only if the operation succeeded. The operation will fail if
     * the user does not have permission to change the access permissions of this abstract
     * pathname.
     */
    fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean {
        return if (isInvalid) {
            false
        } else fs.setWritable(this, writable, ownerOnly)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * A convenience method to set the owner's write permission for this abstract pathname.
     *
     *
     * An invocation of this method of the form <tt>file.setWritable(arg)</tt> behaves in exactly
     * the same way as the invocation
     *
     * <pre>
     * file.setWritable(arg, true) </pre>
     *
     * @param writable If `true`, sets the access permission to allow write operations; if
     * `false` to disallow write operations
     * @return `true` if and only if the operation succeeded. The operation will fail if
     * the user does not have permission to change the access permissions of this abstract
     * pathname.
     */
    fun setWritable(writable: Boolean): Boolean {
        return setWritable(writable, true)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * Sets the owner's or everybody's read permission for this abstract pathname.
     *
     *
     * The [java.nio.file.Files] class defines methods that operate on file attributes
     * including file permissions. This may be used when finer manipulation of file permissions is
     * required.
     *
     * @param readable If `true`, sets the access permission to allow read operations; if
     * `false` to disallow read operations
     * @param ownerOnly If `true`, the read permission applies only to the owner's read
     * permission; otherwise, it applies to everybody. If the underlying file system can not
     * distinguish the owner's read permission from that of others, then the permission will apply
     * to everybody, regardless of this value.
     * @return `true` if and only if the operation succeeded. The operation will fail if
     * the user does not have permission to change the access permissions of this abstract
     * pathname. If `readable` is `false` and the underlying file system
     * does not implement a read permission, then the operation will fail.
     */
    fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean {
        return if (isInvalid) {
            false
        } else fs.setReadable(this, readable, ownerOnly)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * A convenience method to set the owner's read permission for this abstract pathname.
     *
     *
     * An invocation of this method of the form <tt>file.setReadable(arg)</tt> behaves in exactly
     * the same way as the invocation
     *
     * <pre>
     * file.setReadable(arg, true) </pre>
     *
     * @param readable If `true`, sets the access permission to allow read operations; if
     * `false` to disallow read operations
     * @return `true` if and only if the operation succeeded. The operation will fail if
     * the user does not have permission to change the access permissions of this abstract
     * pathname. If `readable` is `false` and the underlying file system
     * does not implement a read permission, then the operation will fail.
     */
    fun setReadable(readable: Boolean): Boolean {
        return setReadable(readable, true)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * Sets the owner's or everybody's execute permission for this abstract pathname.
     *
     *
     * The [java.nio.file.Files] class defines methods that operate on file attributes
     * including file permissions. This may be used when finer manipulation of file permissions is
     * required.
     *
     * @param executable If `true`, sets the access permission to allow execute operations;
     * if `false` to disallow execute operations
     * @param ownerOnly If `true`, the execute permission applies only to the owner's
     * execute permission; otherwise, it applies to everybody. If the underlying file system can
     * not distinguish the owner's execute permission from that of others, then the permission
     * will apply to everybody, regardless of this value.
     * @return `true` if and only if the operation succeeded. The operation will fail if
     * the user does not have permission to change the access permissions of this abstract
     * pathname. If `executable` is `false` and the underlying file system
     * does not implement an execute permission, then the operation will fail.
     */
    fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean {
        return if (isInvalid) {
            false
        } else fs.setExecutable(this, executable, ownerOnly)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * A convenience method to set the owner's execute permission for this abstract pathname.
     *
     *
     * An invocation of this method of the form <tt>file.setExcutable(arg)</tt> behaves in exactly
     * the same way as the invocation
     *
     * <pre>
     * file.setExecutable(arg, true) </pre>
     *
     * @param executable If `true`, sets the access permission to allow execute operations;
     * if `false` to disallow execute operations
     * @return `true` if and only if the operation succeeded. The operation will fail if
     * the user does not have permission to change the access permissions of this abstract
     * pathname. If `executable` is `false` and the underlying file system
     * does not implement an execute permission, then the operation will fail.
     */
    fun setExecutable(executable: Boolean): Boolean {
        return setExecutable(executable, true)
    }
    // Android-changed. Removed javadoc comment about special privileges
    // that doesn't make sense on Android.
    /**
     * Tests whether the application can execute the file denoted by this abstract pathname.
     *
     * @return `true` if and only if the abstract pathname exists *and* the
     * application is allowed to execute the file
     */
    fun canExecute(contextProvider: ContextProvider): Boolean {
        return if (isInvalid) {
            false
        } else fs.canExecute(this, contextProvider)
    }
    /* -- Filesystem interface -- */ // Android-changed: Replaced generic platform info with Android specific one.
    /* -- Disk usage -- */
    /**
     * Returns the size of the partition [named](#partName) by this abstract pathname.
     *
     * @return The size, in bytes, of the partition or <tt>0L</tt> if this abstract pathname does not
     * name a partition If there is no way to determine, total space is -1
     */
    fun getTotalSpace(contextProvider: ContextProvider): Long {
        return if (isInvalid) {
            0L
        } else try {
            fs.getTotalSpace(this, contextProvider)
        } catch (e: NotImplementedError) {
            Log.w(TAG, "Call to unimplemented fuction", e)
            -1
        }
    }

    /**
     * Returns the number of unallocated bytes in the partition [named](#partName) by this
     * abstract path name.
     *
     *
     * The returned number of unallocated bytes is a hint, but not a guarantee, that it is possible
     * to use most or any of these bytes. The number of unallocated bytes is most likely to be
     * accurate immediately after this call. It is likely to be made inaccurate by any external I/O
     * operations including those made on the system outside of this virtual machine. This method
     * makes no guarantee that write operations to this file system will succeed.
     *
     * @return The number of unallocated bytes on the partition or <tt>0L</tt> if the abstract
     * pathname does not name a partition. This value will be less than or equal to the total file
     * system size returned by [.getTotalSpace].
     */
    val freeSpace: Long
        get() = if (isInvalid) {
            0L
        } else fs.getFreeSpace(this)
    // Android-added: Replaced generic platform info with Android specific one.
    /**
     * Returns the number of bytes available to this virtual machine on the partition [named](#partName) by this abstract pathname. When possible, this method checks for
     * write permissions and other operating system restrictions and will therefore usually provide a
     * more accurate estimate of how much new data can actually be written than [.getFreeSpace].
     *
     *
     * The returned number of available bytes is a hint, but not a guarantee, that it is possible
     * to use most or any of these bytes. The number of unallocated bytes is most likely to be
     * accurate immediately after this call. It is likely to be made inaccurate by any external I/O
     * operations including those made on the system outside of this virtual machine. This method
     * makes no guarantee that write operations to this file system will succeed.
     *
     *
     * On Android (and other Unix-based systems), this method returns the number of free bytes
     * available to non-root users, regardless of whether you're actually running as root, and
     * regardless of any quota or other restrictions that might apply to the user. (The `getFreeSpace` method returns the number of bytes potentially available to root.)
     *
     * @return The number of available bytes on the partition or <tt>0L</tt> if the abstract pathname
     * does not name a partition. On systems where this information is not available, this method
     * will be equivalent to a call to [.getFreeSpace]. If there is no way to determine the
     * current space left -1 is returned.
     */
    val usableSpace: Long
        get() = if (isInvalid) {
            0L
        } else try {
            fs.getUsableSpace(this)
        } catch (e: NotImplementedError) {
            Log.w(TAG, "Call to unimplemented fuction", e)
            -1
        }

    /* -- Temporary files -- */
    private object TempDirectory {
        // file name generation
        private val random = SecureRandom()
        @Throws(IOException::class)
        fun generateFile(prefix: String, suffix: String, dir: AmazeFile?): AmazeFile {
            // Android-changed: Use Math.randomIntInternal. This (pseudo) random number
            // is initialized post-fork
            var n = random.nextLong()
            n = if (n == Long.MIN_VALUE) {
                0 // corner case
            } else {
                Math.abs(n)
            }

            // Android-changed: Reject invalid file prefixes
            // Use only the file name from the supplied prefix
            // prefix = (new AmazeFile(prefix)).getName();
            val name = prefix + java.lang.Long.toString(n) + suffix
            val f = AmazeFile(dir, name)
            if (name != f.name || f.isInvalid) {
                if (System.getSecurityManager() != null) {
                    throw IOException("Unable to create temporary file")
                } else {
                    throw IOException("Unable to create temporary file, $f")
                }
            }
            return f
        }
    }

    /**
     * Creates a new empty file in the specified directory, using the given prefix and suffix strings
     * to generate its name. If this method returns successfully then it is guaranteed that:
     *
     *
     *  1. The file denoted by the returned abstract pathname did not exist before this method was
     * invoked, and
     *  1. Neither this method nor any of its variants will return the same abstract pathname again
     * in the current invocation of the virtual machine.
     *
     *
     * This method provides only part of a temporary-file facility. To arrange for a file created by
     * this method to be deleted automatically, use the `[.deleteOnExit]` method.
     *
     *
     * The `prefix` argument must be at least three characters long. It is recommended
     * that the prefix be a short, meaningful string such as `"hjb"` or `"mail"`
     * . The `suffix` argument may be `null`, in which case the suffix `
     * ".tmp"` will be used.
     *
     *
     * To create the new file, the prefix and the suffix may first be adjusted to fit the
     * limitations of the underlying platform. If the prefix is too long then it will be truncated,
     * but its first three characters will always be preserved. If the suffix is too long then it too
     * will be truncated, but if it begins with a period character (`'.'`) then the period
     * and the first three characters following it will always be preserved. Once these adjustments
     * have been made the name of the new file will be generated by concatenating the prefix, five or
     * more internally-generated characters, and the suffix.
     *
     *
     * If the `directory` argument is `null` then the system-dependent
     * default temporary-file directory will be used. The default temporary-file directory is
     * specified by the system property `java.io.tmpdir`. On UNIX systems the default value
     * of this property is typically `"/tmp"` or `"/var/tmp"`; on Microsoft
     * Windows systems it is typically `"C:\\WINNT\\TEMP"`. A different value may be given
     * to this system property when the Java virtual machine is invoked, but programmatic changes to
     * this property are not guaranteed to have any effect upon the temporary directory used by this
     * method.
     *
     * @param prefix The prefix string to be used in generating the file's name; must be at least
     * three characters long
     * @param suffix The suffix string to be used in generating the file's name; may be `null
     ` * , in which case the suffix `".tmp"` will be used
     * @param directory The directory in which the file is to be created, or `null` if the
     * default temporary-file directory is to be used
     * @return An abstract pathname denoting a newly-created empty file
     * @throws IllegalArgumentException If the `prefix` argument contains fewer than three
     * characters
     * @throws IOException If a file could not be created
     */
    @Throws(IOException::class)
    fun createTempFile(
        contextProvider: ContextProvider,
        prefix: String,
        suffix: String?,
        directory: AmazeFile?
    ): AmazeFile {
        require(prefix.length >= 3) { "Prefix string too short" }

        // Android-changed: Handle java.io.tmpdir changes.
        val tmpdir = directory ?: AmazeFile(System.getProperty("java.io.tmpdir", "."))
        var f: AmazeFile
        do {
            f = TempDirectory.generateFile(prefix, suffix ?: ".tmp", tmpdir)
        } while (fs.getBooleanAttributes(f, contextProvider) and AmazeFilesystem.BA_EXISTS != 0)
        if (!fs.createFileExclusively(f.path)) throw IOException("Unable to create temporary file")
        return f
    }

    /**
     * Creates an empty file in the default temporary-file directory, using the given prefix and
     * suffix to generate its name. Invoking this method is equivalent to invoking `
     * [ createTempFile(prefix,&amp;nbsp;suffix,&amp;nbsp;null)][.createTempFile]`.
     *
     *
     * The [ ][java.nio.file.Files.createTempFile] method provides an alternative method to create an empty file in the
     * temporary-file directory. Files created by that method may have more restrictive access
     * permissions to files created by this method and so may be more suited to security-sensitive
     * applications.
     *
     * @param prefix The prefix string to be used in generating the file's name; must be at least
     * three characters long
     * @param suffix The suffix string to be used in generating the file's name; may be `null
     ` * , in which case the suffix `".tmp"` will be used
     * @return An abstract pathname denoting a newly-created empty file
     * @throws IllegalArgumentException If the `prefix` argument contains fewer than three
     * characters
     * @throws IOException If a file could not be created
     * @see java.nio.file.Files.createTempDirectory
     */
    @Throws(IOException::class)
    fun createTempFile(
        contextProvider: ContextProvider,
        prefix: String,
        suffix: String?
    ): AmazeFile {
        return createTempFile(contextProvider, prefix, suffix, null)
    }
    /* -- Basic infrastructure -- */
    /**
     * Compares two abstract pathnames lexicographically. The ordering defined by this method depends
     * upon the underlying system. On UNIX systems, alphabetic case is significant in comparing
     * pathnames; on Microsoft Windows systems it is not.
     *
     * @param pathname The abstract pathname to be compared to this abstract pathname
     * @return Zero if the argument is equal to this abstract pathname, a value less than zero if this
     * abstract pathname is lexicographically less than the argument, or a value greater than zero
     * if this abstract pathname is lexicographically greater than the argument
     */
    override fun compareTo(pathname: AmazeFile?): Int {
        return fs.compare(this, pathname!!)
    }

    /**
     * Tests this abstract pathname for equality with the given object. Returns `true` if
     * and only if the argument is not `null` and is an abstract pathname that denotes the
     * same file or directory as this abstract pathname. Whether or not two abstract pathnames are
     * equal depends upon the underlying system. On UNIX systems, alphabetic case is significant in
     * comparing pathnames; on Microsoft Windows systems it is not.
     *
     * @param obj The object to be compared with this abstract pathname
     * @return `true` if and only if the objects are the same; `false` otherwise
     */
    override fun equals(obj: Any?): Boolean {
        return if (obj is AmazeFile) {
            compareTo(obj as AmazeFile?) == 0
        } else false
    }

    /**
     * Computes a hash code for this abstract pathname. Because equality of abstract pathnames is
     * inherently system-dependent, so is the computation of their hash codes. On UNIX systems, the
     * hash code of an abstract pathname is equal to the exclusive *or* of the hash code of its
     * pathname string and the decimal value `1234321`. On Microsoft Windows systems, the
     * hash code is equal to the exclusive *or* of the hash code of its pathname string
     * converted to lower case and the decimal value `1234321`. Locale is not taken into
     * account on lowercasing the pathname string.
     *
     * @return A hash code for this abstract pathname
     */
    override fun hashCode(): Int {
        return fs.hashCode(this)
    }

    /**
     * Returns the pathname string of this abstract pathname. This is just the string returned by the
     * `[.getPath]` method.
     *
     * @return The string form of this abstract pathname
     */
    override fun toString(): String {
        return path
    }
}
