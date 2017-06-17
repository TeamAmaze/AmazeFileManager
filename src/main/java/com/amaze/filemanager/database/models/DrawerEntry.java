package com.amaze.filemanager.database.models;

/**
 * Created by Vishal on 08-06-2017.
 *
 * Model class storing name and path for bookmarks and smb entries in drawer
 */

public class DrawerEntry {

    String drawerName, path;

    public DrawerEntry(String drawerName, String path) {

        setDrawerName(drawerName);
        setPath(path);
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDrawerName() {
        return this.drawerName;
    }

    public String getPath() {
        return this.path;
    }
}
