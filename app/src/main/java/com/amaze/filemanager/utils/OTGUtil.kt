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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.S
import android.os.Build.VERSION_CODES.TIRAMISU
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.amaze.filemanager.exceptions.DocumentFileNotFoundException
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.fileoperations.filesystem.usb.UsbOtgManager
import com.amaze.filemanager.fileoperations.filesystem.usb.UsbOtgRepresentation
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.utils.OTGUtil.ACTION_USB_PERMISSION
import java.net.URLDecoder

/** Created by Vishal on 27-04-2017.  */
object OTGUtil {
    const val PREFIX_OTG = "otg:/"
    private const val PREFIX_DOCUMENT_FILE = "content:/"
    const val PREFIX_MEDIA_REMOVABLE = "/mnt/media_rw"

    private val TAG = OTGUtil::class.java.simpleName

    /** Action for USB permission broadcast */
    const val ACTION_USB_PERMISSION = "com.amaze.filemanager.USB_PERMISSION"

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
    fun getDocumentFilesList(
        path: String,
        context: Context,
    ): ArrayList<HybridFileParcelable> {
        val files = ArrayList<HybridFileParcelable>()
        getDocumentFiles(
            path,
            context,
            object : OnFileFound {
                override fun onFileFound(file: HybridFileParcelable) {
                    files.add(file)
                }
            },
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
    fun getDocumentFiles(
        path: String,
        context: Context,
        fileFound: OnFileFound,
    ) {
        val deviceKey = extractDeviceKeyFromPath(path)
        val rootUriString =
            if (deviceKey != null) {
                UsbOtgManager.getUsbOtgRoot(deviceKey)
            } else {
                UsbOtgManager.anyUsbOtgRoot
            } ?: throw NullPointerException("USB OTG root not set!")
        return getDocumentFiles(rootUriString, path, context, OpenMode.OTG, fileFound)
    }

    /**
     * Get the files at a specific path in OTG for a specific device
     *
     * @param deviceKey the unique device key
     * @param path the path to the directory tree
     * @param context context for loading
     * @param fileFound callback for each file found
     */
    @JvmStatic
    fun getDocumentFiles(
        deviceKey: String,
        path: String,
        context: Context,
        fileFound: OnFileFound,
    ) {
        val rootUriString =
            UsbOtgManager.getUsbOtgRoot(deviceKey)
                ?: throw NullPointerException("USB OTG root not set for device: $deviceKey")
        return getDocumentFiles(rootUriString, path, context, OpenMode.OTG, fileFound)
    }

    @JvmStatic
    fun getDocumentFiles(
        rootUriString: Uri,
        path: String,
        context: Context,
        openMode: OpenMode,
        fileFound: OnFileFound,
    ) {
        var rootUri = DocumentFile.fromTreeUri(context, rootUriString)

        // Extract device key to skip it during path traversal
        val deviceKey = extractDeviceKeyFromPath(path)

        val parts: Array<String> =
            if (openMode == OpenMode.DOCUMENT_FILE) {
                path.substringAfter(rootUriString.toString())
                    .split("/", PATH_SEPARATOR_ENCODED).toTypedArray()
            } else {
                path.split("/").toTypedArray()
            }
        for (part in parts.filterNot { it.isEmpty() or it.isBlank() }) {
            // first omit 'otg:/' before iterating through DocumentFile
            if (path == "$PREFIX_OTG/" || path == "$PREFIX_DOCUMENT_FILE/") break
            if (part == "otg:" || part == "" || part == "content:") continue
            // Skip device key part (e.g., "1234:5678")
            if (deviceKey != null && part == deviceKey) continue

            // iterating through the required path to find the end point
            rootUri = rootUri?.findFile(part) ?: rootUri
        }

        if (rootUri == null) {
            throw DocumentFileNotFoundException(rootUriString, path)
        }

        // we have the end point DocumentFile, list the files inside it and return
        for (file in rootUri.listFiles()) {
            if (file.exists()) {
                var size: Long = 0
                if (!file.isDirectory) size = file.length()
                Log.d(context.javaClass.simpleName, "Found file: ${file.name}")
                val baseFile =
                    HybridFileParcelable(
                        path + "/" + file.name,
                        RootHelper.parseDocumentFilePermission(file),
                        file.lastModified(),
                        size,
                        file.isDirectory,
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
        createRecursive: Boolean,
    ): DocumentFile? {
        val deviceKey = extractDeviceKeyFromPath(path)
        val rootUriString =
            if (deviceKey != null) {
                UsbOtgManager.getUsbOtgRoot(deviceKey)
            } else {
                UsbOtgManager.anyUsbOtgRoot
            } ?: throw NullPointerException("USB OTG root not set!")

        return getDocumentFile(path, rootUriString, context, OpenMode.OTG, createRecursive)
    }

    /**
     * Traverse to a specified path in OTG for a specific device
     *
     * @param deviceKey the unique device key
     * @param path the path to the file/directory
     * @param context context for loading
     * @param createRecursive flag used to determine whether to create new file while traversing
     */
    @JvmStatic
    fun getDocumentFile(
        deviceKey: String,
        path: String,
        context: Context,
        createRecursive: Boolean,
    ): DocumentFile? {
        val rootUriString =
            UsbOtgManager.getUsbOtgRoot(deviceKey)
                ?: throw NullPointerException("USB OTG root not set for device: $deviceKey")

        return getDocumentFile(path, rootUriString, context, OpenMode.OTG, createRecursive)
    }

    @JvmStatic
    fun getDocumentFile(
        path: String,
        rootUri: Uri,
        context: Context,
        openMode: OpenMode,
        createRecursive: Boolean,
    ): DocumentFile? {
        // start with root of SD card and then parse through document tree.
        var retval: DocumentFile? =
            DocumentFile.fromTreeUri(context, rootUri)
                ?: throw DocumentFileNotFoundException(rootUri, path)

        // Extract device key to skip it during path traversal
        val deviceKey = extractDeviceKeyFromPath(path)

        val parts: Array<String> =
            if (openMode == OpenMode.DOCUMENT_FILE) {
                URLDecoder.decode(path, Charsets.UTF_8.name()).substringAfter(
                    URLDecoder.decode(rootUri.toString(), Charsets.UTF_8.name()),
                )
                    .split("/", PATH_SEPARATOR_ENCODED).toTypedArray()
            } else {
                path.split("/").toTypedArray()
            }
        for (part in parts.filterNot { it.isEmpty() or it.isBlank() }) {
            if (path == "otg:/" || path == "content:/") break
            if (part == "otg:" || part == "" || part == "content:") continue
            // Skip device key part (e.g., "1234:5678")
            if (deviceKey != null && part == deviceKey) continue

            // iterating through the required path to find the end point
            var nextDocument = retval?.findFile(part)
            if (createRecursive && (nextDocument == null || !nextDocument.exists())) {
                nextDocument = retval?.createFile(part.substring(part.lastIndexOf(".")), part)
            }
            retval = nextDocument
        }
        return retval
    }

    /** Check if the usb uri is still accessible  */
    @RequiresApi(api = KITKAT)
    @JvmStatic
    fun isUsbUriAccessible(context: Context?): Boolean {
        val rootUriString = UsbOtgManager.anyUsbOtgRoot
        return DocumentsContract.isDocumentUri(context, rootUriString)
    }

    /** Check if the usb uri for a specific device is still accessible  */
    @RequiresApi(api = KITKAT)
    @JvmStatic
    fun isUsbUriAccessible(
        context: Context?,
        deviceKey: String,
    ): Boolean {
        val rootUriString = UsbOtgManager.getUsbOtgRoot(deviceKey)
        return rootUriString != null && DocumentsContract.isDocumentUri(context, rootUriString)
    }

    /**
     * Check if the app has USB permission for a specific device.
     *
     * @param context the context
     * @param device the USB device to check
     * @return true if permission is granted
     */
    @JvmStatic
    fun hasUsbPermission(
        context: Context,
        device: UsbDevice,
    ): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        return usbManager?.hasPermission(device) ?: false
    }

    /**
     * Request USB permission for a specific device.
     * The result will be delivered via a broadcast with action [ACTION_USB_PERMISSION].
     *
     * @param context the context
     * @param device the USB device to request permission for
     * @param onPermissionResult callback for permission result (device, granted)
     */
    @JvmStatic
    fun requestUsbPermission(
        context: Context,
        device: UsbDevice,
        onPermissionResult: ((UsbDevice, Boolean) -> Unit)? = null,
    ) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return

        if (usbManager.hasPermission(device)) {
            // Already have permission
            onPermissionResult?.invoke(device, true)
            return
        }

        // Create a broadcast receiver for the permission result
        val permissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    if (ACTION_USB_PERMISSION == intent.action) {
                        val usbDevice: UsbDevice? =
                            if (SDK_INT >= TIRAMISU) {
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                            }

                        if (usbDevice != null &&
                            usbDevice.vendorId == device.vendorId &&
                            usbDevice.productId == device.productId
                        ) {
                            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            onPermissionResult?.invoke(device, granted)
                            try {
                                context.unregisterReceiver(this)
                            } catch (e: IllegalArgumentException) {
                                // Receiver already unregistered
                            }
                        }
                    }
                }
            }

        // Register the receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        ContextCompat.registerReceiver(
            context,
            permissionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Create the pending intent
        val pendingIntentFlags =
            if (SDK_INT >= S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        val permissionIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                pendingIntentFlags,
            )

        // Request permission
        usbManager.requestPermission(device, permissionIntent)
    }

    /**
     * Request USB permissions for all mass storage devices that don't have permission yet.
     *
     * @param context the context
     * @param onAllPermissionsHandled callback when all permission requests are handled
     */
    @JvmStatic
    fun requestUsbPermissionsForAllDevices(
        context: Context,
        onAllPermissionsHandled: Runnable? = null,
    ) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val devices = usbManager?.deviceList ?: mapOf()

        val massStorageDevices =
            devices.values.filter { device ->
                (0 until device.interfaceCount).any { i ->
                    device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
                }
            }

        val devicesNeedingPermission = massStorageDevices.filter { !hasUsbPermission(context, it) }

        if (devicesNeedingPermission.isEmpty()) {
            onAllPermissionsHandled?.run()
            return
        }

        var pendingCount = devicesNeedingPermission.size

        devicesNeedingPermission.forEach { device ->
            requestUsbPermission(context, device) { _, _ ->
                pendingCount--
                if (pendingCount == 0) {
                    onAllPermissionsHandled?.run()
                }
            }
        }
    }

    /**
     * Checks if there is at least one USB device connected with class MASS STORAGE.
     * Note: On Android 10+, USB permission may be required to access device details
     * like serial number, manufacturer name, and product name.
     *
     * @param context the context (use Activity context for permission requests)
     * @return list of connected mass storage devices
     */
    @JvmStatic
    fun getMassStorageDevicesConnected(context: Context): List<UsbOtgRepresentation> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val devices = usbManager?.deviceList ?: mapOf()
        return devices.mapNotNullTo(
            ArrayList(),
        ) { entry ->
            val device = entry.value
            var retval: UsbOtgRepresentation? = null
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass ==
                    UsbConstants.USB_CLASS_MASS_STORAGE
                ) {
                    retval = createUsbOtgRepresentation(context, usbManager, device)
                    break
                }
            }
            retval
        }
    }

    /**
     * Creates a UsbOtgRepresentation from a UsbDevice.
     * Attempts to read device details if permission is granted.
     *
     * @param context the context
     * @param usbManager the USB manager
     * @param device the USB device
     * @return the representation with available device info
     */
    private fun createUsbOtgRepresentation(
        context: Context,
        usbManager: UsbManager?,
        device: UsbDevice,
    ): UsbOtgRepresentation {
        var serial: String? = null
        var manufacturerName: String? = null
        var productName: String? = null

        // Check if we have permission to access device details
        val hasPermission = usbManager?.hasPermission(device) ?: false

        if (SDK_INT >= LOLLIPOP) {
            if (hasPermission) {
                // We have permission, try to read all details
                try {
                    serial = device.serialNumber
                    manufacturerName = device.manufacturerName
                    productName = device.productName
                } catch (e: SecurityException) {
                    Log.w(
                        TAG,
                        "Permission denied reading device info of " +
                            "${device.vendorId}:${device.productId}",
                        e,
                    )
                }
            } else {
                // No permission - try to get basic info that doesn't require permission
                // manufacturerName and productName are available without permission on API 21+
                try {
                    manufacturerName = device.manufacturerName
                    productName = device.productName
                } catch (e: SecurityException) {
                    Log.d(
                        TAG,
                        "Cannot read device name without permission: " +
                            "${device.vendorId}:${device.productId}",
                    )
                }
                // Serial number requires permission on Android 10+
                if (SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    try {
                        serial = device.serialNumber
                    } catch (e: SecurityException) {
                        Log.d(TAG, "Cannot read serial without permission")
                    }
                }
            }
        }

        return UsbOtgRepresentation(
            device.productId,
            device.vendorId,
            serial,
            manufacturerName,
            productName,
        )
    }

    /**
     * Get a raw UsbDevice by vendor and product ID.
     *
     * @param context the context
     * @param vendorId the vendor ID
     * @param productId the product ID
     * @return the UsbDevice or null if not found
     */
    @JvmStatic
    fun getUsbDevice(
        context: Context,
        vendorId: Int,
        productId: Int,
    ): UsbDevice? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        return usbManager?.deviceList?.values?.find {
            it.vendorId == vendorId && it.productId == productId
        }
    }

    /**
     * Get a raw UsbDevice by device key.
     *
     * @param context the context
     * @param deviceKey the device key (format: vendorId:productId or vendorId:productId:serial)
     * @return the UsbDevice or null if not found
     */
    @JvmStatic
    fun getUsbDeviceByKey(
        context: Context,
        deviceKey: String,
    ): UsbDevice? {
        val parts = deviceKey.split(":")
        if (parts.size < 2) return null

        val vendorId = parts[0].toIntOrNull() ?: return null
        val productId = parts[1].toIntOrNull() ?: return null

        return getUsbDevice(context, vendorId, productId)
    }

    // ============ Device Key Path Helpers ============

    /**
     * Extract device key from an OTG path.
     * Paths can be in format: "otg:/deviceKey/path" or legacy "otg:/path"
     *
     * @param path the OTG path
     * @return the device key or null if not found (legacy format)
     */
    @JvmStatic
    fun extractDeviceKeyFromPath(path: String): String? {
        if (!path.startsWith(PREFIX_OTG)) {
            return null
        }
        val pathWithoutPrefix = path.removePrefix(PREFIX_OTG)
        if (pathWithoutPrefix.isEmpty() || pathWithoutPrefix == "/") {
            return null
        }
        // Check if path starts with a device key (format: vendorId:productId or vendorId:productId:serial)
        val firstSegment = pathWithoutPrefix.trimStart('/').split("/").firstOrNull() ?: return null
        // Device keys contain colons (e.g., "1234:5678" or "1234:5678:ABC123")
        return if (firstSegment.contains(":") && firstSegment.split(":").size >= 2) {
            firstSegment
        } else {
            null
        }
    }

    /**
     * Build an OTG path with device key.
     *
     * @param deviceKey the unique device key
     * @param subPath the path within the device (can be empty)
     * @return the full OTG path
     */
    @JvmStatic
    fun buildOtgPath(
        deviceKey: String,
        subPath: String = "",
    ): String {
        val cleanSubPath = subPath.trimStart('/')
        return if (cleanSubPath.isEmpty()) {
            "$PREFIX_OTG$deviceKey/"
        } else {
            "$PREFIX_OTG$deviceKey/$cleanSubPath"
        }
    }

    /**
     * Get the sub-path (path within device) from an OTG path.
     *
     * @param path the full OTG path
     * @return the path within the device, or the original path portion if no device key
     */
    @JvmStatic
    fun getSubPathFromOtgPath(path: String): String {
        if (!path.startsWith(PREFIX_OTG)) {
            return path
        }
        val pathWithoutPrefix = path.removePrefix(PREFIX_OTG).trimStart('/')
        val deviceKey = extractDeviceKeyFromPath(path)
        return if (deviceKey != null) {
            pathWithoutPrefix.removePrefix(deviceKey).trimStart('/')
        } else {
            pathWithoutPrefix
        }
    }
}
