package com.amaze.filemanager.utils.color;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.util.Log;

import com.amaze.filemanager.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The ColorPreference class stores the user's preference for each {@link ColorUsage} and provides tools to:
 * - Access these colors as recource id, interer representation or String representation
 * - Load and save the preferences in a {@link SharedPreferences} object
 */
public class ColorPreference {
    public static final List<Integer> availableColors = Arrays.asList(
            R.color.primary_red,
            R.color.primary_pink,
            R.color.primary_purple,
            R.color.primary_deep_purple,
            R.color.primary_indigo,
            R.color.primary_blue,
            R.color.primary_light_blue,
            R.color.primary_cyan,
            R.color.primary_teal,
            R.color.primary_green,
            R.color.primary_light_green,
            R.color.primary_amber,
            R.color.primary_orange,
            R.color.primary_deep_orange,
            R.color.primary_brown,
            R.color.primary_grey_900,
            R.color.primary_blue_grey,
            R.color.primary_teal_900
    );
    private Map<ColorUsage, Integer> colors;
    private Context context;

    private ColorPreference(Context context) {
        this.context = context;
        this.colors = new HashMap<>();
    }

    /**
     * Compatibility code for use with legacy. Returns the index of the ColorResource given in the availableColors array.
     * Used for storing the color prefererences in the {@link SharedPreferences} object.
     *
     * @param colorResource The resouce
     * @return Its index in the availableColors array.
     */
    @Deprecated
    private static int colorIndex(@ColorRes int colorResource) {
        return availableColors.indexOf(colorResource);
    }

    /**
     * Compatibility code for use zith legacy. Retrieves a ColorResource given its index in the availableColors array.
     * Used to load color preferences from the {@link SharedPreferences} object.
     *
     * @param colorIndex An index
     * @return The color with the given index.
     */
    @Deprecated
    @ColorRes
    private static int getColorByIndex(int colorIndex) {
        return availableColors.get(colorIndex);
    }

    public static ColorPreference loadFromPreferences(Context context, SharedPreferences preferences) {
        ColorPreference cp = new ColorPreference(context);
        boolean prefCorrupted = false;

        for (ColorUsage usage : ColorUsage.values()) {
            try {
                int val = preferences.getInt(usage.asString(), colorIndex(usage.getDefaultColor()));
                cp.setRes(usage, getColorByIndex(val));
            } catch (ClassCastException ex) {
                Log.d("ColorPreference", "Got a ClassCastException while retrieving preference " + usage.asString());
                cp.setRes(usage, usage.getDefaultColor());
                prefCorrupted = true;
            }
        }

        if (prefCorrupted) {
            cp.saveToPreferences(preferences);
        }

        return cp;
    }

    /**
     * Combinations used when randimizing color selection at startup.
     */
    private static int[][] combinations = new int[][]{
            {14, 11, 12},
            {4, 1, 4},
            {8, 12, 8},
            {17, 11, 12},
            {3, 1, 3},
            {16, 14, 16},
            {1, 12, 1},
            {16, 0, 16},
            {0, 12, 0},
            {6, 1, 6},
            {7, 1, 7}
    };

    /**
     * Randomizes (but does not save) the colors used by the interface.
     *
     * @return The {@link ColorPreference} object itself.
     */
    public ColorPreference randomize() {
        int[] colorPos = combinations[new Random().nextInt(combinations.length - 1)];
        setRes(ColorUsage.PRIMARY, getColorByIndex(colorPos[0]));
        setRes(ColorUsage.PRIMARY_TWO, getColorByIndex(colorPos[0]));
        setRes(ColorUsage.ACCENT, getColorByIndex(colorPos[1]));
        setRes(ColorUsage.ICON_SKIN, getColorByIndex(colorPos[2]));

        return this;
    }

    /**
     * Saves the colors preferences in the {@link SharedPreferences} object given.
     *
     * @param preferences The {@link SharedPreferences} object in which the preferences are stored
     * @return The {@link ColorPreference} object itself.
     */
    public ColorPreference saveToPreferences(SharedPreferences preferences) {
        SharedPreferences.Editor e = preferences.edit();
        for (ColorUsage usage : colors.keySet()) {
            e.putInt(usage.asString(), colorIndex(getRes(usage)));
        }

        e.apply();

        Log.d("ColorPreference", "ColorPreference saved to SharedPreferences successfully.");

        return this;
    }

    /**
     * Sets the color resource for the given {@link ColorUsage}
     *
     * @param usage The {@link ColorUsage}
     * @param color The new color
     * @return The {@link ColorPreference} instance itself.
     */
    public ColorPreference setRes(ColorUsage usage, @ColorRes int color) {
        colors.put(usage, color);

        return this;
    }

    /**
     * Get the color resource preference for the given {@link ColorUsage}
     *
     * @param usage The {@link ColorUsage}
     * @return The color resource id.
     */
    @ColorRes
    private int getRes(ColorUsage usage) {
        @ColorRes Integer color = colors.get(usage);

        return color == null ? usage.getDefaultColor() : color;
    }

    /**
     * Get the color preference for the given {@link ColorUsage}
     *
     * @param usage The {@link ColorUsage}
     * @return The color int.
     */
    @ColorInt
    public int getColor(ColorUsage usage) {
        try {
            return context.getResources().getColor(getRes(usage));
        } catch (Resources.NotFoundException ex) {
            Log.e("ColorPreference", "Color resource not found for " + usage.asString() + ": " + Integer.toString(getRes(usage)));
            return context.getResources().getColor(usage.getDefaultColor());
        }
    }

    /**
     * Shortcut to get a {@link ColorDrawable} of the color preference associated with the given {@link ColorUsage}
     *
     * @param usage The {@link ColorUsage}
     * @return The {@link ColorDrawable}
     */
    public ColorDrawable getDrawable(ColorUsage usage) {
        return new ColorDrawable(getColor(usage));
    }

    /**
     * Get the color representation assiciated with the {@link ColorUsage} as a 6-digit Hexadecimal String
     *
     * @param usage The {@link ColorUsage}
     * @return The hex String
     */
    @Deprecated
    public String getColorAsString(ColorUsage usage) {
        return String.format("#%06X", (0xFFFFFF & getColor(usage)));
    }

}
