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

package com.amaze.filemanager.utils

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.file_operations.filesystem.usb.SingletonUsbOtg
import com.amaze.filemanager.file_operations.filesystem.usb.UsbOtgRepresentation
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import kotlin.collections.ArrayList

/** Created by Vishal on 27-04-2017.  */
object OTGUtil {

    const val PREFIX_OTG = "otg:/"
    private const val PREFIX_DOCUMENT_FILE = "content:/"
    const val PREFIX_MEDIA_REMOVABLE = "/mnt/media_rw"

    private val TAG = OTGUtil::class.java.simpleName

    // URLEncoder.encode("/", Charsets.UTF_8.name())
    private const val PATH_SEPARATOR_ENCODED = "%2F"
    private const val PRIMARY_STORAGE_PREFIX = "primary%3AA"
    private const val PATH_ELEMENT_DOCUMENT = "document"

    /**
     * Returns an array of list of files at a specific path in OTG
     *
     * @param path the path to the directory tree, starts with prefix 'otg:/' Independent of URI (or
     * mount point) for the OTG
     * @param context context for loading
     * @return an array of list of files at the path
     */
    @Deprecated("use getDocumentFiles()")
    @JvmStatic
    fun getDocumentFilesList(path: String, context: Context): ArrayList<HybridFileParcelable> {
        val files = ArrayList<HybridFileParcelable>()
        getDocumentFiles(
            path, context,
            object : OnFileFound {
                override fun onFileFound(file: HybridFileParcelable) {
                    files.add(file)
                }
            }
        )
        return files
    }

    /**
     * Get the files at a specific path in OTG
     *
     * @param path the path to the directory tree, starts with prefix 'otg:/' Independent of URI (or
     * mount point) for the OTG
     * @param context context for loading
     */
    @JvmStatic
    fun getDocumentFiles(path: String, context: Context, fileFound: OnFileFound) {
        val rootUriString = SingletonUsbOtg.getInstance().usbOtgRoot
            ?: throw NullPointerException("USB OTG root not set!")
        return getDocumentFiles(rootUriString, path, context, OpenMode.OTG, fileFound)
    }

    @JvmStatic
    fun getDocumentFiles(
        rootUriString: Uri,
        path: String,
        context: Context,
        openMode: OpenMode,
        fileFound: OnFileFound
    ) {
        var rootUri = DocumentFile.fromTreeUri(context, rootUriString)
        val parts: Array<String> = if (openMode == OpenMode.DOCUMENT_FILE) {
            path.substringAfter(rootUriString.toString())
                .split("/", PATH_SEPARATOR_ENCODED).toTypedArray()
        } else {
            path.split("/").toTypedArray()
        }
        for (part in parts.filterNot { it.isEmpty() or it.isBlank() }) {
            // first omit 'otg:/' before iterating through DocumentFile
            if (path == "$PREFIX_OTG/" || path == "$PREFIX_DOCUMENT_FILE/") break
            if (part == "otg:" || part == "" || part == "content:") continue

            // iterating through the required path to find the end point
            rootUri = rootUri!!.findFile(part)
        }

        // we have the end point DocumentFile, list the files inside it and return
        for (file in rootUri!!.listFiles()) {
            if (file.exists()) {
                var size: Long = 0
                if (!file.isDirectory) size = file.length()
                Log.d(context.javaClass.simpleName, "Found file: ${file.name}")
                val baseFile = HybridFileParcelable(
                    path + "/" + file.name,
                    RootHelper.parseDocumentFilePermission(file),
                    file.lastModified(),
                    size,
                    file.isDirectory
                )
                baseFile.name = file.name
                baseFile.mode = openMode
                baseFile.fullUri = file.uri
                fileFound.onFileFound(baseFile)
            }
        }
    }

    /**
     * Traverse to a specified path in OTG
     *
     * @param createRecursive flag used to determine whether to create new file while traversing to
     * path, in case path is not present. Notably useful in opening an output stream.
     */
    @JvmStatic
    fun getDocumentFile(
        path: String,
        context: Context,
        createRecursive: Boolean
    ): DocumentFile? {
        val rootUriString = SingletonUsbOtg.getInstance().usbOtgRoot
            ?: throw NullPointerException("USB OTG root not set!")

        return getDocumentFile(path, rootUriString, context, OpenMode.OTG, createRecursive)
    }

    @JvmStatic
    fun getDocumentFile(
        path: String,
        rootUri: Uri,
        context: Context,
        openMode: OpenMode,
        createRecursive: Boolean
    ): DocumentFile? {
        // start with root of SD card and then parse through document tree.
        var retval = DocumentFile.fromTreeUri(context, rootUri)
        val parts: Array<String> = if (openMode == OpenMode.DOCUMENT_FILE) {
            path.substringAfter(rootUri.toString())
                .split("/", PATH_SEPARATOR_ENCODED).toTypedArray()
        } else {
            path.split("/").toTypedArray()
        }
        for (part in parts.filterNot { it.isEmpty() or it.isBlank() }) {
            if (path == "otg:/" || path == "content:/") break
            if (part == "otg:" || part == "" || part == "content:") continue

            // iterating through the required path to find the end point
            var nextDocument = retval!!.findFile(part)
            if (createRecursive && (nextDocument == null || !nextDocument.exists())) {
                nextDocument = retval.createFile(part.substring(part.lastIndexOf(".")), part)
            }
            retval = nextDocument
        }
        return retval
    }

    /** Check if the usb uri is still accessible  */
    @RequiresApi(api = KITKAT)
    @JvmStatic
    fun isUsbUriAccessible(context: Context?): Boolean {
        val rootUriString = SingletonUsbOtg.getInstance().usbOtgRoot
        return DocumentsContract.isDocumentUri(context, rootUriString)
    }

    /** Checks if there is at least one USB device connected with class MASS STORAGE.  */
    @JvmStatic
    fun getMassStorageDevicesConnected(
        context: Context
    ): List<UsbOtgRepresentation> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList
        return devices.mapNotNullTo(
            ArrayList(),
            { entry ->
                val device = entry.value
                var retval: UsbOtgRepresentation? = null
                for (i in 0 until device.interfaceCount) {
                    if (device.getInterface(i).interfaceClass
                        == UsbConstants.USB_CLASS_MASS_STORAGE
                    ) {
                        var serial: String? = null
                        if (SDK_INT >= LOLLIPOP) {
                            try {
                                serial = device.serialNumber
                            } catch (ifPermissionDenied: SecurityException) {
                                // May happen when device is running Android 10 or above.
                                Log.w(
                                    TAG,
                                    "Permission denied reading serial number of device " +
                                        "${device.vendorId}:${device.productId}",
                                    ifPermissionDenied
                                )
                            }
                        }
                        retval = UsbOtgRepresentation(device.productId, device.vendorId, serial)
                    }
                }
                retval
            }
        )
    }
}
