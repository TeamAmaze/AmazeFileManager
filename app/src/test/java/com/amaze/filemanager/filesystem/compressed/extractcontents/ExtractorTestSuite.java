package com.amaze.filemanager.filesystem.compressed.extractcontents;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Bzip2ExtractorTest.class,
        LzmaExtractorTest.class,
        TarGzExtractorTest.class,
        TarXzExtractorTest.class,
        SevenZipExtractorTest.class,
        ZipExtractorTest.class,
        TarExtractorTest.class,
        RarExtractorTest.class
})
public class ExtractorTestSuite {
}
