package com.amaze.filemanager.fileoperations.filesystem.usb

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Facade for opportunistic direct filesystem access to OTG storage, with fallback to SAF.
 *
 * Usage:
 *   val files = OtgFileAccessFacade.listFiles(context, deviceKey, subPath)
 *   val file = OtgFileAccessFacade.getFile(context, deviceKey, filePath)
 */
object OtgFileAccessFacade {
    private const val TAG = "OtgFileAccessFacade"

    /**
     * List files at a given subPath on the OTG device.
     * Tries direct filesystem access if available, otherwise falls back to SAF.
     */
    fun listFiles(
        context: Context,
        deviceKey: String,
        subPath: String = "",
    ): List<File> {
        val device = StorageDeviceManager.findDeviceByKey(context, deviceKey)
        val filePath = device?.filePath
        if (filePath != null && hasDirectAccess(filePath)) {
            val dir = File(filePath, subPath)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                return dir.listFiles()?.toList() ?: emptyList()
            }
        }
        // Fallback to SAF
        return listFilesSaf(deviceKey, subPath)
    }

    /**
     * Get a File for direct access if possible, otherwise null.
     */
    fun getFile(
        context: Context,
        deviceKey: String,
        filePath: String,
    ): File? {
        val device = StorageDeviceManager.findDeviceByKey(context, deviceKey)
        val mountPath = device?.filePath
        if (mountPath != null && hasDirectAccess(mountPath)) {
            val file = File(mountPath, filePath)
            if (file.exists()) return file
        }
        return null
    }

    /**
     * Check if direct filesystem access is available and permitted.
     */
    fun hasDirectAccess(mountPath: String): Boolean {
        // On Android 11+ need MANAGE_EXTERNAL_STORAGE, on older need legacy permissions
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager() && File(mountPath).canRead()
        } else {
            File(mountPath).canRead()
        }
    }

    /**
     * Fallback: List files using SAF/DocumentFile.
     */
    private fun listFilesSaf(
        deviceKey: String,
        subPath: String,
    ): List<File> {
        // This is a stub: actual implementation should use DocumentFile and persisted URI
        Log.w(TAG, "Falling back to SAF for device $deviceKey at $subPath")
        // TODO: Implement SAF-based listing using persisted URI
        return emptyList()
    }

    // Add more methods as needed (read, write, delete, etc.) with similar fallback logic
}
