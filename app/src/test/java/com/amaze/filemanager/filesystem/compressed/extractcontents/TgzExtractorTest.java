package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.operations.AbstractExtractOperation;
import com.amaze.filemanager.filesystem.operations.extract.GzipExtractOperation;

public class TgzExtractorTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tgz";
    }

    @Override
    protected Class<? extends AbstractExtractOperation> extractorClass() {
        return GzipExtractOperation.class;
    }
}
