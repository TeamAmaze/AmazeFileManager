package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.cloud.CloudUtil;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class DropboxFile extends HybridFile {
    private String path;
    final private OpenMode mode = OpenMode.DROPBOX;
    private DataUtils dataUtils = DataUtils.getInstance();

    public DropboxFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public DropboxFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long length(Context context) {
        return dataUtils.getAccount(OpenMode.DROPBOX)
                .getMetadata(CloudUtil.stripPath(OpenMode.DROPBOX, path)).getSize();
    }
}
