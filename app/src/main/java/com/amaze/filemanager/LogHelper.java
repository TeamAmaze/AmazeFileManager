package com.amaze.filemanager;

import android.util.Log;

public final class LogHelper {
    private LogHelper() {}

    public static final void logOnProductionOrCrash(String tag, String message) {
        if(BuildConfig.DEBUG) {
            throw new IllegalStateException(message);
        } else {
            Log.e(tag, message);
        }
    }
}
