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

/**
 * Sealed interface representing a removable storage device.
 * Provides a unified abstraction over USB devices (API 21-23) and StorageVolumes (API 24+).
 */
sealed interface StorageDeviceRepresentation {
    /**
     * Unique key identifying this device.
     * Format: "usb:vendorId:productId[:serial]" for USB devices,
     *         "vol:uuid" for StorageVolume devices.
     */
    val deviceKey: String

    /**
     * User-friendly display name for this device.
     */
    val displayName: String

    /**
     * Filesystem path for direct file access (if available).
     * This is typically available on API 24+ with MANAGE_EXTERNAL_STORAGE permission.
     * Returns null for USB devices on API 21-23 or when direct access is not available.
     */
    val filePath: String?
        get() = null

    companion object {
        /** Prefix for USB-based device keys (API 21-23) */
        const val PREFIX_USB = "usb:"

        /** Prefix for Volume-based device keys (API 24+) */
        const val PREFIX_VOLUME = "vol:"

        /**
         * Check if a device key uses the legacy format (without prefix).
         * Legacy format: "vendorId:productId[:serial]"
         */
        @JvmStatic
        fun isLegacyKey(key: String): Boolean {
            return !key.startsWith(PREFIX_USB) && !key.startsWith(PREFIX_VOLUME)
        }

        /**
         * Check if a device key is USB-based.
         */
        @JvmStatic
        fun isUsbKey(key: String): Boolean {
            return key.startsWith(PREFIX_USB)
        }

        /**
         * Check if a device key is Volume-based.
         */
        @JvmStatic
        fun isVolumeKey(key: String): Boolean {
            return key.startsWith(PREFIX_VOLUME)
        }
    }
}

/**
 * Represents a USB storage device detected via UsbManager (API 21-23).
 *
 * @property vendorId USB vendor ID
 * @property productId USB product ID
 * @property serialNumber Optional device serial number
 * @property manufacturerName Optional manufacturer name
 * @property productName Optional product name
 */
data class UsbStorageDevice(
    val vendorId: Int,
    val productId: Int,
    val serialNumber: String? = null,
    val manufacturerName: String? = null,
    val productName: String? = null,
) : StorageDeviceRepresentation {
    override val deviceKey: String
        get() {
            val base = "${StorageDeviceRepresentation.PREFIX_USB}$vendorId:$productId"
            return if (!serialNumber.isNullOrEmpty()) {
                "$base:$serialNumber"
            } else {
                base
            }
        }

    override val displayName: String
        get() {
            val nameParts =
                buildList {
                    manufacturerName?.takeIf { it.isNotBlank() }?.let { add(it) }
                    productName?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
            return if (nameParts.isNotEmpty()) {
                nameParts.joinToString(" ")
            } else {
                "USB Device (${String.format("%04X:%04X", vendorId, productId)})"
            }
        }

    companion object {
        /**
         * Creates a UsbStorageDevice from a legacy UsbOtgRepresentation.
         */
        @JvmStatic
        fun fromLegacy(legacy: UsbOtgRepresentation): UsbStorageDevice {
            return UsbStorageDevice(
                vendorId = legacy.vendorID,
                productId = legacy.productID,
                serialNumber = legacy.serialNumber,
                manufacturerName = legacy.manufacturerName,
                productName = legacy.productName,
            )
        }

        /**
         * Extract vendor and product IDs from a legacy or USB device key.
         * @return Pair of (vendorId, productId) or null if parsing fails
         */
        @JvmStatic
        fun parseIdsFromKey(key: String): Pair<Int, Int>? {
            val keyWithoutPrefix =
                if (key.startsWith(StorageDeviceRepresentation.PREFIX_USB)) {
                    key.removePrefix(StorageDeviceRepresentation.PREFIX_USB)
                } else {
                    key // Legacy format without prefix
                }
            val parts = keyWithoutPrefix.split(":")
            if (parts.size < 2) return null
            val vendorId = parts[0].toIntOrNull() ?: return null
            val productId = parts[1].toIntOrNull() ?: return null
            return vendorId to productId
        }
    }
}

/**
 * Represents a storage volume detected via StorageManager (API 24+).
 *
 * @property uuid Volume UUID (unique identifier)
 * @property description Volume description from system
 * @property path Optional mount path (may be null on some API levels)
 */
data class VolumeStorageDevice(
    val uuid: String,
    val description: String,
    val path: String? = null,
) : StorageDeviceRepresentation {
    override val deviceKey: String
        get() = "${StorageDeviceRepresentation.PREFIX_VOLUME}$uuid"

    override val displayName: String
        get() = description.ifBlank { "Removable Storage ($uuid)" }

    override val filePath: String?
        get() = path

    companion object {
        /**
         * Extract UUID from a volume device key.
         * @return UUID or null if not a volume key
         */
        @JvmStatic
        fun parseUuidFromKey(key: String): String? {
            return if (key.startsWith(StorageDeviceRepresentation.PREFIX_VOLUME)) {
                key.removePrefix(StorageDeviceRepresentation.PREFIX_VOLUME)
            } else {
                null
            }
        }
    }
}
