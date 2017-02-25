package com.amaze.filemanager.utils.theme;

/**
 * Describes how to change and retrieve the current application theme. {@see AppTheme}
 */
public interface AppThemeManagerInterface {
    /**
     *
     * @return The current Application theme
     */
    AppTheme getAppTheme();

    /**
     * Change the current theme of the application.
     *
     * The change is only done in memory and not saved. Restarting the application will reset to the previously saved AppTheme.
     *
     * @param appTheme The new theme
     * @return The theme manager.
     */
    AppThemeManagerInterface setAppTheme(AppTheme appTheme);

    /**
     * Persists the current value of AppTheme so that it will be reloaded the next time the application starts from scratch.
     *
     * @return The theme manager.
     */
    AppThemeManagerInterface save();
}
