package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import org.robolectric.RuntimeEnvironment;

import java.io.File;

public class EncryptedZipHelperTaskTest extends AbstractCompressedHelperTaskTest {

    protected CompressedHelperTask createTask(String relativePath){
        return new ZipHelperTask(RuntimeEnvironment.application, new File(Environment.getExternalStorageDirectory(),
                "test-archive-encrypted.zip").getAbsolutePath(),
                relativePath, false, (data) -> {});
    }
}
