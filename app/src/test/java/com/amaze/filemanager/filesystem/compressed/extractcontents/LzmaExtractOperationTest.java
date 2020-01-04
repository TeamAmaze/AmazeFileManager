package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.extract.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.LzmaExtractOperation;

public class LzmaExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tar.lzma";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return LzmaExtractOperation.class;
    }
}
