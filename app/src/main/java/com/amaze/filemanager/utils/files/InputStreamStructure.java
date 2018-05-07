package com.amaze.filemanager.utils.files;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

public class InputStreamStructure {

    public FileInputStream inputStream = null;
    public FileChannel inChannel = null;
    public BufferedInputStream bufferedInputStream = null;

    public void setBufferedInputStream(BufferedInputStream bufferedInputStream) {
        this.bufferedInputStream = bufferedInputStream;
    }

    public BufferedInputStream getBufferedInputStream() {
        return bufferedInputStream;
    }

    public void setInChannel(FileChannel inChannel) {
        this.inChannel = inChannel;
    }

    public FileChannel getInChannel() {
        return inChannel;
    }

    public void setInputStream(FileInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public FileInputStream getInputStream() {
        return inputStream;
    }
}
