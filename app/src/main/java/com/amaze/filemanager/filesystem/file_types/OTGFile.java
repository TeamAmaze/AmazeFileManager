package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.files.FileUtils;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class OTGFile extends HybridFile {
    private String path;

    public OTGFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public OTGFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long length(Context context) {
        return OTGUtil.getDocumentFile(path, context, false).length();
    }

    @Override
    public String getName(Context context) {
        return OTGUtil.getDocumentFile(path, context, false).getName();
    }

    @Override
    public boolean isDirectory() {
        // TODO: support for this method in OTG on-the-fly
        // you need to manually call {@link RootHelper#getDocumentFile() method
        return false;
    }

    @Override
    public boolean isDirectory(Context context) {
        return OTGUtil.getDocumentFile(path, context, false).isDirectory();
    }

    @Override
    public long folderSize(Context context) {
        return FileUtils.otgFolderSize(path, context);
    }

    @Override
    public boolean exists(Context context) {
        DocumentFile fileToCheck = OTGUtil.getDocumentFile(path, context, false);
        return fileToCheck != null;
    }
}
