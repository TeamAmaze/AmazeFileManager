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

import java.net.InetAddress;

/**
 * Created by yashwanthreddyg on 19-06-2016.
 */
public class FTPNotification extends BroadcastReceiver{

    private static final int NOTIFICATION_ID = 2123;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()){
            case FTPService.ACTION_STARTED:
                createNotification(context);
                break;
            case FTPService.ACTION_STOPPED:
                removeNotification(context);
                break;
        }
    }

    @SuppressWarnings("NewApi")
    private void createNotification(Context context){

        String notificationService = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(notificationService);

        InetAddress address = FTPService.getLocalInetAddress(context);

        String iptext = "ftp://" + address.getHostAddress() + ":"
                + FTPService.getPort() + "/";

        int icon = R.drawable.ic_ftp_light;
        CharSequence tickerText = context.getResources().getString(R.string.ftp_notif_starting);
        long when = System.currentTimeMillis();


        CharSequence contentTitle = context.getResources().getString(R.string.ftp_notif_title);
        CharSequence contentText = String.format(context.getResources().getString(R.string.ftp_notif_text), iptext);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
        CharSequence stopText = context.getResources().getString(R.string.ftp_notif_stop_server);
        Intent stopIntent = new Intent(FTPService.ACTION_STOP_FTPSERVER);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT);


        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true);

        Notification notification = null;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilder.setPriority(Notification.PRIORITY_MAX);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notificationBuilder.addAction(stopIcon, stopText, stopPendingIntent);
            notificationBuilder.setShowWhen(false);
            notification = notificationBuilder.build();
        } else {
            notification = notificationBuilder.getNotification();
        }

        // Pass Notification to NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void removeNotification(Context context){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nm = (NotificationManager) context.getSystemService(ns);
        nm.cancelAll();
    }
}
