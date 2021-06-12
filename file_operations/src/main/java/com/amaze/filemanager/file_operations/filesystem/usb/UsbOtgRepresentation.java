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

package com.amaze.filemanager.file_operations.filesystem.usb;

import androidx.annotation.Nullable;

/**
 * This class replesents a usb device.
 *
 * @see UsbOtgRepresentation#equals(Object)
 */
public class UsbOtgRepresentation {

  public final int productID, vendorID;
  public final @Nullable String serialNumber;

  public UsbOtgRepresentation(int productID, int vendorID, @Nullable String serialNumber) {
    this.productID = productID;
    this.vendorID = vendorID;
    this.serialNumber = serialNumber;
  }

  /**
   * This does not ensure a device is equal to another! This tests parameters to know to a certain
   * degree of certanty that a device is "similar enough" to another one to be the same one.
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof UsbOtgRepresentation)) return false;

    UsbOtgRepresentation other = (UsbOtgRepresentation) obj;
    return productID == other.productID
        && vendorID == other.vendorID
        && ((serialNumber == null && other.serialNumber == null)
            || serialNumber.equals(other.serialNumber));
  }

  @Override
  public int hashCode() {
    int result = productID;
    result = 37 * result + vendorID;
    result = 37 * result + (serialNumber != null ? serialNumber.hashCode() : 0);
    return result;
  }
}
