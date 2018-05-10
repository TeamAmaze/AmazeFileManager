package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.PreferencesActivity;
import com.amaze.filemanager.adapters.ColorAdapter;
import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.dialogs.ColorPickerDialog;
import com.amaze.filemanager.ui.views.preference.InvalidablePreferenceCategory;
import com.amaze.filemanager.ui.colors.ColorPreference;

/**
 * This class uses two sections, so that there doesn't need to be two different Fragments.
 * For sections info check switchSections() below.
 *
 * Created by Arpit on 21-06-2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com>
 */
public class ColorPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final int SECTION_0 = 0, SECTION_1 = 1;

    private static final String KEY_PRESELECTED_CONFIGS = "preselectedconfigs";
    private static final String KEY_COLOREDNAV = "colorednavigation";

    private static final String[] PREFERENCE_KEYS_SECTION_0 = {KEY_COLOREDNAV,
            "selectcolorconfig"};
    private static final String[] PREFERENCE_KEYS_SECTION_1 = {KEY_PRESELECTED_CONFIGS,
            PreferencesConstants.PREFERENCE_SKIN, PreferencesConstants.PREFERENCE_SKIN_TWO,
            PreferencesConstants.PREFERENCE_ACCENT, PreferencesConstants.PREFERENCE_ICON_SKIN};

    private static final String KEY_SECTION = "section";

    private int currentSection = SECTION_0;

    private MaterialDialog dialog;
    private SharedPreferences sharedPref;
    private PreferencesActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (PreferencesActivity) getActivity();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(savedInstanceState == null) {
            loadSection0();
            reloadListeners();
        } else {
            onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onPause() {
        if (dialog != null) dialog.dismiss();
        super.onPause();
    }

    /**
     * Deal with the "up" button going to last fragment, instead of section 0.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && currentSection != SECTION_0) {
            switchSections();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onBackPressed() {
        if(currentSection != SECTION_0) {
            switchSections();
            return true;//dealt with click
        } else {
            return false;
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch(preference.getKey()) {
            case KEY_COLOREDNAV:
                activity.invalidateNavBar();
                break;
            case PreferencesConstants.PREFERENCE_SKIN:
            case PreferencesConstants.PREFERENCE_SKIN_TWO:
            case PreferencesConstants.PREFERENCE_ACCENT:
            case PreferencesConstants.PREFERENCE_ICON_SKIN:
                colorChangeDialog(preference.getKey());
                return false;
            case "selectcolorconfig":
                switchSections();
                return true;
        }

        return false;
    }

    private void colorChangeDialog(String colorPrefKey) {
        final UserColorPreferences userColorPreferences = activity.getCurrentColorPreference();
        if (userColorPreferences != null) {
            @ColorInt int currentColor = 0;

            switch(colorPrefKey) {
                case PreferencesConstants.PREFERENCE_SKIN:
                    currentColor = userColorPreferences.primaryFirstTab;
                    break;
                case PreferencesConstants.PREFERENCE_SKIN_TWO:
                    currentColor = userColorPreferences.primarySecondTab;
                    break;
                case PreferencesConstants.PREFERENCE_ACCENT:
                    currentColor = userColorPreferences.accent;
                    break;
                case PreferencesConstants.PREFERENCE_ICON_SKIN:
                    currentColor = userColorPreferences.iconSkin;
                    break;
            }

            ColorAdapter adapter = new ColorAdapter(getActivity(),
                    ColorPreference.availableColors, currentColor,
                    (selectedColor) -> {
                        @ColorInt int primaryFirst = userColorPreferences.primaryFirstTab,
                                primarySecond = userColorPreferences.primarySecondTab,
                                accent = userColorPreferences.accent,
                                iconSkin = userColorPreferences.iconSkin;

                        switch(colorPrefKey) {
                            case PreferencesConstants.PREFERENCE_SKIN:
                                primaryFirst = selectedColor;
                                break;
                            case PreferencesConstants.PREFERENCE_SKIN_TWO:
                                primarySecond = selectedColor;
                                break;
                            case PreferencesConstants.PREFERENCE_ACCENT:
                                accent = selectedColor;
                                break;
                            case PreferencesConstants.PREFERENCE_ICON_SKIN:
                                iconSkin = selectedColor;
                                break;
                        }

                        activity.getColorPreference().saveColorPreferences(sharedPref,
                                new UserColorPreferences(primaryFirst, primarySecond, accent,
                                        iconSkin));
                        if (dialog != null) dialog.dismiss();
                        invalidateEverything();
                    });

            GridView v = (GridView) getActivity().getLayoutInflater().inflate(R.layout.dialog_grid, null);
            v.setAdapter(adapter);
            v.setOnItemClickListener(adapter);

            int fab_skin = activity.getAccent();
            dialog = new MaterialDialog.Builder(getActivity())
                    .positiveText(R.string.cancel)
                    .title(R.string.choose_color)
                    .theme(activity.getAppTheme().getMaterialDialogTheme())
                    .autoDismiss(true)
                    .positiveColor(fab_skin)
                    .neutralColor(fab_skin)
                    .neutralText(R.string.defualt)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            super.onNeutral(dialog);
                            if (activity != null) activity.setRestartActivity();
                            activity.getColorPreference().saveColorPreferences(sharedPref, userColorPreferences);
                            invalidateEverything();
                        }
                    })
                    .customView(v, false)
                    .show();
        }
    }

    private void switchSections() {
        getPreferenceScreen().removeAll();

        if(currentSection == SECTION_0) {
            currentSection = SECTION_1;
            loadSection1();
        } else if(currentSection == SECTION_1) {
            currentSection = SECTION_0;
            loadSection0();
        }

        reloadListeners();
    }

    private void loadSection0() {
        if(((PreferencesActivity) getActivity()).getRestartActivity()) {
            ((PreferencesActivity) getActivity()).restartActivity(getActivity());
        }

        addPreferencesFromResource(R.xml.color_prefs);
        if (Build.VERSION.SDK_INT >= 21) {
            findPreference(KEY_COLOREDNAV).setEnabled(true);
        }
    }

    private void loadSection1() {
        addPreferencesFromResource(R.xml.conficolor_prefs);

        ColorPickerDialog selectedColors = (ColorPickerDialog) findPreference(KEY_PRESELECTED_CONFIGS);
        invalidateColorPreference(selectedColors);
        selectedColors.setColorPreference(activity.getColorPreference(), activity.getCurrentColorPreference(), activity.getAppTheme());
        selectedColors.setListener(() -> {
            if (activity != null) activity.setRestartActivity();
            checkCustomization();
            invalidateEverything();

            int colorPickerPref = sharedPref.getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, ColorPickerDialog.NO_DATA);
            if(colorPickerPref == ColorPickerDialog.RANDOM_INDEX) {
                Toast.makeText(getActivity(), R.string.setRandom, Toast.LENGTH_LONG).show();
            }
        });

        ((InvalidablePreferenceCategory) findPreference("category")).invalidate(activity.getAccent());

        checkCustomization();
    }

    private void checkCustomization() {
        boolean enableCustomization =
                sharedPref.getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, ColorPickerDialog.NO_DATA) == ColorPickerDialog.CUSTOM_INDEX;

        findPreference(PreferencesConstants.PREFERENCE_SKIN).setEnabled(enableCustomization);
        findPreference(PreferencesConstants.PREFERENCE_SKIN_TWO).setEnabled(enableCustomization);
        findPreference(PreferencesConstants.PREFERENCE_ACCENT).setEnabled(enableCustomization);
        findPreference(PreferencesConstants.PREFERENCE_ICON_SKIN).setEnabled(enableCustomization);
    }

    private void reloadListeners() {
        for (final String PREFERENCE_KEY :
                (currentSection == SECTION_0? PREFERENCE_KEYS_SECTION_0:PREFERENCE_KEYS_SECTION_1)) {
            findPreference(PREFERENCE_KEY).setOnPreferenceClickListener(this);
        }
    }

    private void invalidateEverything() {
        activity.invalidateRecentsColorAndIcon();
        activity.invalidateToolbarColor();
        activity.invalidateNavBar();
        if(currentSection == SECTION_1) {
            ColorPickerDialog selectedColors = (ColorPickerDialog) findPreference(KEY_PRESELECTED_CONFIGS);
            if (selectedColors != null) {
                invalidateColorPreference(selectedColors);
                selectedColors.invalidateColors();
            }

            ((InvalidablePreferenceCategory) findPreference("category")).invalidate(activity.getAccent());
        }
    }

    private void invalidateColorPreference(ColorPickerDialog selectedColors) {
        int colorPickerPref = sharedPref.getInt(PreferencesConstants.PREFERENCE_COLOR_CONFIG, ColorPickerDialog.NO_DATA);
        boolean isColor = colorPickerPref != ColorPickerDialog.CUSTOM_INDEX
                && colorPickerPref != ColorPickerDialog.RANDOM_INDEX;

        if(isColor) {
            selectedColors.setColorsVisibility(View.VISIBLE);
            UserColorPreferences userColorPreferences = activity.getCurrentColorPreference();

            selectedColors.setColors(userColorPreferences.primaryFirstTab,
                    userColorPreferences.primarySecondTab, userColorPreferences.accent,
                    userColorPreferences.iconSkin);

            if(activity.getAppTheme().getMaterialDialogTheme() == Theme.LIGHT) {
                selectedColors.setDividerColor(Color.WHITE);
            } else {
                selectedColors.setDividerColor(Color.BLACK);
            }
        } else{
            selectedColors.setColorsVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_SECTION, currentSection);
    }

    private void onRestoreInstanceState(Bundle inState) {
        currentSection = inState.getInt(KEY_SECTION, SECTION_0);
        if(currentSection == SECTION_0) {
            loadSection0();
            reloadListeners();
        } else {
            loadSection1();
            reloadListeners();
        }
    }

}
