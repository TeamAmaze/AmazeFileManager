package com.amaze.filemanager.utils;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.AppCompatEditText;
import android.widget.EditText;

/**
 * Created by vishal on 20/2/17.
 *
 * Use this class when dealing with {@link android.support.v7.widget.AppCompatEditText}
 * for it's color states for different user interactions
 */

public class EditTextColorStateUtil {

    public static void setTint(EditText editText, int color) {
        if (Build.VERSION.SDK_INT >= 21) return;
        ColorStateList editTextColorStateList = createEditTextColorStateList(color);
        if (editText instanceof AppCompatEditText) {
            ((AppCompatEditText) editText).setSupportBackgroundTintList(editTextColorStateList);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editText.setBackgroundTintList(editTextColorStateList);
        }
    }

    private static ColorStateList createEditTextColorStateList(int color) {
        int[][] states = new int[3][];
        int[] colors = new int[3];
        int i = 0;
        states[i] = new int[]{-android.R.attr.state_enabled};
        colors[i] = Color.parseColor("#f6f6f6");
        i++;
        states[i] = new int[]{-android.R.attr.state_pressed, -android.R.attr.state_focused};
        colors[i] = Color.parseColor("#666666");
        i++;
        states[i] = new int[]{};
        colors[i] = color;
        return new ColorStateList(states, colors);
    }
}
