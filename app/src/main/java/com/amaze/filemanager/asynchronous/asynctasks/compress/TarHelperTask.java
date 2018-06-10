package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.support.annotation.NonNull;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 2/12/2017, at 00:40.
 */

public class TarHelperTask extends GzTarHelperTask {

    private String filePath, relativePath;

    public TarHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @NonNull
    @Override
    TarArchiveInputStream getTarArchiveInputStream() throws IOException {
        return new TarArchiveInputStream(new FileInputStream(filePath));
    }

}
