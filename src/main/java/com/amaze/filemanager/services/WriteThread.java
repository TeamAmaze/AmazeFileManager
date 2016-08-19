package com.amaze.filemanager.services;

import android.util.Log;

import com.amaze.filemanager.utils.NoMoreFilesException;

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
    long length=0;
    long lastwritten;
    long lasttime,elapsedTime;
    ProgressHandler progressHandler;
    String currentId;
    boolean started=false;
    /**
     * Write bytes from bufferhandler into outputstream
     * @param bufferHandler To store the read buffers from memory and write into the file
     * @param progressHandler Handle progress
     */
    public WriteThread(BufferHandler bufferHandler, ProgressHandler progressHandler)  {
        this.bufferHandler = bufferHandler;
        this.progressHandler=progressHandler;
        lasttime=System.currentTimeMillis()/1000;
    }

    @Override
    public void run() {
        super.run();
        started=true;
        try {
            outputStream=bufferHandler.getNextOutputStream(currentId);
            //if stream is null then stop
            if(outputStream==null)return;
            out = new BufferedOutputStream(outputStream);
            currentId=bufferHandler.getCurrentWriteId();
            progressHandler.setFileName(currentId);
        } catch (NoMoreFilesException e) {
            //Stop if no more files
            return;
        }
        //Check if there's still something to write
        while (bufferHandler.isReading()) {
            DataPacket dataPacket = bufferHandler.get();
            // if no datapackets then try again
            if(dataPacket==null){
                continue;
            }
            //If new packet is not for the current file,then get new stream
            if(!dataPacket.getName().equals(currentId)){
                if(outputStream!=null){
                    try {
                        //close old streams
                        if(out!=null)out.close();
                        if(outputStream!=null)outputStream.close();
                        //Get next stream
                        try {
                            outputStream=bufferHandler.getNextOutputStream(currentId);
                            //if stream is null then stop
                            if(outputStream==null)break;
                            out = new BufferedOutputStream(outputStream);
                            currentId=bufferHandler.getCurrentWriteId();
                            progressHandler.setFileName(currentId);
                        } catch (NoMoreFilesException e) {
                            //Stop if no more files
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            byte[] bytes = dataPacket.getBytes();
            int length = dataPacket.getLength();
            if (bytes != null && length > 0)
                try {
                    //write bytes
                    out.write(bytes);
                    this.length+=length;
                    //calculate progress
                    if((elapsedTime=System.currentTimeMillis()/1000-lasttime)>=1){
                        progressHandler.addWrittenLength(this.length - lastwritten, (float) (this.length - lastwritten) / elapsedTime / 1024 / 1024);
                        lasttime=System.currentTimeMillis()/1000;
                        lastwritten=this.length;
                    }
                } catch (IOException e) {
                    bufferHandler.setWriting(false);
                    e.printStackTrace();
                }
        }
        System.out.println("Wrting Done");
        bufferHandler.setWriting(false);
    }
}
