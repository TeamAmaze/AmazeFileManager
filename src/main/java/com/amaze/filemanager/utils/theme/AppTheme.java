package com.amaze.filemanager.utils.theme;

import com.afollestad.materialdialogs.Theme;

import java.util.Calendar;

/**
 * This enum represents the theme of the app (LIGHT or DARK)
 */
public enum AppTheme {
    LIGHT(0, Theme.LIGHT),
    DARK(1, Theme.DARK);

    public static final int LIGHT_INDEX = 0;
    public static final int DARK_INDEX = 1;
    public static final int TIME_INDEX = 2;

    private int id;
    private Theme materialDialogTheme;

    AppTheme(int id, Theme materialDialogTheme) {
        this.id = id;
        this.materialDialogTheme = materialDialogTheme;
    }

    /**
     * Returns the correct AppTheme. If index == TIME_INDEX, current time is used to select the theme.
     *
     * @param index The theme index
     * @return The AppTheme for the given index
     */
    public static AppTheme fromIndex(int index) {
        switch (index) {
            default:
            case LIGHT_INDEX:
                return LIGHT;
            case DARK_INDEX:
                return DARK;
            case TIME_INDEX:
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour <= 6 || hour >= 18) {
                    return DARK;
                } else {
                    return LIGHT;
                }
        }
    }

    /**
     * @return The Theme enum to provide to {@link com.afollestad.materialdialogs.MaterialDialog.Builder}
     */
    public Theme getMaterialDialogTheme() {
        return materialDialogTheme;
    }

    public int getId() {
        return id;
    }
}
