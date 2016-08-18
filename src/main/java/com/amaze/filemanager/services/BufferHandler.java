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

    void add(byte[] bytes, int length) {
        synchronized (this) {
            bytesArray.add(new DataPacket(length, bytes));
        }
    }

    DataPacket get() {
        synchronized (this) {
            if (bytesArray.size() > 0)
                return bytesArray.remove(0);
            return null;
        }
    }

    boolean canAdd() {
        synchronized (this) {
            if (bytesArray.size() < MAX_READ)
                return true;

            return false;
        }
    }

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
