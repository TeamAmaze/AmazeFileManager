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

public class Preferences extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (Sp.getString("theme", "0").equals("1")) {
            setTheme(android.R.style.Theme_Holo);
        } else {
            setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefsfrag);

        String skin = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("skin_color", "#607d8b");
        getActionBar().setIcon(R.drawable.ic_launcher1);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(skin)));
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
