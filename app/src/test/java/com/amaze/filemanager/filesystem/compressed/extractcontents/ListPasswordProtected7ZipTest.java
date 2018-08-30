package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.os.Environment;

import java.io.File;

public class ListPasswordProtected7ZipTest extends PasswordProtected7ZipTest {
    @Override
    protected File getArchiveFile() {
        return new File(Environment.getExternalStorageDirectory(), "test-archive-encrypted-list." + getArchiveType());
    }
}
