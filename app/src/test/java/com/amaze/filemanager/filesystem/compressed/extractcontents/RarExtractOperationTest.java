package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.extract.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.RarExtractOperation;

public class RarExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "rar";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return RarExtractOperation.class;
    }
}
