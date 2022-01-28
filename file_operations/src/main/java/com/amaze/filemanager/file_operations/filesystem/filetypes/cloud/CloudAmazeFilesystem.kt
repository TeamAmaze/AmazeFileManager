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
package com.amaze.filemanager.file_operations.filesystem.filetypes.cloud

import android.util.Log
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider
import com.cloudrail.si.types.CloudMetaData
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.util.*

abstract class CloudAmazeFilesystem : AmazeFilesystem() {
    abstract val account: Account
    override fun prefixLength(path: String): Int {
        require(path.length != 0) { "This should never happen, all paths must start with OTG prefix" }
        return super.prefixLength(path)
    }

    override fun normalize(path: String): String {
        val canonical: String
        canonical = try {
            canonicalize(path)
        } catch (e: IOException) {
            Log.e(TAG, "Error getting Dropbox file canonical path", e)
            "$path/"
        }
        return canonical.substring(0, canonical.length - 1)
    }

    override fun resolve(parent: String, child: String): String {
        return prefix + File(removePrefix(parent!!), child)
    }

    override val defaultParent: String
        get() = "$prefix/"

    override fun isAbsolute(f: AmazeFile): Boolean {
        return true // We don't accept relative paths for cloud
    }

    override fun resolve(f: AmazeFile): String {
        if (isAbsolute(f)) {
            return f.path
        }
        throw IllegalArgumentException("Relative paths are not supported")
    }

    @Throws(IOException::class)
    override fun canonicalize(path: String?): String {
        return prefix + File(removePrefix(path!!)).canonicalPath
    }

    override fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        return account.account?.exists(noPrefixPath) ?: false
    }

    override fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return true // all files are regular (probably)
    }

    override fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val noPrefixPath = removePrefix(f.path)
        val metadata: CloudMetaData? = account.account?.getMetadata(noPrefixPath)
        return metadata?.folder ?: false
    }

    override fun isHidden(f: AmazeFile): Boolean {
        return false // No way to know if its hidden
    }

    override fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return false // You aren't executing anything at the cloud
    }

    override fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return true // Probably, can't check
    }

    override fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return true // Probably, can't check
    }

    override fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        return account!!.exists(noPrefixPath)
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
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        // TODO check that this actually returns seconds since epoch
        return account!!.getMetadata(noPrefixPath).contentModifiedAt
    }

    @Throws(IOException::class)
    override fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long {
        if (f.isDirectory(contextProvider)) {
            return 0
        }
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        return account!!.getMetadata(noPrefixPath).size.toLong()
    }

    @Throws(IOException::class)
    override fun createFileExclusively(pathname: String): Boolean {
        return false
    }

    override fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        account!!.delete(noPrefixPath)
        return true // This seems to never fail
    }

    override fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String>? {
        val account = account.account ?: return null
        val noPrefixPath = removePrefix(f.path)
        val metadatas = account.getChildren(noPrefixPath)
        return Array(metadatas.size) { i: Int ->
            normalize(prefix + metadatas[i].path)
        }
    }

    override fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream? {
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        return account!!.download(noPrefixPath)
    }

    override fun getOutputStream(f: AmazeFile, contextProvider: ContextProvider): OutputStream? {
        throw NotImplementedError()
    }

    override fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        account!!.createFolder(noPrefixPath)
        return true // This seems to never fail
    }

    override fun rename(f1: AmazeFile, f2: AmazeFile, contextProvider: ContextProvider): Boolean {
        val account = account.account
        Objects.requireNonNull(account)
        account!!.move(removePrefix(f1.path), removePrefix(f2.path))
        return true // This seems to never fail
    }

    override fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean {
        val account = account.account
        Objects.requireNonNull(account)
        val noPrefixPath = removePrefix(f.path)
        // TODO check that this actually returns seconds since epoch
        account!!.getMetadata(noPrefixPath).contentModifiedAt = time
        return true // This seems to never fail
    }

    override fun setReadOnly(f: AmazeFile): Boolean {
        return false // This doesn't seem possible
    }

    override fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long {
        val account = account.account
        Objects.requireNonNull(account)
        val spaceAllocation = account!!.allocation
        return spaceAllocation.total
    }

    override fun getFreeSpace(f: AmazeFile): Long {
        val account = account.account
        Objects.requireNonNull(account)
        val spaceAllocation = account!!.allocation
        return spaceAllocation.total - spaceAllocation.used
    }

    override fun getUsableSpace(f: AmazeFile): Long {
        val account = account.account
        Objects.requireNonNull(account)
        val spaceAllocation = account!!.allocation
        return spaceAllocation.total - spaceAllocation.used
    }

    companion object {
        val TAG = CloudAmazeFilesystem::class.java.simpleName
    }
}