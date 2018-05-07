package com.amaze.filemanager.filesystem.compressed.extractcontents;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * Created by JeongHyeon on 2018-05-07.
 */

public class OutputCloseBufferWriter extends BufferWriter {
    protected void closer(BufferedInputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        outputStream.close();
    }
}
