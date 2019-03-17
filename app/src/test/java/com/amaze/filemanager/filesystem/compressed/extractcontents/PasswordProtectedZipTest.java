package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.ZipExtractor;

import net.lingala.zip4j.exception.ZipException;

public class PasswordProtectedZipTest extends AbstractExtractorPasswordProtectedArchivesTest {

    @Override
    protected Class<? extends Extractor> extractorClass() {
        return ZipExtractor.class;
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
