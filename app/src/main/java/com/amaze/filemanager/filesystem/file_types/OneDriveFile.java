package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.FileUtils;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.SpaceAllocation;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class OneDriveFile extends HybridFile {
    private String path;
    final private OpenMode mode = OpenMode.ONEDRIVE;
    private DataUtils dataUtils = DataUtils.getInstance();

    public OneDriveFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public OneDriveFile(OpenMode mode, String path, String name, boolean isDirectory) {
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
    public long getUsableSpace() {
        SpaceAllocation spaceAllocation = dataUtils.getAccount(mode).getAllocation();
        return spaceAllocation.getTotal() - spaceAllocation.getUsed();
    }

    @Override
    public long getTotal(Context context) {
        return dataUtils.getAccount(mode).getAllocation().getTotal();
    }

    @Override
    public boolean exists() {
        CloudStorage cloudStorageOneDrive = dataUtils.getAccount(mode);
        return cloudStorageOneDrive.exists(CloudUtil.stripPath(mode, path));
    }
}
