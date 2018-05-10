package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.filesystem.compressed.extractcontents.helpers.XzExtractor;

import org.robolectric.RuntimeEnvironment;

public class TarXzExtractorTest extends AbstractExtractorTest {
    @Override
    protected String getArchiveType() {
        return "tar.xz";
    }

    @Override
    protected Class<? extends Extractor> extractorClass() {
        return XzExtractor.class;
    }
}
