package com.amaze.filemanager.asynchronous.asynctasks.compress;

import com.amaze.filemanager.filesystem.compressed.extractcontents.TarGzExtractorTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TarGzHelperTaskTest.class,
    ZipHelperTaskTest.class,
    TarHelperTaskTest.class,
    RarHelperTaskTest.class,
    Bzip2HelperTaskTest.class,
    LzmaHelperTaskTest.class,
    XzHelperTaskTest.class,
    XzHelperTaskTest2.class,
    SevenZipHelperTaskTest.class,
    SevenZipHelperTaskTest2.class,
    EncryptedZipHelperTaskTest.class,
    EncryptedSevenZipHelperTaskTest.class,
    ListEncryptedSevenZipHelperTaskTest.class
})
public class CompressedHelperTaskTestSuite {
}
