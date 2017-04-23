package com.amaze.filemanager.fragments.preference_fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 22:49.
 */

public class FoldersPref extends NamePathSwitchPref {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (BaseActivity) getActivity();

        preferences = (com.amaze.filemanager.activities.Preferences) getActivity();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.folders_prefs;
    }

}
