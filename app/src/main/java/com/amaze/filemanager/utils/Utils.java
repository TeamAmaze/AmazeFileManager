package com.amaze.filemanager.utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFileParcelable;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Contains useful functions and methods (NOTHING HERE DEALS WITH FILES)
 *
 * @author Emmanuel
 *         on 14/5/2017, at 14:39.
 */

public class Utils {

    private static final int INDEX_NOT_FOUND = -1;
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

    public static int dpToPx(Context c, int dp) {
        DisplayMetrics displayMetrics = c.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /**
     *   Compares two Strings, and returns the portion where they differ.  (More precisely,
     *   return the remainder of the second String, starting from where it's different from the first.)
     *
     *   For example, difference("i am a machine", "i am a robot") -> "robot".
     *
     *   StringUtils.difference(null, null) = null
     *   StringUtils.difference("", "") = ""
     *   StringUtils.difference("", "abc") = "abc"
     *   StringUtils.difference("abc", "") = ""
     *   StringUtils.difference("abc", "abc") = ""
     *   StringUtils.difference("ab", "abxyz") = "xyz"
     *   StringUtils.difference("abcde", "abxyz") = "xyz"
     *   StringUtils.difference("abcde", "xyz") = "xyz"
     *
     *  @param str1 - the first String, may be null
     *  @param str2 - the second String, may be null
     *  @return the portion of str2 where it differs from str1; returns the empty String if they are equal
     *
     *  Stolen from Apache's StringUtils
     *  (https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/StringUtils.html#difference(java.lang.String,%20java.lang.String))
     */
    public static String differenceStrings(String str1, String str2) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        int at = indexOfDifferenceStrings(str1, str2);
        if (at == INDEX_NOT_FOUND) {
            return "";
        }
        return str2.substring(at);
    }

    private static int indexOfDifferenceStrings(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2) {
            return INDEX_NOT_FOUND;
        }
        if (cs1 == null || cs2 == null) {
            return 0;
        }
        int i;
        for (i = 0; i < cs1.length() && i < cs2.length(); ++i) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                break;
            }
        }
        if (i < cs2.length() || i < cs1.length()) {
            return i;
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * Force disables screen rotation. Useful when we're temporarily in activity because of external intent,
     * and don't have to really deal much with filesystem.
     * @param mainActivity
     */
    public static void disableScreenRotation(MainActivity mainActivity) {
        int screenOrientation = mainActivity.getResources().getConfiguration().orientation;

        if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {

            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {

            mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * Sanitizes input from external application to avoid any attempt of command injection
     * @param input
     * @return
     */
    public static String sanitizeInput(String input) {
        String sanitizedInput = input.replaceAll("[;|[&{2,}][.{3,}]]", "");
        return sanitizedInput;
    }

    /**
     * Returns uri associated to specific basefile
     * @param baseFile
     * @return
     */
    public static Uri getUriForBaseFile(Context context, HybridFileParcelable baseFile) {

        switch (baseFile.getMode()) {
            case FILE:
            case ROOT:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                    return GenericFileProvider.getUriForFile(context, GenericFileProvider.PROVIDER_NAME,
                            new File(baseFile.getPath()));
                } else {
                    return Uri.fromFile(new File(baseFile.getPath()));
                }
            case OTG:
                return OTGUtil.getDocumentFile(baseFile.getPath(), context, true).getUri();
            case SMB:
            case DROPBOX:
            case GDRIVE:
            case ONEDRIVE:
            case BOX:
                Toast.makeText(context, context.getResources().getString(R.string.smb_launch_error),
                        Toast.LENGTH_LONG).show();
                return null;
            default:
                return null;
        }
    }

}
