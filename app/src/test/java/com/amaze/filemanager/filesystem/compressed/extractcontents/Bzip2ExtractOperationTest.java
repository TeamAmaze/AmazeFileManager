package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.extract.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.Bzip2ExtractOperation;

public class Bzip2ExtractOperationTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tar.bz2";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return Bzip2ExtractOperation.class;
    }
}