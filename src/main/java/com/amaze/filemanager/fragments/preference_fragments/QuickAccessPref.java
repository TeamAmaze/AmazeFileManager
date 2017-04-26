package com.amaze.filemanager.fragments.preference_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.TinyDB;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.TRUE;

/**
 * @author Emmanuel
 *         on 17/4/2017, at 23:17.
 */

public class QuickAccessPref extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    public static final String KEY = "quick access array";
    public static final String[] KEYS = {"fastaccess", "recent", "image", "video", "audio",
            "documents", "apks"};
    public static final Boolean[] DEFAULT = {TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE};
    private static Map<String, Integer> prefPos = new HashMap<>();
    static {
        Map<String, Integer> mem = new HashMap<>();
        for(int i = 0; i < KEYS.length; i++)
            mem.put(KEYS[i], i);
        prefPos = Collections.unmodifiableMap(mem);
    }

    private SharedPreferences preferences;
    private Boolean[] currentValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fastaccess_prefs);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        currentValue = TinyDB.getBooleanArray(preferences, KEY, DEFAULT);

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        currentValue[prefPos.get(preference.getKey())] = ((SwitchPreference) preference).isChecked();
        TinyDB.putBooleanArray(preferences, KEY, currentValue);
        return true;
    }


}
