package com.amaze.filemanager.filesystem.compressed.showcontents;

import android.content.Context;
import android.content.Intent;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.compress.CompressedHelperTask;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.util.ArrayList;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 17:14.
 */

public abstract class Decompressor {

    protected Context context;
    protected String filePath;

    public Decompressor(Context context) {
        this.context = context;
    }

    public void setFilePath(String path) {
        filePath = path;
    }

    /**
     * Separator must be "/"
     * @param path end with "/" if it is a directory, does not if it's a file
     */
    public abstract CompressedHelperTask changePath(String path, boolean addGoBackItem,
                                                    OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish);

    /**
     * Decompress a file somewhere
     */
    public final void decompress(String whereToDecompress) {
        Intent intent = new Intent(context, ExtractService.class);
        intent.putExtra(ExtractService.KEY_PATH_ZIP, filePath);
        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, new String[0]);
        intent.putExtra(ExtractService.KEY_PATH_EXTRACT, whereToDecompress);
        ServiceWatcherUtil.runService(context, intent);
    }

    /**
     * Decompress files or dirs inside the compressed file.
     * @param subDirectories separator is "/", ended with "/" if it is a directory, does not if it's a file
     */
    public final void decompress(String whereToDecompress, String[] subDirectories) {
        for (int i = 0; i < subDirectories.length; i++) {
            subDirectories[i] = realRelativeDirectory(subDirectories[i]);
        }

        Intent intent = new Intent(context, ExtractService.class);
        intent.putExtra(ExtractService.KEY_PATH_ZIP, filePath);
        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, subDirectories);
        intent.putExtra(ExtractService.KEY_PATH_EXTRACT, whereToDecompress);
        ServiceWatcherUtil.runService(context, intent);
    }

    /**
     * Get the real relative directory path (useful if you converted the separator or something)
     */
    protected String realRelativeDirectory(String dir) {
        return dir;
    }

}
