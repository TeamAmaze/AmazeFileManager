package com.amaze.filemanager.utils.color;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amaze.filemanager.R;

/**
 * Created by Remi Piotaix <remi.piotaix@gmail.com> on 2016-10-15.
 */
public enum ColorUsage {
    PRIMARY("skin", R.color.primary_indigo),
    PRIMARY_TWO("skin_two", R.color.primary_indigo),
    ACCENT("accent_skin", R.color.primary_pink),
    ICON_SKIN("icon_skin", R.color.primary_pink),
    CURRENT_TAB("current_tab", R.color.primary_pink);

    private String usage;
    @ColorRes
    private int defaultColor;

    ColorUsage(@NonNull String value, @ColorRes int defaultColor) {
        this.usage = value;
        this.defaultColor = defaultColor;
    }

    @Nullable
    public static ColorUsage fromString(@NonNull String name) {
        switch (name) {
            case "skin":
                return PRIMARY;
            case "skin_two":
                return PRIMARY_TWO;
            case "accent_skin":
                return ACCENT;
            case "icon_skin":
                return ICON_SKIN;
            case "currrent_tab":
                return CURRENT_TAB;
            default:
                return null;
        }
    }

    public String asString() {
        return usage;
    }

    @ColorRes
    public int getDefaultColor() {
        return defaultColor;
    }

    @NonNull
    public static ColorUsage getPrimary(int num) {
        return num == 1 ? PRIMARY : PRIMARY_TWO;
    }
}
