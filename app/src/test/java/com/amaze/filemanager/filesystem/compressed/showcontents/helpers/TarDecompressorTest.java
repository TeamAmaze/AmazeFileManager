package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

public class TarDecompressorTest extends AbstractDecompressorTest {

    @Override
    protected Class<? extends Decompressor> decompressorClass() {
        return TarDecompressor.class;
    }

    @Override
    protected String getArchiveType() {
        return "tar";
    }
}
