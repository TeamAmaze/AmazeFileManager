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
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.preference_fragments.ColorPref;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import static android.os.Build.VERSION.SDK_INT;

public class Preferences extends BaseActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    int select = 0;
    public int changed = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefsfrag);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (SDK_INT >= 21) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription("Amaze",
                    ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap(),
                    getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));
            setTaskDescription(taskDescription);
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_HOME_AS_UP | android.support.v7.app.ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setBackgroundDrawable(getColorPreference().getDrawable(ColorUsage.getPrimary(MainActivity.currentTab)));

        if (SDK_INT == 20 || SDK_INT == 19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab)));

            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.preferences).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        } else if (SDK_INT >= 21) {
            boolean colourednavigation = Sp.getBoolean("colorednavigation", true);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));
            if (colourednavigation)
                window.setNavigationBarColor(PreferenceUtils.getStatusColor(getColorPreference().getColorAsString(ColorUsage.getPrimary(MainActivity.currentTab))));

        }
        selectItem(0);
    }

    @Override
    public void onBackPressed() {
        if (select == 1 && changed == 1)
            restartPC(this);
        else if (select == 1 || select == 2) {
            selectItem(0);
        } else {
            Intent in = new Intent(Preferences.this, MainActivity.class);
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
                // Navigate "up" the demo structure to the launchpad activity.
                if (select == 1 && changed == 1)
                    restartPC(this);
                else if (select == 1) {
                    selectItem(0);
                } else {
                    Intent in = new Intent(Preferences.this, MainActivity.class);
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
        return true;
    }

    public void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }

    Preffrag p;

    public void selectItem(int i) {
        switch (i) {
            case 0:
                p = new Preffrag();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.prefsfragment, p);
                transaction.commit();
                select = 0;
                getSupportActionBar().setTitle(R.string.setting);
                break;
            case 1:
                FragmentTransaction transaction1 = getFragmentManager().beginTransaction();
                transaction1.replace(R.id.prefsfragment, new ColorPref());
                transaction1.commit();
                select = 1;
                getSupportActionBar().setTitle(R.string.color_title);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 66) {
            p.invalidateGplus();
        }

    }
}
