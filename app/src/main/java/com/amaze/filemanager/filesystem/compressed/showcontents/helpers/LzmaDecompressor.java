/*
 * LzmaDecompressor.java
 *
 * Copyright © 2018 Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */
package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import android.content.Context;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.asynchronous.asynctasks.compress.CompressedHelperTask;
import com.amaze.filemanager.asynchronous.asynctasks.compress.LzmaHelperTask;
import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.util.ArrayList;

public class LzmaDecompressor extends Decompressor {

    public LzmaDecompressor(Context context) {
        super(context);
    }

    @Override
    public CompressedHelperTask changePath(String path, boolean addGoBackItem,
                                           OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>> onFinish) {
        return new LzmaHelperTask(filePath, path, addGoBackItem, onFinish);
    }

}
