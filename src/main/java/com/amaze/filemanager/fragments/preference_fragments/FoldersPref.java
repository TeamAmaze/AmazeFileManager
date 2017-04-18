package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:49.
 */

public class FoldersPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    SharedPreferences sharedPref;
    com.amaze.filemanager.activities.Preferences preferences;
    BaseActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (BaseActivity) getActivity();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.folders_prefs);
        preferences = (com.amaze.filemanager.activities.Preferences) getActivity();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preferences != null) preferences.setChanged();



        return false;
    }

}
