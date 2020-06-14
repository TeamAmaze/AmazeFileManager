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

package com.amaze.filemanager.filesystem.usb;

import static android.content.Context.USB_SERVICE;
import static android.hardware.usb.UsbConstants.USB_CLASS_MASS_STORAGE;
import static org.robolectric.Shadows.shadowOf;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowUsbManager;

import android.app.Activity;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

class ReflectionHelpers {

  static void addUsbOtgDevice(Activity activity) {
    ShadowUsbManager sUsbManager = shadowOf((UsbManager) activity.getSystemService(USB_SERVICE));

    UsbDevice device;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          device =
              callUsbDeviceConstructor(
                  "usb", 0, 0, USB_CLASS_MASS_STORAGE, 0, 0, null, null, "v2", null);
        } else {
          device =
              callUsbDeviceConstructor("usb", 0, 0, USB_CLASS_MASS_STORAGE, 0, 0, null, null, null);
        }
        configureUsbDevice(device);
      } else {
        device =
            callUsbDeviceConstructor(
                "usb", 0, 0, USB_CLASS_MASS_STORAGE, 0, 0, configureUsbDevice());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    sUsbManager.addOrUpdateUsbDevice(device, true);

    File storageDir1 = new File("dir");
    ShadowEnvironment.setExternalStorageRemovable(storageDir1, true);
    ShadowEnvironment.setExternalStorageState(storageDir1, Environment.MEDIA_MOUNTED);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  static void configureUsbDevice(UsbDevice device)
      throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException,
          InstantiationException, IllegalAccessException {
    UsbConfiguration usbConfiguration = callUsbConfigurationConstructor(0, "", 0, 0);
    configureUsbConfiguration(usbConfiguration);

    Method configureMethod =
        device.getClass().getDeclaredMethod("setConfigurations", Parcelable[].class);
    configureMethod.invoke(device, (Object) new Parcelable[] {usbConfiguration});
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  static void configureUsbConfiguration(UsbConfiguration usbConfiguration)
      throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException,
          InstantiationException, IllegalAccessException {
    UsbInterface usbInterface =
        callUsbInterfaceConstructor(01, 0, "", USB_CLASS_MASS_STORAGE, 0, 0);

    Method configureMethod =
        usbConfiguration.getClass().getDeclaredMethod("setInterfaces", Parcelable[].class);
    configureMethod.invoke(usbConfiguration, (Object) new Parcelable[] {usbInterface});
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  static Parcelable[] configureUsbDevice()
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
          InstantiationException, IllegalAccessException {
    UsbInterface usbInterface =
        (UsbInterface) callUsbInterfaceConstructor(01, USB_CLASS_MASS_STORAGE, 0, 0, null);

    return new Parcelable[] {usbInterface};
  }

  @RequiresApi(Build.VERSION_CODES.M)
  static UsbDevice callUsbDeviceConstructor(
      @NonNull String name,
      int vendorId,
      int productId,
      int usbClass,
      int subClass,
      int protocol,
      @Nullable String manufacturerName,
      @Nullable String productName,
      @NonNull String version,
      @Nullable String serialNumber)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {

    Class<UsbDevice> clazz = (Class<UsbDevice>) Class.forName("android.hardware.usb.UsbDevice");
    Constructor<UsbDevice> constructor =
        clazz.getConstructor(
            String.class,
            int.class,
            int.class,
            int.class,
            int.class,
            int.class,
            String.class,
            String.class,
            String.class,
            String.class);

    return constructor.newInstance(
        name,
        vendorId,
        productId,
        usbClass,
        subClass,
        protocol,
        manufacturerName,
        productName,
        version,
        serialNumber);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  static UsbDevice callUsbDeviceConstructor(
      @NonNull String name,
      int vendorId,
      int productId,
      int usbClass,
      int subClass,
      int protocol,
      @Nullable String manufacturerName,
      @Nullable String productName,
      @Nullable String serialNumber)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {

    Class<UsbDevice> clazz = (Class<UsbDevice>) Class.forName("android.hardware.usb.UsbDevice");
    Constructor<UsbDevice> constructor =
        clazz.getConstructor(
            String.class,
            int.class,
            int.class,
            int.class,
            int.class,
            int.class,
            String.class,
            String.class,
            String.class);

    return constructor.newInstance(
        name,
        vendorId,
        productId,
        usbClass,
        subClass,
        protocol,
        manufacturerName,
        productName,
        serialNumber);
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  static UsbDevice callUsbDeviceConstructor(
      @NonNull String name,
      int vendorId,
      int productId,
      int usbClass,
      int subClass,
      int protocol,
      @NonNull Parcelable[] interfaces)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {

    Class<UsbDevice> clazz = (Class<UsbDevice>) Class.forName("android.hardware.usb.UsbDevice");
    Constructor<UsbDevice> constructor =
        clazz.getConstructor(
            String.class,
            int.class,
            int.class,
            int.class,
            int.class,
            int.class,
            Parcelable[].class);

    return constructor.newInstance(
        name, vendorId, productId, usbClass, subClass, protocol, interfaces);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  static UsbConfiguration callUsbConfigurationConstructor(
      int id, @Nullable String name, int attributes, int maxPower)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {

    Class<UsbConfiguration> clazz =
        (Class<UsbConfiguration>) Class.forName("android.hardware.usb.UsbConfiguration");
    Constructor<UsbConfiguration> constructor =
        clazz.getConstructor(int.class, String.class, int.class, int.class);

    return constructor.newInstance(id, name, attributes, maxPower);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  static UsbInterface callUsbInterfaceConstructor(
      int id, int alternateSetting, @Nullable String name, int usbClass, int subClass, int protocol)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {

    Class<UsbInterface> clazz =
        (Class<UsbInterface>) Class.forName("android.hardware.usb.UsbInterface");
    Constructor<UsbInterface> constructor =
        clazz.getConstructor(int.class, int.class, String.class, int.class, int.class, int.class);

    return constructor.newInstance(id, alternateSetting, name, usbClass, subClass, protocol);
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  static UsbInterface callUsbInterfaceConstructor(
      int id, int usbClass, int subClass, int protocol, @Nullable Parcelable[] endpoints)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException, InstantiationException {

    Class<UsbInterface> clazz =
        (Class<UsbInterface>) Class.forName("android.hardware.usb.UsbInterface");
    Constructor<UsbInterface> constructor =
        clazz.getConstructor(int.class, int.class, int.class, int.class, Parcelable[].class);

    return constructor.newInstance(id, usbClass, subClass, protocol, endpoints);
  }
}
