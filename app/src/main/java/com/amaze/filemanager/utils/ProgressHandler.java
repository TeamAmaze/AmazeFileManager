package com.amaze.filemanager.utils;

import com.amaze.filemanager.fragments.ProcessViewerFragment;

/**
 * Created by arpitkh96 on 18/8/16.
 *
 * Base class to handle progress of services operation
 * Utilized for generation of notification,
 * talking to {@link ProcessViewerFragment} through
 * {@link DatapointParcelable}
 *
 */
public class ProgressHandler {

    /**
     * total number of bytes to be processed
     * Volatile because non volatile long r/w are not atomic (see Java Language Specification 17.7)
     */
    private volatile long totalSize = 0L;

    /**
     * total bytes written in process so far
     * Volatile because non volatile long r/w are not atomic (see Java Language Specification 17.7)
     */
    private volatile long writtenSize = 0L;
    /**
     * total number of source files to be processed
     */
    private volatile int sourceFiles = 0;

    /**
     * number of source files processed so far
     */
    private volatile int sourceFilesProcessed = 0;
    /**
     * file name currently being processed
     */
    private volatile String fileName;

    /**
     * boolean manages the lifecycle of service and whether it should be canceled
     */
    private volatile boolean isCancelled = false;

    /**
     * callback interface to interact with process viewer fragment and notification
     */
    private volatile ProgressListener progressListener;

    /**
     * Constructor to start an instance
     * @param sourceFiles the total number of source files selected by the user for operation
     */
    public ProgressHandler(int sourceFiles, long totalSize) {
        this.sourceFiles = sourceFiles;
        this.totalSize = totalSize;
    }

    /**
     * Constructor to start an instance when we don't know of total files or size
     */
    public ProgressHandler() {

    }

    /**
     * publish progress after calculating the write length
     *
     * @param newPosition the position of byte for file being processed
     */
    public synchronized void addWrittenLength(long newPosition) {
        long speedRaw = (newPosition - writtenSize);
        this.writtenSize = newPosition;

        progressListener.onProgressed(speedRaw);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setSourceFilesProcessed(int sourceFilesProcessed) {
        this.sourceFilesProcessed = sourceFilesProcessed;
    }

    public int getSourceFilesProcessed() {
        return sourceFilesProcessed;
    }

    public void setSourceSize(int sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public int getSourceSize() {
        return sourceFiles;
    }

    // dynamically setting total size, useful in case files are compressed
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public boolean getCancelled() {
        return isCancelled;
    }

    public long getWrittenSize() {
        return writtenSize;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public synchronized float getPercentProgress() {
        if(totalSize == 0) return 0f;//Sometimes the total size is 0, because of metadata not being measured
        return ((float) writtenSize / totalSize) * 100;
    }

    /**
     * An interface responsible for talking to this object
     * Utilized by relevant service and eventually for notification generation
     * and process viewer fragment
     */
    public interface ProgressListener {
        /**
         * @param speed raw write speed in bytes
         */
        void onProgressed(long speed);
    }
}
