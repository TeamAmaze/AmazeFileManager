package com.amaze.filemanager.filesystem.compressed.extractcontents;

import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.GenericCopyUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by JeongHyeon on 2018-05-07.
 */

abstract class BufferWriter {
    public static void writeBuffer(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {
        try {
            int length;
            byte buffer[] = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
                ServiceWatcherUtil.position += length;
            }
        } finally {
            closer(inputStream,outputStream);
        }
    }

    protected static void closer(InputStream inputStream, BufferedOutputStream outputStream) throws IOException {} ;


}
