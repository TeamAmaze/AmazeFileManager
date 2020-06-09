package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.SevenZipExtractor;

import org.apache.commons.compress.PasswordRequiredException;
import org.tukaani.xz.CorruptedInputException;

public class PasswordProtected7ZipTest extends AbstractExtractorPasswordProtectedArchivesTest {

    @Override
    protected Class<? extends Extractor> extractorClass() {
        return SevenZipExtractor.class;
    }

    @Override
    protected Class[] expectedRootExceptionClass() {
        return new Class[]{PasswordRequiredException.class, CorruptedInputException.class};
    }

    @Override
    protected String getArchiveType() {
        return "7z";
    }
}
