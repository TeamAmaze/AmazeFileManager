package com.amaze.filemanager.utils.theme;

import com.afollestad.materialdialogs.Theme;

import java.util.Calendar;

/**
 * This enum represents the theme of the app (LIGHT or DARK)
 */
public enum AppTheme {
    LIGHT(0),
    DARK(1),
    TIMED(2),
    BLACK(3);

    public static final int LIGHT_INDEX = 0;
    public static final int DARK_INDEX = 1;
    public static final int TIME_INDEX = 2;
    public static final int BLACK_INDEX = 3;

    private int id;

    AppTheme(int id) {
        this.id = id;
    }

    /**
     * Returns the correct AppTheme. If index == TIME_INDEX, TIMED is returned.
     *
     * @param index The theme index
     * @return The AppTheme for the given index
     */
    public static AppTheme getTheme(int index) {
        switch (index) {
            default:
            case LIGHT_INDEX:
                return LIGHT;
            case DARK_INDEX:
                return DARK;
            case TIME_INDEX:
                return TIMED;
            case BLACK_INDEX:
                return BLACK;
        }
    }

    /**
     * @return The Theme enum to provide to {@link com.afollestad.materialdialogs.MaterialDialog.Builder}
     */
    public Theme getMaterialDialogTheme() {
        switch (id) {
            default:
            case LIGHT_INDEX:
                return Theme.LIGHT;
            case DARK_INDEX:
            case BLACK_INDEX:
                return Theme.DARK;
            case TIME_INDEX:
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (hour <= 6 || hour >= 18) {
                    return Theme.DARK;
                } else {
                    return Theme.LIGHT;
                }
        }
    }

    /**
     * Returns the correct AppTheme. If index == TIME_INDEX, current time is used to select the theme.
     *
     * @return The AppTheme for the given index
     */
    public AppTheme getSimpleTheme() {
        switch (id) {
            default:
            case LIGHT_INDEX:
                return LIGHT;
            case DARK_INDEX:
                return DARK;
            case TIME_INDEX:
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (hour <= 6 || hour >= 18) {
                    return DARK;
                } else {
                    return LIGHT;
                }
            case BLACK_INDEX:
                return BLACK;
        }
    }

    public int getId() {
        return id;
    }
}
