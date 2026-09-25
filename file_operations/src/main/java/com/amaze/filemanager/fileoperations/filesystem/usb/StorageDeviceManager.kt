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
import android.content.Intent
import android.os.Build

/**
 * Unified facade for storage device management.
 * Automatically selects the appropriate strategy based on API level:
 * - API 21-23: UsbManagerStrategy (requires USB permissions)
 * - API 24+: VolumeManagerStrategy (uses StorageManager, no USB permissions needed)
 *
 * Usage:
 * ```kotlin
 * // Get connected devices
 * val devices = StorageDeviceManager.getRemovableDevices(context)
 *
 * // Create SAF permission intent
 * val intent = StorageDeviceManager.createSafIntent(context, device)
 * startActivityForResult(intent, REQUEST_CODE_SAF)
 *
 * // Listen for device changes
 * StorageDeviceManager.registerChangeCallback(context) { devices ->
 *     // Handle device list update
 * }
 * ```
 */
object StorageDeviceManager {
    private val strategy: StorageDeviceStrategy by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            VolumeManagerStrategy()
        } else {
            UsbManagerStrategy()
        }
    }

    /**
     * Get list of currently connected removable storage devices.
     *
     * @param context Android context
     * @return List of detected removable storage devices
     */
    @JvmStatic
    fun getRemovableDevices(context: Context): List<StorageDeviceRepresentation> {
        return strategy.getRemovableDevices(context)
    }

    /**
     * Create an Intent to request SAF (Storage Access Framework) permission for a device.
     *
     * On API 29+, the intent will be pre-configured to open the specific volume.
     * On older APIs, the user must manually navigate to the storage location.
     *
     * @param context Android context
     * @param device The device to request access for
     * @return Intent to launch SAF permission request
     */
    @JvmStatic
    fun createSafIntent(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Intent {
        return strategy.createSafIntent(context, device)
    }

    /**
     * Create an Intent to request SAF permission for a device by its key.
     *
     * @param context Android context
     * @param deviceKey The device key
     * @return Intent to launch SAF permission request, or generic SAF intent if device not found
     */
    @JvmStatic
    fun createSafIntent(
        context: Context,
        deviceKey: String,
    ): Intent {
        val device = getRemovableDevices(context).find { it.deviceKey == deviceKey }
        return if (device != null) {
            strategy.createSafIntent(context, device)
        } else {
            // Fallback to generic SAF intent
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
            }
        }
    }

    /**
     * Register a callback for device change events.
     *
     * @param context Android context (should be Application or Activity context)
     * @param listener Callback to invoke when devices change
     */
    @JvmStatic
    fun registerChangeCallback(
        context: Context,
        listener: OnDeviceChangeListener,
    ) {
        strategy.registerChangeCallback(context, listener)
    }

    /**
     * Unregister the device change callback.
     *
     * @param context Android context used during registration
     */
    @JvmStatic
    fun unregisterChangeCallback(context: Context) {
        strategy.unregisterChangeCallback(context)
    }

    /**
     * Check if this manager requires explicit USB permission requests.
     * Returns true on API 21-23, false on API 24+.
     */
    @JvmStatic
    fun requiresUsbPermission(): Boolean {
        return strategy.requiresUsbPermission
    }

    /**
     * Request USB permission for a device (only applicable on API 21-23).
     * On API 24+, this immediately invokes the callback with granted=true.
     *
     * @param context Android context
     * @param device The device to request permission for
     * @param callback Called with (device, granted) when permission result is available
     */
    @JvmStatic
    fun requestUsbPermission(
        context: Context,
        device: StorageDeviceRepresentation,
        callback: ((StorageDeviceRepresentation, Boolean) -> Unit)? = null,
    ) {
        strategy.requestUsbPermission(context, device, callback)
    }

    /**
     * Check if USB permission is granted for a device (only applicable on API 21-23).
     * On API 24+, always returns true.
     *
     * @param context Android context
     * @param device The device to check
     * @return true if permission is granted or not required
     */
    @JvmStatic
    fun hasUsbPermission(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Boolean {
        return strategy.hasUsbPermission(context, device)
    }

    /**
     * Find a device by its key.
     *
     * @param context Android context
     * @param deviceKey The device key to search for
     * @return The device, or null if not found
     */
    @JvmStatic
    fun findDeviceByKey(
        context: Context,
        deviceKey: String,
    ): StorageDeviceRepresentation? {
        return getRemovableDevices(context).find { it.deviceKey == deviceKey }
    }

    /**
     * Check if any removable devices are connected.
     *
     * @param context Android context
     * @return true if at least one removable device is connected
     */
    @JvmStatic
    fun hasRemovableDevices(context: Context): Boolean {
        return getRemovableDevices(context).isNotEmpty()
    }
}
