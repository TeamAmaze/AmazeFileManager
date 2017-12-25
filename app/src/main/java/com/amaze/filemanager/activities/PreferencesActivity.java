/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.fragments.preference_fragments.AdvancedSearchPref;
import com.amaze.filemanager.fragments.preference_fragments.ColorPref;
import com.amaze.filemanager.fragments.preference_fragments.FoldersPref;
import com.amaze.filemanager.fragments.preference_fragments.PrefFrag;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.fragments.preference_fragments.QuickAccessPref;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import static android.os.Build.VERSION.SDK_INT;

public class PreferencesActivity extends ThemedActivity {

    //Start is the first activity you see
    public static final int START_PREFERENCE = 0;
    public static final int COLORS_PREFERENCE = 1;
    public static final int FOLDERS_PREFERENCE = 2;
    public static final int QUICKACCESS_PREFERENCE = 3;
    public static final int ADVANCEDSEARCH_PREFERENCE = 4;

    private boolean restartActivity = false;
    //The preference fragment currently selected
    private int selectedItem = 0;

    private PreferenceFragment currentFragment;

    private static final String KEY_CURRENT_FRAG_OPEN = "current_frag_open";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefsfrag);

        Toolbar toolbar = findViewById(R.id.toolbar);
        invalidateRecentsColorAndIcon();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_HOME_AS_UP | android.support.v7.app.ActionBar.DISPLAY_SHOW_TITLE);
        invalidateToolbarColor();
        invalidateNavBar();

        if (savedInstanceState != null){
            selectedItem = savedInstanceState.getInt(KEY_CURRENT_FRAG_OPEN, 0);
        } else if(getIntent().getExtras() != null) {
            selectItem(getIntent().getExtras().getInt(KEY_CURRENT_FRAG_OPEN));
        } else {
            selectItem(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_FRAG_OPEN, selectedItem);
    }

    @Override
    public void onBackPressed() {
        if(currentFragment instanceof ColorPref) {
            if(((ColorPref) currentFragment).onBackPressed()) return;
        }

        if (selectedItem != START_PREFERENCE && restartActivity) {
            restartActivity(this);
        } else if (selectedItem != START_PREFERENCE) {
            selectItem(START_PREFERENCE);
        } else {
            Intent in = new Intent(PreferencesActivity.this, MainActivity.class);
            in.setAction(Intent.ACTION_MAIN);
            in.setAction(Intent.CATEGORY_LAUNCHER);
            this.startActivity(in);
            this.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(currentFragment.onOptionsItemSelected(item)) return true;

                if (selectedItem != START_PREFERENCE && restartActivity) {
                    restartActivity(this);
                } else if (selectedItem != START_PREFERENCE) {
                    selectItem(START_PREFERENCE);
                } else {
                    Intent in = new Intent(PreferencesActivity.this, MainActivity.class);
                    in.setAction(Intent.ACTION_MAIN);
                    in.setAction(Intent.CATEGORY_LAUNCHER);

                    final int enter_anim = android.R.anim.fade_in;
                    final int exit_anim = android.R.anim.fade_out;
                    Activity activity = this;
                    activity.overridePendingTransition(enter_anim, exit_anim);
                    activity.finish();
                    activity.overridePendingTransition(enter_anim, exit_anim);
                    activity.startActivity(in);
                }
                return true;
        }
        return false;
    }

    public void setRestartActivity() {
        restartActivity = true;
    }

    public boolean getRestartActivity() {
        return restartActivity;
    }

    public void invalidateRecentsColorAndIcon() {
        if (SDK_INT >= 21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze",
                    ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(),
                    getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            setTaskDescription(taskDescription);
        }
    }

    public void invalidateToolbarColor() {
        getSupportActionBar().setBackgroundDrawable(getColorPreference().getDrawable(ColorUsage.getPrimary(MainActivity.currentTab)));
    }

    public void invalidateNavBar() {
        if (SDK_INT == 20 || SDK_INT == 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));

            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.preferences).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (SDK_INT >= 21) {
            SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
            boolean colourednavigation = Sp.getBoolean(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION, true);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int tabStatusColor = PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab)));
            window.setStatusBarColor(tabStatusColor);
            if (colourednavigation) {
                window.setNavigationBarColor(tabStatusColor);
            } else if(window.getNavigationBarColor() != Color.BLACK){
                window.setNavigationBarColor(Color.BLACK);
            }
        }

        if (getAppTheme().equals(AppTheme.BLACK)) getWindow().getDecorView().setBackgroundColor(Utils.getColor(this, android.R.color.black));
    }

    /**
     * This 'elegantly' destroys the activity and recreates it so that the different widgets and texts
     * change their inner states's colors.
     */
    public void restartActivity(final Activity activity) {
        if (activity == null) throw new NullPointerException();

        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        if(selectedItem != START_PREFERENCE) {
            Intent i = activity.getIntent();
            i.putExtra(KEY_CURRENT_FRAG_OPEN, selectedItem);
        }
        activity.startActivity(activity.getIntent());
    }

    /**
     * When a Preference (that requires an independent fragment) is selected this is called.
     * @param item the Preference in question
     */
    public void selectItem(int item) {
        selectedItem = item;
        switch (item) {
            case START_PREFERENCE:
                loadPrefFragment(new PrefFrag(), R.string.setting);
                break;
            case COLORS_PREFERENCE:
                loadPrefFragment(new ColorPref(), R.string.color_title);
                break;
            case FOLDERS_PREFERENCE:
                loadPrefFragment(new FoldersPref(), R.string.sidebarfolders_title);
                break;
            case QUICKACCESS_PREFERENCE:
                loadPrefFragment(new QuickAccessPref(), R.string.sidebarquickaccess_title);
                break;
            case ADVANCEDSEARCH_PREFERENCE:
                loadPrefFragment(new AdvancedSearchPref(), R.string.advanced_search);
                break;
        }
    }

    private void loadPrefFragment(PreferenceFragment fragment, @StringRes int titleBarName) {
        currentFragment = fragment;

        FragmentTransaction t = getFragmentManager().beginTransaction();
        t.replace(R.id.prefsfragment, fragment);
        t.commit();
        getSupportActionBar().setTitle(titleBarName);
    }

}
