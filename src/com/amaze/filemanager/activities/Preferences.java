package com.amaze.filemanager.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.amaze.filemanager.R;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class Preferences extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (Sp.getString("theme", "0").equals("1")) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.LightTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefsfrag);
        getActionBar().setIcon(R.drawable.ic_launcher1);
        if(Build.VERSION.SDK_INT>=19) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            // enable status bar tint
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.theme_primary));
        }
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    @Override
    public void onBackPressed() {
        Intent in = new Intent(Preferences.this, MainActivity.class);
        finish();
        startActivity(in);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navigate "up" the demo structure to the launchpad activity.
                // See http://developer.android.com/design/patterns/navigation.html for more.
                finish();
                startActivity(new Intent(this, MainActivity.class));
                return true;


        }
        return true;
    }

}
