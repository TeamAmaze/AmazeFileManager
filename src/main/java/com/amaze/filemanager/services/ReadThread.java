package com.amaze.filemanager.services;

import android.util.Log;

import com.amaze.filemanager.utils.NoMoreFilesException;

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
    ProgressHandler progressHandler;
    String currentID;
    boolean started=false;
    /**
     * Read input stream into memory and store into bufferhandler
     * @param bufferHandler To store the read buffers into memory and provide them to write thread
     * @param progressHandler Handle progress
     */
    public ReadThread(BufferHandler bufferHandler,ProgressHandler progressHandler)  {
        this.bufferHandler = bufferHandler;
        this.progressHandler=progressHandler;
    }

    @Override
    public void run() {
        super.run();
        started=true;
        try {
            //get First inputstream
            inputStream=bufferHandler.getNextInputStream(currentID);
            //Stop if null
            if(inputStream==null)
                return;
            in = new BufferedInputStream(inputStream);
            currentID=bufferHandler.getCurrentReadId();

        } catch (NoMoreFilesException e) {
            //Stop if Nofiles
            return;
        }
        int length;
        byte[] buffer;
        try {
            while (true) {
                //Check if buffer has space
                if(bufferHandler.canAdd()){
                    if ((length = in.read(buffer = new byte[1024 * 60])) > 0) {
                        //read file
                        bufferHandler.add(currentID,buffer, length);
                        //update progress
                        progressHandler.addReadLength(length);
                    } else {
                        Log.e("Read","Read done now changing data");
                        //File has ended,proceed to next File
                        try {
                            //Close the old streams
                            in.close();
                            inputStream.close();
                            //Get new streams and id
                            inputStream=bufferHandler.getNextInputStream(currentID);
                            //if inputstream is null then stop
                            if(inputStream==null)
                                break;
                            in = new BufferedInputStream(inputStream);
                            currentID=bufferHandler.getCurrentReadId();
                        } catch (NoMoreFilesException e) {
                            //Stop if nofiles
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            bufferHandler.setReading(false);
            e.printStackTrace();
        }
        bufferHandler.setReading(false);

    }
}
