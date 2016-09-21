package com.amaze.filemanager.services;

import java.util.ArrayList;

/**
 * Created by arpitkh96 on 18/8/16.
 */
public class ProgressHandler {
    Long totalSize, writtenSize = 0l,readSize=0l;
    String fileName;
    Float speed = 0f;
    ProgressListener progressListener;
    float lastavg=0;
    float lastavg_count=0;
    public ProgressHandler(long totalSize) {
        this.totalSize = totalSize;
    }

    void addWrittenLength(long length, float speed) {
        writtenSize += length;
        this.speed = speed;
        calculateProgress();
    }

    void addReadLength(int length){
        readSize+=length;
    }
    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    void calculateProgress(){
        if(progressListener!=null && totalSize!=-1){
            lastavg_count++;
            lastavg=(lastavg*(lastavg_count-1)+speed)/lastavg_count;
            progressListener.onProgressed(fileName,(float)writtenSize*100/totalSize,(float)readSize*100/totalSize,speed,lastavg);
        }
    }
    interface ProgressListener{
        /**
         * @param fileName File currently being copied
         * @param p1 Primary progress
         * @param p2 Secondary progress
         * @param speed Write Speed
         * @param avg Avg Write progress
         */
        void onProgressed(String fileName,float p1,float p2,float speed,float avg);
    }
}
