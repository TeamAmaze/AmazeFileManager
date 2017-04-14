package com.amaze.filemanager.utils;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;

/**
 * Created by vishal on 23/2/17.
 *
 * Class determines whether there was a config change
 *
 * Supposed to be used to determine recursive callbacks to fragment/activity/loader
 * Make sure to recycle after you're done
 */

public class InterestingConfigChange {

    private static Configuration lastConfiguration = new Configuration();
    private static int lastDensity = -1;

    /**
     * Check for any config change between various callbacks to this method.
     * Make sure to recycle after done
     * @param resources
     * @return
     */
    public static boolean isConfigChanged(Resources resources) {
        int changedFieldsMask = lastConfiguration.updateFrom(resources.getConfiguration());
        boolean densityChanged = lastDensity!=resources.getDisplayMetrics().densityDpi;

        if (densityChanged || (changedFieldsMask &
                (ActivityInfo.CONFIG_SCREEN_LAYOUT | ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_LOCALE)) != 0) {
            // we have density changed from last time we came here
            return true;
        }

        return false;
    }

    /**
     * Recycle after usage, to avoid getting inconsistent result because of static modifiers
     */
    public static void recycle() {
        lastConfiguration = new Configuration();
        lastDensity = -1;
    }
}
