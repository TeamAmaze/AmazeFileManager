package com.amaze.filemanager.utils;

import com.amaze.filemanager.utils.provider.DatabaseContract;

import java.util.HashMap;

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
    OTG,
    GDRIVE,
    DROPBOX,
    BOX,
    ONEDRIVE;

    public static HashMap<OpenMode, String> ACCOUNT_MAP = new HashMap<>();

    static {

        // adding account names wrt their providers
        ACCOUNT_MAP.put(GDRIVE, DatabaseContract.ACCOUNT_TYPE_GOOGLE_DRIVE);
        ACCOUNT_MAP.put(DROPBOX, DatabaseContract.ACCOUNT_TYPE_DROPBOX);
        ACCOUNT_MAP.put(BOX, DatabaseContract.ACCOUNT_TYPE_BOX);
        ACCOUNT_MAP.put(ONEDRIVE, DatabaseContract.ACCOUNT_TYPE_ONE_DRIVE);
    }

    /**
     * Get open mode based on the id assigned.
     * Generally used to retrieve this type after config change or to send enum as argument
     * @param ordinal the position of enum starting from 0 for first element
     * @return
     */
    public static OpenMode getOpenMode(int ordinal) {
        for (OpenMode openMode : OpenMode.values()) {
            if (openMode.ordinal()==ordinal) return openMode;
        }
        return null;
    }
}
