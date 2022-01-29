package com.amaze.filemanager.filesystem.files

import android.content.ContentResolver
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider
import com.amaze.filemanager.filesystem.FileProperties.getDeviceStorageRemainingSpace
import com.amaze.filemanager.filesystem.SafRootHolder.uriRoot
import com.amaze.filemanager.filesystem.SafRootHolder.volumeLabel
import com.amaze.filemanager.filesystem.otg.OtgAmazeFilesystem
import com.amaze.filemanager.utils.OTGUtil.getDocumentFile
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object DocumentFileAmazeFilesystem: AmazeFilesystem() {
    @JvmStatic
    val TAG = DocumentFileAmazeFilesystem::class.java.simpleName

    const val DOCUMENT_FILE_PREFIX = "content://com.android.externalstorage.documents"

    init {
        AmazeFile.addFilesystem(this)
    }

    override val prefix: String = DOCUMENT_FILE_PREFIX

    override fun normalize(path: String): String {
        val documentFile = getDocumentFile(path, false) ?: return defaultParent
        return documentFile.uri.path ?: defaultParent
    }

    override fun resolve(parent: String, child: String): String {
        val documentFile = getDocumentFile(parent, false) ?: return defaultParent
        val childDocumentFile = documentFile.findFile(child) ?: return defaultParent
        return childDocumentFile.uri.path ?: return defaultParent
    }

    override fun resolve(f: AmazeFile): String {
        TODO("Not yet implemented")
    }

    override val defaultParent: String = DOCUMENT_FILE_PREFIX + getSeparator()

    override fun isAbsolute(f: AmazeFile): Boolean = true // All paths are absolute

    override fun canonicalize(path: String?): String {
        TODO("Not yet implemented")
    }

    override fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val documentFile = getDocumentFile(f.path, false) ?: return false
        return documentFile.exists()
    }

    override fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val documentFile = getDocumentFile(f.path, false) ?: return false
        return documentFile.isFile
    }

    override fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val documentFile = getDocumentFile(f.path, false) ?: return false
        return documentFile.isDirectory
    }

    override fun isHidden(f: AmazeFile): Boolean {
        throw NotImplementedError()
    }

    override fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        throw NotImplementedError()
    }

    override fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val documentFile = getDocumentFile(f.path, false) ?: return false
        return documentFile.canWrite()
    }

    override fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val documentFile = getDocumentFile(f.path, false) ?: return false
        return documentFile.canRead()
    }

    override fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return exists(f, contextProvider)
    }

    override fun setExecutable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return false //Can't set
    }

    override fun setWritable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return false //Can't set
    }

    override fun setReadable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return false //Can't set
    }

    override fun getLastModifiedTime(f: AmazeFile): Long {
        val documentFile = getDocumentFile(f.path, false) ?: return 0
        return documentFile.lastModified()
    }

    override fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long {
        val documentFile = getDocumentFile(f.path, false)
                ?: throw IOException("Could not create DocumentFile for length")
        return documentFile.length()
    }

    override fun createFileExclusively(pathname: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val documentFile = getDocumentFile(f.path, false) ?: return false
        return documentFile.delete()
    }

    override fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String>? {
        val documentFile = getDocumentFile(f.path, false) ?: return null
        return documentFile.listFiles().mapNotNull { it.uri.path }.toTypedArray()
    }

    override fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream? {
        val context = contextProvider.getContext() ?: return null
        val contentResolver: ContentResolver = context.contentResolver
        val documentSourceFile = getDocumentFile(f.path, false) ?: return null
        return try {
            contentResolver.openInputStream(documentSourceFile.uri)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Error getting input stream for DocumentFile", e)
            null
        }
    }

    override fun getOutputStream(f: AmazeFile, contextProvider: ContextProvider): OutputStream? {
        val context = contextProvider.getContext() ?: return null
        val contentResolver: ContentResolver = context.contentResolver
        val documentSourceFile = getDocumentFile(f.path, true) ?: return null
        return try {
            contentResolver.openOutputStream(documentSourceFile.uri)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Error getting output stream for DocumentFile", e)
            null
        }
    }

    override fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        if (!exists(f, contextProvider)) {
            val uriRoot = uriRoot ?: return false
            val parent = f.parent ?: return false
            val parentDirectory = getDocumentFile(
                    parent,
                    uriRoot,
                    context,
                    OpenMode.DOCUMENT_FILE,
                    true
            ) ?: return false

            if (parentDirectory.isDirectory) {
                parentDirectory.createDirectory(f.name) ?: return false
                return true
            }
        }

        return false
    }

    override fun rename(file1: AmazeFile, file2: AmazeFile, contextProvider: ContextProvider): Boolean {
        val context = contextProvider.getContext() ?: return false
        val uriRoot = uriRoot ?: return false

        val documentFile = getDocumentFile(
                file1.path,
                uriRoot,
                context,
                OpenMode.DOCUMENT_FILE,
                false
        ) ?: return false

        return try {
            documentFile.renameTo(file2.name)
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming DocumentFile file", e)
            false
        }
    }

    override fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean {
        return false //Can't set
    }

    override fun setReadOnly(f: AmazeFile): Boolean {
        return false //Can't set
    }

    override fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long {
        return getDocumentFile(defaultParent, false)?.length() ?: 0
    }

    override fun getFreeSpace(f: AmazeFile): Long {
        return getUsableSpace(f) // Assume both are equal
    }

    override fun getUsableSpace(f: AmazeFile): Long {
        val volumeLabel = volumeLabel ?: return 0
        return getDeviceStorageRemainingSpace(volumeLabel)
    }

    fun getDocumentFile(path: String, createRecursive: Boolean): DocumentFile? {
        val uriRoot = uriRoot ?: return null

        return getDocumentFile(
                path,
                uriRoot,
                AppConfig.getInstance(),
                OpenMode.DOCUMENT_FILE,
                createRecursive
        )
    }

}