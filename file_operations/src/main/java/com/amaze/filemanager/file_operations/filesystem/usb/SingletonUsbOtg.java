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

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SingletonUsbOtg {
  private static SingletonUsbOtg instance = null;

  public static SingletonUsbOtg getInstance() {
    if (instance == null) instance = new SingletonUsbOtg();
    return instance;
  }

  private UsbOtgRepresentation connectedDevice = null;
  private @Nullable Uri usbOtgRoot;

  private SingletonUsbOtg() {}

  public void setConnectedDevice(UsbOtgRepresentation connectedDevice) {
    this.connectedDevice = connectedDevice;
  }

  public boolean isDeviceConnected() {
    return connectedDevice != null;
  }

  public void setUsbOtgRoot(@Nullable Uri root) {
    if (connectedDevice == null) throw new IllegalStateException("No device connected!");
    usbOtgRoot = root;
  }

  public void resetUsbOtgRoot() {
    connectedDevice = null;
    usbOtgRoot = null;
  }

  public @Nullable Uri getUsbOtgRoot() {
    return usbOtgRoot;
  }

  public boolean checkIfRootIsFromDevice(@NonNull UsbOtgRepresentation device) {
    return usbOtgRoot != null && connectedDevice.hashCode() == device.hashCode();
  }
}
