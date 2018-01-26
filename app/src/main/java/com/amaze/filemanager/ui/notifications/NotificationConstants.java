package com.amaze.filemanager.ui.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.amaze.filemanager.R;

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *         on 17/9/2017, at 13:34.
 */

public class NotificationConstants {

    public static final int WAIT_ID = -1;
    public static final int COPY_ID = 0;
    public static final int EXTRACT_ID = 1;
    public static final int ZIP_ID = 2;
    public static final int DECRYPT_ID = 3;
    public static final int ENCRYPT_ID = 4;
    public static final int FTP_ID = 5;
    public static final int FAILED_ID = 6;

    public static final String CHANNEL_NORMAL_ID = "normalChannel";

    /**
     * This creates a channel (API >= 26) or applies the correct metadata to a notification (API < 26)
     */
    public static void setMetadata(Context context, NotificationCompat.Builder notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNormalChannel(context);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notification.setCategory(Notification.CATEGORY_SERVICE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification.setPriority(Notification.PRIORITY_MIN);
            }
        }
    }

    /**
     * You CANNOT call this from android < O.
     * THis channel is set so it doesn't bother the user, with the lowest importance.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createNormalChannel(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(mNotificationManager.getNotificationChannel(CHANNEL_NORMAL_ID) == null) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_NORMAL_ID,
                    context.getString(R.string.channelname_normal), NotificationManager.IMPORTANCE_MIN);
            // Configure the notification channel.
            mChannel.setDescription(context.getString(R.string.channeldescription_normal));
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

}
