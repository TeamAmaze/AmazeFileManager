/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for multiple USB OTG devices.
 * Replaces SingletonUsbOtg to support multiple connected devices simultaneously.
 */
object UsbOtgManager {
    /** Map of device key to UsbOtgRepresentation */
    private val connectedDevices: MutableMap<String, UsbOtgRepresentation> = ConcurrentHashMap()

    /** Map of device key to SAF root URI */
    private val deviceRoots: MutableMap<String, Uri> = ConcurrentHashMap()

    /**
     * Add a device to the manager.
     *
     * @param device the USB device representation
     */
    @JvmStatic
    fun addDevice(device: UsbOtgRepresentation) {
        connectedDevices[device.deviceKey] = device
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
     * @param device the USB device representation
     */
    @JvmStatic
    fun removeDevice(device: UsbOtgRepresentation) {
        removeDevice(device.deviceKey)
    }

    /**
     * Get a device by its key.
     *
     * @param deviceKey the unique device key
     * @return the device or null if not found
     */
    @JvmStatic
    fun getDevice(deviceKey: String): UsbOtgRepresentation? {
        return connectedDevices[deviceKey]
    }

    /**
     * Get all connected devices.
     *
     * @return unmodifiable collection of all connected devices
     */
    @JvmStatic
    fun getDevices(): Collection<UsbOtgRepresentation> {
        return connectedDevices.values.toList()
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
     * Get any connected device.
     * Used for backward compatibility when single device was expected.
     *
     * @return the first connected device or null if none
     */
    @JvmStatic
    val anyDevice: UsbOtgRepresentation?
        get() = connectedDevices.values.firstOrNull()

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
     *
     * @param detectedDevices list of currently detected devices
     */
    @JvmStatic
    fun updateDevices(detectedDevices: Collection<UsbOtgRepresentation>) {
        // Build set of detected device keys
        val detectedMap = detectedDevices.associateBy { it.deviceKey }

        // Remove devices that are no longer present
        val keysToRemove = connectedDevices.keys.filter { it !in detectedMap }
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
}
