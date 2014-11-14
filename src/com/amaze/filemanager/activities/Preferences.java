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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.amaze.filemanager.R;

import java.util.Calendar;

public class Preferences extends Activity {
    int theme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);

        int th = Integer.parseInt(Sp.getString("theme", "0"));
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        theme = th;
        if (th == 2) {
            if(hour<=6 || hour>=18) {
                theme = 1;
            } else
                theme = 0;
        }

        if (theme == 1) {
            setTheme(android.R.style.Theme_Holo);
        } else {
            setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefsfrag);

        String skin = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("skin_color", "#5677fc");
        getActionBar().setIcon(R.drawable.ic_launcher1);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    @Override
    public void onBackPressed() {
        Intent in = new Intent(Preferences.this, MainActivity.class);
        in.setAction(Intent.ACTION_MAIN);
        finish();
        startActivity(in);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navigate "up" the demo structure to the launchpad activity.
                // See http://developer.android.com/design/patterns/navigation.html for more.
                Intent in = new Intent(Preferences.this, MainActivity.class);
                in.setAction(Intent.ACTION_MAIN);
                finish();
                startActivity(in);
                return true;


        }
        return true;
    }
}
