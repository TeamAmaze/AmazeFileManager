package com.amaze.filemanager.database;

/**
 * Created by vishal on 8/4/17.
 */

public class EncryptedEntry {

    private int _id;
    private String path, password;

    public EncryptedEntry() {}

    public EncryptedEntry(int _id, String path, String password) {
        this._id = _id;
        this.path = path;
        this.password = password;
    }

    public void setId(int _id) {
        this._id = _id;
    }

    public int getId() {
        return this._id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }
}
