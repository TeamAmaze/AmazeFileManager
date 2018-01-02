package com.amaze.filemanager.utils.color;

import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;

/**
 * A ColorUsage is a key used to index color preferences in {@link ColorPreference}
 */
public enum ColorUsage {
    PRIMARY(PreferencesConstants.PREFERENCE_SKIN, R.color.primary_indigo),
    PRIMARY_TWO(PreferencesConstants.PREFERENCE_SKIN_TWO, R.color.primary_indigo),
    ACCENT(PreferencesConstants.PREFERENCE_ACCENT, R.color.primary_pink),
    ICON_SKIN(PreferencesConstants.PREFERENCE_ICON_SKIN, R.color.primary_pink),
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
            case PreferencesConstants.PREFERENCE_SKIN:
                return PRIMARY;
            case PreferencesConstants.PREFERENCE_SKIN_TWO:
                return PRIMARY_TWO;
            case PreferencesConstants.PREFERENCE_ACCENT:
                return ACCENT;
            case PreferencesConstants.PREFERENCE_ICON_SKIN:
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
