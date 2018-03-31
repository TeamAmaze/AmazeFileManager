package com.amaze.filemanager.database.models;


public class ListOperationData implements OperationData{
    private String path;

    public ListOperationData(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
