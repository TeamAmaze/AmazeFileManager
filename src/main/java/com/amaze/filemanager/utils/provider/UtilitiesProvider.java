package com.amaze.filemanager.utils.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.color.ColorPreference;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.amaze.filemanager.utils.theme.AppThemeManagerInterface;
import com.amaze.filemanager.utils.theme.PreferencesAppThemeManager;

/**
 * Created by piotaixr on 16/01/17.
 */

public class UtilitiesProvider implements UtilitiesProviderInterface {
    private Futils futils;
    private ColorPreference colorPreference;
    private AppThemeManagerInterface appThemeManager;

    public UtilitiesProvider(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        futils = new Futils();

        colorPreference = ColorPreference.loadFromPreferences(context, sharedPreferences);
        appThemeManager = new PreferencesAppThemeManager(sharedPreferences);
    }

    @Override
    public Futils getFutils() {
        return futils;
    }

    @Override
    public ColorPreference getColorPreference() {
        return colorPreference;
    }

    @Override
    public AppTheme getAppTheme() {
        return appThemeManager.getAppTheme();
    }

    @Override
    public AppThemeManagerInterface getThemeManager() {
        return appThemeManager;
    }
}
