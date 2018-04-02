package com.amaze.filemanager.database.models;


public class SMPOperationData implements OperationData{
    public final String path;
    public final String name;

    public SMPOperationData(String path, String name) {
        this.path = path;
        this.name = name;
    }
}
