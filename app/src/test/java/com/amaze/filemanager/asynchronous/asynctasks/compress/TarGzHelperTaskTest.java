package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import org.robolectric.RuntimeEnvironment;

import java.io.File;

public class TarGzHelperTaskTest extends AbstractCompressedHelperTaskTest {

    @Override
    protected CompressedHelperTask createTask(String relativePath) {
        return new GzipHelperTask(RuntimeEnvironment.application,
                new File(Environment.getExternalStorageDirectory(),"test-archive.tar.gz").getAbsolutePath(),
                relativePath, false, (data) -> {});
    }

}
