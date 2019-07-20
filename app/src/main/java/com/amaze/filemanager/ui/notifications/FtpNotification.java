package com.amaze.filemanager.ui.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.asynchronous.services.ftp.FtpService;

import java.net.InetAddress;

/**
 * Created by yashwanthreddyg on 19-06-2016.
 */
public class FtpNotification extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()){
            case FtpService.ACTION_STARTED:
                createNotification(context, intent.getBooleanExtra(FtpService.TAG_STARTED_BY_TILE, false));
                break;
            case FtpService.ACTION_STOPPED:
                removeNotification(context);
                break;
        }
    }

    private void createNotification(Context context, boolean noStopButton) {

        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(notificationService);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int port = sharedPreferences.getInt(FtpService.PORT_PREFERENCE_KEY, FtpService.DEFAULT_PORT);
        boolean secureConnection = sharedPreferences.getBoolean(FtpService.KEY_PREFERENCE_SECURE, FtpService.DEFAULT_SECURE);

        InetAddress address = FtpService.getLocalInetAddress(context);

        String iptext = (secureConnection ? FtpService.INITIALS_HOST_SFTP : FtpService.INITIALS_HOST_FTP)
                + address.getHostAddress() + ":"
                + port + "/";

        //TODO: is this an appropriate way to get the builder from FtpService to update the notification here?
        NotificationCompat.Builder builder = FtpService.getBuilder();

        int icon = R.drawable.ic_ftp_light;
        CharSequence tickerText = context.getString(R.string.ftp_notif_starting);
        long when = System.currentTimeMillis();

        CharSequence contentTitle = context.getString(R.string.ftp_notif_title);
        CharSequence contentText = String.format(context.getString(R.string.ftp_notif_text), iptext);

        builder.setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true);

        notificationManager.notify(NotificationConstants.FTP_ID, builder.build());
    }

    private void removeNotification(Context context){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);
        nm.cancelAll();
    }
}
