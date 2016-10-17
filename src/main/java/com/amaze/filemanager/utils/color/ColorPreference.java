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
 * Created by Remi Piotaix <remi.piotaix@gmail.com> on 2016-10-15.
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

    @Deprecated
    private static int colorIndex(@ColorRes int colorResource) {
        return availableColors.indexOf(colorResource);
    }

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

    public ColorPreference randomize() {
        int[] colorPos = combinations[new Random().nextInt(combinations.length -1)];
        setRes(ColorUsage.PRIMARY, getColorByIndex(colorPos[0]));
        setRes(ColorUsage.PRIMARY_TWO, getColorByIndex(colorPos[0]));
        setRes(ColorUsage.ACCENT, getColorByIndex(colorPos[1]));
        setRes(ColorUsage.ICON_SKIN, getColorByIndex(colorPos[2]));

        return this;
    }

    public ColorPreference saveToPreferences(SharedPreferences preferences) {
        SharedPreferences.Editor e = preferences.edit();
        for (ColorUsage usage : colors.keySet()) {
            e.putInt(usage.asString(), colorIndex(getRes(usage)));
        }

        e.apply();

        Log.d("ColorPreference", "ColorPreference saved to SharedPreferences successfully.");

        return this;
    }

    public ColorPreference setRes(ColorUsage usage, @ColorRes int color) {
        colors.put(usage, color);

        return this;
    }

    @ColorRes
    private int getRes(ColorUsage usage) {
        @ColorRes Integer color = colors.get(usage);

        return color == null ? usage.getDefaultColor() : color;
    }

    @ColorInt
    public int getColor(ColorUsage usage) {
        try {
            return context.getResources().getColor(getRes(usage));
        } catch (Resources.NotFoundException ex) {
            Log.e("ColorPreference", "Color resource not found for " + usage.asString() + ": " + Integer.toString(getRes(usage)));
            return context.getResources().getColor(usage.getDefaultColor());
        }
    }

    public ColorDrawable getDrawable(ColorUsage usage) {
        return new ColorDrawable(getColor(usage));
    }

    @Deprecated
    public String getColorAsString(ColorUsage usage) {
        return String.format("#%06X", (0xFFFFFF & getColor(usage)));
    }

}
