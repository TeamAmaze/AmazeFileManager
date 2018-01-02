package com.amaze.filemanager.utils.theme;

import android.content.SharedPreferences;

import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;

/**
 * Saves and restores the AppTheme
 */
public class AppThemeManager {
    private SharedPreferences preferences;
    private AppTheme appTheme;

    public AppThemeManager(SharedPreferences preferences) {
        this.preferences = preferences;
        String themeId = preferences.getString(PreferencesConstants.FRAGMENT_THEME, "0");
        appTheme = AppTheme.getTheme(Integer.parseInt(themeId)).getSimpleTheme();
    }

    /**
     * @return The current Application theme
     */
    public AppTheme getAppTheme() {
        return appTheme.getSimpleTheme();
    }

    /**
     * Change the current theme of the application. The change is saved.
     *
     * @param appTheme The new theme
     * @return The theme manager.
     */
    public AppThemeManager setAppTheme(AppTheme appTheme) {
        this.appTheme = appTheme;
        preferences.edit().putString(PreferencesConstants.FRAGMENT_THEME, Integer.toString(appTheme.getId())).apply();
        return this;
    }

}
