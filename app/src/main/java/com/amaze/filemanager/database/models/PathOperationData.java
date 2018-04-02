package com.amaze.filemanager.database.models;


public class PathOperationData implements OperationData {
    private String path;
    private int operationType;


    public PathOperationData(String path, int operationType) {
        this.path = path;
        this.operationType = operationType;
    }

    public String getPath() {
        return path;
    }

    public int getOperationType() {
        return operationType;
    }

}
