package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.RarExtractor;
import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.SevenZipExtractor;

public class SevenZipExtractorTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "7z";
    }

    @Override
    protected Class<? extends Extractor> extractorClass() {
        return SevenZipExtractor.class;
    }
}
