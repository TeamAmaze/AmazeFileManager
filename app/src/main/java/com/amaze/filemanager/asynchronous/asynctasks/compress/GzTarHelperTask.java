package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.support.annotation.NonNull;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

/**
 * Created by JeongHyeon on 2018-06-04.
 */

/* In GzipHelperTask and TarHelperTask do almost same lines in addElements()
    This class is for use template method pattern for only them, not zipHelperTask, RarHelperTask
    if there will be big differences, this change have to be roll-back.
 */

abstract class GzTarHelperTask extends CompressedHelperTask {
    private String filePath, relativePath;

    GzTarHelperTask(boolean goBack, OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @Override
    void addElements(ArrayList<CompressedObjectParcelable> elements) {
        TarArchiveInputStream tarInputStream = null;
        try {
            tarInputStream = getTarArchiveInputStream();

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(SEPARATOR)) name = name.substring(0, name.length() - 1);

                boolean isInBaseDir = relativePath.equals("") && !name.contains(SEPARATOR);
                boolean isInRelativeDir = name.contains(SEPARATOR)
                        && name.substring(0, name.lastIndexOf(SEPARATOR)).equals(relativePath);

                if (isInBaseDir || isInRelativeDir) {
                    elements.add(new CompressedObjectParcelable(entry.getName(),
                            entry.getLastModifiedDate().getTime(), entry.getSize(), entry.isDirectory()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    abstract TarArchiveInputStream getTarArchiveInputStream() throws IOException;
}
