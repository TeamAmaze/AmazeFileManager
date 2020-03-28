package com.amaze.filemanager.filesystem;

import android.content.Context;

import com.amaze.filemanager.R;

import java.io.File;

public final class StorageNaming {

    /**
     * Retrofit of {@link android.os.storage.StorageVolume#getDescription(Context)} to older apis
     */
    public static String getDeviceDescriptionLegacy(Context context, File file) {
        String path = file.getPath();

        switch (path) {
            case "/storage/emulated/legacy":
            case "/storage/emulated/0":
            case "/mnt/sdcard":
                return context.getString(R.string.storage_internal);
            case "/storage/sdcard":
            case "/storage/sdcard1":
                return context.getString(R.string.storage_sd_card);
            case "/":
                return context.getString(R.string.root_directory);
            default:
                return file.getName();
        }
    }
}
