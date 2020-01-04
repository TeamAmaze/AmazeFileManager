package com.amaze.filemanager.filesystem.compressed.extractcontents;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        TarGzExtractorTest.class,
        TgzExtractorTest.class,
        ZipExtractOperationTest.class,
        TarExtractOperationTest.class,
        RarExtractOperationTest.class,
        Bzip2ExtractOperationTest.class,
        Bzip2ExtractorTest2.class,
        LzmaExtractOperationTest.class,
        XzExtractOperationTest.class,
        SevenZipExtractOperationTest.class,
        PasswordProtectedZipTest.class,
        PasswordProtected7ZipTest.class,
        ListPasswordProtected7ZipTest.class
})
public class ExtractorTestSuite {
}
