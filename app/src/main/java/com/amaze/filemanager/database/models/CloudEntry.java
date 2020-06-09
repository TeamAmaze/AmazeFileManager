package com.amaze.filemanager.database.models;

import com.amaze.filemanager.utils.OpenMode;

/**
 * Created by vishal on 18/4/17.
 */

public class CloudEntry {

    private int _id;
    private OpenMode serviceType;
    private String persistData;

    public CloudEntry() {}

    public CloudEntry(OpenMode serviceType, String persistData) {
        this.serviceType = serviceType;
        this.persistData = persistData;
    }

    public void setId(int _id) {
        this._id = _id;
    }

    public int getId() {
        return this._id;
    }

    public void setPersistData(String persistData) {
        this.persistData = persistData;
    }

    public String getPersistData() {
        return this.persistData;
    }

    /**
     * Set the service type
     * Support values from {@link com.amaze.filemanager.utils.OpenMode}
     */
    public void setServiceType(OpenMode openMode) {
        this.serviceType = openMode;
    }

    /**
     * Returns ordinal value of service from {@link com.amaze.filemanager.utils.OpenMode}
     */
    public OpenMode getServiceType() {
        return this.serviceType;
    }
}
