package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

public class ZipDecompressorTest extends AbstractDecompressorTest {

    @Override
    protected Class<? extends Decompressor> decompressorClass() {
        return ZipDecompressor.class;
    }

    @Override
    protected String getArchiveType() {
        return "zip";
    }
}
