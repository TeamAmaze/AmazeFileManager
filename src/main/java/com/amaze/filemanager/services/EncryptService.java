package com.amaze.filemanager.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.CryptUtil;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.util.ArrayList;

/**
 * Created by vishal on 8/4/17.
 */

public class EncryptService extends Service {

    public static final String TAG_SOURCE = "crypt_source";     // source file to encrypt or decrypt
    public static final String TAG_DECRYPT_PATH = "decrypt_path";
    public static final String TAG_OPEN_MODE = "open_mode";
    public static final String TAG_CRYPT_MODE = "crypt_mode";   // ordinal of type of service
                                                                // expected (encryption or decryption)

    private static final int ID_NOTIFICATION = 27978;

    public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";

    // list of data packages which contains progress
    private ArrayList<DataPackage> dataPackages = new ArrayList<>();
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Context context;
    private IBinder mBinder = new LocalBinder();
    private ProgressHandler progressHandler;
    private ServiceWatcherUtil serviceWatcherUtil;
    private long totalSize = 0l;
    private OpenMode openMode;
    private String decryptPath;
    private BaseFile baseFile;
    private CryptEnum cryptEnum;
    private ArrayList<HFile> failedOps = new ArrayList<>();
    private ProgressListener progressListener;

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

        openMode = OpenMode.values()[intent.getIntExtra(TAG_OPEN_MODE, OpenMode.UNKNOWN.ordinal())];
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setContentIntent(pendingIntent);

        if (cryptEnum == CryptEnum.ENCRYPT) {
            // we have to encrypt the source

            notificationBuilder.setContentTitle(getResources().getString(R.string.crypt_encrypting));
            notificationBuilder.setSmallIcon(R.drawable.ic_folder_lock_white_36dp);
        } else {

            decryptPath = intent.getStringExtra(TAG_DECRYPT_PATH);
            notificationBuilder.setContentTitle(getResources().getString(R.string.crypt_decrypting));
            notificationBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);
        }

        startForeground(ID_NOTIFICATION, notificationBuilder.build());

        new BackgroundTask().execute();


        return START_STICKY;
    }

    class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            if (baseFile.isDirectory())  totalSize = baseFile.folderSize(context);
            else totalSize = baseFile.length(context);

            progressHandler = new ProgressHandler(1, totalSize);
            progressHandler.setProgressListener(new ProgressHandler.ProgressListener() {
                @Override
                public void onProgressed(String fileName, int sourceFiles, int sourceProgress,
                                         long totalSize, long writtenSize, int speed) {

                    publishResults(fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed);
                }
            });
            serviceWatcherUtil = new ServiceWatcherUtil(progressHandler, totalSize);

            DataPackage dataPackage = new DataPackage();
            dataPackage.setName(baseFile.getName());
            dataPackage.setSourceFiles(1);
            dataPackage.setSourceProgress(1);
            dataPackage.setTotal(totalSize);
            dataPackage.setByteProgress(0);
            dataPackage.setSpeedRaw(0);
            dataPackage.setMove(cryptEnum==CryptEnum.ENCRYPT ? false : true);   // we're using encrypt as
                                                                                // move flag false
            dataPackage.setCompleted(false);
            putDataPackage(dataPackage);

            if (FileUtil.checkFolder(baseFile.getPath(), context) == 1) {
                serviceWatcherUtil.watch();

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
            Intent intent = new Intent("loadlist");
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
            if (cryptEnum == CryptEnum.DECRYPT) title = R.string.crypt_decrypting;
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
            DataPackage intent = new DataPackage();
            intent.setName(fileName);
            intent.setSourceFiles(sourceFiles);
            intent.setSourceProgress(sourceProgress);
            intent.setTotal(totalSize);
            intent.setByteProgress(writtenSize);
            intent.setSpeedRaw(speed);
            intent.setMove(cryptEnum==CryptEnum.ENCRYPT ? false : true);
            intent.setCompleted(false);
            putDataPackage(intent);
            if(progressListener!=null) {
                progressListener.onUpdate(intent);
                if(false) progressListener.refresh();
            }
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
    void generateNotification(ArrayList<HFile> failedOps, boolean move) {
        notificationManager.cancelAll();

        if(failedOps.size()==0)return;

        NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(context);
        mBuilder.setContentTitle(context.getString(R.string.operationunsuccesful));
        mBuilder.setContentText(context.getString(R.string.copy_error).replace("%s",
                move ? context.getString(R.string.crypt_encrypted).toLowerCase() :
                        context.getString(R.string.crypt_decrypted).toLowerCase()));
        mBuilder.setAutoCancel(true);

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

        notificationManager.notify(741,mBuilder.build());

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


    public interface ProgressListener {
        void onUpdate(DataPackage dataPackage);
        void refresh();
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Returns the {@link #dataPackages} list which contains
     * data to be transferred to {@link com.amaze.filemanager.fragments.ProcessViewer}
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
     * @return
     */
    public synchronized DataPackage getDataPackage(int index) {
        return this.dataPackages.get(index);
    }

    public synchronized int getDataPackageSize() {
        return this.dataPackages.size();
    }

    /**
     * Puts a {@link DataPackage} into a list
     * Method call is synchronized so as to avoid modifying the list
     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
     * @param dataPackage
     */
    private synchronized void putDataPackage(DataPackage dataPackage) {
        this.dataPackages.add(dataPackage);
    }
}
