package com.amaze.filemanager.fragments;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.preference.*;
import com.amaze.filemanager.*;
public class Preffrag extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
	final	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
	final	EditTextPreference b=(EditTextPreference) findPreference("Ipath");
    final 	ListPreference ui=(ListPreference) findPreference("uimode");
	int vl=Integer.parseInt(sharedPref.getString("theme","0"));
	if(vl==1){ui.setEnabled(false);}
	ListPreference th=(ListPreference) findPreference("theme");
		th.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener(){

				public boolean onPreferenceChange(Preference p1, Object p2)
				{ 
			    int value=Integer.parseInt(	sharedPref.getString("theme","0"));
				
				if(value==0){sharedPref.edit().putString("uimode","0").commit();ui.setEnabled(false);}
				else{ui.setEnabled(true);}
				restartPC(getActivity());
					// TODO: Implement this method
					return true;
				}
			});
final	CheckBoxPreference a=(CheckBoxPreference) findPreference("Ipathset"); 
		b.setEnabled(!a.isChecked());
	
		a.setOnPreferenceChangeListener(new CheckBoxPreference.OnPreferenceChangeListener(){

				public boolean onPreferenceChange(Preference p1, Object p2)
				{
					if(a.isChecked()){b.setEnabled(true);	}
					else{b.setEnabled(false);	sharedPref.edit().putString("Ipath",Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DCIM).commit();}
				// TODO: Implement this method
					return true;
				}
			});
    }public static void restartPC(final Activity activity) {
        if (activity == null)
            return;
        final int enter_anim = android.R.anim.fade_in;
        final int exit_anim = android.R.anim.fade_out;
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.finish();
        activity.overridePendingTransition(enter_anim, exit_anim);
        activity.startActivity(activity.getIntent());
    }
}
