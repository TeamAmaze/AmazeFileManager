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
     * Toggle for preferring native filesystem access over SAF.
     * When false, always falls back to SAF even if direct access is available.
     * Default is FALSE (opt-in to direct access) to prioritize data safety.
     * This is useful for testing SAF code paths on devices that support direct access.
     */
    @Volatile
    var preferNativeAccess: Boolean = false

    /**
     * Check if a mount point is currently accessible.
     * Used to detect if device was unplugged or unmounted.
     */
    private fun isMountPointAccessible(mountPath: String): Boolean {
        return try {
            val dir = File(mountPath)
            // Check if path exists and is readable - detects unplugged devices
            dir.exists() && dir.canRead() && dir.canExecute()
        } catch (e: Exception) {
            Log.w(TAG, "Mount point check failed for $mountPath", e)
            false
        }
    }

    /**
     * List files at a given subPath on the OTG device.
     * Tries direct filesystem access if available, otherwise falls back to SAF.
     * Returns empty list if device is detected as unplugged/unmounted.
     */
    fun listFiles(
        context: Context,
        deviceKey: String,
        subPath: String = "",
    ): List<File> {
        if (!preferNativeAccess) {
            // User disabled native access - always use SAF
            return listFilesSaf(deviceKey, subPath)
        }
        val device = StorageDeviceManager.findDeviceByKey(context, deviceKey)
        val filePath = device?.filePath
        if (filePath != null && hasDirectAccess(filePath)) {
            // Check if device is still mounted before attempting to list
            if (!isMountPointAccessible(filePath)) {
                Log.w(TAG, "OTG device at $filePath is no longer accessible (unplugged?), falling back to SAF")
                return listFilesSaf(deviceKey, subPath)
            }
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
     * Returns null if device is detected as unplugged/unmounted.
     */
    fun getFile(
        context: Context,
        deviceKey: String,
        filePath: String,
    ): File? {
        if (!preferNativeAccess) {
            // User disabled native access - don't return direct File
            return null
        }
        val device = StorageDeviceManager.findDeviceByKey(context, deviceKey)
        val mountPath = device?.filePath
        if (mountPath != null && hasDirectAccess(mountPath)) {
            // Check if device is still mounted before attempting to access
            if (!isMountPointAccessible(mountPath)) {
                Log.w(TAG, "OTG device at $mountPath is no longer accessible (unplugged?)")
                return null
            }
            val file = File(mountPath, filePath)
            if (file.exists()) return file
        }
        return null
    }

    /**
     * Check if direct filesystem access is available and permitted.
     */
    fun hasDirectAccess(mountPath: String): Boolean {
        if (!preferNativeAccess) {
            // User disabled native access preference
            return false
        }
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
