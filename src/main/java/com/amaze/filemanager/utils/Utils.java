package com.amaze.filemanager.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;

/**
 * Contains useful functions and methods
 *
 * @author Emmanuel
 *         on 14/5/2017, at 14:39.
 */

public class Utils {

    /**
     * Gets color
     *
     * @param c Context
     * @param color the resource id for the color
     * @return the color
     */
    public static int getColor(Context c, @ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return c.getColor(color);
        } else {
            return c.getResources().getColor(color);
        }
    }

}
