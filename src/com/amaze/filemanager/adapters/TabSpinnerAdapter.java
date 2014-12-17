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

package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.database.Tab;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.TabFragment;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by Vishal on 10/8/2014.
 */
public class TabSpinnerAdapter extends ArrayAdapter<String> {
    ArrayList<String> items;
    Context context;
    Spinner spinner;
    TabFragment tabFragment;
    public TabSpinnerAdapter(Context context, int resource, ArrayList<String> items, Spinner spin,TabFragment tabFragment) {
        super(context, resource, items);
        this.items = items;
        this.context = context;
        this.spinner=spin;
        this.tabFragment=tabFragment;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_layout, parent, false);

        TextView textView = (TextView) row.findViewById(R.id.spinnerText);
        try {
            if(items.get(position).equals("/"))
                textView.setText(R.string.rootdirectory);
            else
                textView.setText(new File(items.get(position)).getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return row;
    }

    @Override
    public View getDropDownView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_dropdown_layout, parent, false);

        final TextView textView = (TextView) row.findViewById(R.id.spinnerText);
        LinearLayout linearLayout = (LinearLayout) row.findViewById(R.id.textParent);
        final SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(context);
        String skin = sharedPreferences1.getString("skin_color", "#5677fc");
        ImageButton imageButton = (ImageButton) row.findViewById(R.id.spinnerButton);
        if(items.get(position).equals("/"))
            textView.setText(R.string.rootdirectory);
        else
        textView.setText(new File(items.get(position)).getName());
        imageButton.setBackgroundColor(Color.parseColor(skin));

        if (position == tabFragment.mViewPager.getCurrentItem()) {

            textView.setTextColor(Color.parseColor(skin));
            textView.setTypeface(null, Typeface.BOLD);

        }

        linearLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                hideSpinnerDropDown(spinner);

                if (position == tabFragment.mViewPager.getCurrentItem()) {
                }
                else {
                tabFragment.mViewPager.setCurrentItem(position,true);
                }
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
           if(position==tabFragment.mViewPager.getCurrentItem()) tabFragment.removeTab();
                else Toast.makeText(tabFragment.getActivity(),R.string.not_allowed,Toast.LENGTH_SHORT).show();
                hideSpinnerDropDown(spinner);
            }
        });
        return row;
    }

    public static void hideSpinnerDropDown(Spinner spinner) {
        try {
            Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
            method.setAccessible(true);
            method.invoke(spinner);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
