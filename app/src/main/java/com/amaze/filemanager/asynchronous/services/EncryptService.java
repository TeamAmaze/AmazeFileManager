package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.notifications.NotificationConstants;
import com.amaze.filemanager.utils.CopyDataParcelable;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherProgressAbstract;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.EncryptDecryptUtils;

import java.util.ArrayList;

/**
 * Created by vishal on 8/4/17.
 */

public class EncryptService extends ServiceWatcherProgressAbstract {

    public static final String TAG_SOURCE = "crypt_source";     // source file to encrypt or decrypt
    public static final String TAG_DECRYPT_PATH = "decrypt_path";
    public static final String TAG_OPEN_MODE = "open_mode";
    public static final String TAG_CRYPT_MODE = "crypt_mode";   // ordinal of type of service
                                                                // expected (encryption or decryption)
    public static final String TAG_BROADCAST_RESULT = "broadcast_result";
    public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";
    public static final int ID_NOTIFICATION = 3627;

    private Context context;
    private IBinder mBinder = new LocalBinder();
    private ServiceWatcherUtil serviceWatcherUtil;
    private long totalSize = 0l;
    private String decryptPath;
    private HybridFileParcelable baseFile;
    private CryptEnum cryptEnum;
    private ArrayList<HybridFile> failedOps = new ArrayList<>();
    private boolean broadcastResult = false;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        registerReceiver(cancelReceiver, new IntentFilter(TAG_BROADCAST_CRYPT_CANCEL));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        baseFile = intent.getParcelableExtra(TAG_SOURCE);
        cryptEnum = CryptEnum.values()[intent.getIntExtra(TAG_CRYPT_MODE, CryptEnum.ENCRYPT.ordinal())];
        broadcastResult = intent.getBooleanExtra(TAG_BROADCAST_RESULT, false);

        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mBuilder = new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID);
        mBuilder.setContentIntent(pendingIntent);

        if (cryptEnum == CryptEnum.ENCRYPT) {
            // we have to encrypt the source

            mBuilder.setContentTitle(getResources().getString(R.string.crypt_encrypting));
            mBuilder.setSmallIcon(R.drawable.ic_folder_lock_white_36dp);
        } else {

            decryptPath = intent.getStringExtra(TAG_DECRYPT_PATH);
            mBuilder.setContentTitle(getResources().getString(R.string.crypt_decrypting));
            mBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);
        }

        NotificationConstants.setMetadata(getApplicationContext(), mBuilder);

        startForeground((notificationID = ID_NOTIFICATION), mBuilder.build());

        new BackgroundTask().execute();


        return START_STICKY;
    }

    @Override
    public EncryptService getServiceType() {
        return this;
    }

    class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            if (baseFile.isDirectory())  totalSize = baseFile.folderSize(context);
            else totalSize = baseFile.length(context);

            progressHandler = new ProgressHandler(1, totalSize);
            progressHandler.setProgressListener((fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed) -> {
                publishResults(ID_NOTIFICATION, fileName, sourceFiles, sourceProgress, totalSize,
                        writtenSize, speed, false, cryptEnum==CryptEnum.ENCRYPT ? false : true);
            });
            serviceWatcherUtil = new ServiceWatcherUtil(progressHandler, totalSize);

            CopyDataParcelable dataPackage = new CopyDataParcelable(baseFile.getName(),
                    1, 1, totalSize, 0, 0,
                    cryptEnum==CryptEnum.ENCRYPT ? false : true,  // we're using encrypt as move flag false
                    false);
            putDataPackage(dataPackage);

            if (FileUtil.checkFolder(baseFile.getPath(), context) == 1) {
                serviceWatcherUtil.watch(EncryptService.this);

                if (cryptEnum == CryptEnum.ENCRYPT) {
                    // we're here to encrypt
                    try {
                        new CryptUtil(context, baseFile, progressHandler, failedOps);
                    } catch (Exception e) {
                        e.printStackTrace();
                        failedOps.add(baseFile);
                    }
                } else {

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
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            serviceWatcherUtil.stopWatch();
            generateNotification(failedOps, cryptEnum==CryptEnum.ENCRYPT ? false : true);

            if (!broadcastResult) {

                Intent intent = new Intent(MainActivity.KEY_INTENT_LOAD_LIST);
                intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, decryptPath);
                sendBroadcast(intent);
            } else {
                Intent intent = new Intent(EncryptDecryptUtils.DECRYPT_BROADCAST);
                sendBroadcast(intent);
            }
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {

        public EncryptService getService() {
            return EncryptService.this;
        }
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
     * @param move
     */
    void generateNotification(ArrayList<HybridFile> failedOps, boolean move) {
        mNotifyManager.cancelAll();

        if(failedOps.size()==0)return;

        String error = move ? context.getString(R.string.crypt_encrypted).toLowerCase():context.getString(R.string.crypt_decrypted).toLowerCase();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID)
            .setContentTitle(context.getString(R.string.operationunsuccesful))
            .setContentText(context.getString(R.string.copy_error, error))
            .setAutoCancel(true);

        NotificationConstants.setMetadata(context, mBuilder);

        progressHandler.setCancelled(true);

        Intent intent= new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra("move", move);

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pIntent);
        if (cryptEnum == CryptEnum.ENCRYPT) {
            mBuilder.setSmallIcon(R.drawable.ic_folder_lock_white_36dp);
        } else {
            mBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);
        }

        mNotifyManager.notify(741,mBuilder.build());

        intent=new Intent(MainActivity.TAG_INTENT_FILTER_GENERAL);
        intent.putExtra(MainActivity.TAG_INTENT_FILTER_FAILED_OPS, failedOps);
        intent.putExtra(TAG_CRYPT_MODE, move);

        sendBroadcast(intent);
    }

    public enum CryptEnum {
        ENCRYPT,
        DECRYPT
    }

    private BroadcastReceiver cancelReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //cancel operation
            progressHandler.setCancelled(true);
        }
    };

}
