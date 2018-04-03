package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.util.ArrayList;

/**
 * Created by vishal on 8/4/17 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 */

public class EncryptService extends AbstractProgressiveService {

    public static final String TAG_SOURCE = "crypt_source";     // source file to encrypt or decrypt
    public static final String TAG_DECRYPT_PATH = "decrypt_path";
    public static final String TAG_OPEN_MODE = "open_mode";

    public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Context context;
    private IBinder mBinder = new ObtainableServiceBinder<>(this);
    private ProgressHandler progressHandler = new ProgressHandler();
    private volatile float progressPercent = 0f;
    private ProgressListener progressListener;
    // list of data packages, to initiate chart in process viewer fragment
    private ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();
    private ServiceWatcherUtil serviceWatcherUtil;
    private long totalSize = 0l;
    private OpenMode openMode;
    private HybridFileParcelable baseFile;
    private ArrayList<HybridFile> failedOps = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        registerReceiver(cancelReceiver, new IntentFilter(TAG_BROADCAST_CRYPT_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        baseFile = intent.getParcelableExtra(TAG_SOURCE);

        openMode = OpenMode.values()[intent.getIntExtra(TAG_OPEN_MODE, OpenMode.UNKNOWN.ordinal())];
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID);
        notificationBuilder.setContentIntent(pendingIntent);

        // we have to encrypt the source

        notificationBuilder.setContentTitle(getResources().getString(R.string.crypt_encrypting));
        notificationBuilder.setSmallIcon(R.drawable.ic_folder_lock_white_36dp);

        NotificationConstants.setMetadata(getApplicationContext(), notificationBuilder, NotificationConstants.TYPE_NORMAL);

        startForeground(NotificationConstants.ENCRYPT_ID, notificationBuilder.build());

        super.onStartCommand(intent, flags, startId);
        super.progressHalted();
        new BackgroundTask().execute();

        return START_STICKY;
    }

    @Override
    protected NotificationManager getNotificationManager() {
        return notificationManager;
    }

    @Override
    protected NotificationCompat.Builder getNotificationBuilder() {
        return notificationBuilder;
    }

    @Override
    protected int getNotificationId() {
        return NotificationConstants.ENCRYPT_ID;
    }

    @Override
    protected float getPercentProgress() {
        return progressPercent;
    }

    @Override
    protected void setPercentProgress(float progress) {
        this.progressPercent = progress;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    @Override
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    protected ArrayList<DatapointParcelable> getDataPackages() {
        return dataPackages;
    }

    @Override
    protected ProgressHandler getProgressHandler() {
        return progressHandler;
    }

    class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            if (baseFile.isDirectory())  totalSize = baseFile.folderSize(context);
            else totalSize = baseFile.length(context);

            progressHandler.setSourceSize(1);
            progressHandler.setTotalSize(totalSize);
            progressHandler.setProgressListener((fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed) -> {
                publishResults(fileName, sourceFiles, sourceProgress, totalSize,
                        writtenSize, speed, false, false);
            });
            serviceWatcherUtil = new ServiceWatcherUtil(progressHandler);

            addFirstDatapoint(baseFile.getName(), 1, totalSize, true);// we're using encrypt as move flag false

            if (FileUtil.checkFolder(baseFile.getPath(), context) == 1) {
                serviceWatcherUtil.watch(EncryptService.this);

                // we're here to encrypt
                try {
                    new CryptUtil(context, baseFile, progressHandler, failedOps);
                } catch (Exception e) {
                    e.printStackTrace();
                    failedOps.add(baseFile);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            serviceWatcherUtil.stopWatch();
            generateNotification(failedOps, false);

            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, "");
            sendBroadcast(intent);

            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(cancelReceiver);
    }

    private BroadcastReceiver cancelReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //cancel operation
            progressHandler.setCancelled(true);
        }
    };

}
