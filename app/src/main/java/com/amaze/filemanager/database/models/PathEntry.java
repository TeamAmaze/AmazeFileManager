package com.amaze.filemanager.database.models;

/**
 * Created by Vishal on 08-06-2017.
 *
 * Model class for storing path for list, grid, history and hidden objects
 */

public class PathEntry {

    private String path;

    public PathEntry(String path) {
        this.path = path;
    }

    public PathEntry() {}

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
