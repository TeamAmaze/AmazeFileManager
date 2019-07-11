package com.amaze.filemanager.filesystem.compressed.extractcontents;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TarGzExtractorTest.class,
        TgzExtractorTest.class,
        ZipExtractorTest.class,
        TarExtractorTest.class,
        RarExtractorTest.class
})
public class ExtractorTestSuite {
}
