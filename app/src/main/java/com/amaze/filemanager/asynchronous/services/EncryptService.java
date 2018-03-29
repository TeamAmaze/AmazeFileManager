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
import android.text.format.Formatter;

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

public class EncryptService extends ProgressiveService {

    public static final String TAG_SOURCE = "crypt_source";     // source file to encrypt or decrypt
    public static final String TAG_ENCRYPT_TARGET = "crypt_target"; //name of encrypted file
    public static final String TAG_DECRYPT_PATH = "decrypt_path";
    public static final String TAG_OPEN_MODE = "open_mode";

    public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Context context;
    private IBinder mBinder = new ObtainableServiceBinder<>(this);
    private ProgressHandler progressHandler;
    private ServiceWatcherUtil serviceWatcherUtil;
    private long totalSize = 0l;
    private OpenMode openMode;
    private HybridFileParcelable baseFile;
    private ArrayList<HybridFile> failedOps = new ArrayList<>();
    private String targetFilename;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        registerReceiver(cancelReceiver, new IntentFilter(TAG_BROADCAST_CRYPT_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        baseFile = intent.getParcelableExtra(TAG_SOURCE);
        targetFilename = intent.getStringExtra(TAG_ENCRYPT_TARGET);

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

        new BackgroundTask().execute();


        return START_STICKY;
    }

    class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            if (baseFile.isDirectory())  totalSize = baseFile.folderSize(context);
            else totalSize = baseFile.length(context);

            progressHandler = new ProgressHandler(1, totalSize);
            progressHandler.setProgressListener(EncryptService.this::publishResults);
            serviceWatcherUtil = new ServiceWatcherUtil(progressHandler, totalSize);

            addFirstDatapoint(baseFile.getName(), 1, totalSize, true);// we're using encrypt as move flag false

            if (FileUtil.checkFolder(baseFile.getPath(), context) == 1) {
                serviceWatcherUtil.watch();

                // we're here to encrypt
                try {
                    new CryptUtil(context, baseFile, progressHandler, failedOps, targetFilename);
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
            generateNotification(failedOps);

            Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, "");
            sendBroadcast(intent);

            stopSelf();
        }
    }

    private void publishResults(String fileName, int sourceFiles, int sourceProgress,
                                long totalSize, long writtenSize, int speed) {

        if (!progressHandler.getCancelled()) {

            //notification
            float progressPercent = ((float) writtenSize/totalSize)*100;
            notificationBuilder.setProgress(100, Math.round(progressPercent), false);
            notificationBuilder.setOngoing(true);
            int title = R.string.crypt_encrypting;
            notificationBuilder.setContentTitle(context.getResources().getString(title));
            notificationBuilder.setContentText(fileName + " " + Formatter.formatFileSize(context,
                    writtenSize) + "/" +
                    Formatter.formatFileSize(context, totalSize));

            notificationManager.notify(NotificationConstants.ENCRYPT_ID, notificationBuilder.build());
            if (writtenSize == totalSize || totalSize == 0) {

                notificationBuilder.setContentText("");
                notificationBuilder.setOngoing(false);
                notificationBuilder.setAutoCancel(true);
                notificationManager.notify(NotificationConstants.ENCRYPT_ID, notificationBuilder.build());
                notificationManager.cancel(NotificationConstants.ENCRYPT_ID);
            }

            //for processviewer
            DatapointParcelable intent = new DatapointParcelable(fileName, sourceFiles, sourceProgress,
                    totalSize, writtenSize, speed, false);
            addDatapoint(intent);
        } else notificationManager.cancel(NotificationConstants.ENCRYPT_ID);
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

    /**
     * Displays a notification, sends intent and cancels progress if there were some failures
     * in copy progress
     * @param failedOps
     *
     */
    void generateNotification(ArrayList<HybridFile> failedOps) {
        notificationManager.cancelAll();

        if(failedOps.size()==0)return;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID)
            .setContentTitle(context.getString(R.string.operationunsuccesful))
            .setContentText(context.getString(R.string.copy_error, context.getString(R.string.copy_error).replace("%s",
                    context.getString(R.string.crypt_encrypted).toLowerCase())))
            .setAutoCancel(true);

        NotificationConstants.setMetadata(context, mBuilder, NotificationConstants.TYPE_NORMAL);

        progressHandler.setCancelled(true);

        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra("move", true);

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_folder_lock_white_36dp);

        notificationManager.notify(NotificationConstants.FAILED_ID, mBuilder.build());

        intent=new Intent(MainActivity.TAG_INTENT_FILTER_GENERAL);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);

        sendBroadcast(intent);
    }

    private BroadcastReceiver cancelReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //cancel operation
            progressHandler.setCancelled(true);
        }
    };

}
