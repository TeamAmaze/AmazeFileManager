package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.fragments.ProcessViewerFragment;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.ui.notifications.ProcessingNotificationBuilder;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;

import java.util.ArrayList;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 28/11/2017, at 19:32.
 */

public abstract class AbstractProgressiveService extends Service implements ServiceWatcherUtil.ServiceWatcherInteractionInterface {

    private Context context;

    private boolean isNotificationTitleSet = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    protected abstract NotificationManager getNotificationManager();

    protected abstract NotificationCompat.Builder getNotificationBuilder();

    protected abstract int getNotificationId();

    protected abstract float getPercentProgress();

    protected abstract void setPercentProgress(float progress);

    protected abstract RemoteViews getNotificationCustomViewSmall();

    protected abstract RemoteViews getNotificationCustomViewBig();

    protected abstract ProcessingNotificationBuilder getProcessingNotificationBuilder();

    public abstract ProgressListener getProgressListener();

    public abstract void setProgressListener(ProgressListener progressListener);

    /**
     * @return list of data packages, to initiate chart in process viewer fragment
     */
    protected abstract ArrayList<DatapointParcelable> getDataPackages();

    protected abstract ProgressHandler getProgressHandler();

    @Override
    public void progressHalted() {
        // set notification to indeterminate unless progress resumes
        getNotificationManager().notify(getNotificationId(), getProcessingNotificationBuilder().build());
    }

    @Override
    public void progressResumed() {
        // set notification to indeterminate unless progress resumes
        getNotificationCustomViewSmall().setProgressBar(R.id.notification_service_progressBar_small,
                100, Math.round(getPercentProgress()), false);
        getNotificationCustomViewBig().setProgressBar(R.id.notification_service_progressBar_big,
                100, Math.round(getPercentProgress()), false);
        getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
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
                                     long totalSize, long writtenSize, long speed, boolean isComplete,
                                     boolean move) {
        if (!getProgressHandler().getCancelled()) {

            context = getApplicationContext();

            //notification
            setPercentProgress(((float) writtenSize / totalSize) * 100);

            if (!isNotificationTitleSet) {
                int titleResource;

                switch (getNotificationId()) {
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

                getNotificationBuilder().setSubText(context.getResources().getString(titleResource));
                isNotificationTitleSet = true;
            }

            if (ServiceWatcherUtil.state != ServiceWatcherUtil.ServiceWatcherInteractionInterface.STATE_HALTED) {

                String written = Formatter.formatFileSize(context, writtenSize) + "/" +
                        Formatter.formatFileSize(context, totalSize);
                getNotificationCustomViewBig().setTextViewText(R.id.notification_service_textView_filename_big, fileName);
                getNotificationCustomViewSmall().setTextViewText(R.id.notification_service_textView_filename_small, fileName);
                getNotificationCustomViewBig().setTextViewText(R.id.notification_service_textView_written_big, written);
                getNotificationCustomViewSmall().setTextViewText(R.id.notification_service_textView_written_small, written);
                getNotificationCustomViewBig().setTextViewText(R.id.notification_service_textView_transferRate_big,
                        Formatter.formatFileSize(context, speed) + "/s");

                String remainingTime;
                if (speed != 0) {
                    remainingTime = Utils.formatTimer(Math.round((totalSize-writtenSize)/speed));
                } else {
                    remainingTime = getResources().getString(R.string.unknown);
                }
                getNotificationCustomViewBig().setTextViewText(R.id.notification_service_textView_timeRemaining_big, remainingTime);
                getNotificationCustomViewSmall().setProgressBar(R.id.notification_service_progressBar_small,
                        100, Math.round(getPercentProgress()), false);
                getNotificationCustomViewBig().setProgressBar(R.id.notification_service_progressBar_big,
                        100, Math.round(getPercentProgress()), false);
                getNotificationManager().notify(getNotificationId(), getNotificationBuilder().build());
            }

            if (writtenSize == totalSize || totalSize == 0) {
                if (move && getNotificationId() == NotificationConstants.COPY_ID) {
                    // set progress to indeterminate as deletion might still be going on from source while moving the file
                    getNotificationManager().notify(getNotificationId(), getProcessingNotificationBuilder().build());
                } else {
                    publishCompletedResult(getNotificationId());
                }
            }

            //for processviewer
            DatapointParcelable intent = new DatapointParcelable(fileName, sourceFiles, sourceProgress,
                    totalSize, writtenSize, speed, move, isComplete);
            //putDataPackage(intent);
            addDatapoint(intent);
        } else publishCompletedResult(getNotificationId());
    }

    private void publishCompletedResult(int id1) {
        try {
            getNotificationManager().cancel(id1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void addFirstDatapoint(String name, int amountOfFiles, long totalBytes, boolean move) {
        if(!getDataPackages().isEmpty()) {
            throw new IllegalStateException("This is not the first datapoint!");
        }

        DatapointParcelable intent1 = new DatapointParcelable(name, amountOfFiles, totalBytes, move);
        putDataPackage(intent1);
    }

    protected void addDatapoint(DatapointParcelable datapoint) {
        if(getDataPackages().isEmpty()) {
            throw new IllegalStateException("This is the first datapoint!");
        }

        putDataPackage(datapoint);
        if (getProgressListener() != null) {
            getProgressListener().onUpdate(datapoint);
            if (datapoint.completed) getProgressListener().refresh();
        }
    }

    /**
     * Returns the {@link #getDataPackages()} list which contains
     * data to be transferred to {@link ProcessViewerFragment}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     */
    public final synchronized DatapointParcelable getDataPackage(int index) {
        return getDataPackages().get(index);
    }

    public final synchronized int getDataPackageSize() {
        return getDataPackages().size();
    }

    /**
     * Puts a {@link DatapointParcelable} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link ProcessViewerFragment}
     */
    private synchronized void putDataPackage(DatapointParcelable dataPackage) {
        getDataPackages().add(dataPackage);
    }

    public interface ProgressListener {
        void onUpdate(DatapointParcelable dataPackage);
        void refresh();
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    @Override
    public boolean isDecryptService() {
        return false;
    }

    /**
     * Displays a notification, sends intent and cancels progress if there were some failures
     */
    void finalizeNotification(ArrayList<HybridFile> failedOps, boolean move) {
        if (!move) getNotificationManager().cancelAll();

        if(failedOps.size()==0)return;
        context = getApplicationContext();
        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID);
        mBuilder.setContentTitle(context.getString(R.string.operationunsuccesful));

        String titleResource;

        switch (getNotificationId()) {
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

        getProgressHandler().setCancelled(true);

        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra("move", move);

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);

        getNotificationManager().notify(NotificationConstants.FAILED_ID,mBuilder.build());

        intent=new Intent(MainActivity.TAG_INTENT_FILTER_GENERAL);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);

        sendBroadcast(intent);
    }

    /**
     * Initializes notification views to initial (processing..) state
     */
    public void initNotificationViews() {
        getNotificationManager().notify(getNotificationId(), getProcessingNotificationBuilder().build());
    }

}
