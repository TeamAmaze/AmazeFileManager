package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.XzExtractOperation;

public class XzExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tar.xz";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return XzExtractOperation.class;
    }
}
