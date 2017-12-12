package com.amaze.filemanager.filesystem.compressed.helpers;

import android.content.Context;
import android.content.Intent;

import com.amaze.filemanager.asynchronous.asynctasks.compress.RarHelperTask;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedInterface;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.github.junrar.rarfile.FileHeader;

import java.util.ArrayList;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 17:23.
 */

public class RarHelper implements CompressedInterface {
    private Context context;
    private String filePath;

    public RarHelper(Context context) {
        this.context = context;
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
        for (int i = 0; i < subDirectories.length; i++) {
            subDirectories[i] = deconvertName(subDirectories[i]);
        }

        Intent intent = new Intent(context, ExtractService.class);
        intent.putExtra(ExtractService.KEY_PATH_ZIP, filePath);
        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, subDirectories);
        intent.putExtra(ExtractService.KEY_PATH_EXTRACT, whereToDecompress);
        ServiceWatcherUtil.runService(context, intent);
    }


    public static String convertName(FileHeader file) {
        String name = file.getFileNameString().replace('\\', '/');

        if(file.isDirectory()) return name + "/";
        else return name;
    }

    public static String deconvertName(String dir) {
        if(dir.endsWith("/")) dir = dir.substring(0, dir.length()-1);
        return dir.replace('/', '\\');
    }

}
