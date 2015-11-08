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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.utils.HFile;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Vishal on 10/8/2014.
 */
public class TabSpinnerAdapter extends ArrayAdapter<String> {
    ArrayList<String> items;
    Context context;
    Spinner spinner;
    TabFragment tabFragment;
    HashMap<String,Float[]> colors=new HashMap<String,Float[]>();
    Float[] color;
    String fabSkin;
    public TabSpinnerAdapter(Context context, int resource, ArrayList<String> items, Spinner spin,TabFragment tabFragment) {
        super(context, resource, items);
        this.items = items;
        this.context = context;
        this.spinner=spin;
        this.tabFragment=tabFragment;
        final SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(context);
        fabSkin = PreferenceUtils.getFabColor(sharedPreferences1.getInt("fab_skin_color_position", 1));
        color=colors.get(fabSkin);
        if(color==null){color=colors.get("#3f51b5");}
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_layout, parent, false);

        TextView textView = (TextView) row.findViewById(R.id.spinnerText);
        textView.setText(items.get(position));

        return row;
    }

    @Override
    public View getDropDownView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.spinner_dropdown_layout, parent, false);
        final TextView textView = (TextView) row.findViewById(R.id.spinnerText);
        if(tabFragment.theme1==1) {
            row.setBackgroundResource(R.color.holo_dark_background);
            textView.setTextColor(Color.parseColor("#ffffff"));
        }
        textView.setText((items.get(position)));
        if (position == tabFragment.mViewPager.getCurrentItem()) {
            textView.setTextColor(Color.parseColor(fabSkin));
        }        row.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                hideSpinnerDropDown(spinner);

                if (position != tabFragment.mViewPager.getCurrentItem()) {

                    tabFragment.mViewPager.setCurrentItem(position,true);
                }
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

}
