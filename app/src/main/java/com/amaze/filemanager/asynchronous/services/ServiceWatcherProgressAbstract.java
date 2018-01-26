package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.utils.CopyDataParcelable;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.util.ArrayList;

/**
 * Created by Vishal on 12-01-2018.
 *
 * Abstract class which handles pause/resumes of notification progress of services
 * as returned by the {@link ServiceWatcherUtil}
 */

public abstract class ServiceWatcherProgressAbstract extends Service implements com.amaze.filemanager.utils.ServiceWatcherUtil.ServiceWatcherInteractionInterface {

    public NotificationManager mNotifyManager;
    public NotificationCompat.Builder mBuilder;
    public int notificationID;
    public volatile float progressPercent = 0f;
    public ProgressHandler progressHandler;
    public Context context;
    // list of data packages, to initiate chart in process viewer fragment
    public ArrayList<CopyDataParcelable> dataPackages;
    public ProgressListener progressListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initVariables();
        return super.onStartCommand(intent, flags, startId);
    }

    public abstract void initVariables();

    @Override
    public void progressHalted() {

        // set notification to indeterminate unless progress resumes
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(notificationID, mBuilder.build());
    }

    @Override
    public void progressResumed() {

        // set notification to indeterminate unless progress resumes
        mBuilder.setProgress(100, Math.round(progressPercent), false);
        mNotifyManager.notify(notificationID, mBuilder.build());
    }

    /**
     * Publish the results of the progress to notification and {@link CopyDataParcelable}
     * and eventually to {@link ProcessViewerFragment}
     *
     * @param id             id of current service
     * @param fileName       file name of current file being copied
     * @param sourceFiles    total number of files selected by user for copy
     * @param sourceProgress files been copied out of them
     * @param totalSize      total size of selected items to copy
     * @param writtenSize    bytes successfully copied
     * @param speed          number of bytes being copied per sec
     * @param isComplete     whether operation completed or ongoing (not supported at the moment)
     * @param move           if the files are to be moved
     *                       In case of encryption, this is true for decrypting operation
     */
    public final void publishResults(int id, String fileName, int sourceFiles, int sourceProgress,
                                long totalSize, long writtenSize, int speed, boolean isComplete,
                                boolean move) {

        if (!progressHandler.getCancelled()) {

            context = getApplicationContext();

            //notification
            progressPercent = ((float) writtenSize / totalSize) * 100;
            mBuilder.setProgress(100, Math.round(progressPercent), false);
            mBuilder.setOngoing(true);

            int titleResource;

            switch (id) {
                case CopyService.NOTIFICATION_ID:
                    titleResource = move ? R.string.moving : R.string.copying;
                    break;
                case EncryptService.ID_NOTIFICATION:
                    titleResource = move ? R.string.crypt_decrypting : R.string.crypt_encrypting;
                    break;
                case ExtractService.ID_NOTIFICATION:
                    titleResource = R.string.extracting;
                    break;
                case ZipService.ID_NOTIFICATION:
                    titleResource = R.string.compressing;
                    break;
                default:
                    titleResource = R.string.processing;
                    break;
            }

            mBuilder.setContentTitle(context.getResources().getString(titleResource));
            mBuilder.setContentText(fileName + " " + Formatter.formatFileSize(context, writtenSize) + "/" +
                    Formatter.formatFileSize(context, totalSize));
            mNotifyManager.notify(id, mBuilder.build());

            if (writtenSize == totalSize || totalSize == 0) {
                if (move && id == CopyService.NOTIFICATION_ID) {

                    //mBuilder.setContentTitle(getString(R.string.move_complete));
                    // set progress to indeterminate as deletion might still be going on from source
                    mBuilder.setProgress(0, 0, true);

                    mBuilder.setContentText(context.getResources().getString(R.string.processing));
                    mBuilder.setOngoing(false);
                    mBuilder.setAutoCancel(true);
                    mNotifyManager.notify(id, mBuilder.build());
                } else {
                    publishCompletedResult(id);
                }
            }

            //for processviewer
            CopyDataParcelable intent = new CopyDataParcelable(fileName, sourceFiles, sourceProgress,
                    totalSize, writtenSize, speed, move, isComplete);
            putDataPackage(intent);
            if (progressListener != null) {
                progressListener.onUpdate(intent);
                if (isComplete) progressListener.refresh();
            }
        } else publishCompletedResult(id);
    }

    private void publishCompletedResult(int id1) {
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ProgressListener {

        void onUpdate(CopyDataParcelable dataPackage);

        void refresh();
    }

    public void setProgressListener(ServiceWatcherProgressAbstract.ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Returns the {@link #dataPackages} list which contains
     * data to be transferred to {@link ProcessViewerFragment}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link com.amaze.filemanager.utils.ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     *
     * @return
     */
    public synchronized CopyDataParcelable getDataPackage(int index) {
        return this.dataPackages.get(index);
    }

    public synchronized int getDataPackageSize() {
        return this.dataPackages.size();
    }

    /**
     * Puts a {@link CopyDataParcelable} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link com.amaze.filemanager.utils.ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     *
     * @param dataPackage
     */
    public synchronized void putDataPackage(CopyDataParcelable dataPackage) {
        this.dataPackages.add(dataPackage);
    }
}
