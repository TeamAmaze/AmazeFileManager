package com.amaze.filemanager.utils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public abstract class RandomAccessStream extends InputStream {

    private long markedPosition;
    private long length;

    public RandomAccessStream(long length) {
        this.length = length;

        mark(-1);
    }

    @Override
    public synchronized void reset() {
        moveTo(markedPosition);
    }

    @Override
    public synchronized void mark(int readLimit) {
        if(readLimit != -1) {
            throw new IllegalArgumentException("readLimit argument of RandomAccessStream.mark() is not used, please set to -1!");
        }

        markedPosition = getCurrentPosition();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    public long availableExact() {
        return length - getCurrentPosition();
    }

    public long length() {
        return length;
    }

    @Override
    public int available() throws IOException {
        throw new IOException("Use availableExact()!");
    }

    public abstract int read() throws IOException;

    public abstract void moveTo(long position);

    protected abstract long getCurrentPosition();
}
