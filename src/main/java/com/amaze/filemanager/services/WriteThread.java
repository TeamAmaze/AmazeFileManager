package com.amaze.filemanager.services;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Created by arpitkh996 on 17-08-2016.
 */

public class WriteThread extends Thread {
    BufferHandler bufferHandler;
    OutputStream outputStream;
    BufferedOutputStream out;
    float length=0;
    float lastwritten;
    long lasttime;
    /**
     * Write bytes from bufferhandler into outputstream
     * @param bufferHandler To store the read buffers from memory and write into the file
     * @param outputStream stream to write into
     */
    public WriteThread(BufferHandler bufferHandler, OutputStream outputStream) throws NullPointerException {
        this.bufferHandler = bufferHandler;
        this.outputStream = outputStream;
        out = new BufferedOutputStream(outputStream);
        if (outputStream == null) {
            bufferHandler.setWriting(false);
            throw new NullPointerException("OutputStream Null");
        }
        lasttime=System.currentTimeMillis()/1000;
    }

    @Override
    public void run() {
        super.run();
        while (bufferHandler.isReading()) {
            DataPacket dataPacket = bufferHandler.get();
            if(dataPacket==null){
                continue;
            }
            byte[] bytes = dataPacket.getBytes();
            int length = dataPacket.getLength();
            if (bytes != null && length > 0)
                try {
                    out.write(bytes);
                    this.length+=length;
                    if(System.currentTimeMillis()/1000-lasttime>=1){
                        System.out.println("Written "+(this.length/1024/1024)+" Speed "+((this.length-lastwritten))/1024/1024);
                        lasttime=System.currentTimeMillis()/1000;
                        lastwritten=this.length;
                    }
                } catch (IOException e) {
                    bufferHandler.setWriting(false);
                    e.printStackTrace();
                }
        }
        try {
            out.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Wrting Done");
        bufferHandler.setWriting(false);
    }
}
