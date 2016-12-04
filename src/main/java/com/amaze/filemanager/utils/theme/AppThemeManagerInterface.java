package com.amaze.filemanager.utils.theme;

public interface AppThemeManagerInterface {
    AppTheme getAppTheme();

    AppThemeManagerInterface setAppTheme(AppTheme appTheme);

    AppThemeManagerInterface save();
}
