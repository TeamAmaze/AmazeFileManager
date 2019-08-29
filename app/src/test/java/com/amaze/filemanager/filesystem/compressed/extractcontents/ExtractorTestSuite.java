package com.amaze.filemanager.filesystem.compressed.extractcontents;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TarGzExtractorTest.class,
        TgzExtractorTest.class,
        ZipExtractorTest.class,
        TarExtractorTest.class,
        RarExtractorTest.class,
        Bzip2ExtractorTest.class,
        Bzip2ExtractorTest2.class,
        LzmaExtractorTest.class,
        XzExtractorTest.class,
        SevenZipExtractorTest.class,
        PasswordProtectedZipTest.class,
        PasswordProtected7ZipTest.class,
        ListPasswordProtected7ZipTest.class
})
public class ExtractorTestSuite {
}
