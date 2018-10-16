package com.amaze.filemanager.activities.superclasses;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;

import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_BOOKMARKS_ADDED;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_CHANGEPATHS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_COLORIZE_ICONS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_NEED_TO_SET_HOME;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_ROOTMODE;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_DIVIDERS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_FILE_SIZE;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_GOBACK_BUTTON;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HEADERS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_HIDDENFILES;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_LAST_MODIFIED;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_PERMISSIONS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_FOLDERS;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_SHOW_THUMB;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_TEXTEDITOR_NEWSTACK;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_USE_CIRCULAR_IMAGES;
import static com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants.PREFERENCE_VIEW;

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
        return getBoolean(PREFERENCE_ROOTMODE);
    }

    public boolean getBoolean(String key) {
        boolean defaultValue;

        switch (key) {
            case PREFERENCE_SHOW_PERMISSIONS:
            case PREFERENCE_SHOW_GOBACK_BUTTON:
            case PREFERENCE_SHOW_HIDDENFILES:
            case PREFERENCE_BOOKMARKS_ADDED:
            case PREFERENCE_ROOTMODE:
            case PREFERENCE_COLORED_NAVIGATION:
            case PREFERENCE_TEXTEDITOR_NEWSTACK:
            case PREFERENCE_CHANGEPATHS:
                defaultValue = false;
                break;
            case PREFERENCE_SHOW_FILE_SIZE:
            case PREFERENCE_SHOW_DIVIDERS:
            case PREFERENCE_SHOW_HEADERS:
            case PREFERENCE_USE_CIRCULAR_IMAGES:
            case PREFERENCE_COLORIZE_ICONS:
            case PREFERENCE_SHOW_THUMB:
            case PREFERENCE_SHOW_SIDEBAR_QUICKACCESSES:
            case PREFERENCE_NEED_TO_SET_HOME:
            case PREFERENCE_SHOW_SIDEBAR_FOLDERS:
            case PREFERENCE_VIEW:
            case PREFERENCE_SHOW_LAST_MODIFIED:
                defaultValue = true;
                break;
            default:
                throw new IllegalArgumentException("Please map \'" + key + "\'");
        }

        return sharedPrefs.getBoolean(key, defaultValue);
    }

}
