package com.amaze.filemanager.utils.theme;

import android.content.SharedPreferences;

/**
 * Implements {@link AppThemeManagerInterface} by saving the theme preference via the {@link SharedPreferences} object given at initialization.
 * If an error occurs while loading the theme preference from the {@link SharedPreferences} object, LIGHT is the default.
 */
public class PreferencesAppThemeManager implements AppThemeManagerInterface {
    private SharedPreferences preferences;
    private AppTheme appTheme;

    public PreferencesAppThemeManager(SharedPreferences preferences) {

        this.preferences = preferences;
        appTheme = loadFromPreferences(preferences);
    }

    private AppTheme loadFromPreferences(SharedPreferences preferences) {
        try {
            String themeId = preferences.getString("theme", "0");
            switch (themeId) {
                case "0":
                case "1":
                case "2":
                    return AppTheme.fromIndex(Integer.parseInt(themeId));
                default:
                    return AppTheme.LIGHT;
            }
        } catch (ClassCastException ex) {
            return AppTheme.LIGHT;
        }
    }

    @Override
    public AppTheme getAppTheme() {
        return appTheme;
    }

    @Override
    public AppThemeManagerInterface setAppTheme(AppTheme appTheme) {
        this.appTheme = appTheme;

        return this;
    }

    @Override
    public AppThemeManagerInterface save() {
        preferences.edit()
                   .putString("theme", Integer.toString(appTheme.getId()))
                   .apply();

        return this;
    }
}
