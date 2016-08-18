package com.amaze.filemanager.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by arpitkh996 on 17-08-2016.
 */

public class ReadThread extends Thread{
    BufferHandler bufferHandler;
    InputStream inputStream;
    BufferedInputStream in;
    public ReadThread(BufferHandler bufferHandler, InputStream inputStream) throws NullPointerException {
        this.bufferHandler = bufferHandler;
        this.inputStream = inputStream;
        in = new BufferedInputStream(inputStream);
        if(inputStream==null){
            bufferHandler.setWriting(false);
            throw new NullPointerException("InputStream Null");
        }
    }

    @Override
    public void run() {
        super.run();
        int length;
        byte[] buffer;
        try {
            while (true) {
                if(bufferHandler.canAdd()){
                    if ((length = in.read(buffer = new byte[1024 * 60])) > 0) {
                        bufferHandler.add(buffer, length);
                    } else break;

                }
            }
            in.close();
            inputStream.close();
        } catch (IOException e) {
            bufferHandler.setReading(false);
            e.printStackTrace();
        }
        System.out.println("Reading Done");
        bufferHandler.setReading(false);

    }
}
