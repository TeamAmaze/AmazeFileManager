package com.amaze.filemanager.utils.theme;

import android.content.SharedPreferences;

import com.afollestad.materialdialogs.Theme;

import java.util.Calendar;

/**
 * Created by rpiotaix on 01/11/16.
 */

public enum AppTheme {
    LIGHT(0, Theme.LIGHT),
    DARK(1, Theme.DARK);

    private int id;
    private Theme materialDialogTheme;

    public static AppTheme fromPreferences(SharedPreferences preferences) {
        try {
            String themeId = preferences.getString("theme", "0");
            switch (themeId) {
                case "0":
                case "1":
                case "2":
                    return fromIndex(Integer.parseInt(themeId));
                default:
                    return LIGHT;
            }
        } catch (ClassCastException ex) {
            return LIGHT;
        }
    }

    public static AppTheme fromIndex(int index) {
        switch (index) {
            default:
            case 0:
                return LIGHT;
            case 1:
                return DARK;
            case 2:
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour <= 6 || hour >= 18) {
                    return DARK;
                } else {
                    return LIGHT;
                }
        }
    }

    AppTheme(int id, Theme materialDialogTheme) {
        this.id = id;
        this.materialDialogTheme = materialDialogTheme;
    }

    public Theme getMaterialDialogTheme() {
        return materialDialogTheme;
    }

    public int getId() {
        return id;
    }
}
