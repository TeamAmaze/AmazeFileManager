package com.amaze.filemanager.database.models;


import com.amaze.filemanager.database.UtilsHandler;

public class PathOperationData implements OperationData {
    private String path;
    UtilsHandler.Operation operationType;


    public PathOperationData(String path, UtilsHandler.Operation operationType) {
        this.path = path;
        this.operationType = operationType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public UtilsHandler.Operation getOperationType() {
        return operationType;
    }

    public void setOperationType(UtilsHandler.Operation operationType) {
        this.operationType = operationType;
    }
}
