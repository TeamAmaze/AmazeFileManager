/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.fileoperations.filesystem.usb

import android.content.Context
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.util.Log
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Listener interface for device disconnection events.
 * Called when a storage device is removed/disconnected.
 */
fun interface DeviceDisconnectedListener {
    /**
     * Called when a device is disconnected.
     * @param deviceKey The unique key of the disconnected device
     * @param device The device that was disconnected (contains path info)
     */
    fun onDeviceDisconnected(
        deviceKey: String,
        device: StorageDeviceRepresentation,
    )
}

/**
 * Manager for multiple USB OTG / removable storage devices.
 * Supports both legacy [UsbOtgRepresentation] and new [StorageDeviceRepresentation] types.
 *
 * For new code, prefer using [StorageDeviceManager] facade which automatically
 * handles API-level differences.
 */
object UsbOtgManager {
    private const val TAG = "UsbOtgManager"

    /** Map of device key to StorageDeviceRepresentation */
    private val connectedDevices: MutableMap<String, StorageDeviceRepresentation> = ConcurrentHashMap()

    /** Map of device key to SAF root URI */
    private val deviceRoots: MutableMap<String, Uri> = ConcurrentHashMap()

    /** Flag to track if migration has been performed */
    private var migrationPerformed = false

    /** Listeners for device disconnection events */
    private val disconnectionListeners: MutableList<DeviceDisconnectedListener> = CopyOnWriteArrayList()

    /**
     * Add a listener for device disconnection events.
     *
     * @param listener the listener to add
     */
    @JvmStatic
    fun addDisconnectionListener(listener: DeviceDisconnectedListener) {
        disconnectionListeners.add(listener)
    }

    /**
     * Remove a device disconnection listener.
     *
     * @param listener the listener to remove
     */
    @JvmStatic
    fun removeDisconnectionListener(listener: DeviceDisconnectedListener) {
        disconnectionListeners.remove(listener)
    }

    /**
     * Add a device to the manager.
     *
     * @param device the storage device representation
     */
    @JvmStatic
    fun addDevice(device: StorageDeviceRepresentation) {
        connectedDevices[device.deviceKey] = device
    }

    /**
     * Add a device to the manager (legacy support).
     *
     * @param device the USB device representation
     */
    @JvmStatic
    @Deprecated(
        "Use addDevice(StorageDeviceRepresentation) instead",
        ReplaceWith("addDevice(UsbStorageDevice.fromLegacy(device))"),
    )
    fun addDevice(device: UsbOtgRepresentation) {
        val storageDevice = UsbStorageDevice.fromLegacy(device)
        connectedDevices[storageDevice.deviceKey] = storageDevice
    }

    /**
     * Remove a device from the manager.
     *
     * @param deviceKey the unique device key
     */
    @JvmStatic
    fun removeDevice(deviceKey: String) {
        connectedDevices.remove(deviceKey)
        deviceRoots.remove(deviceKey)
    }

    /**
     * Remove a device from the manager.
     *
     * @param device the storage device representation
     */
    @JvmStatic
    fun removeDevice(device: StorageDeviceRepresentation) {
        removeDevice(device.deviceKey)
    }

    /**
     * Remove a device from the manager (legacy support).
     *
     * @param device the USB device representation
     */
    @JvmStatic
    @Deprecated(
        "Use removeDevice(StorageDeviceRepresentation) instead",
        ReplaceWith("removeDevice(device.deviceKey)"),
    )
    fun removeDevice(device: UsbOtgRepresentation) {
        removeDevice(device.deviceKey)
    }

    /**
     * Get a storage device by its key.
     *
     * @param deviceKey the unique device key
     * @return the device or null if not found
     */
    @JvmStatic
    fun getStorageDevice(deviceKey: String): StorageDeviceRepresentation? {
        return connectedDevices[deviceKey]
    }

    /**
     * Get a device by its key (legacy support).
     *
     * @param deviceKey the unique device key
     * @return the device or null if not found
     */
    @JvmStatic
    @Deprecated(
        "Use getStorageDevice(deviceKey) instead",
        ReplaceWith("getStorageDevice(deviceKey)"),
    )
    fun getDevice(deviceKey: String): UsbOtgRepresentation? {
        val device = connectedDevices[deviceKey]
        return when (device) {
            is UsbStorageDevice ->
                UsbOtgRepresentation(
                    device.productId,
                    device.vendorId,
                    device.serialNumber,
                    device.manufacturerName,
                    device.productName,
                )
            else -> null
        }
    }

    /**
     * Get all connected devices.
     *
     * @return unmodifiable collection of all connected storage devices
     */
    @JvmStatic
    fun getStorageDevices(): Collection<StorageDeviceRepresentation> {
        return connectedDevices.values.toList()
    }

    /**
     * Get all connected devices (legacy support).
     *
     * @return unmodifiable collection of all connected devices
     */
    @JvmStatic
    @Deprecated(
        "Use getStorageDevices() instead",
        ReplaceWith("getStorageDevices()"),
    )
    fun getDevices(): Collection<UsbOtgRepresentation> {
        return connectedDevices.values
            .filterIsInstance<UsbStorageDevice>()
            .map { device ->
                UsbOtgRepresentation(
                    device.productId,
                    device.vendorId,
                    device.serialNumber,
                    device.manufacturerName,
                    device.productName,
                )
            }
    }

    /**
     * Get all device keys.
     *
     * @return unmodifiable collection of all device keys
     */
    @JvmStatic
    fun getDeviceKeys(): Collection<String> {
        return connectedDevices.keys.toList()
    }

    /**
     * Check if any device is connected.
     *
     * @return true if at least one device is connected
     */
    @JvmStatic
    fun hasDevicesConnected(): Boolean {
        return connectedDevices.isNotEmpty()
    }

    /**
     * Check if a specific device is connected.
     *
     * @param deviceKey the unique device key
     * @return true if the device is connected
     */
    @JvmStatic
    fun isDeviceConnected(deviceKey: String): Boolean {
        return connectedDevices.containsKey(deviceKey)
    }

    /**
     * Set the SAF root URI for a specific device.
     *
     * @param deviceKey the unique device key
     * @param root the SAF root URI
     * @throws IllegalStateException if the device is not connected
     */
    @JvmStatic
    fun setUsbOtgRoot(
        deviceKey: String,
        root: Uri?,
    ) {
        if (!connectedDevices.containsKey(deviceKey)) {
            throw IllegalStateException("Device not connected: $deviceKey")
        }
        if (root != null) {
            deviceRoots[deviceKey] = root
        } else {
            deviceRoots.remove(deviceKey)
        }
    }

    /**
     * Get the SAF root URI for a specific device.
     *
     * @param deviceKey the unique device key
     * @return the SAF root URI or null if not set
     */
    @JvmStatic
    fun getUsbOtgRoot(deviceKey: String): Uri? {
        return deviceRoots[deviceKey]
    }

    /**
     * Check if a device has a SAF root URI set.
     *
     * @param deviceKey the unique device key
     * @return true if the device has a root URI
     */
    @JvmStatic
    fun hasUsbOtgRoot(deviceKey: String): Boolean {
        return deviceRoots.containsKey(deviceKey)
    }

    /**
     * Get any available SAF root URI.
     * Used for backward compatibility when device key is unknown.
     *
     * @return the first available SAF root URI or null if none
     */
    @JvmStatic
    val anyUsbOtgRoot: Uri?
        get() = deviceRoots.values.firstOrNull()

    /**
     * Get any connected storage device.
     *
     * @return the first connected device or null if none
     */
    @JvmStatic
    val anyStorageDevice: StorageDeviceRepresentation?
        get() = connectedDevices.values.firstOrNull()

    /**
     * Get any connected device (legacy support).
     * Used for backward compatibility when single device was expected.
     *
     * @return the first connected device or null if none
     */
    @JvmStatic
    @Deprecated(
        "Use anyStorageDevice instead",
        ReplaceWith("anyStorageDevice"),
    )
    val anyDevice: UsbOtgRepresentation?
        get() {
            val device = connectedDevices.values.firstOrNull()
            return when (device) {
                is UsbStorageDevice ->
                    UsbOtgRepresentation(
                        device.productId,
                        device.vendorId,
                        device.serialNumber,
                        device.manufacturerName,
                        device.productName,
                    )
                else -> null
            }
        }

    /**
     * Find device key that owns the given URI.
     *
     * @param root the SAF root URI
     * @return the device key or null if not found
     */
    @JvmStatic
    fun findDeviceKeyByRoot(root: Uri): String? {
        return deviceRoots.entries.find { it.value == root }?.key
    }

    /**
     * Reset all OTG state. Clears all devices and roots.
     */
    @JvmStatic
    fun resetAll() {
        connectedDevices.clear()
        deviceRoots.clear()
    }

    /**
     * Update the device list with newly detected devices.
     * Removes devices no longer present, adds new devices, keeps existing ones with their roots.
     * Notifies disconnection listeners for any removed devices.
     *
     * @param detectedDevices list of currently detected devices
     */
    @JvmStatic
    fun updateDevices(detectedDevices: Collection<StorageDeviceRepresentation>) {
        // Build set of detected device keys
        val detectedMap = detectedDevices.associateBy { it.deviceKey }

        // Find devices that are no longer present
        val keysToRemove = connectedDevices.keys.filter { it !in detectedMap }

        // Notify listeners about disconnected devices before removing them
        keysToRemove.forEach { key ->
            val device = connectedDevices[key]
            if (device != null) {
                Log.d(TAG, "Device disconnected: $key")
                disconnectionListeners.forEach { listener ->
                    try {
                        listener.onDeviceDisconnected(key, device)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error notifying disconnection listener", e)
                    }
                }
            }
        }

        // Remove disconnected devices
        keysToRemove.forEach { key ->
            connectedDevices.remove(key)
            deviceRoots.remove(key)
        }

        // Add new devices
        detectedMap.forEach { (key, device) ->
            if (key !in connectedDevices) {
                connectedDevices[key] = device
            }
        }
    }

    /**
     * Update the device list with newly detected devices (legacy support).
     *
     * @param detectedDevices list of currently detected devices
     */
    @JvmStatic
    @Deprecated(
        "Use updateDevices(Collection<StorageDeviceRepresentation>) instead",
        ReplaceWith("updateDevices(detectedDevices.map { UsbStorageDevice.fromLegacy(it) })"),
    )
    fun updateLegacyDevices(detectedDevices: Collection<UsbOtgRepresentation>) {
        updateDevices(detectedDevices.map { UsbStorageDevice.fromLegacy(it) })
    }

    /**
     * Migrate persisted device roots from legacy key format to new format.
     * Should be called once on app startup before using device management.
     *
     * @param context Android context
     * @return Map of old keys to new keys for successfully migrated entries
     */
    @JvmStatic
    fun migratePersistedRoots(context: Context): Map<String, String> {
        if (migrationPerformed) {
            Log.d(TAG, "Migration already performed, skipping")
            return emptyMap()
        }

        migrationPerformed = true
        return DeviceKeyMigration.migratePersistedRoots(context)
    }

    /**
     * Check if migration has been performed.
     *
     * @return true if migratePersistedRoots has been called
     */
    @JvmStatic
    fun isMigrationPerformed(): Boolean = migrationPerformed

    /**
     * Reset migration flag (for testing purposes).
     */
    @JvmStatic
    fun resetMigrationFlag() {
        migrationPerformed = false
    }

    /**
     * Restore persisted SAF URI permissions from Android's ContentResolver.
     * This should be called during app startup to restore URIs that were
     * previously granted and persisted via takePersistableUriPermission.
     *
     * URIs are matched to connected devices by extracting the volume UUID
     * from the URI path and comparing it to connected device keys.
     *
     * @param context Android context
     * @return Number of URIs successfully restored
     */
    @JvmStatic
    fun restorePersistedUriPermissions(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return 0
        }

        val persistedPermissions: List<UriPermission> =
            context.contentResolver.persistedUriPermissions

        var restoredCount = 0

        for (permission in persistedPermissions) {
            if (!permission.isReadPermission) continue

            val uri = permission.uri

            // Only process external storage document URIs (USB/OTG)
            if (uri.authority != "com.android.externalstorage.documents") continue

            // Skip primary storage URIs
            val path = uri.path ?: continue
            if (path.contains("primary")) continue

            // Extract volume UUID from URI
            // URI format: content://com.android.externalstorage.documents/tree/{uuid}%3A[path]
            val volumeUuid = extractVolumeUuidFromUri(uri)
            if (volumeUuid == null) {
                Log.d(TAG, "Could not extract volume UUID from URI: $uri")
                continue
            }

            // Try to match with a connected device
            val matchedDeviceKey = findMatchingDeviceKey(volumeUuid)
            if (matchedDeviceKey != null) {
                // Check if we already have a root for this device
                if (!hasUsbOtgRoot(matchedDeviceKey)) {
                    try {
                        setUsbOtgRoot(matchedDeviceKey, uri)
                        restoredCount++
                        Log.i(TAG, "Restored persisted URI for device $matchedDeviceKey: $uri")
                    } catch (e: IllegalStateException) {
                        Log.w(TAG, "Could not restore URI, device not in manager: $matchedDeviceKey", e)
                    }
                } else {
                    Log.d(TAG, "Device $matchedDeviceKey already has a root URI, skipping")
                }
            } else {
                Log.d(TAG, "No connected device matches volume UUID: $volumeUuid")
            }
        }

        return restoredCount
    }

    /**
     * Extract volume UUID from a SAF URI.
     *
     * @param uri The SAF URI
     * @return Volume UUID or null if cannot be extracted
     */
    private fun extractVolumeUuidFromUri(uri: Uri): String? {
        val path = uri.path ?: return null

        // URI path format: /tree/{uuid}%3A or /tree/{uuid}%3A{path}
        // %3A is URL-encoded colon (:)
        val treePrefix = "/tree/"
        if (!path.startsWith(treePrefix)) return null

        val afterTree = path.removePrefix(treePrefix)
        val decoded =
            try {
                URLDecoder.decode(afterTree, "UTF-8")
            } catch (e: Exception) {
                afterTree
            }

        // Format is now: {uuid}:{path} or just {uuid}:
        val colonIndex = decoded.indexOf(':')
        return if (colonIndex > 0) {
            decoded.substring(0, colonIndex)
        } else if (decoded.isNotEmpty()) {
            decoded
        } else {
            null
        }
    }

    /**
     * Find a connected device key that matches the given volume UUID.
     *
     * @param volumeUuid The volume UUID to match
     * @return Matching device key or null if no match found
     */
    private fun findMatchingDeviceKey(volumeUuid: String): String? {
        // First check volume-based keys (API 24+)
        val volumeKey = "${StorageDeviceRepresentation.PREFIX_VOLUME}$volumeUuid"
        if (connectedDevices.containsKey(volumeKey)) {
            return volumeKey
        }

        // Also check if any connected VolumeStorageDevice has matching UUID
        for ((key, device) in connectedDevices) {
            if (device is VolumeStorageDevice && device.uuid.equals(volumeUuid, ignoreCase = true)) {
                return key
            }
        }

        return null
    }
}
