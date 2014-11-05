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

package com.amaze.filemanager.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
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

        // Changelog
        Preference preference1 = (Preference) findPreference("changelog");
        preference1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(getActivity())
                        .setTitle("Changelog")
                        .setMessage(Html.fromHtml(getActivity().getString(R.string.changelog_version_1) +
                            getActivity().getString(R.string.changelog_change_1)))
                        .setNegativeButton("Full Changelog", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/arpitkh96/AmazeFileManager/commits/master"));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        }).show();
                return false;
            }
        });

        // Open Source Licenses
        Preference preference2 = (Preference) findPreference("os");
        //Defining dialog layout
        final Dialog dialog = new Dialog(getActivity(), android.R.style.Theme_Holo_Light_DialogWhenLarge_NoActionBar);
        //dialog.setTitle("Open-Source Licenses");
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View dialog_view = inflater.inflate(R.layout.open_source_licenses, null);
        dialog.setContentView(dialog_view);

        preference2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                String oss_dialog = "<html><body>" +
                        "<h3>Notices for files:</h3> " +
                        "<ul><li>CircularImageView</ul></li>" +	//CircularImageView
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* The MIT License (MIT)<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Copyright (c) 2014 Pkmmte Xeleon<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Permission is hereby granted, free of charge, to any person obtaining a copy<br>" +
                        "&nbsp;* of this software and associated documentation files (the \"Software\"), to deal<br>" +
                        "&nbsp;* in the Software without restriction, including without limitation the rights<br>" +
                        "&nbsp;* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell<br>" +
                        "&nbsp;* copies of the Software, and to permit persons to whom the Software is<br>" +
                        "&nbsp;* furnished to do so, subject to the following conditions:" +
                        "&nbsp;*<br>" +
                        "&nbsp;* The above copyright notice and this permission notice shall be included in<br>" +
                        "&nbsp;* all copies or substantial portions of the Software.<br>" +
                        "&nbsp;* THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR<br>" +
                        "&nbsp;* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,<br>" +
                        "&nbsp;* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE<br>" +
                        "&nbsp;* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER<br>" +
                        "&nbsp;* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,<br>" +
                        "&nbsp;* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN<br>" +
                        "&nbsp;* THE SOFTWARE.<br>" +
                        "&nbsp;*/ " +
                        "<br><br></code></p>" +
                        "<h3>Notices for files:</h3>" +
                        "<ul><li>nineoldandroids-2.4.0.jar</ul></li>" +	//nineoldandroids
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* Copyright 2012 Jake Wharton<br>" +
                        "&nbsp;* <br>" +
                        "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
                        "&nbsp;* you may not use this file except in compliance with the License.<br>" +
                        "&nbsp;* You may obtain a copy of the License at<br>" +
                        "&nbsp;* <br>" +
                        "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
                        "&nbsp;* <br>" +
                        "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
                        "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
                        "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
                        "&nbsp;* See the License for the specific language governing permissions and<br>" +
                        "&nbsp;* limitations under the License.<br>" +
                        "&nbsp;*/ " +
                        "<br><br></code></p>" +
                        "<h3>Notices for files:</h3> " +
                        "<ul><li>FloatingActionButton</ul></li>" +	//Floating Action Button
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* The MIT License (MIT)<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Copyright (c) 2014 Faiz Malkani<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Permission is hereby granted, free of charge, to any person obtaining a copy<br>" +
                        "&nbsp;* of this software and associated documentation files (the \"Software\"), to deal<br>" +
                        "&nbsp;* in the Software without restriction, including without limitation the rights<br>" +
                        "&nbsp;* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell<br>" +
                        "&nbsp;* copies of the Software, and to permit persons to whom the Software is<br>" +
                        "&nbsp;* furnished to do so, subject to the following conditions:" +
                        "&nbsp;*<br>" +
                        "&nbsp;* The above copyright notice and this permission notice shall be included in<br>" +
                        "&nbsp;* all copies or substantial portions of the Software.<br>" +
                        "&nbsp;* THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR<br>" +
                        "&nbsp;* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,<br>" +
                        "&nbsp;* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE<br>" +
                        "&nbsp;* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER<br>" +
                        "&nbsp;* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,<br>" +
                        "&nbsp;* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN<br>" +
                        "&nbsp;* THE SOFTWARE.<br>" +
                        "&nbsp;*/ " +
                        "<br><br></code></p>" +
                        "<h3>Notices for libraries:</h3>" +
                        "<ul><li>PoppyView</ul></li>" +	//PoppyView
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* Copyright 2013 Flavien Laurent<br>" +
                        "&nbsp;* <br>" +
                        "&nbsp;* Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
                        "&nbsp;* you may not use this file except in compliance with the License.<br>" +
                        "&nbsp;* You may obtain a copy of the License at<br>" +
                        "&nbsp;* <br>" +
                        "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
                        "&nbsp;* <br>" +
                        "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
                        "&nbsp;* distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
                        "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
                        "&nbsp;* See the License for the specific language governing permissions and<br>" +
                        "&nbsp;* limitations under the License.<br>" +
                        "&nbsp;*/ " +
                        "<br><br></code></p>" +
                        "</body></html>";
                WebView wv = (WebView) dialog_view.findViewById(R.id.webView1);
                wv.loadData(oss_dialog, "text/html", null);
                dialog.show();
                return false;
            }
        });

        // Feedback
        Preference preference3 = (Preference) findPreference("feedback");
        preference3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("plain/text");
                intent.setData(Uri.parse("arpitkh96@gmail.com"));
                intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                intent.putExtra(Intent.EXTRA_SUBJECT, "test_subject");
                startActivity(intent);
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
