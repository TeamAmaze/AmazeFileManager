package com.amaze.filemanager.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.stericson.RootTools.RootTools;

import java.util.Random;

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

                final String[] colors = new String[]{
                        "#e51c23",
                        "#e91e63",
                        "#9c27b0",
                        "#673ab7",
                        "#3f51b5",
                        "#5677fc",
                        "#0288d1",
                        "#0097a7",
                        "#009688",
                        "#259b24",
                        "#8bc34a",
                        "#ffa000",
                        "#f57c00",
                        "#e64a19",
                        "#795548",
                        "#212121",
                        "#607d8b"
                };

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.skin)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setPositiveButton("Random", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                //sharedPref.edit().putString("skin", "" + i).commit();
                                dialogInterface.cancel();
                                restartPC(getActivity());

                                // Random
                                Random random = new Random();
                                int pos = random.nextInt(colors.length - 1);
                                sharedPref.edit().putString("skin_color", colors[pos]).commit();
                            }
                        })
                        .setSingleChoiceItems(R.array.skin, current, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sharedPref.edit().putString("skin", "" + i).commit();
                                dialogInterface.cancel();
                                restartPC(getActivity());
                                sharedPref.edit().putString("skin_color", colors[i]).apply();

                            }
                        }).show();
                return false;
            }
        });

        final CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("random");
        boolean check = sharedPref.getBoolean("random_checkbox", false);
        checkBoxPreference.setChecked(check);
        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!checkBoxPreference.isChecked()) {
                    sharedPref.edit().putBoolean("random_checkbox", true).apply();
                    checkBoxPreference.setChecked(true);
                } else {
                    sharedPref.edit().putBoolean("random_checkbox", false).apply();
                    checkBoxPreference.setChecked(false);
                }
                Toast.makeText(getActivity(), "Changes will take place after you restart the app", Toast.LENGTH_LONG).show();
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
