package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

public class LzmaDecompressorTest extends AbstractDecompressorTest {

    @Override
    protected Class<? extends Decompressor> decompressorClass() {
        return LzmaDecompressor.class;
    }

    @Override
    protected String getArchiveType() {
        return "tar.lzma";
    }
}
