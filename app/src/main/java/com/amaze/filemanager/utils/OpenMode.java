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
    SFTP,

    /**
     * Custom file types like apk/images/downloads (which don't have a defined path)
     */
    CUSTOM,

    ROOT,
    OTG,
    GDRIVE,
    DROPBOX,
    BOX,
    ONEDRIVE;

    /**
     * Get open mode based on the id assigned.
     * Generally used to retrieve this type after config change or to send enum as argument
     * @param ordinal the position of enum starting from 0 for first element
     */
    public static OpenMode getOpenMode(int ordinal) {
        return OpenMode.values()[ordinal];
    }
}
