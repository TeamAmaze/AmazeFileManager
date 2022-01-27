package com.amaze.filemanager.filesystem.otg

import android.util.Log
import com.amaze.filemanager.utils.OTGUtil.getDocumentFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider
import java.io.*
import java.util.ArrayList

class OtgAmazeFilesystem private constructor() : AmazeFilesystem() {
    companion object {
        val TAG = OtgAmazeFilesystem::class.java.simpleName

        val INSTANCE = OtgAmazeFilesystem()

        const val PREFIX = "otg:/"

        init {
            AmazeFile.addFilesystem(INSTANCE)
        }
    }

    override val prefix = PREFIX

    override fun prefixLength(path: String): Int {
        require(path.length != 0) { "This should never happen, all paths must start with OTG prefix" }
        return super.prefixLength(path)
    }

    override fun normalize(path: String): String {
        return simpleUnixNormalize(path)
    }

    override fun resolve(parent: String?, child: String?): String {
        val prefix = parent!!.substring(0, prefixLength(parent))
        val simplePathParent = parent.substring(prefixLength(parent))
        val simplePathChild = child!!.substring(prefixLength(child))
        return prefix + basicUnixResolve(simplePathParent, simplePathChild)
    }

    override val defaultParent: String
        get() = prefix + getSeparator()

    override fun isAbsolute(f: AmazeFile): Boolean {
        return true // Relative paths are not supported
    }

    override fun resolve(f: AmazeFile): String {
        return f.path
    }

    @Throws(IOException::class)
    override fun canonicalize(path: String?): String {
        return normalize(path!!)
    }

    override fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        return getDocumentFile(f.path, context, false) != null
    }

    override fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        return getDocumentFile(f.path, context, false)!!.isFile
    }

    override fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        return getDocumentFile(f.path, context, false)!!.isDirectory
    }

    override fun isHidden(f: AmazeFile): Boolean {
        return false // There doesn't seem to be hidden files for OTG
    }

    override fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        throw NotImplementedError() // No way to check
    }

    override fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        return getDocumentFile(f.path, context, false)!!.canWrite()
    }

    override fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        return getDocumentFile(f.path, context, false)!!.canRead()
    }

    override fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return exists(f, contextProvider) //If the system says it exists, we can access it
    }

    override fun setExecutable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return false //TODO find way to set
    }

    override fun setWritable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return false //TODO find way to set
    }

    override fun setReadable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return false //TODO find way to set
    }

    override fun getLastModifiedTime(f: AmazeFile): Long {
        return 0
    }

    @Throws(IOException::class)
    override fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long {
        val context = contextProvider.getContext()
                ?: throw IOException("Error obtaining context for OTG")
        return getDocumentFile(f.path, context, false)!!.length()
    }

    @Throws(IOException::class)
    override fun createFileExclusively(pathname: String?): Boolean {
        return false
    }

    override fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        return getDocumentFile(f.path, context, false)!!.delete()
    }

    override fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String>? {
        val context = contextProvider.getContext() ?: return null
        val list = ArrayList<String>()
        val rootUri = getDocumentFile(f.path, context, false) ?: return null
        for (file in rootUri.listFiles()) {
            file.uri.path?.let { list.add(it) }
        }
        return list.toTypedArray()
    }

    override fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream? {
        val context = contextProvider.getContext()
        val contentResolver = context!!.contentResolver
        val documentSourceFile = getDocumentFile(f.path, context, false)
        return try {
            contentResolver.openInputStream(documentSourceFile!!.uri)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Error obtaining input stream for OTG", e)
            null
        }
    }

    override fun getOutputStream(f: AmazeFile, contextProvider: ContextProvider): OutputStream? {
        val context = contextProvider.getContext()
        val contentResolver = context!!.contentResolver
        val documentSourceFile = getDocumentFile(f.path, context, true)
        return try {
            contentResolver.openOutputStream(documentSourceFile!!.uri)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Error output stream for OTG", e)
            null
        }
    }

    override fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        if (f.exists(contextProvider)) {
            return true
        }
        val context = contextProvider.getContext() ?: return false
        val parent = f.parent ?: return false
        val parentDirectory = getDocumentFile(parent, context, true)
        if (parentDirectory!!.isDirectory) {
            parentDirectory.createDirectory(f.name)
            return true
        }
        return false
    }

    override fun rename(f1: AmazeFile, f2: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext()
        val contentResolver = context!!.contentResolver
        val documentSourceFile = getDocumentFile(f1.path, context, true)
        return documentSourceFile!!.renameTo(f2.path)
    }

    override fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean {
        throw NotImplementedError()
    }

    override fun setReadOnly(f: AmazeFile): Boolean {
        return false
    }

    override fun listRoots(): Array<AmazeFile> {
        val roots = File.listRoots()
        val amazeRoots = arrayOfNulls<AmazeFile>(roots.size)
        for (i in roots.indices) {
            amazeRoots[i] = AmazeFile(roots[i].path)
        }
        return arrayOf(AmazeFile(defaultParent))
    }

    override fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long {
        val context = contextProvider.getContext()
        // TODO: Find total storage space of OTG when {@link DocumentFile} API adds support
        val documentFile = getDocumentFile(f.path, context!!, false)
        return documentFile!!.length()
    }

    override fun getFreeSpace(f: AmazeFile): Long {
        return 0 //TODO
    }

    override fun getUsableSpace(f: AmazeFile): Long {
        // TODO: Get free space from OTG when {@link DocumentFile} API adds support
        return 0
    }
}