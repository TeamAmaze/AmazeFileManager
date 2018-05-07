package com.amaze.filemanager.utils.files;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class OutputStreamStructure {

    public FileOutputStream outputStream = null;
    public FileChannel outChannel = null;
    public BufferedOutputStream bufferedOutputStream = null;

    public void setBufferedOutputStream(BufferedOutputStream bufferedOutputStream) {
        this.bufferedOutputStream = bufferedOutputStream;
    }

    public BufferedOutputStream getBufferedOutputStream() {
        return bufferedOutputStream;
    }

    public void setOutChannel(FileChannel outChannel) {
        this.outChannel = outChannel;
    }

    public FileChannel getOutChannel() {
        return outChannel;
    }

    public void setOutputStream(FileOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public FileOutputStream getOutputStream() {
        return outputStream;
    }
}
