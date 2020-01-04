package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.extract.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.ZipExtractOperation;

import net.lingala.zip4j.exception.ZipException;

public class PasswordProtectedZipTest extends AbstractExtractorPasswordProtectedArchivesTest {

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return ZipExtractOperation.class;
    }

    @Override
    protected Class[] expectedRootExceptionClass() {
        return new Class[]{ZipException.class};
    }

    @Override
    protected String getArchiveType() {
        return "zip";
    }
}
