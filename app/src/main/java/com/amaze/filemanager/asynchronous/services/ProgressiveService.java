package com.amaze.filemanager.asynchronous.services;

import android.app.Service;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.util.ArrayList;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 28/11/2017, at 19:32.
 */

public abstract class ProgressiveService extends Service {
    // list of data packages which contains progress
    private ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();
    private EncryptService.ProgressListener progressListener;

    public final void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public final ProgressListener getProgressListener() {
        return progressListener;
    }

    protected void addFirstDatapoint(String name, int amountOfFiles, long totalBytes, boolean move) {
        if(!dataPackages.isEmpty()) throw new IllegalStateException("This is not the first datapoint!");

        DatapointParcelable intent1 = new DatapointParcelable(name, amountOfFiles, totalBytes, move);
        putDataPackage(intent1);
    }

    protected void addDatapoint(DatapointParcelable datapoint) {
        if(dataPackages.isEmpty()) throw new IllegalStateException("This is the first datapoint!");

        putDataPackage(datapoint);
        if (getProgressListener() != null) {
            getProgressListener().onUpdate(datapoint);
            if (datapoint.completed) getProgressListener().refresh();
        }
    }

    /**
     * Returns the {@link #dataPackages} list which contains
     * data to be transferred to {@link ProcessViewerFragment}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     */
    public final synchronized DatapointParcelable getDataPackage(int index) {
        return this.dataPackages.get(index);
    }

    public final synchronized int getDataPackageSize() {
        return this.dataPackages.size();
    }

    /**
     * Puts a {@link DatapointParcelable} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     */
    private synchronized void putDataPackage(DatapointParcelable dataPackage) {
        this.dataPackages.add(dataPackage);
    }

    public interface ProgressListener {
        void onUpdate(DatapointParcelable dataPackage);
        void refresh();
    }

}
