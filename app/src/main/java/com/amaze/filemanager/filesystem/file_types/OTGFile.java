package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;


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
}
