package com.amaze.filemanager.filesystem.compressed;

import android.content.Context;

import com.amaze.filemanager.asynchronous.asynctasks.ZipHelperTask;
import com.amaze.filemanager.ui.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.util.ArrayList;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 17:19.
 */

public class ZipHelper implements CompressedInterface {
    private String filePath;
    private Context context;

    public ZipHelper(Context context) {
        this.context = context;
    }

    @Override
    public void setFilePath(String path) {
        filePath = path;
    }

    @Override
    public void changePath(String path, boolean addGoBackItem,
                           OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish) {
        new ZipHelperTask(context, filePath, path, addGoBackItem, onFinish).execute();
    }
}
