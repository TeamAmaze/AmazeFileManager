package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

public class XzDecompressorTest extends AbstractDecompressorTest {

    @Override
    protected Class<? extends Decompressor> decompressorClass() {
        return XzDecompressor.class;
    }

    @Override
    protected String getArchiveType() {
        return "tar.xz";
    }
}
