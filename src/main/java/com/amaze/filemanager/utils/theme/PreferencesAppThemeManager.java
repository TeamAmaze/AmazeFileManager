package com.amaze.filemanager.utils.theme;

import android.content.SharedPreferences;

public class PreferencesAppThemeManager implements AppThemeManagerInterface {
    private SharedPreferences preferences;
    private AppTheme appTheme;

    public PreferencesAppThemeManager(SharedPreferences preferences) {
        this.preferences = preferences;
        appTheme = loadFromPreferences(preferences);
    }

    private AppTheme loadFromPreferences(SharedPreferences preferences) {
        try {
            String themeId = preferences.getString("appTheme", "0");
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
