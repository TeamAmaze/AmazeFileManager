package com.amaze.filemanager.filesystem.file_types;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.OpenMode;

import java.util.ArrayList;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class RootFile extends HybridFile {
    private String path;
    public RootFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public RootFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long length() {
        HybridFileParcelable baseFile = generateBaseFileFromParent();
        if (baseFile != null) return baseFile.getSize();
        return super.length();
    }

    HybridFileParcelable generateBaseFileFromParent() {
        ArrayList<HybridFileParcelable> arrayList = RootHelper.getFilesList(getFile().getParent(), true, true, null);
        for (HybridFileParcelable baseFile : arrayList) {
            if (baseFile.getPath().equals(path))
                return baseFile;
        }
        return null;
    }
}
