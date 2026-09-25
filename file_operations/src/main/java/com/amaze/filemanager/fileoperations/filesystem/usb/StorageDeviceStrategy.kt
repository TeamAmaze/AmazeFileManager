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

/**
 * Listener interface for storage device change events.
 */
fun interface OnDeviceChangeListener {
    /**
     * Called when storage devices have changed (attached or detached).
     * @param devices Current list of connected removable storage devices
     */
    fun onDevicesChanged(devices: List<StorageDeviceRepresentation>)
}

/**
 * Internal strategy interface for storage device detection and management.
 * Different implementations handle different Android API levels.
 */
internal interface StorageDeviceStrategy {
    /**
     * Get list of currently connected removable storage devices.
     * @param context Android context
     * @return List of detected removable storage devices
     */
    fun getRemovableDevices(context: Context): List<StorageDeviceRepresentation>

    /**
     * Create an Intent to request SAF (Storage Access Framework) permission for a device.
     * @param context Android context
     * @param device The device to request access for
     * @return Intent to launch SAF permission request
     */
    fun createSafIntent(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Intent

    /**
     * Register a callback for device change events.
     * @param context Android context (should be Application or Activity context)
     * @param listener Callback to invoke when devices change
     */
    fun registerChangeCallback(
        context: Context,
        listener: OnDeviceChangeListener,
    )

    /**
     * Unregister the device change callback.
     * @param context Android context used during registration
     */
    fun unregisterChangeCallback(context: Context)

    /**
     * Check if this strategy requires explicit USB permission requests.
     * USB-based strategy (API 21-23) requires permission, Volume-based (API 24+) does not.
     */
    val requiresUsbPermission: Boolean

    /**
     * Request USB permission for a device (only applicable for USB strategy).
     * @param context Android context
     * @param device The device to request permission for
     * @param callback Called with (device, granted) when permission result is available
     */
    fun requestUsbPermission(
        context: Context,
        device: StorageDeviceRepresentation,
        callback: ((StorageDeviceRepresentation, Boolean) -> Unit)? = null,
    ) {
        // Default implementation does nothing - only UsbManagerStrategy overrides
        callback?.invoke(device, true)
    }

    /**
     * Check if USB permission is granted for a device (only applicable for USB strategy).
     * @param context Android context
     * @param device The device to check
     * @return true if permission is granted or not required
     */
    fun hasUsbPermission(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Boolean = true
}
