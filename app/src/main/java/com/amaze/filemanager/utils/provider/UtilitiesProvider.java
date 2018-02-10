package com.amaze.filemanager.utils.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.amaze.filemanager.utils.color.ColorPreference;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.amaze.filemanager.utils.theme.AppThemeManager;

/**
 * Created by piotaixr on 16/01/17.
 */

public class UtilitiesProvider {
    private ColorPreference colorPreference;
    private AppThemeManager appThemeManager;

    public UtilitiesProvider(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        colorPreference = ColorPreference.loadFromPreferences(context, sharedPreferences);
        appThemeManager = new AppThemeManager(sharedPreferences);
    }

    public ColorPreference getColorPreference() {
        return colorPreference;
    }

    public AppTheme getAppTheme() {
        return appThemeManager.getAppTheme();
    }

    public AppThemeManager getThemeManager() {
        return appThemeManager;
    }
}
