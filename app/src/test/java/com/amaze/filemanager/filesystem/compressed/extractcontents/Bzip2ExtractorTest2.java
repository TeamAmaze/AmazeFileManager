package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.Bzip2Extractor;

public class Bzip2ExtractorTest2 extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tbz";
    }

    @Override
    protected Class<? extends Extractor> extractorClass() {
        return Bzip2Extractor.class;
    }
}