package com.amaze.filemanager.fragments.preference_fragments;

import android.preference.PreferenceFragment;
import android.support.annotation.StringRes;

import com.amaze.filemanager.R;

public class PrefFragmentFactory {

    //Start is the first activity you see
    public static final int START_PREFERENCE = 0;
    public static final int COLORS_PREFERENCE = 1;
    public static final int FOLDERS_PREFERENCE = 2;
    public static final int QUICKACCESS_PREFERENCE = 3;
    public static final int ADVANCEDSEARCH_PREFERENCE = 4;

    public PreferenceFragment getPrefFragmentInstance(int item) {
        switch (item) {
            case START_PREFERENCE:
                return new PrefFrag();
            case COLORS_PREFERENCE:
                return new ColorPref();
            case FOLDERS_PREFERENCE:
                return new FoldersPref();
            case QUICKACCESS_PREFERENCE:
                return new QuickAccessPref();
            case ADVANCEDSEARCH_PREFERENCE:
                return new AdvancedSearchPref();
        }
        return null;
    }

    public int getStringTitleInstance(int item) {
        switch (item) {
            case START_PREFERENCE:
                return R.string.setting;
            case COLORS_PREFERENCE:
                return R.string.color_title;
            case FOLDERS_PREFERENCE:
                return R.string.sidebarfolders_title;
            case QUICKACCESS_PREFERENCE:
                return R.string.sidebarquickaccess_title;
            case ADVANCEDSEARCH_PREFERENCE:
                return R.string.advanced_search;
        }
        return 0;
    }
}
