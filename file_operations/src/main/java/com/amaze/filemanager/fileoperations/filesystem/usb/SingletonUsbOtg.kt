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

/**
 * USB OTG device representation.
 */
object SingletonUsbOtg {
    var connectedDevice: UsbOtgRepresentation? = null
    var _usbOtgRoot: Uri? = null

    val isDeviceConnected: Boolean
        get() = connectedDevice != null

    /**
     * Get the root of the connected USB OTG device.
     *
     * @return the root URI of the connected USB OTG device, or null if no device is connected.
     */
    fun getUsbOtgRoot(): Uri? {
        return _usbOtgRoot
    }

    /**
     * Set the root of the connected USB OTG device. Will throw exception if no device is connected.
     *
     * @param root the root URI of the connected USB OTG device.
     */
    fun setUsbOtgRoot(root: Uri?) {
        checkNotNull(connectedDevice) { "No device connected!" }
        _usbOtgRoot = root
    }

    /**
     * Clear the reference to connected device and root.
     */
    fun resetUsbOtgRoot() {
        connectedDevice = null
        _usbOtgRoot = null
    }

    /**
     * Check if the root is from the given device.
     *
     * Used by [MainActivity.updateUsbInformation].
     */
    fun checkIfRootIsFromDevice(device: UsbOtgRepresentation): Boolean {
        return _usbOtgRoot != null && connectedDevice.hashCode() == device.hashCode()
    }
}
