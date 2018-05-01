package com.amaze.filemanager.ui.colors;

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

public class ColorPreference {
    public static final Integer[] availableColors = {
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
    };
}
