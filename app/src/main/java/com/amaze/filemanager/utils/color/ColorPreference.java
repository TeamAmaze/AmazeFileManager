package com.amaze.filemanager.utils.color;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Utils;

import java.util.ArrayList;
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
            R.color.primary_teal_900,
            R.color.accent_pink,
            R.color.accent_amber,
            R.color.accent_light_blue,
            R.color.accent_light_green
    );

    public static List<Integer> getUniqueAvailableColors(Context context) {
        List<Integer> uniqueAvailableColors = new ArrayList<>();
        List<Integer> tempColorsHex = new ArrayList<>();
        for(Integer color : availableColors) {
            Integer colorHex = ContextCompat.getColor(context, color);
            if(!(tempColorsHex.contains(colorHex))) {
                uniqueAvailableColors.add(color);
            }
            tempColorsHex.add(colorHex);
        }
        return uniqueAvailableColors;
    }

}
