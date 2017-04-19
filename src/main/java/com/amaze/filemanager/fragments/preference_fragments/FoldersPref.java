package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.ui.views.preference.NamePathSwitchPreference;

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

        for(int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++)
            getPreferenceScreen().getPreference(i).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preferences != null) preferences.setChanged();
        if(preference instanceof NamePathSwitchPreference) {
            NamePathSwitchPreference p = (NamePathSwitchPreference) preference;
            switch (p.getLastItemClicked()) {
                case NamePathSwitchPreference.EDIT:
                    break;
                case NamePathSwitchPreference.SWITCH:
                    break;
                case NamePathSwitchPreference.DELETE:
                    break;
            }
        }
        return false;
    }

}
