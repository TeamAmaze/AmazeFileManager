package com.amaze.filemanager.filesystem.compressed;

import com.amaze.filemanager.ui.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import java.util.ArrayList;

/**
 * @author Emmanuel
 *         on 20/11/2017, at 17:14.
 */

public interface CompressedInterface {
    void setFilePath(String path);

    /**
     * Separator must be "/"
     * @param path end with "/" if it is a directory, does not if it's a file
     */
    void changePath(String path, boolean addGoBackItem,
                    OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> onFinish);
}
