package com.amaze.filemanager.database.models;


public class HistoryOperationData implements OperationData{
    private String path;

    public HistoryOperationData(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
