package com.amaze.filemanager.database.models;


public class BookmarksOperationData implements OperationData{
    public final String path;
    public final String name;

    public BookmarksOperationData(String path, String name) {
        this.path = path;
        this.name = name;
    }

}
