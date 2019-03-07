package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import java.io.File;

public class RarHelperTaskTest extends AbstractCompressedHelperTaskTest {

    @Override
    protected CompressedHelperTask createTask(String relativePath) {
        return new RarHelperTask(new File(Environment.getExternalStorageDirectory(),
                "test-archive.rar").getAbsolutePath(),
                relativePath, false, (data) -> {});
    }
}
