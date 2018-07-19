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

        progressListener.onProgressed(fileName, sourceFiles, sourceFilesProcessed,
                totalSize, writtenSize, speedRaw);
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

    public void setSourceSize(int sourceFiles) {
        this.sourceFiles = sourceFiles;
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
                          long writtenSize, long speed);
    }
}
