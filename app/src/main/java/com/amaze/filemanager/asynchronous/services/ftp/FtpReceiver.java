package com.amaze.filemanager.asynchronous.services.ftp;

/**
 * Created by yashwanthreddyg on 09-06-2016.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FtpReceiver extends BroadcastReceiver {

    static final String TAG = FtpReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Received: " + intent.getAction());

        try {
            Intent service = new Intent(context, FtpService.class);
            service.putExtras(intent);
            if (intent.getAction().equals(FtpService.ACTION_START_FTPSERVER) &&
                    !FtpService.isRunning()) {
                context.startService(service);
            } else if (intent.getAction().equals(FtpService.ACTION_STOP_FTPSERVER)) {
                context.stopService(service);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start/stop on intent " + e.getMessage());
        }
    }


}
