package com.amaze.filemanager.filesystem.compressed.showcontents.helpers;

import com.amaze.filemanager.filesystem.compressed.showcontents.Decompressor;

public class BZip2DecompressorTest extends AbstractDecompressorTest {

    @Override
    protected Class<? extends Decompressor> decompressorClass() {
        return Bzip2Decompressor.class;
    }

    @Override
    protected String getArchiveType() {
        return "tar.bz2";
    }
}
