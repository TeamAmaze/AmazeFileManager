package com.amaze.filemanager.activities;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.color.ColorPreference;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.amaze.filemanager.utils.theme.AppThemeManagerInterface;
import com.amaze.filemanager.utils.theme.PreferencesAppThemeManager;

/**
 * Created by rpiotaix on 17/10/16.
 */
public class BasicActivity extends AppCompatActivity implements UtilitiesProviderInterface {
    private boolean initialized = false;
    protected ColorPreference colorPreference;
    private Futils utils;
    private PreferencesAppThemeManager themeManager;

    private void initialize() {
        SharedPreferences sharedPrefeences = PreferenceManager.getDefaultSharedPreferences(this);

        utils = new Futils();

        colorPreference = ColorPreference.loadFromPreferences(this, sharedPrefeences);
        themeManager = new PreferencesAppThemeManager(sharedPrefeences);
        initialized = true;
    }

    @Override
    public Futils getFutils() {
        if (!initialized)
            initialize();

        return utils;
    }

    public ColorPreference getColorPreference() {
        if (!initialized)
            initialize();

        return colorPreference;
    }

    @Override
    public AppTheme getAppTheme() {
        if (!initialized)
            initialize();

        return themeManager.getAppTheme();
    }

    @Override
    public AppThemeManagerInterface getThemeManager() {
        return themeManager;

    }
}
