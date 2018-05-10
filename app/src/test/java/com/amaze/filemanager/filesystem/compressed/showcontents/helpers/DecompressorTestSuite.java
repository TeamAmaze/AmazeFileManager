package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.extractcontents.Bzip2ExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.LzmaExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.RarExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.SevenZipExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.TarExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.TarGzExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.TarXzExtractorTest;
import com.amaze.filemanager.filesystem.compressed.extractcontents.ZipExtractorTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        BZip2DecompressorTest.class,
        LzmaDecompressorTest.class,
        GzipDecompressorTest.class,
        XzDecompressorTest.class,
        SevenZipDecompressorTest.class,
        ZipDecompressorTest.class,
        TarDecompressorTest.class
})
public class DecompressorTestSuite {
}
