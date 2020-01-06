package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.ZipExtractOperation;

public class ZipExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "zip";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return ZipExtractOperation.class;
    }
}
