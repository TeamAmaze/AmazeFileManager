package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import java.io.File;

public class SevenZipHelperTaskTest extends AbstractCompressedHelperTaskTest {

    protected CompressedHelperTask createTask(String relativePath){
        return new SevenZipHelperTask(new File(Environment.getExternalStorageDirectory(),
                "test-archive." + getArchiveType()).getAbsolutePath(),
                relativePath, false, (data) -> {});
    }

    protected String getArchiveType(){
        return "7z";
    }
}
