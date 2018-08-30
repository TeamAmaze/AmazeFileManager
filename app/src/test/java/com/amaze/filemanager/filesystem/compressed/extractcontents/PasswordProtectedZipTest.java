package com.amaze.filemanager.filesystem.compressed.extractcontents;

import android.os.Environment;

import java.io.File;

public class PasswordProtectedZipTest extends ZipExtractorTest {

    @Override
    protected File getArchiveFile() {
        return new File(Environment.getExternalStorageDirectory(), "test-archive-encrypted." + getArchiveType());
    }

    @Override
    protected String getArchivePassword() {
        return "123456";
    }
}
