package com.amaze.filemanager.activities.superclasses;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;

/**
 * @author Emmanuel
 *         on 24/8/2017, at 23:13.
 */

public class PreferenceActivity extends BasicActivity {

    private SharedPreferences sharedPrefs;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public SharedPreferences getPrefs() {
        return sharedPrefs;
    }

    public boolean isRootExplorer() {
        return sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
    }

}
