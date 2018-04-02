package com.amaze.filemanager.database.models;


public class PathOperationData implements OperationData {
    public final String path;
    public final int operationType;

    public PathOperationData(String path, int operationType) {
        this.path = path;
        this.operationType = operationType;
    }
}
