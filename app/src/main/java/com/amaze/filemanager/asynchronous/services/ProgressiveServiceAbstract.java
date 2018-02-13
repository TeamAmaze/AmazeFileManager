package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.util.ArrayList;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 28/11/2017, at 19:32.
 */

public abstract class ProgressiveServiceAbstract extends Service implements ServiceWatcherUtil.ServiceWatcherInteractionInterface {

    // list of data packages which contains progress
    public NotificationManager mNotifyManager;
    public NotificationCompat.Builder mBuilder;
    public int notificationID;
    public volatile float progressPercent = 0f;
    public ProgressHandler progressHandler;
    public Context context;
    // list of data packages, to initiate chart in process viewer fragment
    public ArrayList<DatapointParcelable> dataPackages;
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
     * Publish the results of the progress to notification and {@link DatapointParcelable}
     * and eventually to {@link ProcessViewerFragment}
     *
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
    public final void publishResults(String fileName, int sourceFiles, int sourceProgress,
                                     long totalSize, long writtenSize, int speed, boolean isComplete,
                                     boolean move) {

        if (!progressHandler.getCancelled()) {

            context = getApplicationContext();

            //notification
            progressPercent = ((float) writtenSize / totalSize) * 100;
            mBuilder.setProgress(100, Math.round(progressPercent), false);
            mBuilder.setOngoing(true);

            int titleResource;

            switch (notificationID) {
                case NotificationConstants.COPY_ID:
                    titleResource = move ? R.string.moving : R.string.copying;
                    break;
                case NotificationConstants.ENCRYPT_ID:
                    titleResource = move ? R.string.crypt_decrypting : R.string.crypt_encrypting;
                    break;
                case NotificationConstants.EXTRACT_ID:
                    titleResource = R.string.extracting;
                    break;
                case NotificationConstants.ZIP_ID:
                    titleResource = R.string.compressing;
                    break;
                case NotificationConstants.DECRYPT_ID:
                    titleResource = R.string.crypt_decrypting;
                    break;
                default:
                    titleResource = R.string.processing;
                    break;
            }

            mBuilder.setContentTitle(context.getResources().getString(titleResource));
            mBuilder.setContentText(fileName + " " + Formatter.formatFileSize(context, writtenSize) + "/" +
                    Formatter.formatFileSize(context, totalSize));
            mNotifyManager.notify(notificationID, mBuilder.build());

            if (writtenSize == totalSize || totalSize == 0) {
                if (move && notificationID == NotificationConstants.COPY_ID) {

                    //mBuilder.setContentTitle(getString(R.string.move_complete));
                    // set progress to indeterminate as deletion might still be going on from source
                    // while moving the file
                    mBuilder.setProgress(0, 0, true);

                    mBuilder.setContentText(context.getResources().getString(R.string.processing));
                    mBuilder.setOngoing(false);
                    mBuilder.setAutoCancel(true);
                    mNotifyManager.notify(notificationID, mBuilder.build());
                } else {
                    publishCompletedResult(notificationID);
                }
            }

            //for processviewer
            DatapointParcelable intent = new DatapointParcelable(fileName, sourceFiles, sourceProgress,
                    totalSize, writtenSize, speed, move, isComplete);
            putDataPackage(intent);
            addDatapoint(intent);
        } else publishCompletedResult(notificationID);
    }

    private void publishCompletedResult(int id1) {
        try {
            mNotifyManager.cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    /**
     * Displays a notification, sends intent and cancels progress if there were some failures
     *
     * @param failedOps
     */
    void generateNotification(ArrayList<HybridFile> failedOps, boolean move) {
        mNotifyManager.cancelAll();

        if(failedOps.size()==0)return;

        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.operationunsuccesful));

        String titleResource;

        switch (notificationID) {
            case NotificationConstants.COPY_ID:
                titleResource = move ? context.getString(R.string.moved) : context.getString(R.string.copied);
                break;
            case NotificationConstants.ENCRYPT_ID:
                titleResource = context.getString(R.string.crypt_encrypted);
                break;
            case NotificationConstants.EXTRACT_ID:
                titleResource = context.getString(R.string.extracted);
                break;
            case NotificationConstants.ZIP_ID:
                titleResource = context.getString(R.string.compressed);
                break;
            case NotificationConstants.DECRYPT_ID:
                titleResource = context.getString(R.string.crypt_decrypted);
                break;
            default:
                titleResource = context.getString(R.string.processed);
                break;
        }

        mBuilder.setContentText(context.getString(R.string.copy_error).replace("%s",
                titleResource.toLowerCase()));
        mBuilder.setAutoCancel(true);

        progressHandler.setCancelled(true);

        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra("move", move);

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);

        mNotifyManager.notify(NotificationConstants.FAILED_ID,mBuilder.build());

        intent=new Intent(MainActivity.TAG_INTENT_FILTER_GENERAL);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);

        sendBroadcast(intent);
    }
}
