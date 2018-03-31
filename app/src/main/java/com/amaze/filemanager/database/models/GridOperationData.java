package com.amaze.filemanager.database.models;


public class GridOperationData implements OperationData {
    private String path;

    public GridOperationData(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
