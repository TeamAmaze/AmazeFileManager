package com.amaze.filemanager.filesystem;

import android.net.Uri;
import android.support.annotation.Nullable;

public class SingletonUsbOtg {
    private static SingletonUsbOtg instance = null;

    public static SingletonUsbOtg getInstance() {
        if(instance == null) instance = new SingletonUsbOtg();
        return instance;
    }

    private Uri usbOtgRoot = null;
    /**
     * Indicates whether last app exit was for setting {@link #usbOtgRoot} or not
     */
    private boolean hasRootBeenRequested = false;

    private SingletonUsbOtg() { }

    public void setUsbOtgRoot(@Nullable Uri root) {
        usbOtgRoot = root;
    }

    public @Nullable Uri getUsbOtgRoot() {
        return usbOtgRoot;
    }

    public void setHasRootBeenRequested(boolean hasRootBeenRequested) {
        this.hasRootBeenRequested = hasRootBeenRequested;
    }

    public boolean hasRootBeenRequested() {
        return hasRootBeenRequested;
    }

}
