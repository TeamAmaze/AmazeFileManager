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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.Preferences;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

public class Preffrag extends PreferenceFragment {
    int theme;
    SharedPreferences sharedPref;
    String skin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        skin = sharedPref.getString("skin_color", "#03A9F4");
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        final Preference ui = (Preference) findPreference("uimode");
        int th1 = Integer.parseInt(sharedPref.getString("theme", "0"));
        theme = th1;
        if (th1 == 2) {
            ui.setEnabled(false);
            if(hour<=6 || hour>=18) {
                theme = 1;
            } else
                theme = 0;
        }if(th1==1){ui.setEnabled(false);}
        findPreference("columns").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] sort = getResources().getStringArray(R.array.columns);
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if(theme==1)a.theme(Theme.DARK);
                a.title(R.string.gridcolumnno);
                int current = Integer.parseInt(sharedPref.getString("columns", "0"));
                if(current!=0)current=current-2;
                else current=1;
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("columns", "" + sort[which]).commit();
                        dialog.dismiss();
                    }
                });
                a.build().show();
                return true;
            }
        });
        findPreference("uimode").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] sort = getResources().getStringArray(R.array.uimode);
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if(theme==1)a.theme(Theme.DARK);
                a.title(R.string.directorysort);
                int current = Integer.parseInt(sharedPref.getString("uimode", "0"));
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("uimode", "" + which).commit();
                        dialog.dismiss();
                    }
                });
                a.build().show();
                return true;
            }
        });
        findPreference("dirontop").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] sort = getResources().getStringArray(R.array.directorysortmode);
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if(theme==1)a.theme(Theme.DARK);
                a.title(R.string.directorysort);
                int current = Integer.parseInt(sharedPref.getString("dirontop", "0"));
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("dirontop", "" + which).commit();
                        dialog.dismiss();
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
                if(theme==1)a.theme(Theme.DARK);
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("theme", "" + which).commit();
                        if(which!=0)
                            sharedPref.edit().putString("uimode","0").commit();
                        dialog.dismiss();
                        restartPC(getActivity());}
                });
                a.title(R.string.theme);
                a.build().show();
                return true;
            }
        });
        findPreference("sortby").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] sort = getResources().getStringArray(R.array.sortby);
                int current = Integer.parseInt(sharedPref.getString("sortby", "0"));
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if(theme==1)a.theme(Theme.DARK);
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                        sharedPref.edit().putString("sortby", "" + which).commit();
                        dialog.dismiss();    }
                });
                a.title(R.string.sortby);
                a.build().show();
                return true;
            }
        });
        final Preference preference = (Preference) findPreference("skin");
        final int current = Integer.parseInt(sharedPref.getString("skin", ""+6));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final int current = Integer.parseInt(sharedPref.getString("skin", ""+6));
                final String[] colors = new String[]{
                        "#F44336",
                        "#e91e63",
                        "#9c27b0",
                        "#673ab7",
                        "#3f51b5",
                        "#2196F3",
                        "#03A9F4",
                        "#00BCD4",
                        "#009688",
                        "#4CAF50",
                        "#8bc34a",
                        "#FFC107",
                        "#FF9800",
                        "#FF5722",
                        "#795548",
                        "#212121",
                        "#607d8b",
                        "#004d40"
                };

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.skin)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setPositiveButton(R.string.randomDialog, new DialogInterface.OnClickListener() {
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
                Toast.makeText(getActivity(), R.string.setRandom, Toast.LENGTH_LONG).show();
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

        // Authors
        Preference preference4 = (Preference) findPreference("authors");
        preference4.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                skin = sharedPref.getString("skin_color", "#03A9F4");
                if(theme==1)
                    a.theme(Theme.DARK);

                a.positiveText(R.string.close);
                a.positiveColor(Color.parseColor(skin));
                LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.authors, null);
                a.customView(view);
                a.title(R.string.authors);
                a.callback(new MaterialDialog.Callback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {

                        materialDialog.cancel();
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                    }
                });
                /*a.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });*/
                a.build().show();

                final Intent intent = new Intent(Intent.ACTION_VIEW);

                TextView googlePlus1 = (TextView) view.findViewById(R.id.googlePlus1);
                googlePlus1.setTextColor(Color.parseColor(skin));
                TextView googlePlus2 = (TextView) view.findViewById(R.id.googlePlus2);
                googlePlus2.setTextColor(Color.parseColor(skin));
                TextView git1 = (TextView) view.findViewById(R.id.git1);
                git1.setTextColor(Color.parseColor(skin));
                TextView git2 = (TextView) view.findViewById(R.id.git2);
                git2.setTextColor(Color.parseColor(skin));

                googlePlus1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        intent.setData(Uri.parse("https://plus.google.com/u/0/110424067388738907251/"));
                        startActivity(intent);
                    }
                });
                googlePlus2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        intent.setData(Uri.parse("https://plus.google.com/+VishalNehra/"));
                        startActivity(intent);
                    }
                });
                git1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        intent.setData(Uri.parse("https://github.com/arpitkh96"));
                        startActivity(intent);
                    }
                });
                git2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        intent.setData(Uri.parse("https://github.com/vishal0071"));
                        startActivity(intent);
                    }
                });


                return false;
            }
        });

        // Changelog
        Preference preference1 = (Preference) findPreference("changelog");
        preference1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if(theme==1)a.theme(Theme.DARK);
                a.title(R.string.changelog);
                a.content(Html.fromHtml(getActivity().getString(R.string.changelog_version_3) +
                        getActivity().getString(R.string.changelog_change_3) +
                        getActivity().getString(R.string.changelog_version_2) +
                        getActivity().getString(R.string.changelog_change_2) +
                        getActivity().getString(R.string.changelog_version_1) +
                        getActivity().getString(R.string.changelog_change_1)));
                a.negativeText(R.string.close);
                a.negativeColor(Color.parseColor(skin));
                a.positiveText(R.string.fullChangelog);
                a.positiveColor(Color.parseColor(skin));
                a.callback(new MaterialDialog.Callback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {

                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/arpitkh96/AmazeFileManager/commits/master"));
                        startActivity(intent);
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                        materialDialog.cancel();
                    }
                }).build().show();
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
                        "<ul><li>RootTools.jar</ul></li>" +	//RootTools
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* This file is part of the RootTools Project: http://code.google.com/p/roottools/<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* This code is dual-licensed under the terms of the Apache License Version 2.0 and<br>" +
                        "&nbsp;* the terms of the General Public License (GPL) Version 2.<br>" +
                        "&nbsp;* You may use this code according to either of these licenses as is most appropriate<br>" +
                        "&nbsp;* for your project on a case-by-case basis.<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* The terms of each license can be found in the root directory of this project's repository as well as at:<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.apache.org/licenses/LICENSE-2.0<br>" +
                        "&nbsp;* &nbsp;&nbsp;&nbsp;http://www.gnu.org/licenses/gpl-2.0.txt<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Unless required by applicable law or agreed to in writing, software<br>" +
                        "&nbsp;* distributed under these Licenses is distributed on an \"AS IS\" BASIS,<br>" +
                        "&nbsp;* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
                        "&nbsp;* See each License for the specific language governing permissions and<br>" +
                        "&nbsp;* limitations under that License.<br>" +
                        "&nbsp;*/ " +
                        "<br><br></code></p>" +
                        "<h3>Notices for libraries:</h3> " +
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
                        "<h3>Notices for libraries:</h3> " +
                        "<ul><li>FloatingActionButton</ul></li>" +	//FloatingActionBar
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* The MIT License (MIT)<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Copyright (c) 2014 Oleksandr Melnykov<br>" +
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
                        "<ul><li>Material-ish Progress</ul></li>" +	//progressBar
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* Copyright 2014 Nico Hormazábal<br>" +
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
                        "<h3>Notices for libraries:</h3> " +
                        "<ul><li>Material Dialogs</ul></li>" +	//Material Dialogs
                        "<p style = 'background-color:#eeeeee;padding-left:1em'><code>" +
                        "<br>/*<br>" +
                        "&nbsp;* The MIT License (MIT)<br>" +
                        "&nbsp;*<br>" +
                        "&nbsp;* Copyright (c) 2014 Aidan Michael Follestad<br>" +
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
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback : Amaze File Manager");
                startActivity(intent);
                return false;
            }
        });

        // rate
        Preference preference5 = (Preference) findPreference("rate");
        preference5.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                intent1.setData(Uri.parse("market://details?id=com.amaze.filemanager"));
                startActivity(intent1);
                return false;
            }
        });

        //xda
        Preference preference6 = findPreference("xda");
        preference6.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("http://forum.xda-developers.com/android/apps-games/app-amaze-file-managermaterial-theme-t2937314");
                Intent intent = new Intent();
                intent.setData(uri);
                startActivity(intent);
                return false;
            }
        });

        //go back

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
