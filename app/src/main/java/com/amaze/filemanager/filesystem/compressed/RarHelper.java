package com.amaze.filemanager.filesystem.compressed;

import com.amaze.filemanager.asynchronous.asynctasks.RarHelperTask;
import com.amaze.filemanager.ui.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.util.ArrayList;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 17:23.
 */

public class RarHelper implements CompressedInterface {
    private String filePath;

    public RarHelper() {
    }

    @Override
    public void setFilePath(String path) {
        filePath = path;
    }

    @Override
    public void changePath(String path, boolean addGoBackItem,
                           OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish) {
        new RarHelperTask(filePath, path, addGoBackItem, onFinish).execute();
    }
}
