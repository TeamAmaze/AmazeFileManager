package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;

import com.amaze.filemanager.R;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Contains useful functions and methods (NOTHING HERE DEALS WITH FILES)
 *
 * @author Emmanuel
 *         on 14/5/2017, at 14:39.
 */

public class Utils {

    private static final SimpleDateFormat DATE_NO_MINUTES = new SimpleDateFormat("MMM dd, yyyy");
    private static final SimpleDateFormat DATE_WITH_MINUTES = new SimpleDateFormat("MMM dd yyyy | KK:mm a");

    //methods for fastscroller
    public static float clamp(float min, float max, float value) {
        float minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    /**
     * TODO
     *
     * @param view
     * @return
     */
    public static float getViewRawY(View view) {
        int[] location = new int[2];
        location[0] = 0;
        location[1] = (int) view.getY();
        ((View) view.getParent()).getLocationInWindow(location);
        return location[1];
    }

    public static void setTint(Context context, CheckBox box, int color) {
        if (Build.VERSION.SDK_INT >= 21) return;
        ColorStateList sl = new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_checked},
                new int[]{android.R.attr.state_checked}
        }, new int[]{getColor(context, R.color.grey), color});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            box.setButtonTintList(sl);
        } else {
            Drawable drawable = DrawableCompat.wrap(ContextCompat.getDrawable(box.getContext(), R.drawable.abc_btn_check_material));
            DrawableCompat.setTintList(drawable, sl);
            box.setButtonDrawable(drawable);
        }
    }

    public String getDate(File f) {
        return getDate(f.lastModified());
    }

    public static String getDate(long f) {
        return DATE_WITH_MINUTES.format(f);
    }

    public static String getDate(long f, String year) {
        String date = DATE_NO_MINUTES.format(f);
        if (date.substring(date.length() - 4, date.length()).equals(year))
            date = date.substring(0, date.length() - 6);
        return date;
    }

    /**
     * TODO
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public boolean isAtleastKitkat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Gets color
     *
     * @param c     Context
     * @param color the resource id for the color
     * @return the color
     */
    public static int getColor(Context c, @ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return c.getColor(color);
        } else {
            return c.getResources().getColor(color);
        }
    }

    public static int dpToPx(int dp, Context c) {
        DisplayMetrics displayMetrics = c.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
