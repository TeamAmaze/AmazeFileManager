package com.amaze.filemanager.utils;

/**
 * Created by vishal on 10/11/16.
 *
 * Class denotes the type of file being handled
 */

public enum OpenMode {

    UNKNOWN(-1),
    FILE(0),
    SMB(1),

    /**
     * Custom file types like apk/images/downloads (which don't have a defined path)
     */
    CUSTOM(2),

    DRIVE(3);

    int id;

    OpenMode(int _id) {
        this.id = _id;
    }

    public int getId() {
        return this.id;
    }

    /**
     * Get open mode based on the id assigned
     * @param _id the {@link #id} of OpenMode
     * @return
     */
    public static OpenMode getOpenMode(int _id) {
        for (OpenMode openMode : OpenMode.values()) {
            if (openMode.getId()==_id) return openMode;
        }
        return null;
    }
}
