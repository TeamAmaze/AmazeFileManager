package com.amaze.filemanager.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by arpitkh996 on 03-03-2016.
 */
public class BaseActivity extends BasicActivity {
    public SharedPreferences sharedPref;

    // Accent and Primary hex color string respectively
    @Deprecated
    public static String accentSkin;
    public static boolean rootMode;
    boolean checkStorage = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // checking if theme should be set light/dark or automatic
        if (sharedPref.getBoolean("random_checkbox", false)) {
            getColorPreference().randomize()
                    .saveToPreferences(sharedPref);
        }

        accentSkin = getColorPreference().getColorAsString(ColorUsage.ACCENT);
        setTheme();

        rootMode = sharedPref.getBoolean(PreferenceUtils.KEY_ROOT, false);

        //requesting storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkStorage)
            if (!checkStoragePermission())
                requestStoragePermission();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataUtils.clear();
    }
    public boolean checkStoragePermission() {

        // Verify that all required contact permissions have been granted.
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.
            final MaterialDialog materialDialog = Futils.showBasicDialog(this, accentSkin, getAppTheme(), new String[]{getResources().getString(R.string.granttext), getResources().getString(R.string.grantper), getResources().getString(R.string.grant), getResources().getString(R.string.cancel), null});
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat
                            .requestPermissions(BaseActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 77);
                    materialDialog.dismiss();
                }
            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            materialDialog.setCancelable(false);
            materialDialog.show();

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 77);
        }
    }

    void setTheme() {
        AppTheme theme = getAppTheme();
        if (Build.VERSION.SDK_INT >= 21) {

            switch (accentSkin.toUpperCase()) {
                case "#F44336":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#E91E63":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9C27B0":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673AB7":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3F51B5":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8BC34A":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607D8B":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004D40":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme.equals(AppTheme.LIGHT)) {
                setTheme(R.style.appCompatLight);
            } else {
                setTheme(R.style.appCompatDark);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTheme();
    }

}