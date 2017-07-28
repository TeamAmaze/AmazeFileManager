package com.amaze.filemanager.utils;

/**
 * Created by arpitkh96 on 18/8/16.
 *
 * Base class to handle progress of services operation
 * Utilized for generation of notification,
 * talking to {@link com.amaze.filemanager.fragments.ProcessViewer} through
 * {@link com.amaze.filemanager.utils.DataPackage}
 *
 */
public class ProgressHandler {

    // total number of bytes to be processed
    long totalSize = 0L;

    // total bytes written in process so far
    long writtenSize = 0L;

    // total number of source files to be processed
    int sourceFiles = 0;

    // number of source files processed so far
    int sourceFilesProcessed = 0;

    // file name currently being processed
    String fileName;

    // current processing speed (bytes processed in 1000ms time)
    int speedRaw = 0;

    // boolean manages the lifecycle of service and whether it should be canceled
    private boolean isCancelled = false;

    // callback interface to interact with process viewer fragment and notification
    ProgressListener progressListener;

    /**
     * Constructor to start an instance
     * @param sourceFiles the total number of source files selected by the user for operation
     */
    public ProgressHandler(int sourceFiles, long totalSize) {
        this.sourceFiles = sourceFiles;
        this.totalSize = totalSize;
    }

    /**
     * publish progress after calculating the write length
     *
     * @param newPosition the position of byte for file being processed
     */
    public synchronized void addWrittenLength(long newPosition) {

        this.speedRaw = (int) (newPosition - writtenSize);
        this.writtenSize = newPosition;

        progressListener.onProgressed(fileName, sourceFiles, sourceFilesProcessed,
                totalSize, writtenSize, speedRaw);
    }

    public synchronized void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public synchronized String getFileName() {
        return this.fileName;
    }

    public synchronized void setSourceFilesProcessed(int sourceFilesProcessed) {
        this.sourceFilesProcessed = sourceFilesProcessed;
    }

    // dynamically setting total size, useful in case files are compressed
    public synchronized void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public synchronized void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public synchronized boolean getCancelled() {
        return this.isCancelled;
    }

    public synchronized long getWrittenSize() {
        return writtenSize;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * An interface responsible for talking to this object
     * Utilized by relevant service and eventually for notification generation
     * and process viewer fragment
     */
    public interface ProgressListener {
        /**
         * @param fileName File name currently being processed (can be recursive, irrespective of selections)
         * @param sourceFiles how many total number of files did the user selected
         * @param sourceProgress which file is being processed from total number of files
         * @param totalSize total size of source files
         * @param writtenSize where are we at from total number of bytes
         * @param speed raw write speed in bytes
         */
        void onProgressed(String fileName, int sourceFiles, int sourceProgress, long totalSize,
                          long writtenSize, int speed);
    }
}
