package com.amaze.filemanager.ui.notifications;

import android.app.Notification;
import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.amaze.filemanager.R;

/**
 * This simply generates a "Processing..." notification,
 * useful for loading data before a service starts
 */
public class ProcessingNotificationBuilder {
    private NotificationCompat.Builder builder;
    private RemoteViews customView;
    private boolean isFilenameSet = false;

    public ProcessingNotificationBuilder(Context context, int notificationType) {
        customView = new RemoteViews(context.getPackageName(),
                R.layout.notification_processing);

        customView.setProgressBar(R.id.progressBar, 0, 0, true);

        builder = new NotificationCompat.Builder(context, NotificationConstants.CHANNEL_NORMAL_ID)
                .setSmallIcon(R.drawable.ic_content_copy_white_36dp)
                .setCustomContentView(customView)
                .setCustomHeadsUpContentView(customView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setOngoing(true);

        NotificationConstants.setMetadata(context, builder, notificationType);
    }

    public ProcessingNotificationBuilder setFilename(@NonNull CharSequence filename) {
        customView.setTextViewText(R.id.nameText, filename);
        isFilenameSet = true;
        return this;
    }

    public ProcessingNotificationBuilder setStyle(@NonNull NotificationCompat.DecoratedCustomViewStyle style) {
        builder.setStyle(style);
        return this;
    }

    public ProcessingNotificationBuilder addAction(@NonNull NotificationCompat.Action action) {
        builder.addAction(action);
        return this;
    }

    public ProcessingNotificationBuilder setColor(@ColorInt int color) {
        builder.setColor(color);
        return this;
    }

    public ProcessingNotificationBuilder setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
        return this;
    }

    public Notification build() {
        if(!isFilenameSet) {
            throw new NullPointerException("Please set a filename for ProcessingNotificationBuilder with setFilename()!");
        }

        return builder.build();
    }

}
