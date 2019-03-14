package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import java.io.File;

public class ListEncryptedSevenZipHelperTaskTest extends AbstractCompressedHelperTaskTest {

    protected CompressedHelperTask createTask(String relativePath){
        return new SevenZipHelperTask(new File(Environment.getExternalStorageDirectory(),
                "test-archive-encrypted-list.7z").getAbsolutePath(),
                relativePath, false, (data) -> {});
    }
}
