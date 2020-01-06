package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.SevenZipExtractOperation;

public class SevenZipExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "7z";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return SevenZipExtractOperation.class;
    }
}
