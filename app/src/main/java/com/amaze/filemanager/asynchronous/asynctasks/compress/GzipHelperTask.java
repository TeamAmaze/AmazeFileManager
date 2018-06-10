package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.support.annotation.NonNull;

import com.amaze.filemanager.adapters.data.CompressedObjectParcelable;
import com.amaze.filemanager.filesystem.compressed.CompressedHelper;
import com.amaze.filemanager.utils.OnAsyncTaskFinished;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import static com.amaze.filemanager.filesystem.compressed.CompressedHelper.SEPARATOR;

public class GzipHelperTask extends GzTarHelperTask {

    private String filePath, relativePath;

    public GzipHelperTask(String filePath, String relativePath, boolean goBack,
                         OnAsyncTaskFinished<ArrayList<CompressedObjectParcelable>> l) {
        super(goBack, l);
        this.filePath = filePath;
        this.relativePath = relativePath;
    }

    @NonNull
    @Override
    TarArchiveInputStream getTarArchiveInputStream() throws IOException {
        return new TarArchiveInputStream(
                new GzipCompressorInputStream(new FileInputStream(filePath)));
    }

}
