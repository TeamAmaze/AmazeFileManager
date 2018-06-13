package com.amaze.filemanager.activities.superclasses;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.colors.ColorPreferenceHelper;
import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.dialogs.ColorPickerDialog;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.theme.AppTheme;

/**
 * Created by arpitkh996 on 03-03-2016.
 */
public class ThemedActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // checking if theme should be set light/dark or automatic
        int colorPickerPref = getPrefs().getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, ColorPickerDialog.NO_DATA);
        if (colorPickerPref == ColorPickerDialog.RANDOM_INDEX) {
            getColorPreference().saveColorPreferences(getPrefs(), ColorPreferenceHelper.randomize(this));
        }

        setTheme();
    }

    public UserColorPreferences getCurrentColorPreference() {
        return getColorPreference().getCurrentUserColorPreferences(this, getPrefs());
    }

    public @ColorInt int getAccent() {
        return getColorPreference().getCurrentUserColorPreferences(this, getPrefs()).accent;
    }

    void setTheme() {
        AppTheme theme = getAppTheme().getSimpleTheme();
        if (Build.VERSION.SDK_INT >= 21) {

            String stringRepresentation = String.format("#%06X", (0xFFFFFF & getAccent()));

            switch (stringRepresentation.toUpperCase()) {
                case "#F44336":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_red);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#E91E63":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_pink);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9C27B0":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_purple);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673AB7":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3F51B5":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_indigo);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_blue);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_light_blue);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_cyan);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_teal);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_green);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8BC34A":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_light_green);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_amber);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_orange);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_brown);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_black);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607D8B":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004D40":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_super_su);
                    else if (theme.equals(AppTheme.BLACK))
                        setTheme(R.style.pref_accent_black_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme.equals(AppTheme.LIGHT)) {
                setTheme(R.style.appCompatLight);
            } else if (theme.equals(AppTheme.BLACK)) {
                setTheme(R.style.appCompatBlack);
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