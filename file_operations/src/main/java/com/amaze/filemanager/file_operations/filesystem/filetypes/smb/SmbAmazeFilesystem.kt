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
package com.amaze.filemanager.file_operations.filesystem.filetypes.smb

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.CifsContexts.createWithDisableIpcSigningCheck
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem
import jcifs.smb.SmbFile
import jcifs.smb.NtlmPasswordAuthenticator
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider
import com.amaze.filemanager.file_operations.filesystem.filetypes.smb.CifsContexts.SMB_URI_PREFIX
import jcifs.smb.SmbException
import jcifs.SmbConstants
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.MalformedURLException
import java.util.regex.Pattern

/**
 * Root is "smb://<user>:<password>@<ip>" or "smb://<ip>" or
 * "smb://<user>:<password>@<ip>/?disableIpcSigningCheck=true" or
 * "smb://<ip>/?disableIpcSigningCheck=true"
 * Relative paths are not supported
 * </ip></ip></password></user></ip></ip></password></user>
 */
object SmbAmazeFilesystem: AmazeFilesystem() {
    @JvmStatic
    val TAG = SmbAmazeFilesystem::class.java.simpleName

    const val PARAM_DISABLE_IPC_SIGNING_CHECK = "disableIpcSigningCheck"
    @JvmStatic
    private val IPv4_PATTERN = Pattern.compile("[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+.[0-9]{1,3}+")
    @JvmStatic
    private val METADATA_PATTERN = Pattern.compile("([?][a-zA-Z]+=(\")?[a-zA-Z]+(\")?)+")

    @JvmStatic
    @Throws(MalformedURLException::class)
    fun create(path: String?): SmbFile {
        val processedPath: String
        processedPath = if (!path!!.endsWith(STANDARD_SEPARATOR + "")) {
            path + STANDARD_SEPARATOR
        } else {
            path
        }
        val uri = Uri.parse(processedPath)
        val disableIpcSigningCheck = java.lang.Boolean.parseBoolean(uri.getQueryParameter(PARAM_DISABLE_IPC_SIGNING_CHECK))
        val userInfo = uri.userInfo
        val noExtraInfoPath: String
        noExtraInfoPath = if (path.contains("?")) {
            path.substring(0, path.indexOf('?'))
        } else {
            path
        }
        val context = createWithDisableIpcSigningCheck(path, disableIpcSigningCheck)
                .withCredentials(createFrom(userInfo))
        return SmbFile(noExtraInfoPath, context)
    }

    /**
     * Create [NtlmPasswordAuthenticator] from given userInfo parameter.
     *
     *
     * Logic borrowed directly from jcifs-ng's own code. They should make that protected
     * constructor public...
     *
     * @param userInfo authentication string, must be already URL decoded. [Uri] shall do this
     * for you already
     * @return [NtlmPasswordAuthenticator] instance
     */
    @JvmStatic
    private fun createFrom(userInfo: String?): NtlmPasswordAuthenticator {
        return if (!TextUtils.isEmpty(userInfo)) {
            var dom: String? = null
            var user: String? = null
            var pass: String? = null
            var i: Int
            var u: Int
            val end = userInfo!!.length
            i = 0
            u = 0
            while (i < end) {
                val c = userInfo[i]
                if (c == ';') {
                    dom = userInfo.substring(0, i)
                    u = i + 1
                } else if (c == ':') {
                    pass = userInfo.substring(i + 1)
                    break
                }
                i++
            }
            user = userInfo.substring(u, i)
            NtlmPasswordAuthenticator(dom, user, pass)
        } else {
            NtlmPasswordAuthenticator()
        }
    }

    init {
        AmazeFile.addFilesystem(this)
    }

    override val prefix: String = SMB_URI_PREFIX

    override fun normalize(pathname: String): String {
        val canonical: String
        canonical = try {
            canonicalize(pathname)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file canonical path", e)
            pathname.substring(0, prefixLength(pathname)) + "/"
        }
        return canonical
    }

    override fun prefixLength(path: String): Int {
        require(path.length != 0) { "This should never happen, all paths must start with SMB prefix" }
        val matcherMetadata = METADATA_PATTERN.matcher(path)
        if (matcherMetadata.find()) {
            return matcherMetadata.end()
        }
        val matcher = IPv4_PATTERN.matcher(path)
        matcher.find()
        return matcher.end()
    }

    override fun resolve(parent: String?, child: String?): String {
        val prefix = parent!!.substring(0, prefixLength(parent))
        val simplePathParent = parent.substring(prefixLength(parent))
        val simplePathChild = child!!.substring(prefixLength(child))
        return prefix + basicUnixResolve(simplePathParent, simplePathChild)
    }

    /** This makes no sense for SMB  */
    override val defaultParent: String
        get() {
            throw IllegalStateException("There is no default SMB path")
        }

    override fun isAbsolute(f: AmazeFile): Boolean {
        return f.path.startsWith(prefix)
    }

    override fun resolve(f: AmazeFile): String {
        if (isAbsolute(f)) {
            return f.path
        }
        throw IllegalArgumentException("Relative paths are not supported")
    }

    @Throws(MalformedURLException::class)
    override fun canonicalize(path: String?): String {
        return create(path).canonicalPath
    }

    override fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            val smbFile = create(f.path)
            smbFile.exists()
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        }
    }

    override fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            val smbFile = create(f.path)
            smbFile.type == SmbConstants.TYPE_FILESYSTEM
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        }
    }

    override fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            val smbFile = create(f.path)
            smbFile.isDirectory
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        }
    }

    override fun isHidden(f: AmazeFile): Boolean {
        return try {
            val smbFile = create(f.path)
            smbFile.isHidden
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Failed to get attributes for SMB file", e)
            false
        }
    }

    override fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        throw NotImplementedError()
    }

    override fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            create(f.path).canWrite()
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to check access", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB file to check access", e)
            false
        }
    }

    override fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            create(f.path).canRead()
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to check access", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB file to check access", e)
            false
        }
    }

    override fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            val file = create(f.path)
            file.connectTimeout = 2000
            file.exists()
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to check access", e)
            false
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB file to check access", e)
            false
        }
    }

    override fun setExecutable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        throw NotImplementedError()
    }

    override fun setWritable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        throw NotImplementedError()
    }

    override fun setReadable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        throw NotImplementedError()
    }

    override fun getLastModifiedTime(f: AmazeFile): Long {
        return try {
            create(f.path).lastModified
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to get last modified time", e)
            0
        }
    }

    @Throws(SmbException::class, MalformedURLException::class)
    override fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long {
        return create(f.path).length()
    }

    @Throws(IOException::class)
    override fun createFileExclusively(pathname: String?): Boolean {
        create(pathname).mkdirs()
        return true
    }

    override fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            create(f.path).delete()
            true
        } catch (e: SmbException) {
            Log.e(TAG, "Error deleting SMB file", e)
            false
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error deleting SMB file", e)
            false
        }
    }

    override fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String>? {
        val list: Array<String?> = try {
            create(f.path).list()
        } catch (e: SmbException) {
            Log.e(TAG, "Error listing SMB files", e)
            return null
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error listing SMB files", e)
            return null
        }
        val prefix = f.path.substring(0, prefixLength(f.path))
        return Array(list.size) { i: Int ->
            normalize(prefix + getSeparator() + list[i])
        }
    }

    override fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream? {
        return try {
            create(f.path).inputStream
        } catch (e: IOException) {
            Log.e(TAG, "Error creating SMB output stream", e)
            null
        }
    }

    override fun getOutputStream(f: AmazeFile, contextProvider: ContextProvider): OutputStream? {
        return try {
            create(f.path).outputStream
        } catch (e: IOException) {
            Log.e(TAG, "Error creating SMB output stream", e)
            null
        }
    }

    override fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            create(f.path).mkdir()
            true
        } catch (e: SmbException) {
            Log.e(TAG, "Error creating SMB directory", e)
            false
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error creating SMB directory", e)
            false
        }
    }

    override fun rename(f1: AmazeFile, f2: AmazeFile, contextProvider: ContextProvider): Boolean {
        return try {
            create(f1!!.path).renameTo(create(f2!!.path))
            true
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB files for a rename", e)
            false
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB files for a rename", e)
            false
        }
    }

    override fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean {
        return try {
            create(f.path).lastModified = time
            true
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB file to set modified time", e)
            false
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to set modified time", e)
            false
        }
    }

    override fun setReadOnly(f: AmazeFile): Boolean {
        return try {
            create(f.path).setReadOnly()
            true
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB file to set read only", e)
            false
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to set read only", e)
            false
        }
    }

    override fun listRoots(): Array<AmazeFile> {
        throw NotImplementedError()
    }

    override fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long {
        // TODO: Find total storage space of SMB when JCIFS adds support
        throw NotImplementedError()
    }

    override fun getFreeSpace(f: AmazeFile): Long {
        return try {
            create(f.path).diskFreeSpace
        } catch (e: SmbException) {
            Log.e(TAG, "Error getting SMB file to read free volume space", e)
            0
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Error getting SMB file to read free volume space", e)
            0
        }
    }

    override fun getUsableSpace(f: AmazeFile): Long {
        // TODO: Find total storage space of SMB when JCIFS adds support
        throw NotImplementedError()
    }
}