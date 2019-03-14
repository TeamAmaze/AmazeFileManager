package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.compress.SevenZipHelperTask;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.util.ArrayList;

public class SevenZipDecompressor extends Decompressor {

    public SevenZipDecompressor(@NonNull Context context) {
        super(context);
    }

    @Override
    public SevenZipHelperTask changePath(String path, boolean addGoBackItem,
                                       OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish) {
        return new SevenZipHelperTask(filePath, path, addGoBackItem, onFinish);
    }
}
