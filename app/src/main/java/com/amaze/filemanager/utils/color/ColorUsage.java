package com.amaze.filemanager.utils.color;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amaze.filemanager.R;

/**
 * A ColorUsage is a key used to index color preferences in {@link ColorPreference}
 */
public enum ColorUsage {
    PRIMARY("skin", R.color.primary_indigo),
    PRIMARY_TWO("skin_two", R.color.primary_indigo),
    ACCENT("accent_skin", R.color.primary_pink),
    ICON_SKIN("icon_skin", R.color.primary_pink),
    CURRENT_TAB("current_tab", R.color.primary_pink);

    /**
     * The String representation of the ColorUsage
     */
    private String usage;
    @ColorRes
    private int defaultColor;

    ColorUsage(@NonNull String value, @ColorRes int defaultColor) {
        this.usage = value;
        this.defaultColor = defaultColor;
    }

    /**
     * @param name The string repersentation of the ColorUsage
     * @return The ColorUsage with the given string representation, null if it does not exist.
     */
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

    /**
     * Syntactic sugar to ease the retrieval of primary colors ColorUsage.
     * If the index is out of bounds, the first primary color is returned as default.
     *
     * @param num The primary color index
     * @return The ColorUsage for the given primary color.
     */
    @NonNull
    public static ColorUsage getPrimary(int num) {
        return num == 1 ? PRIMARY_TWO : PRIMARY;
    }
}
