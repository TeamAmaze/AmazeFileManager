package com.amaze.filemanager.filesystem.compressed.helpers;

import android.content.Context;

import com.amaze.filemanager.asynchronous.asynctasks.compress.RarHelperTask;
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.Decompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;
import com.github.junrar.rarfile.FileHeader;

import java.util.ArrayList;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 17:23.
 */

public class RarDecompressor extends Decompressor {

    public RarDecompressor(Context context) {
        super(context);
    }

    @Override
    public RarHelperTask changePath(String path, boolean addGoBackItem,
                                       OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish) {
        return new RarHelperTask(filePath, path, addGoBackItem, onFinish);
    }

    public static String convertName(FileHeader file) {
        String name = file.getFileNameString().replace('\\', '/');

        if(file.isDirectory()) return name + "/";
        else return name;
    }

    @Override
    protected String realRelativeDirectory(String dir) {
        if(dir.endsWith("/")) dir = dir.substring(0, dir.length()-1);
        return dir.replace('/', '\\');
    }

}
