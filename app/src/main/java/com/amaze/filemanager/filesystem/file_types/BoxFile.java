package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.cloudrail.si.interfaces.CloudStorage;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class BoxFile extends HybridFile {
    private String path;
    final private OpenMode mode = OpenMode.BOX;
    private DataUtils dataUtils = DataUtils.getInstance();

    public BoxFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public BoxFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long length(Context context) {
        return dataUtils.getAccount(mode)
                .getMetadata(CloudUtil.stripPath(mode, path)).getSize();
    }

    @Override
    public boolean isDirectory(Context context) {
        return dataUtils.getAccount(mode)
                .getMetadata(CloudUtil.stripPath(mode, path)).getFolder();
    }

    @Override
    public long folderSize(Context context) {
        return FileUtils.folderSizeCloud(mode,
                dataUtils.getAccount(mode).getMetadata(CloudUtil.stripPath(mode, path)));
    }

    @Override
    public boolean exists() {
        CloudStorage cloudStorageBox = dataUtils.getAccount(mode);
        return cloudStorageBox.exists(CloudUtil.stripPath(mode, path));
    }
}
