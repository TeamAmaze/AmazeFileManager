package com.amaze.filemanager.utils;

import android.app.NotificationManager;
import android.app.Service;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Vishal on 12-01-2018.
 *
 * Abstract class which handles pause/resumes of notification progress of services
 * as returned by the {@link ServiceWatcherUtil}
 */

public abstract class ServiceWatcherProgressAbstract extends Service implements com.amaze.filemanager.utils.ServiceWatcherUtil.ServiceWatcherInteractionInterface {

    public NotificationManager mNotifyManager;
    public NotificationCompat.Builder mBuilder;
    public int notificationID;
    public volatile float progressPercent = 0f;

    @Override
    public void progressHalted() {

        // set notification to indeterminate unless progress resumes
        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(notificationID, mBuilder.build());
    }

    @Override
    public void progressResumed() {

        // set notification to indeterminate unless progress resumes
        mBuilder.setProgress(100, Math.round(progressPercent), false);
        mNotifyManager.notify(notificationID, mBuilder.build());
    }
}
