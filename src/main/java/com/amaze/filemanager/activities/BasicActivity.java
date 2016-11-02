package com.amaze.filemanager.activities;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.color.ColorPreference;

/**
 * Created by rpiotaix on 17/10/16.
 */
public class BasicActivity extends AppCompatActivity implements UtilitiesProviderInterface {
    private boolean initialized = false;
    protected ColorPreference colorPreference;
    private Futils utils;

    private void initialize() {
        utils = new Futils();

        colorPreference = ColorPreference.loadFromPreferences(this, PreferenceManager.getDefaultSharedPreferences(this));
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
}
