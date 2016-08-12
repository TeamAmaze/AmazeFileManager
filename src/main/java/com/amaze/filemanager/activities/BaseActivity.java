package com.amaze.filemanager.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.stericson.RootTools.RootTools;

import java.io.IOException;

/**
 * Created by arpitkh996 on 03-03-2016.
 */
public class BaseActivity extends AppCompatActivity {
    public static int theme1;
    public SharedPreferences Sp;

    // Accent and Primary hex color string respectively
    public static String accentSkin;
    public static String skin, skinTwo;
    Futils utils;
    boolean  rootmode,checkStorage=true;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sp = PreferenceManager.getDefaultSharedPreferences(this);
        int th = Integer.parseInt(Sp.getString("theme", "0"));
        // checking if theme should be set light/dark or automatic
        theme1 = th == 2 ? PreferenceUtils.hourOfDay() : th;
        utils=new Futils();
        boolean random = Sp.getBoolean("random_checkbox", false);
        if (random)  {

            skin = PreferenceUtils.random(Sp);
            skinTwo = PreferenceUtils.random(Sp);
        } else {

            skin = PreferenceUtils.getPrimaryColorString(Sp);
            skinTwo = PreferenceUtils.getPrimaryTwoColorString(Sp);
        }
        accentSkin = PreferenceUtils.getAccentString(Sp);
        setTheme();
        rootmode = Sp.getBoolean("rootmode", false);
        if (rootmode) {
            if (!RootTools.isAccessGiven()) {
                rootmode = false;
                Sp.edit().putBoolean("rootmode", false).commit();
            }
        }

        //requesting storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkStorage)
            if (!checkStoragePermission())
                requestStoragePermission();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rootmode) {
            try {
                RootTools.closeAllShells();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        DataUtils.clear();
    }
    public boolean checkStoragePermission() {

        // Verify that all required contact permissions have been granted.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.
            final MaterialDialog materialDialog = utils.showBasicDialog(this,accentSkin,theme1, new String[]{getResources().getString(R.string.granttext), getResources().getString(R.string.grantper), getResources().getString(R.string.grant), getResources().getString(R.string.cancel), null});
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
        if (Build.VERSION.SDK_INT >= 21) {

            switch (accentSkin) {
                case "#F44336":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#e91e63":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9c27b0":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673ab7":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3f51b5":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8bc34a":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607d8b":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004d40":
                    if (theme1 == 0)
                        setTheme(R.style.pref_accent_light_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme1 == 1) {
                setTheme(R.style.appCompatDark);
            } else {
                setTheme(R.style.appCompatLight);
            }
        }

    }

}