package com.amaze.filemanager.filesystem.compressed.helpers;

import android.content.Context;
import android.content.Intent;

import com.amaze.filemanager.asynchronous.asynctasks.compress.ZipHelperTask;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedInterface;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

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

    @Override
    public void decompress(String whereToDecompress) {
        Intent intent = new Intent(context, ExtractService.class);
        intent.putExtra(ExtractService.KEY_PATH_ZIP, filePath);
        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, new String[0]);
        intent.putExtra(ExtractService.KEY_PATH_EXTRACT, whereToDecompress);
        ServiceWatcherUtil.runService(context, intent);
    }

    @Override
    public void decompress(String whereToDecompress, String[] subDirectories) {
        Intent intent = new Intent(context, ExtractService.class);
        intent.putExtra(ExtractService.KEY_PATH_ZIP, filePath);
        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, subDirectories);
        intent.putExtra(ExtractService.KEY_PATH_EXTRACT, whereToDecompress);
        ServiceWatcherUtil.runService(context, intent);
    }

}
