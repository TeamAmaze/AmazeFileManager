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
        putColors();
        final SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(context);
        fabSkin = sharedPreferences1.getString("fab_skin_color", "#e91e63");
        color=colors.get(fabSkin);
        if(color==null){color=colors.get("#3f51b5");}
    }

    void putColor(String x,float a,float b,float c){colors.put(x,new Float[]{a,b,c});}
    void putColors(){putColor("#F44336",0.956862f,0.2627450f,0.21176470f);
        putColor("#e91e63",0.91372549f,0.11764706f,0.38823529f);
        putColor("#9c27b0",0.61176471f,0.15294118f,0.69019608f);
        putColor("#673ab7",0.40392157f,0.22745098f,0.71764706f);
        putColor("#3f51b5",0.24705882f,0.31764706f,0.70980392f);
        putColor("#2196F3",0.12941176f,0.58823529f,0.952941176470f);
        putColor("#03A9F4",0.01176470f,0.66274509f,0.9568627450f);
        putColor("#00BCD4",0.0f,0.73725490f,0.831372549f);
        putColor("#009688",0.0f,0.58823529f,0.53333f);
        putColor("#4CAF50",0.298039f,0.68627450f,0.31372549f);
        putColor("#8bc34a",0.54509804f,0.76470588f,0.29019608f);
        putColor("#FFC107",1.0f,0.7568627450f,0.0274509f);
        putColor("#FF9800",1.0f,0.596078f,0.0f);
        putColor("#FF5722",1.0f,0.341176470f,0.1333333f);
        putColor("#795548",0.4745098f,0.3333f,0.28235294f);
        putColor("#212121",0.12941176f,0.12941176f,0.12941176f);
        putColor("#607d8b",0.37647059f,0.49019608f,0.54509804f);
        putColor("#004d40",0.0f, 0.301960f, 0.250980f);

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
        if(tabFragment.theme1==1) {
            row.setBackgroundResource(R.color.holo_dark_background);
            textView.setTextColor(Color.parseColor("#ffffff"));
        }
        if(items.get(position).equals("/"))
            textView.setText(R.string.rootdirectory);
        else
        textView.setText(new File(items.get(position)).getName());
        if (position == tabFragment.mViewPager.getCurrentItem()) {

            textView.setTextColor(Color.parseColor(fabSkin));
       //     textView.setTypeface(null, Typeface.BOLD);

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
