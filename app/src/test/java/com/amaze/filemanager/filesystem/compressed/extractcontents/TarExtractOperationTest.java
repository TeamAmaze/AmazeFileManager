package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.TarExtractOperation;

public class TarExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tar";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return TarExtractOperation.class;
    }
}
