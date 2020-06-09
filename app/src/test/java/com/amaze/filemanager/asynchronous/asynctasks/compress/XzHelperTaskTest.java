package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import java.io.File;

public class XzHelperTaskTest extends AbstractCompressedHelperTaskTest {

    @Override
    protected CompressedHelperTask createTask(String relativePath) {
        return new XzHelperTask(new File(Environment.getExternalStorageDirectory(),
                "test-archive.tar.xz").getAbsolutePath(),
                relativePath, false, (data) -> {});
    }
}
