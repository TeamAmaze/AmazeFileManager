/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.google.android.material.snackbar.Snackbar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageVolume;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * Contains useful functions and methods (NOTHING HERE DEALS WITH FILES)
 *
 * @author Emmanuel on 14/5/2017, at 14:39.
 */
public class Utils {

  private static final int INDEX_NOT_FOUND = -1;
  private static final String INPUT_INTENT_BLACKLIST_COLON = ";";
  private static final String INPUT_INTENT_BLACKLIST_PIPE = "\\|";
  private static final String INPUT_INTENT_BLACKLIST_AMP = "&&";
  private static final String INPUT_INTENT_BLACKLIST_DOTS = "\\.\\.\\.";
  private static final String DATE_TIME_FORMAT = "%s | %s";
  private static final String EMAIL_EMMANUEL = "emmanuelbendavid@gmail.com";
  private static final String EMAIL_RAYMOND = "airwave209gt@gmail.com";
  private static final String EMAIL_VISHAL = "vishalmeham2@gmail.com";
  private static final String URL_TELEGRAM = "https://t.me/AmazeFileManager";

  public static final String EMAIL_NOREPLY_REPORTS = "no-reply@teamamaze.xyz";
  public static final String EMAIL_SUPPORT = "support@teamamaze.xyz";

  // methods for fastscroller
  public static float clamp(float min, float max, float value) {
    float minimum = Math.max(min, value);
    return Math.min(minimum, max);
  }

  public static float getViewRawY(View view) {
    int[] location = new int[2];
    location[0] = 0;
    location[1] = (int) view.getY();
    ((View) view.getParent()).getLocationInWindow(location);
    return location[1];
  }

  public static void setTint(Context context, CheckBox box, int color) {
    if (Build.VERSION.SDK_INT >= 21) return;
    ColorStateList sl =
        new ColorStateList(
            new int[][] {
              new int[] {-android.R.attr.state_checked}, new int[] {android.R.attr.state_checked}
            },
            new int[] {getColor(context, R.color.grey), color});

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      box.setButtonTintList(sl);
    } else {
      Drawable drawable =
          DrawableCompat.wrap(
              ContextCompat.getDrawable(box.getContext(), R.drawable.abc_btn_check_material));
      DrawableCompat.setTintList(drawable, sl);
      box.setButtonDrawable(drawable);
    }
  }

  public static String getDate(@NonNull Context c, long f) {
    return String.format(
        DATE_TIME_FORMAT,
        DateUtils.formatDateTime(c, f, DateUtils.FORMAT_ABBREV_MONTH),
        DateUtils.formatDateTime(c, f, DateUtils.FORMAT_SHOW_TIME));
  }

  /**
   * Gets color
   *
   * @param color the resource id for the color
   * @return the color
   */
  @SuppressWarnings("deprecation")
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
   * Compares two Strings, and returns the portion where they differ. (More precisely, return the
   * remainder of the second String, starting from where it's different from the first.)
   *
   * <p>For example, difference("i am a machine", "i am a robot") -> "robot".
   *
   * <p>StringUtils.difference(null, null) = null StringUtils.difference("", "") = ""
   * StringUtils.difference("", "abc") = "abc" StringUtils.difference("abc", "") = ""
   * StringUtils.difference("abc", "abc") = "" StringUtils.difference("ab", "abxyz") = "xyz"
   * StringUtils.difference("abcde", "abxyz") = "xyz" StringUtils.difference("abcde", "xyz") = "xyz"
   *
   * @param str1 - the first String, may be null
   * @param str2 - the second String, may be null
   * @return the portion of str2 where it differs from str1; returns the empty String if they are
   *     equal
   *     <p>Stolen from Apache's StringUtils
   *     (https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/StringUtils.html#difference(java.lang.String,%20java.lang.String))
   */
  public static String differenceStrings(String str1, String str2) {
    if (str1 == null) return str2;
    if (str2 == null) return str1;

    int at = indexOfDifferenceStrings(str1, str2);

    if (at == INDEX_NOT_FOUND) return "";

    return str2.substring(at);
  }

  private static int indexOfDifferenceStrings(CharSequence cs1, CharSequence cs2) {
    if (cs1 == cs2) return INDEX_NOT_FOUND;
    if (cs1 == null || cs2 == null) return 0;

    int i;
    for (i = 0; i < cs1.length() && i < cs2.length(); ++i) {
      if (cs1.charAt(i) != cs2.charAt(i)) break;
    }

    if (i < cs2.length() || i < cs1.length()) return i;

    return INDEX_NOT_FOUND;
  }

  /**
   * Force disables screen rotation. Useful when we're temporarily in activity because of external
   * intent, and don't have to really deal much with filesystem.
   */
  public static void disableScreenRotation(@NonNull Activity activity) {
    int screenOrientation = activity.getResources().getConfiguration().orientation;

    if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    } else if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
  }

  public static void enableScreenRotation(@NonNull Activity activity) {
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  public static boolean isDeviceInLandScape(Activity activity) {
    return activity.getResources().getConfiguration().orientation
        == Configuration.ORIENTATION_LANDSCAPE;
  }

  /** Sanitizes input from external application to avoid any attempt of command injection */
  public static String sanitizeInput(String input) {
    // iterate through input and keep sanitizing until it's fully injection proof
    String sanitizedInput;
    String sanitizedInputTemp = input;

    while (true) {
      sanitizedInput = sanitizeInputOnce(sanitizedInputTemp);
      if (sanitizedInput.equals(sanitizedInputTemp)) break;
      sanitizedInputTemp = sanitizedInput;
    }

    return sanitizedInput;
  }

  private static String sanitizeInputOnce(String input) {
    return input
        .replaceAll(INPUT_INTENT_BLACKLIST_PIPE, "")
        .replaceAll(INPUT_INTENT_BLACKLIST_AMP, "")
        .replaceAll(INPUT_INTENT_BLACKLIST_DOTS, "")
        .replaceAll(INPUT_INTENT_BLACKLIST_COLON, "");
  }

  /** Returns uri associated to specific basefile */
  public static Uri getUriForBaseFile(
      @NonNull Context context, @NonNull HybridFileParcelable baseFile) {
    switch (baseFile.getMode()) {
      case FILE:
      case ROOT:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          return FileProvider.getUriForFile(
              context, context.getPackageName(), new File(baseFile.getPath()));
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
        Toast.makeText(context, context.getString(R.string.smb_launch_error), Toast.LENGTH_LONG)
            .show();
        return null;
      default:
        return null;
    }
  }

  /**
   * Gets position of nth to last char in String. nthToLastCharIndex(1, "a.tar.gz") = 1
   * nthToLastCharIndex(0, "a.tar.gz") = 5
   */
  public static int nthToLastCharIndex(int elementNumber, String str, char element) {
    if (elementNumber <= 0) throw new IllegalArgumentException();

    int occurencies = 0;
    for (int i = str.length() - 1; i >= 0; i--) {
      if (str.charAt(i) == element && ++occurencies == elementNumber) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Formats input to plain mm:ss format
   *
   * @param timerInSeconds duration in seconds
   * @return time in mm:ss format
   */
  public static String formatTimer(long timerInSeconds) {
    final long min = TimeUnit.SECONDS.toMinutes(timerInSeconds);
    final long sec = TimeUnit.SECONDS.toSeconds(timerInSeconds - TimeUnit.MINUTES.toSeconds(min));
    return String.format("%02d:%02d", min, sec);
  }

  @TargetApi(Build.VERSION_CODES.N)
  public static File getVolumeDirectory(StorageVolume volume) {
    try {
      Field f = StorageVolume.class.getDeclaredField("mPath");
      f.setAccessible(true);
      return (File) f.get(volume);
    } catch (Exception e) {
      // This shouldn't fail, as mPath has been there in every version
      throw new RuntimeException(e);
    }
  }

  public static boolean isNullOrEmpty(final Collection<?> list) {
    return list == null || list.size() == 0;
  }

  public static boolean isNullOrEmpty(final String string) {
    return string == null || string.length() == 0;
  }

  public static Snackbar showThemedSnackbar(
      MainActivity mainActivity,
      CharSequence text,
      int length,
      @StringRes int actionTextId,
      Runnable actionCallback) {
    Snackbar snackbar =
        Snackbar.make(mainActivity.findViewById(R.id.content_frame), text, length)
            .setAction(actionTextId, v -> actionCallback.run());
    if (mainActivity.getAppTheme().equals(AppTheme.LIGHT)) {
      snackbar
          .getView()
          .setBackgroundColor(mainActivity.getResources().getColor(android.R.color.white));
      snackbar.setTextColor(mainActivity.getResources().getColor(android.R.color.black));
    }
    snackbar.show();
    return snackbar;
  }

  public static Snackbar showCutCopySnackBar(
      MainActivity mainActivity,
      CharSequence text,
      int length,
      @StringRes int actionTextId,
      Runnable actionCallback,
      Runnable cancelCallback) {

    final Snackbar snackbar =
        Snackbar.make(mainActivity.findViewById(R.id.content_frame), "", length);

    View customSnackView =
        View.inflate(mainActivity.getApplicationContext(), R.layout.snackbar_view, null);
    snackbar.getView().setBackgroundColor(Color.TRANSPARENT);

    Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
    snackBarLayout.setPadding(0, 0, 0, 0);

    Button actionButton = customSnackView.findViewById(R.id.snackBarActionButton);
    Button cancelButton = customSnackView.findViewById(R.id.snackBarCancelButton);
    TextView textView = customSnackView.findViewById(R.id.snackBarTextTV);

    actionButton.setText(actionTextId);
    textView.setText(text);

    actionButton.setOnClickListener(v -> actionCallback.run());
    cancelButton.setOnClickListener(v -> cancelCallback.run());

    snackBarLayout.addView(customSnackView, 0);

    ((CardView) snackBarLayout.findViewById(R.id.snackBarCardView))
        .setCardBackgroundColor(mainActivity.getAccent());

    snackbar.show();
    return snackbar;
  }

  /**
   * Open url in browser
   *
   * @param url given url
   */
  public static void openURL(String url, Context context) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    context.startActivity(intent);
  }

  /** Open telegram in browser */
  public static void openTelegramURL(Context context) {
    openURL(URL_TELEGRAM, context);
  }

  /**
   * Builds a email intent for amaze feedback
   *
   * @param text email content
   * @param supportMail support mail for given intent
   * @return intent
   */
  public static Intent buildEmailIntent(String text, String supportMail) {
    Intent emailIntent = new Intent(Intent.ACTION_SEND);
    String[] aEmailList = {supportMail};
    String[] aEmailCCList = {EMAIL_VISHAL, EMAIL_EMMANUEL, EMAIL_RAYMOND};
    emailIntent.putExtra(Intent.EXTRA_EMAIL, aEmailList);
    emailIntent.putExtra(Intent.EXTRA_CC, aEmailCCList);
    emailIntent.putExtra(
        Intent.EXTRA_SUBJECT, "Feedback : Amaze File Manager for " + BuildConfig.VERSION_NAME);
    if (!Utils.isNullOrEmpty(text)) {
      emailIntent.putExtra(Intent.EXTRA_TEXT, text);
    }
    emailIntent.setType("message/rfc822");
    return emailIntent;
  }

  public static void zoom(Float scaleX, Float scaleY, PointF pivot, View view) {
    view.setPivotX(pivot.x);
    view.setPivotY(pivot.y);
    view.setScaleX(scaleX);
    view.setScaleY(scaleY);
  }
}
