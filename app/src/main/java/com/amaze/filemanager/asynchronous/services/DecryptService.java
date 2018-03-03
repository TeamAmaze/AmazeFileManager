package com.amaze.filemanager.asynchronous.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

public class DecryptService extends ProgressiveServiceAbstract {

    public static final String TAG_SOURCE = "crypt_source";     // source file to encrypt or decrypt
    public static final String TAG_DECRYPT_PATH = "decrypt_path";
    public static final String TAG_OPEN_MODE = "open_mode";

    public static final String TAG_BROADCAST_CRYPT_CANCEL = "crypt_cancel";

    private Context context;
    private IBinder mBinder = new ObtainableServiceBinder<>(this);
    private ProgressHandler progressHandler = new ProgressHandler();
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private volatile float progressPercent = 0f;
    private ProgressListener progressListener;
    // list of data packages, to initiate chart in process viewer fragment
    private ArrayList<DatapointParcelable> dataPackages = new ArrayList<>();
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
        notificationBuilder = new NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID);
        notificationBuilder.setContentIntent(pendingIntent);

        decryptPath = intent.getStringExtra(TAG_DECRYPT_PATH);
        notificationBuilder.setContentTitle(getResources().getString(R.string.crypt_decrypting));
        notificationBuilder.setSmallIcon(R.drawable.ic_folder_lock_open_white_36dp);


        startForeground(NotificationConstants.DECRYPT_ID, notificationBuilder.build());

        super.onStartCommand(intent, flags, startId);
        new DecryptService.BackgroundTask().execute();

        return START_STICKY;
    }

    class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String baseFileFolder = baseFile.isDirectory()?
                    baseFile.getPath():
                    baseFile.getPath().substring(0, baseFile.getPath().lastIndexOf('/'));

            if (baseFile.isDirectory())  totalSize = baseFile.folderSize(context);
            else totalSize = baseFile.length(context);

            progressHandler.setSourceSize(1);
            progressHandler.setTotalSize(totalSize);
            progressHandler.setProgressListener((fileName, sourceFiles, sourceProgress, totalSize, writtenSize, speed) -> {
                publishResults(fileName, sourceFiles, sourceProgress, totalSize,
                        writtenSize, speed, false, false);
            });
            serviceWatcherUtil = new ServiceWatcherUtil(progressHandler, totalSize);

            addFirstDatapoint(baseFile.getName(), 1, totalSize, false);// we're using encrypt as move flag false

            if (FileUtil.checkFolder(baseFileFolder, context) == 1) {
                serviceWatcherUtil.watch(DecryptService.this);

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
            generateNotification(failedOps, false);

            Intent intent = new Intent(EncryptDecryptUtils.DECRYPT_BROADCAST);
            intent.putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, "");
            sendBroadcast(intent);
            stopSelf();
        }
    }

    @Override
    public DecryptService getServiceType() {
        return this;
    }

    @Override
    public void initVariables() {

        super.mNotifyManager = notificationManager;
        super.mBuilder = notificationBuilder;
        super.notificationID = NotificationConstants.DECRYPT_ID;
        super.progressPercent = progressPercent;
        super.progressListener = progressListener;
        super.dataPackages = dataPackages;
        super.progressHandler = progressHandler;
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
