package com.amaze.filemanager.asynchronous.asynctasks.compress;

import android.os.Environment;

import com.amaze.filemanager.filesystem.compressed.ArchivePasswordCache;

import java.io.File;

public class ListEncryptedSevenZipHelperTaskTest extends AbstractCompressedHelperTaskTest {

    protected CompressedHelperTask createTask(String relativePath){
        String filePath = new File(Environment.getExternalStorageDirectory(),
                "test-archive-encrypted-list.7z").getAbsolutePath();
        ArchivePasswordCache.getInstance().put(filePath, "123456");
        return new SevenZipHelperTask(filePath,
                relativePath, false, (data) -> {});
    }
}
