package com.amaze.filemanager.activities;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.view.*;
import com.amaze.filemanager.*;

public class Preferences extends Activity{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		SharedPreferences Sp=PreferenceManager.getDefaultSharedPreferences(this);
		if(Sp.getString("theme", "0").equals("1")){setTheme(R.style.DarkTheme);}else{setTheme(R.style.LightTheme);}
		super.onCreate(savedInstanceState);
		
	//	getActionBar().setIcon(R.drawable.folder_orange);
		setContentView(R.layout.prefsfrag);
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP|ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE);}
	@Override
	public void onBackPressed(){
		Intent in=new Intent(Preferences.this,MainActivity.class);
		finish();
		startActivity(in);
	}   @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navigate "up" the demo structure to the launchpad activity.
                // See http://developer.android.com/design/patterns/navigation.html for more.
				finish();
                startActivity(new Intent(this, MainActivity.class));
                return true;
				

           
        }return true;}
	
}
