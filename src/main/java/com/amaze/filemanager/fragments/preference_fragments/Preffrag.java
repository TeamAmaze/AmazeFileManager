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

package com.amaze.filemanager.fragments.preference_fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.AboutActivity;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.ui.views.CheckBx;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

public class Preffrag extends PreferenceFragment{

    private static final CharSequence PREFERENCE_KEY_ABOUT = "about";

    private UtilitiesProviderInterface utilsProvider;

    SharedPreferences sharedPref;
    CheckBx gplus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utilsProvider = (UtilitiesProviderInterface) getActivity();

        PreferenceUtils.reset();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        findPreference("columns").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] sort = getResources().getStringArray(R.array.columns);
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                a.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                a.title(R.string.gridcolumnno);
                int current = Integer.parseInt(sharedPref.getString("columns", "-1"));
                current=current==-1?0:current;
                if(current!=0)current=current-1;
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("columns", "" + (which!=0?sort[which]:""+-1)).commit();
                        dialog.dismiss();
                        return true;
                    }
                });
                a.build().show();
                return true;
            }
        });

        findPreference("theme").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] sort = getResources().getStringArray(R.array.theme);
                int current = Integer.parseInt(sharedPref.getString("theme", "0"));
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
//                a.theme(utilsProvider.getAppTheme().getMaterialDialogTheme());
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        utilsProvider.getThemeManager()
                                     .setAppTheme(AppTheme.fromIndex(which))
                                     .save();

                        Log.d("theme", AppTheme.fromIndex(which).name());

                        dialog.dismiss();
                        restartPC(getActivity());
                        return true;
                    }
                });
                a.title(R.string.theme);
                a.build().show();
                return true;
            }
        });
        findPreference("colors").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((com.amaze.filemanager.activities.Preferences) getActivity()).selectItem(1);
                return true;
            }
        });



        /*final CheckBx rootmode = (CheckBx) findPreference("rootmode");
        rootmode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                boolean b = sharedPref.getBoolean("rootmode", false);
                if (b) {
                    if (MainActivity.shellInteractive.isRunning()) {
                        rootmode.setChecked(true);
                    
                    } else {  rootmode.setChecked(false);
				
                        Toast.makeText(getActivity(), getResources().getString(R.string.rootfailure), Toast.LENGTH_LONG).show();
                    }
                } else {
                    rootmode.setChecked(false);
                    
                }
                return false;
            }
        });*/

        // Feedback
        Preference preference3 = (Preference) findPreference("feedback");
        preference3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","vishalmeham2@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback : Amaze File Manager");
                startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.feedback)));
                return false;
            }
        });

        // About
        Preference aboutPreference = findPreference(PREFERENCE_KEY_ABOUT);
        aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return false;
            }
        });

        // G+
        gplus = (CheckBx) findPreference("plus_pic");
        gplus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(gplus.isChecked()){
                    boolean b=checkGplusPermission();
                    if(!b)requestGplusPermission();
                }
                return false;
            }
        });
        if (BuildConfig.IS_VERSION_FDROID)
            gplus.setEnabled(false);

        // Colored navigation bar
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

    public void invalidateGplus(){
        boolean a=checkGplusPermission();
        if(!a)gplus.setChecked(false);
    }
    public boolean checkGplusPermission() {
        // Verify that all required contact permissions have been granted.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }
    private void requestGplusPermission() {
        final String[] PERMISSIONS = {Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.INTERNET};
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.GET_ACCOUNTS) || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.INTERNET)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.

            String fab_skin = (BaseActivity.accentSkin);
            final MaterialDialog materialDialog = Futils.showBasicDialog(getActivity(), fab_skin, utilsProvider.getAppTheme(), new String[]{getResources().getString(R.string.grantgplus), getResources().getString(R.string.grantper), getResources().getString(R.string.grant), getResources().getString(R.string.cancel), null});
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat
                            .requestPermissions(getActivity(),PERMISSIONS, 66);
                    materialDialog.dismiss();
                }
            });
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
            materialDialog.setCancelable(false);
            materialDialog.show();

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat
                    .requestPermissions(getActivity(), PERMISSIONS, 66);
        }
    }}