package com.amaze.filemanager.services;

import android.content.Context;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.NoMoreFilesException;

import java.io.InputStream;
import java.io.OutputStream;
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
    ArrayList<FilePacket> filelist;
    Context context;
    //Do not depend on these much
    String currentReadId,currentWriteId;
    public BufferHandler(Context context) {
        this.context = context;
        filelist=new ArrayList<>();
        bytesArray = new ArrayList<>(MAX_READ);
    }

    /**
     * Store byte array read from readthread
     * @param name Current File Name
     * @param bytes To store the read buffers into {@link BufferHandler#bytesArray} and provide them to write thread
     * @param length length of bytes array
     */
    void add(String name,byte[] bytes, int length) {
        synchronized (bytesArray) {
            bytesArray.add(new DataPacket(name,length, bytes));
        }
    }
    /**
     * Read the first {@link BufferHandler#bytesArray} from bytesArray and simulatenously remove it
     * */
    DataPacket get() {
        synchronized (bytesArray) {
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
        synchronized (bytesArray) {
            if (bytesArray.size() < MAX_READ)
                return true;

            return false;
        }
    }
    /**
     * Tells write thread  whether reading is still in progress or has ended
     * */
    public boolean isReading() {
        synchronized (bytesArray) {
            return reading || bytesArray.size() > 0;
        }
    }
    void addFile(BaseFile sourceFile,HFile targetFile){
        synchronized (filelist){
                filelist.add(new FilePacket(sourceFile,targetFile,sourceFile.getPath()));
        }
    }
    OutputStream getNextOutputStream(String id) throws NoMoreFilesException{
        FilePacket i;
        if(id==null)
            i=filelist.get(0);
        else
            i=filelist.get(filelist.indexOf( new FilePacket(null,null,id)));
        int nextIndex=0;
        if(id!=null) {
            i.setWriteDone(true);
            nextIndex = filelist.indexOf(i) + 1;
        }
        if(nextIndex<filelist.size()) {
            FilePacket filePacket = filelist.get(nextIndex);
            currentWriteId=filePacket.getId();
            return filePacket.target.getOutputStream(context);
        }
        throw new NoMoreFilesException();
    }
    InputStream getNextInputStream(String id) throws NoMoreFilesException{
        FilePacket i;
        if(id==null)
          i=filelist.get(0);
        else
        i=filelist.get(filelist.indexOf( new FilePacket(null,null,id)));
        int nextIndex=0;
        if(id!=null) {
            i.setReadDone(true);
             nextIndex= filelist.indexOf(i) + 1;
        }
        if(nextIndex<filelist.size()) {
            FilePacket filePacket = filelist.get(nextIndex);
            currentReadId=filePacket.getId();
            return filePacket.source.getInputStream();
        }
        throw new NoMoreFilesException();
    }

    public String getCurrentReadId() {
        return currentReadId;
    }

    public String getCurrentWriteId() {
        return currentWriteId;
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
