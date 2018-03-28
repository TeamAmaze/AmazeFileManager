package com.amaze.filemanager.utils;


import android.app.Activity;
import android.util.DisplayMetrics;

public class ScreenUtils {

    private static final int TOOLBAR_HEIGHT_IN_DP_MDPI = 40;  //160 dpi
    private static final int TOOLBAR_HEIGHT_IN_DP_HDPI = 48;  //240 dpi
    private static final int TOOLBAR_HEIGHT_IN_DP_XHDPI = 56;   //320 dpi
    private static final int TOOLBAR_HEIGHT_IN_DP_XXHDPI = 64;   //480 dpi
    private static final int TOOLBAR_HEIGHT_IN_DP_XXXHDPI = 72;   //640 dpi


    Activity activity;


    public ScreenUtils(Activity activity) {
        this.activity = activity;
    }


    public int convertDbToPx(float dp) {
        return Math.round(activity.getResources().getDisplayMetrics().density * dp);
    }

    public int convertPxToDb(float px) {
        return Math.round(px / activity.getResources().getDisplayMetrics().density);
    }

    public int getScreenWidthInPx() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public int getScreenHeightInPx() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public int getScreenWidthInDp() {
        return convertPxToDb(getScreenWidthInPx());
    }

    public int getScreeHeightInDb() {
        return convertPxToDb(getScreenHeightInPx());
    }


    public int getToolbarHeightInDb() {
        float density = activity.getResources().getDisplayMetrics().density;
        int toolbarHeightInDp = TOOLBAR_HEIGHT_IN_DP_MDPI;

        if (density == 1.0f) {
            toolbarHeightInDp = TOOLBAR_HEIGHT_IN_DP_MDPI;
        } else if (density == 1.5f) {
            toolbarHeightInDp = TOOLBAR_HEIGHT_IN_DP_HDPI;
        } else if (density == 2.0f) {
            toolbarHeightInDp = TOOLBAR_HEIGHT_IN_DP_XHDPI;
        } else if (density == 3.0f) {
            toolbarHeightInDp = TOOLBAR_HEIGHT_IN_DP_XXHDPI;
        } else if (density == 4.0f) {
            toolbarHeightInDp = TOOLBAR_HEIGHT_IN_DP_XXXHDPI;
        }
        return toolbarHeightInDp;
    }
}
