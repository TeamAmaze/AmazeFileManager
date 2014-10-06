package com.amaze.filemanager.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.stericson.RootTools.RootTools;

public class Preffrag extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final ListPreference ui = (ListPreference) findPreference("uimode");
        int vl = Integer.parseInt(sharedPref.getString("theme", "0"));
        if (vl == 1) {
            ui.setEnabled(false);
        }
        ListPreference th = (ListPreference) findPreference("theme");
        th.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference p1, Object p2) {
                int value = Integer.parseInt(sharedPref.getString("theme", "0"));

                if (value == 0) {
                    sharedPref.edit().putString("uimode", "0").commit();
                    ui.setEnabled(false);
                } else {
                    ui.setEnabled(true);
                }
                restartPC(getActivity());
                // TODO: Implement this method
                return true;
            }
        });

        final Preference preference = (Preference) findPreference("skin");
        final int current = Integer.parseInt(sharedPref.getString("skin", ""+3));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.skin)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setSingleChoiceItems(R.array.skin, current, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sharedPref.edit().putString("skin", ""+i).commit();
                                dialogInterface.cancel();
                                restartPC(getActivity());
                                switch (i) {
                                    case 0:
                                        // Red
                                        sharedPref.edit().putString("skin_color", "#e51c23").commit();
                                        break;
                                    case 1:
                                        // Pink
                                        sharedPref.edit().putString("skin_color", "#e91e63").commit();
                                        break;
                                    case 2:
                                        // Purple
                                        sharedPref.edit().putString("skin_color", "#9c27b0").commit();
                                        break;
                                    case 3:
                                        // Deep Purple
                                        sharedPref.edit().putString("skin_color", "#673ab7").commit();
                                        break;
                                    case 4:
                                        // Indigo
                                        sharedPref.edit().putString("skin_color", "#3f51b5").commit();
                                        break;
                                    case 5:
                                        // Blue
                                        sharedPref.edit().putString("skin_color", "#5677fc").commit();
                                        break;
                                    case 6:
                                        // Light Blue
                                        sharedPref.edit().putString("skin_color", "#03a9f4").commit();
                                        break;
                                    case 7:
                                        // Cyan
                                        sharedPref.edit().putString("skin_color", "#00bcd4").commit();
                                        break;
                                    case 8:
                                        // Teal
                                        sharedPref.edit().putString("skin_color", "#009688").commit();
                                        break;
                                    case 9:
                                        // Green
                                        sharedPref.edit().putString("skin_color", "#259b24").commit();
                                        break;
                                    case 10:
                                        // Light Green
                                        sharedPref.edit().putString("skin_color", "#8bc34a").commit();
                                        break;
                                    case 11:
                                        // Lime
                                        sharedPref.edit().putString("skin_color", "#cddc39").commit();
                                        break;
                                    case 12:
                                        // Yellow
                                        sharedPref.edit().putString("skin_color", "#ffeb3b").commit();
                                        break;
                                    case 13:
                                        // Amber
                                        sharedPref.edit().putString("skin_color", "#ffc107").commit();
                                        break;
                                    case 14:
                                        // Orange
                                        sharedPref.edit().putString("skin_color", "#ff9800").commit();
                                        break;
                                    case 15:
                                        // Deep Orange
                                        sharedPref.edit().putString("skin_color", "#ff5722").commit();
                                        break;
                                    case 16:
                                        // Brown
                                        sharedPref.edit().putString("skin_color", "#795548").commit();
                                        break;
                                    case 17:
                                        // Grey
                                        sharedPref.edit().putString("skin_color", "#9e9e9e").commit();
                                        break;
                                    case 18:
                                        // Blue Grey
                                        sharedPref.edit().putString("skin_color", "#607d8b").commit();
                                        break;
                                }
                            }
                        }).show();
                return false;
            }
        });

        final CheckBoxPreference rootmode = (CheckBoxPreference) findPreference("rootmode");
        rootmode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean b = sharedPref.getBoolean("rootmode", false);
                if (b) {
                    if (RootTools.isAccessGiven()) {
                        rootmode.setChecked(true);
                    
                    } else {  rootmode.setChecked(false);
				
                        Toast.makeText(getActivity(), getResources().getString(R.string.rootfailure), Toast.LENGTH_LONG).show();
                    }
                } else {
                    rootmode.setChecked(false);
                    
                }


                return false;
            }
        });

    }

    public static void restartPC(final Activity activity) {
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
