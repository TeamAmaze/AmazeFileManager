package com.amaze.filemanager.utils;

/**
 * Created by vishal on 10/11/16.
 *
 * Class denotes the type of file being handled
 */

public enum OpenMode {

    UNKNOWN,
    FILE,
    SMB,

    /**
     * Custom file types like apk/images/downloads (which don't have a defined path)
     */
    CUSTOM,

    ROOT,
    OTG;

    /**
     * Get open mode based on the id assigned
     * @param ordinal the position of enum
     * @return
     */
    public static OpenMode getOpenMode(int ordinal) {
        return OpenMode.values()[ordinal];
    }
}
