package com.amaze.filemanager.filesystem.files

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.preference.PreferenceManager
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.ExternalSdCardOperation
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.MediaStoreHack
import com.amaze.filemanager.file_operations.filesystem.filetypes.file.UriForSafPersistance
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.MakeDirectoryCommand
import java.io.*

/**
 * This is in in essence calls to UnixFilesystem, but that class is not public so all calls must go
 * through java.io.File
 */
object FileAmazeFilesystem : AmazeFilesystem() {
    const val PREFERENCE_ROOTMODE = "rootmode"

    @JvmStatic
    val TAG = FileAmazeFilesystem::class.java.simpleName

    init {
        AmazeFile.addFilesystem(this)
    }

    override fun isPathOfThisFilesystem(path: String): Boolean {
        return path[0] == getSeparator()
    }

    override val prefix: String = ""

    override fun getSeparator(): Char {
        return File.separatorChar
    }

    override fun normalize(path: String): String {
        return File(path).path
    }

    override fun prefixLength(path: String): Int {
        if (path.length == 0) return 0
        return if (path[0] == '/') 1 else 0
    }

    override fun resolve(parent: String, child: String): String {
        return File(parent, child).path
    }

    override val defaultParent: String
        get() = File(File(""), "").path

    override fun isAbsolute(f: AmazeFile): Boolean {
        return File(f.path).isAbsolute
    }

    override fun resolve(f: AmazeFile): String {
        return File(f.path).absolutePath
    }

    @Throws(IOException::class)
    override fun canonicalize(path: String?): String {
        return File(path).canonicalPath
    }

    override fun exists(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return runAsNormalOrRoot(f, contextProvider, {
            File(f.path).exists()
        }) {
            RootHelper.fileExists(f.path)
        }
    }

    override fun isFile(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).isFile
    }

    override fun isDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).isDirectory
    }

    override fun isHidden(f: AmazeFile): Boolean {
        return File(f.path).isHidden
    }

    override fun canExecute(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).canExecute()
    }

    override fun canWrite(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).canWrite()
    }

    override fun canRead(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).canRead()
    }

    override fun canAccess(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return File(f.path).exists()
    }

    override fun setExecutable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return File(f.path).setExecutable(enable, owneronly)
    }

    override fun setWritable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return File(f.path).setWritable(enable, owneronly)
    }

    override fun setReadable(f: AmazeFile, enable: Boolean, owneronly: Boolean): Boolean {
        return File(f.path).setReadable(enable, owneronly)
    }

    override fun getLastModifiedTime(f: AmazeFile): Long {
        return File(f.path).lastModified()
    }

    @Throws(IOException::class)
    override fun getLength(f: AmazeFile, contextProvider: ContextProvider): Long {
        return File(f.path).length()
    }

    @Throws(IOException::class)
    override fun createFileExclusively(pathname: String): Boolean {
        return File(pathname).createNewFile()
    }

    override fun delete(f: AmazeFile, contextProvider: ContextProvider): Boolean {

        if (f.isDirectory(contextProvider)) {
            val children = f.listFiles(contextProvider)
            if (children != null) {
                for (child in children) {
                    delete(child, contextProvider)
                }
            }

            // Try the normal way
            if (File(f.path).delete()) {
                return true
            }
            val context = contextProvider.getContext()

            // Try with Storage Access Framework.
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val document = ExternalSdCardOperation.getDocumentFile(
                        f, true, context, UriForSafPersistance.get(context)
                )
                if (document != null && document.delete()) {
                    return true
                }
            }

            // Try the Kitkat workaround.
            if (context != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                val resolver = context.contentResolver
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DATA, f.absolutePath)
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                // Delete the created entry, such that content provider will delete the file.
                resolver.delete(
                        MediaStore.Files.getContentUri("external"),
                    MediaStore.MediaColumns.DATA + "=?", arrayOf(f.absolutePath)
                )
            }
            return !f.exists(contextProvider)
        }
        if (File(f.path).delete()) {
            return true
        }
        val context = contextProvider.getContext()

        // Try with Storage Access Framework.
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                ExternalSdCardOperation.isOnExtSdCard(f, context)
        ) {
            val document = ExternalSdCardOperation.getDocumentFile(
                    f, false, context, UriForSafPersistance.get(context)
            ) ?: return true
            if (document.delete()) {
                return true
            }
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            val resolver = context!!.contentResolver
            try {
                val uri = MediaStoreHack.getUriFromFile(f.absolutePath, context)
                    ?: return false
                resolver.delete(uri, null, null)
                return !f.exists(contextProvider)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception when checking for file " + f.absolutePath, e)
            }
        }
        return false
    }

    override fun list(f: AmazeFile, contextProvider: ContextProvider): Array<String>? {
        return File(f.path).list()
    }

    override fun getInputStream(f: AmazeFile, contextProvider: ContextProvider): InputStream? {
        return try {
            FileInputStream(f.path)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot find file", e)
            null
        }
    }

    override fun getOutputStream(f: AmazeFile, contextProvider: ContextProvider): OutputStream? {
        return try {
            if (f.canWrite(contextProvider)) {
                return FileOutputStream(f.path)
            } else {
                val context = contextProvider.getContext() ?: return null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    val targetDocument = ExternalSdCardOperation.getDocumentFile(
                            f, false, context, UriForSafPersistance.get(context)
                    ) ?: return null
                    return context.contentResolver.openOutputStream(targetDocument.uri)
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    return MediaStoreHack.getOutputStream(context, f.path)
                }
            }
            null
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot find file", e)
            null
        }
    }

    override fun createDirectory(f: AmazeFile, contextProvider: ContextProvider): Boolean {
        return runAsNormalOrRoot(f, contextProvider, {
            if (File(f.path).mkdir()) {
                return@runAsNormalOrRoot true
            }
            val context = contextProvider.getContext()

            // Try with Storage Access Framework.
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    ExternalSdCardOperation.isOnExtSdCard(f, context)
            ) {
                val preferenceUri = UriForSafPersistance.get(context)
                val document = ExternalSdCardOperation.getDocumentFile(f, true, context, preferenceUri)
                        ?: return@runAsNormalOrRoot false
                // getDocumentFile implicitly creates the directory.
                return@runAsNormalOrRoot document.exists()
            }

            // Try the Kitkat workaround.
            return@runAsNormalOrRoot if (context != null && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                try {
                    MediaStoreHack.mkdir(context, f)
                } catch (e: IOException) {
                    false
                }
            } else false
        }) {
            val parent = f.parent ?: return@runAsNormalOrRoot false
            MakeDirectoryCommand.makeDirectory(parent, f.name)
            return@runAsNormalOrRoot true
        }
    }

    override fun rename(
            file1: AmazeFile,
            file2: AmazeFile,
            contextProvider: ContextProvider
    ): Boolean {
        return File(file1.path).renameTo(File(file2.path))
    }

    override fun setLastModifiedTime(f: AmazeFile, time: Long): Boolean {
        return File(f.path).setLastModified(time)
    }

    override fun setReadOnly(f: AmazeFile): Boolean {
        return File(f.path).setReadOnly()
    }

    override fun getTotalSpace(f: AmazeFile, contextProvider: ContextProvider): Long {
        return File(f.path).totalSpace
    }

    override fun getFreeSpace(f: AmazeFile): Long {
        return File(f.path).freeSpace
    }

    override fun getUsableSpace(f: AmazeFile): Long {
        return File(f.path).usableSpace
    }

    override fun compare(f1: AmazeFile, f2: AmazeFile): Int {
        return File(f1.path).compareTo(File(f2.path))
    }

    override fun hashCode(f: AmazeFile): Int {
        return File(f.path).hashCode()
    }

    private fun <T> runAsNormalOrRoot(file: AmazeFile, contextProvider: ContextProvider,
                                      normalMode: () -> T, rootMode: () -> T): T {
        val context = contextProvider.getContext() ?: return normalMode()

        val rootmode = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFERENCE_ROOTMODE, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            if (rootmode && !canRead(file, contextProvider)) {
                return rootMode()
            }

            return normalMode()
        }

        if (ExternalSdCardOperation.isOnExtSdCard(file, context)) {
            return normalMode()
        } else if (rootmode && !canRead(file, contextProvider)) {
            return rootMode()
        }

        return normalMode()
    }
}