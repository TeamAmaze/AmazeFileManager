package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.extract.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.SevenZipExtractOperation;

import org.apache.commons.compress.PasswordRequiredException;
import org.tukaani.xz.CorruptedInputException;

public class PasswordProtected7ZipTest extends AbstractExtractorPasswordProtectedArchivesTest {

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return SevenZipExtractOperation.class;
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
