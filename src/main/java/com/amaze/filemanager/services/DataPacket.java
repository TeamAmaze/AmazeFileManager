package com.amaze.filemanager.services;

/**
 * Created by arpitkh996 on 17-08-2016.
 */
/**
 * Stores data bytes to be written along with length of bytes
 */
public class DataPacket {
    byte[] bytes;
    int length;
    String name;
    public DataPacket(String name,int length, byte[] bytes) {
        this.length = length;
        this.bytes = bytes;
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
