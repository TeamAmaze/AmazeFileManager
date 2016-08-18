package com.amaze.filemanager.services;

import java.util.ArrayList;

/**
 * Created by arpitkh96 on 18/8/16.
 */
public class ProgressHandler {
    Long totalSize, doneSize = 0l, currentFileSize, currentFileDoneSize = 0l;
    String fileName;
    Float speed = 0f;
    ProgressListener progressListener;
    float lastavg=0;
    float lastavg_count=0;
    public ProgressHandler(long totalSize) {
        this.totalSize = totalSize;
    }

    void addSize(long length, float speed) {
        doneSize += length;
        currentFileDoneSize += length;
        this.speed = speed;
        calculateProgress();
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public void setCurrentFileSize(Long currentFileSize) {
        this.currentFileSize = currentFileSize;
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
            progressListener.onProgressed(fileName,(float)doneSize*100/totalSize,speed,lastavg);
        }
    }
    interface ProgressListener{
        void onProgressed(String f,float p,float speed,float avg);
    }
}
