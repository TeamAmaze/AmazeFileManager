/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
 * This class replesents a usb device.
 *
 * @see UsbOtgRepresentation.equals
 */
class UsbOtgRepresentation(val productID: Int, val vendorID: Int, val serialNumber: String?) {
    /**
     * This does not ensure a USB OTG device is equal to another! This tests parameters to know to a certain
     * degree of certainty that a device is "similar enough" to another one to be the same one.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is UsbOtgRepresentation) return false
        return productID ==
            other.productID &&
            vendorID == other.vendorID &&
            (
                serialNumber == null && other.serialNumber == null ||
                    serialNumber == other.serialNumber
                )
    }

    /**
     * why? exactly why?
     */
    override fun hashCode() = (37 * (37 * productID + vendorID) + (serialNumber?.hashCode() ?: 0))
}
