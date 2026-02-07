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
import android.net.Uri
import android.util.Log

/**
 * Utility for migrating device keys from legacy format to new format.
 *
 * Legacy format (pre-migration): "vendorId:productId[:serial]"
 * New USB format: "usb:vendorId:productId[:serial]"
 * New Volume format: "vol:uuid"
 *
 * Migration strategy:
 * 1. If a legacy key matches a currently connected USB device, convert to "usb:" prefixed key
 * 2. If on API 24+ and a volume is connected, attempt to associate with "vol:" key
 * 3. If no match found, return null (SAF permission needs to be re-granted)
 */
object DeviceKeyMigration {
    private const val TAG = "DeviceKeyMigration"

    /**
     * Result of a device key migration attempt.
     */
    sealed class MigrationResult {
        /**
         * Successfully migrated to a new key.
         */
        data class Migrated(val newKey: String) : MigrationResult()

        /**
         * Key is already in new format, no migration needed.
         */
        data class AlreadyMigrated(val key: String) : MigrationResult()

        /**
         * Could not find a matching device to migrate to.
         * The persisted SAF URI may need to be re-granted.
         */
        data object NoMatch : MigrationResult()
    }

    /**
     * Attempt to migrate a device key to the new format.
     *
     * @param legacyKey The key to migrate (may already be in new format)
     * @param currentDevices List of currently connected devices
     * @return Migration result
     */
    @JvmStatic
    fun migrateKey(
        legacyKey: String,
        currentDevices: List<StorageDeviceRepresentation>,
    ): MigrationResult {
        // Check if already in new format
        if (StorageDeviceRepresentation.isUsbKey(legacyKey) ||
            StorageDeviceRepresentation.isVolumeKey(legacyKey)
        ) {
            return MigrationResult.AlreadyMigrated(legacyKey)
        }

        // Parse legacy key format: "vendorId:productId[:serial]"
        val parts = legacyKey.split(":")
        if (parts.size < 2) {
            Log.w(TAG, "Invalid legacy key format: $legacyKey")
            return MigrationResult.NoMatch
        }

        val vendorId = parts[0].toIntOrNull()
        val productId = parts[1].toIntOrNull()
        val serial = parts.getOrNull(2)

        if (vendorId == null || productId == null) {
            Log.w(TAG, "Could not parse vendor/product ID from legacy key: $legacyKey")
            return MigrationResult.NoMatch
        }

        // First, try to match with USB devices (exact match preferred)
        val usbMatch =
            currentDevices.filterIsInstance<UsbStorageDevice>().find { device ->
                device.vendorId == vendorId &&
                    device.productId == productId &&
                    (serial == null || device.serialNumber == serial)
            }

        if (usbMatch != null) {
            Log.d(TAG, "Migrated legacy key '$legacyKey' to USB key '${usbMatch.deviceKey}'")
            return MigrationResult.Migrated(usbMatch.deviceKey)
        }

        // On API 24+, we might have VolumeStorageDevices instead
        // Try to find any connected volume - user may need to re-confirm if multiple volumes
        val volumeMatch = currentDevices.filterIsInstance<VolumeStorageDevice>().firstOrNull()

        if (volumeMatch != null && currentDevices.size == 1) {
            // Only auto-migrate to volume if there's exactly one device connected
            // This reduces the chance of associating with the wrong volume
            Log.d(TAG, "Migrated legacy key '$legacyKey' to volume key '${volumeMatch.deviceKey}'")
            return MigrationResult.Migrated(volumeMatch.deviceKey)
        }

        Log.d(TAG, "No matching device found for legacy key: $legacyKey")
        return MigrationResult.NoMatch
    }

    /**
     * Migrate all persisted device roots in UsbOtgManager.
     *
     * @param context Android context
     * @return Map of old keys to new keys for successfully migrated entries
     */
    @JvmStatic
    fun migratePersistedRoots(context: Context): Map<String, String> {
        val currentDevices = StorageDeviceManager.getRemovableDevices(context)
        val migrations = mutableMapOf<String, String>()

        // Get current device keys and roots from UsbOtgManager
        val keysToMigrate =
            UsbOtgManager.getDeviceKeys().filter { key ->
                StorageDeviceRepresentation.isLegacyKey(key)
            }

        for (oldKey in keysToMigrate) {
            when (val result = migrateKey(oldKey, currentDevices)) {
                is MigrationResult.Migrated -> {
                    val root = UsbOtgManager.getUsbOtgRoot(oldKey)
                    if (root != null) {
                        migrations[oldKey] = result.newKey
                    }
                }
                is MigrationResult.AlreadyMigrated -> {
                    // Nothing to do
                }
                MigrationResult.NoMatch -> {
                    Log.w(TAG, "Could not migrate persisted root for key: $oldKey")
                }
            }
        }

        // Apply migrations to UsbOtgManager
        for ((oldKey, newKey) in migrations) {
            val root = UsbOtgManager.getUsbOtgRoot(oldKey)
            if (root != null) {
                // Remove old entry
                try {
                    UsbOtgManager.removeDevice(oldKey)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove old device entry: $oldKey", e)
                }

                // Add with new key (device must be re-added first)
                val device = currentDevices.find { it.deviceKey == newKey }
                if (device != null) {
                    UsbOtgManager.addDevice(device)
                    UsbOtgManager.setUsbOtgRoot(newKey, root)
                    Log.i(TAG, "Successfully migrated root from '$oldKey' to '$newKey'")
                }
            }
        }

        return migrations
    }

    /**
     * Check if a URI authority matches a specific device key pattern.
     * Used for correlating SAF URIs with devices.
     *
     * @param uri The SAF URI
     * @param deviceKey The device key to match
     * @return true if the URI likely belongs to this device
     */
    @JvmStatic
    fun uriMatchesDevice(
        uri: Uri,
        deviceKey: String,
    ): Boolean {
        val path = uri.path ?: return false

        // Check for volume UUID in the URI path
        if (StorageDeviceRepresentation.isVolumeKey(deviceKey)) {
            val uuid = VolumeStorageDevice.parseUuidFromKey(deviceKey)
            if (uuid != null && path.contains(uuid, ignoreCase = true)) {
                return true
            }
        }

        // For USB keys, we can't reliably match URIs since they use different identifiers
        // Return false to be safe
        return false
    }
}
