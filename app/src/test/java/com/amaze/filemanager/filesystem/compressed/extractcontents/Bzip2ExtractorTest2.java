package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.extract.Bzip2ExtractOperation;

public class Bzip2ExtractorTest2 extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tbz";
    }

    @Override
    protected Class<? extends AbstractOperationExtract> extractorClass() {
        return Bzip2ExtractOperation.class;
    }
}