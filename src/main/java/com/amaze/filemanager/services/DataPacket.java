package com.amaze.filemanager.services;

/**
 * Created by arpitkh996 on 17-08-2016.
 */

public class DataPacket {
    byte[] bytes;
    int length;

    public DataPacket(int length, byte[] bytes) {
        this.length = length;
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
