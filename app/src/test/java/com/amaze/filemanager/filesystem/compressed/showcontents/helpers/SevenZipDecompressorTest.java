package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

public class SevenZipDecompressorTest extends AbstractDecompressorTest {

    @Override
    protected Class<? extends Decompressor> decompressorClass() {
        return SevenZipDecompressor.class;
    }

    @Override
    protected String getArchiveType() {
        return "7z";
    }
}
