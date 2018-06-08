package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.files.FileUtils;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class SimpleFile extends HybridFile {
    final private OpenMode mode = OpenMode.FILE;

    private String path;

    public SimpleFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public SimpleFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long lastModified() {
        return new java.io.File(path).lastModified();
    }

    @Override
    public long length() {
        return length(AppConfig.getInstance());
    }

    @Override
    public long length(Context context) {
        return new java.io.File(path).length();
    }

    @Override
    public String getName() {
        return getName(AppConfig.getInstance());
    }

    @Override
    public String getName(Context context) {
        return new java.io.File(path).getName();
    }

    @Override
    public String getParent() {
        return getParent(AppConfig.getInstance());
    }

    @Override
    public String getParent(Context context) {
        return new java.io.File(path).getParent();
    }

    @Override
    public boolean isDirectory() {
        return isDirectory(AppConfig.getInstance());
    }

    @Override
    public boolean isDirectory(Context context) {
        return new java.io.File(path).isDirectory();
    }

    @Override
    public long folderSize() {
        return folderSize(AppConfig.getInstance());
    }

    @Override
    public long folderSize(Context context) {
        return FileUtils.folderSize(new java.io.File(path), null);
    }

    @Override
    public long getUsableSpace() {
        return new java.io.File(path).getUsableSpace();
    }

    @Override
    public long getTotal(Context context) {
        return new java.io.File(path).getTotalSpace();
    }

    @Override
    public boolean exists() {
        return new java.io.File(path).exists();
    }

    @Override
    public LayoutElementParcelable generateLayoutElement(boolean showThumbs) {
        java.io.File file = new java.io.File(path);
        LayoutElementParcelable layoutElement;
        if (isDirectory()) {
            layoutElement = new LayoutElementParcelable(path, RootHelper.parseFilePermission(file),
                    "", folderSize() + "", 0, true,
                    false, file.lastModified() + "", showThumbs);
        } else {
            layoutElement = new LayoutElementParcelable(
                    file.getPath(), RootHelper.parseFilePermission(file),
                    file.getPath(), file.length() + "", file.length(), false,
                    false, file.lastModified() + "", showThumbs);
        }
        layoutElement.setMode(mode);
        return layoutElement;
    }
}
