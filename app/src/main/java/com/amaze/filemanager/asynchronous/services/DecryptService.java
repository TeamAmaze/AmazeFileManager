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
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;

import java.util.ArrayList;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 28/11/2017, at 20:59.
 */

public class DecryptService extends ProgressiveService {
    public static final String TAG_SOURCE = "crypt_source";     // source file to encrypt or decrypt
    public static final String TAG_DECRYPT_PATH = "decrypt_path";
    public static final String TAG_OPEN_MODE = "open_mode";

    private static final int ID_NOTIFICATION = 27978;

    public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Context context;
    private IBinder mBinder = new ObtainableServiceBinder<>(this);
    private ProgressHandler progressHandler;
    private ServiceWatcherUtil serviceWatcherUtil;
    private long totalSize = 0l;
    private OpenMode openMode;
    private String decryptPath;
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
        notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setContentIntent(pendingIntent);

        decryptPath = intent.getStringExtra(TAG_DECRYPT_PATH);
        notificationBuilder.setContentTitle(getResources().getString(R.string.crypt_decrypting));
        notificationBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);


        startForeground(ID_NOTIFICATION, notificationBuilder.build());

        new DecryptService.BackgroundTask().execute();


        return START_STICKY;
    }

    class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            if (baseFile.isDirectory())  totalSize = baseFile.folderSize(context);
            else totalSize = baseFile.length(context);

            progressHandler = new ProgressHandler(1, totalSize);
            progressHandler.setProgressListener(DecryptService.this::publishResults);
            serviceWatcherUtil = new ServiceWatcherUtil(progressHandler, totalSize);

            addFirstDatapoint(baseFile.getName(), 1, totalSize, false);// we're using encrypt as move flag false

            if (FileUtil.checkFolder(baseFile.getPath(), context) == 1) {
                serviceWatcherUtil.watch();

                // we're here to decrypt, we'll decrypt at a custom path.
                // the path is to the same directory as in encrypted one in normal case
                // and the cache directory in case we're here because of the viewer
                try {
                    new CryptUtil(context, baseFile, decryptPath, progressHandler, failedOps);
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

            Intent intent = new Intent(EncryptDecryptUtils.DECRYPT_BROADCAST);
            sendBroadcast(intent);
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
            title = R.string.crypt_decrypting;
            notificationBuilder.setContentTitle(context.getResources().getString(title));
            notificationBuilder.setContentText(fileName + " " + Formatter.formatFileSize(context,
                    writtenSize) + "/" +
                    Formatter.formatFileSize(context, totalSize));

            notificationManager.notify(ID_NOTIFICATION, notificationBuilder.build());
            if (writtenSize == totalSize || totalSize == 0) {

                notificationBuilder.setContentText("");
                notificationBuilder.setOngoing(false);
                notificationBuilder.setAutoCancel(true);
                notificationManager.notify(ID_NOTIFICATION, notificationBuilder.build());
                publishCompletedResult();
            }

            //for processviewer
            DatapointParcelable intent = new DatapointParcelable(fileName, sourceFiles,
                    sourceProgress, totalSize, writtenSize, speed, false,false);
            addDatapoint(intent);
        } else publishCompletedResult();
    }

    public void publishCompletedResult(){
        try {
            notificationManager.cancel(ID_NOTIFICATION);
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * Displays a notification, sends intent and cancels progress if there were some failures
     * in copy progress
     * @param failedOps
     */
    void generateNotification(ArrayList<HybridFile> failedOps) {
        notificationManager.cancelAll();

        if(failedOps.size()==0)return;

        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.operationunsuccesful));
        mBuilder.setContentText(context.getString(R.string.copy_error).replace("%s",
                        context.getString(R.string.crypt_decrypted).toLowerCase()));
        mBuilder.setAutoCancel(true);

        progressHandler.setCancelled(true);

        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra("move", false);

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);

        notificationManager.notify(741,mBuilder.build());

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
