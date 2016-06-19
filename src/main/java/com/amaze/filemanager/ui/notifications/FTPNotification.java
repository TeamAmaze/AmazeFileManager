package com.amaze.filemanager.ui.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.services.ftpservice.FTPService;
import com.amaze.filemanager.utils.Futils;

import java.net.InetAddress;

/**
 * Created by yashwanthreddyg on 19-06-2016.
 */
public class FTPNotification extends BroadcastReceiver{

    private static final int NOTIFICATION_ID = 2123;
    Futils utils = new Futils();
    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()){
            case FTPService.ACTION_STARTED:
                createNotification(context);
                break;
            case FTPService.ACTION_STOPPED:
                removeNotifiation(context);
                break;
        }
    }

    @SuppressWarnings("NewApi")
    private void createNotification(Context context){
        // Get NotificationManager reference
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);

        // get ip address
        InetAddress address = FTPService.getLocalInetAddress(context);

        String iptext = "ftp://" + address.getHostAddress() + ":"
                + FTPService.getPort() + "/";

        // Instantiate a Notification
        int icon = R.drawable.ic_ftp_light;
        CharSequence tickerText = utils.getString(context,R.string.ftp_notif_starting);
        long when = System.currentTimeMillis();

        // Define Notification's message and Intent
        CharSequence contentTitle = utils.getString(context,R.string.ftp_notif_title);
        CharSequence contentText = String.format(utils.getString(context,R.string.ftp_notif_text), iptext);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
        CharSequence stopText = utils.getString(context,R.string.ftp_notif_stop_server);
        Intent stopIntent = new Intent(FTPService.ACTION_STOP_FTPSERVER);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT);

//        int preferenceIcon = android.R.drawable.ic_menu_preferences;
//        CharSequence preferenceText = context.getString(R.string.notif_settings_text);
//        Intent preferenceIntent = new Intent(context, FsPreferenceActivity.class);
//        preferenceIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent preferencePendingIntent = PendingIntent.getActivity(context, 0, preferenceIntent, 0);

        Notification.Builder nb = new Notification.Builder(context)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true);

        Notification notification = null;

        // go from hight to low android version adding extra options
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nb.setVisibility(Notification.VISIBILITY_PUBLIC);
            nb.setCategory(Notification.CATEGORY_SERVICE);
            nb.setPriority(Notification.PRIORITY_MAX);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            nb.addAction(stopIcon, stopText, stopPendingIntent);
//            nb.addAction(preferenceIcon, preferenceText, preferencePendingIntent);
            nb.setShowWhen(false);
            notification = nb.build();
        } else {
            notification = nb.getNotification();
        }

        // Pass Notification to NotificationManager
        nm.notify(NOTIFICATION_ID, notification);

    }

    private void removeNotifiation(Context context){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);
        nm.cancelAll();
    }
}
