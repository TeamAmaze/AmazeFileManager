package com.amaze.filemanager.activities;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.color.ColorPreference;

/**
 * Created by rpiotaix on 17/10/16.
 */
public class BasicActivity extends AppCompatActivity implements UtilitiesProviderInterface {
    protected ColorPreference colorPreference;
    private Futils utils;

    @Override
    public Futils getFutils() {
        return utils;
    }

    public ColorPreference getColorPreference() {
        return colorPreference;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Futils();

        colorPreference = ColorPreference.loadFromPreferences(this, PreferenceManager.getDefaultSharedPreferences(this));
    }
}
