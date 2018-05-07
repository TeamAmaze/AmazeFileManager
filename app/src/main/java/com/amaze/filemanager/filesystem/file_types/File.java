package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OpenMode;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class File extends HybridFile {
    final private OpenMode mode = OpenMode.FILE;

    private String path;

    public File(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public File(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long lastModified() {
        return new java.io.File(path).lastModified();
    }

    @Override
    public long length() {
        return getLength();
    }

    @Override
    public long length(Context context) {
        return getLength();
    }

    private long getLength() {
        return new java.io.File(path).length();
    }

    @Override
    public String getName() {
        return new java.io.File(path).getName();
    }

    @Override
    public String getName(Context context) {
        return new java.io.File(path).getName();
    }

    @Override
    public boolean exists() {
        return new java.io.File(path).exists();
    }
}
