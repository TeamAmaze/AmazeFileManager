package com.amaze.filemanager.database.models;


public class SMPOperationData implements OperationData{
    private String path;
    private String name;

    public SMPOperationData(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }


}
