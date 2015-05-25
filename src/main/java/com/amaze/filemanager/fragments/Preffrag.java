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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.BuildConfig;
import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.stericson.RootTools.RootTools;

import java.util.ArrayList;

public class Preffrag extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    int theme;
    SharedPreferences sharedPref;
    String skin;
    private int COUNT = 0;
    private Toast toast;
    private final String TAG = getClass().getName();
    private String fabSkin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        skin = sharedPref.getString("skin_color", "#3f51b5");
        fabSkin = sharedPref.getString("fab_skin_color", "#e91e63");

        final int th1 = Integer.parseInt(sharedPref.getString("theme", "0"));
        theme = th1==2 ? PreferenceUtils.hourOfDay() : th1;

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
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("columns", "" + sort[which]).commit();
                        dialog.dismiss();
                        return true;
                    }
                });
                a.build().show();
                return true;
            }
        });
        Preference hideModePreference = (Preference) findPreference("hidemode");
        hideModePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] sort = getResources().getStringArray(R.array.hidemode);
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if (theme == 1) a.theme(Theme.DARK);
                a.title(getString(R.string.hide_mode_title));
                int current = sharedPref.getInt("hidemode", 0);
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putInt("hidemode", which).commit();
                        restartPC(getActivity());
                        dialog.dismiss();
                        return true;
                    }
                });
                a.build().show();
                return true;
            }
        });
        switch (sharedPref.getInt("hidemode", 0)) {
            case 0:
                hideModePreference.setSummary(getResources().getString(R.string.hide_mode_nothing));
                break;
            case 1:
                hideModePreference.setSummary(getResources().getString(R.string.hide_mode_toolbar));
                break;
            case 2:
                hideModePreference.setSummary(getResources().getString(R.string.hide_mode_app_bar));
                break;
        }

        if(Build.VERSION.SDK_INT>=21)
        findPreference("colorednavigation").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                restartPC(getActivity());
                return false;
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
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("dirontop", "" + which).commit();
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
                if(theme==1)a.theme(Theme.DARK);
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        sharedPref.edit().putString("theme", "" + which).commit();
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
        findPreference("sortby").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String[] sort = getResources().getStringArray(R.array.sortby);
                int current = Integer.parseInt(sharedPref.getString("sortby", "0"));
                MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
                if (theme == 1) a.theme(Theme.DARK);
                a.items(sort).itemsCallbackSingleChoice(current, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                        sharedPref.edit().putString("sortby", "" + which).commit();
                        dialog.dismiss();
                        return true;
                    }
                });
                a.title(R.string.sortby);
                a.build().show();
                return true;
            }
        });

        findPreference("skin").setOnPreferenceClickListener(this);
        findPreference("fab_skin").setOnPreferenceClickListener(this);
        findPreference("icon_skin").setOnPreferenceClickListener(this);

        final SwitchPreference checkBoxPreference = (SwitchPreference) findPreference("random");
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

        final SwitchPreference rootmode = (SwitchPreference) findPreference("rootmode");
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
                a.positiveColor(Color.parseColor(fabSkin));
                LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.authors, null);
                a.customView(view, true);
                a.title(R.string.authors);
                a.callback(new MaterialDialog.ButtonCallback() {
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

                // icon credits
                TextView textView = (TextView) view.findViewById(R.id.icon_credits);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setLinksClickable(true);
                textView.setText(Html.fromHtml(getActivity().getString(R.string.icon_credits)));

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
                a.content(Html.fromHtml(getActivity().getString(R.string.changelog_version_6) +
                        getActivity().getString(R.string.changelog_change_6) +
                        getActivity().getString(R.string.changelog_version_5) +
                        getActivity().getString(R.string.changelog_change_5) +
                        getActivity().getString(R.string.changelog_version_4) +
                        getActivity().getString(R.string.changelog_change_4) +
                        getActivity().getString(R.string.changelog_version_3) +
                        getActivity().getString(R.string.changelog_change_3) +
                        getActivity().getString(R.string.changelog_version_2) +
                        getActivity().getString(R.string.changelog_change_2) +
                        getActivity().getString(R.string.changelog_version_1) +
                        getActivity().getString(R.string.changelog_change_1)));
                a.negativeText(R.string.close);
                a.negativeColor(Color.parseColor(fabSkin));
                a.positiveText(R.string.fullChangelog);
                a.positiveColor(Color.parseColor(fabSkin));
                a.callback(new MaterialDialog.ButtonCallback() {
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

                WebView wv = (WebView) dialog_view.findViewById(R.id.webView1);
                PreferenceUtils preferenceUtils = new PreferenceUtils();
                wv.loadData(PreferenceUtils.LICENCE_TERMS, "text/html", null);
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

        // studio
        Preference studio = findPreference("studio");
        studio.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                COUNT++;
                if (COUNT >= 5) {
                    if (toast!=null)
                        toast.cancel();
                    toast = Toast.makeText(getActivity(), "Studio Mode : " + COUNT, Toast.LENGTH_SHORT);
                    toast.show();

                    sharedPref.edit().putInt("studio", Integer.parseInt(Integer.toString(COUNT) + "000")).apply();
                } else {
                    sharedPref.edit().putInt("studio", 0).apply();
                }
                return false;
            }
        });

        // G+
        SwitchPreference preference7 = (SwitchPreference) findPreference("plus_pic");
        if (BuildConfig.IS_VERSION_FDROID)
            preference7.setEnabled(false);

        // Colored navigation bar
        SwitchPreference preference8 = (SwitchPreference) findPreference("colorednavigation");
        if (Build.VERSION.SDK_INT >= 21)
            preference8.setEnabled(true);
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

    @Override
    public boolean onPreferenceClick(Preference preference) {

        final MaterialDialog.Builder a = new MaterialDialog.Builder(getActivity());
        a.positiveText(R.string.cancel);
        a.positiveColor(Color.parseColor(fabSkin));
        a.title(R.string.choose_color);
        if(theme==1)
            a.theme(Theme.DARK);
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.list_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setDivider(null);
        a.customView(view, true);
        a.autoDismiss(true);
        MaterialDialog x=a.build();
        ColorAdapter adapter = null;
        ArrayList<String> arrayList = new ArrayList<>();
        for(String c : getResources().getStringArray(R.array.material_primary_color_codes)) {
            arrayList.add(c);
        }
        switch (preference.getKey()) {
            case "skin":
                adapter = new ColorAdapter(getActivity(), arrayList, "skin_color","skin");
                break;
            case "fab_skin":
                adapter = new ColorAdapter(getActivity(), arrayList, "fab_skin_color","skin");
                break;
            case "icon_skin":
                adapter = new ColorAdapter(getActivity(), arrayList, "icon_skin_color","skin");
                break;
        }
        listView.setAdapter(adapter);
        x.show();
        return false;
    }

    class ColorAdapter extends ArrayAdapter<String> {

        String pref,pref1;
        String[] strings;
        final String[] colors = getResources().getStringArray(R.array.material_primary_color_codes);
        boolean restart;
        public ColorAdapter(Context context, ArrayList<String> arrayList, String pref, String pref1) {
            super(context, R.layout.rowlayout, arrayList);
            strings = getResources().getStringArray(R.array.skin);
            this.pref = pref;
            this.pref1 = pref1;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater)getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
            TextView a=(TextView)rowView.findViewById(R.id.firstline);
            a.setText(strings[position]);
            if(theme==1)a.setTextColor(Color.parseColor("#ffffff"));
            ImageView imageView=(ImageView)rowView.findViewById(R.id.icon);
            GradientDrawable gradientDrawable = (GradientDrawable) imageView.getBackground();
            gradientDrawable.setColor(Color.parseColor(colors[position]));
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sharedPref.edit().putString(pref,colors[position]).apply();
                    sharedPref.edit().putString(pref1,""+position).apply();
                    restartPC(getActivity());
                }
            });
            return rowView;
        }
    }
}
