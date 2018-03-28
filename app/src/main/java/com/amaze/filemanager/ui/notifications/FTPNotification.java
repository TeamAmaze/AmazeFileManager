package com.amaze.filemanager.ui.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.asynchronous.services.ftp.FTPService;

import java.net.InetAddress;

/**
 * Created by yashwanthreddyg on 19-06-2016.
 */
public class FTPNotification extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()){
            case FTPService.ACTION_STARTED:
                createNotification(context, intent.getBooleanExtra(FTPService.TAG_STARTED_BY_TILE, false));
                break;
            case FTPService.ACTION_STOPPED:
                removeNotification(context);
                break;
        }
    }

    private void createNotification(Context context, boolean noStopButton) {

        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(notificationService);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int port = sharedPreferences.getInt(FTPService.PORT_PREFERENCE_KEY, FTPService.DEFAULT_PORT);
        boolean secureConnection = sharedPreferences.getBoolean(FTPService.KEY_PREFERENCE_SECURE, FTPService.DEFAULT_SECURE);

        InetAddress address = FTPService.getLocalInetAddress(context);

        String iptext = (secureConnection ? FTPService.INITIALS_HOST_SFTP : FTPService.INITIALS_HOST_FTP)
                + address.getHostAddress() + ":"
                + port + "/";

        int icon = R.drawable.ic_ftp_light;
        CharSequence tickerText = context.getResources().getString(R.string.ftp_notif_starting);
        long when = System.currentTimeMillis();


        CharSequence contentTitle = context.getResources().getString(R.string.ftp_notif_title);
        CharSequence contentText = String.format(context.getResources().getString(R.string.ftp_notif_text), iptext);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_FTP_ID)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true);

        NotificationConstants.setMetadata(context, notificationBuilder, NotificationConstants.TYPE_FTP);

        Notification notification;
        if (!noStopButton && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
            CharSequence stopText = context.getResources().getString(R.string.ftp_notif_stop_server);
            Intent stopIntent = new Intent(FTPService.ACTION_STOP_FTPSERVER);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0,
                    stopIntent, PendingIntent.FLAG_ONE_SHOT);

            notificationBuilder.addAction(stopIcon, stopText, stopPendingIntent);
            notificationBuilder.setShowWhen(false);
            notification = notificationBuilder.build();
        } else {
            notification = notificationBuilder.getNotification();
        }

        // Pass Notification to NotificationManager
        notificationManager.notify(NotificationConstants.FTP_ID, notification);
    }

    private void removeNotification(Context context){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);
        nm.cancelAll();
    }
}
