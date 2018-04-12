package com.amaze.filemanager.filesystem;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class UsbOtgSingleton {
    private static UsbOtgSingleton instance = null;

    public static UsbOtgSingleton getInstance() {
        if(instance == null) instance = new UsbOtgSingleton();
        return instance;
    }

    private String usbOtgRoot = null;

    private UsbOtgSingleton() { }

    public void setUsbOtgRoot(@Nullable String root) {
        usbOtgRoot = root;
    }

    public @Nullable String getUsbOtgRoot() {
        return usbOtgRoot;
    }

}
