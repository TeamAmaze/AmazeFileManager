package com.amaze.filemanager.services;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * Created by arpitkh996 on 17-08-2016.
 */

public class BufferHandler {
    ArrayList<DataPacket> bytesArray;
    public final int MAX_READ = 15;
    boolean reading = true, writing = true;

    public BufferHandler() {
        bytesArray = new ArrayList<>(MAX_READ);
    }
    /**
     * Store byte array read from readthread
     * @param bytes To store the read buffers into {@link BufferHandler#bytesArray} and provide them to write thread
     * @param length length of bytes array
     */
    void add(byte[] bytes, int length) {
        synchronized (this) {
            bytesArray.add(new DataPacket(length, bytes));
        }
    }
    /**
     * Read the first {@link BufferHandler#bytesArray} from bytesArray and simulatenously remove it
     * */
    DataPacket get() {
        synchronized (this) {
            if (bytesArray.size() > 0)
                return bytesArray.remove(0);
            return null;
        }
    }
    /**
     * It tells whether read thread should continue reading or whether {@link BufferHandler#bytesArray} is full and
     * it should wait for {@link BufferHandler#bytesArray} to have some space
     * @return boolean
     */
    boolean canAdd() {
        synchronized (this) {
            if (bytesArray.size() < MAX_READ)
                return true;

            return false;
        }
    }
    /**
     * Tells write thread  whether reading is still in progress or has ended
     * */
    public boolean isReading() {
        synchronized (this) {
            return reading || bytesArray.size() > 0;
        }
    }

    public void setReading(boolean reading) {
        this.reading = reading;
    }

    public boolean isWriting() {
        return writing;
    }

    public void setWriting(boolean writing) {
        this.writing = writing;
    }

}
