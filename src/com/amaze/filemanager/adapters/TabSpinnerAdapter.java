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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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
    String skin;
    public TabSpinnerAdapter(Context context, int resource, ArrayList<String> items, Spinner spin,TabFragment tabFragment) {
        super(context, resource, items);
        this.items = items;
        this.context = context;
        this.spinner=spin;
        this.tabFragment=tabFragment;
        putColors();
        final SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(context);
         skin = sharedPreferences1.getString("skin_color", "#5677fc");
        color=colors.get(skin);
    }

    void putColor(String x,float a,float b,float c){colors.put(x,new Float[]{a,b,c});}
    void putColors(){
        putColor("#e51c23",0.89803922f,0.10980392f,0.1372549f);
        putColor("#e91e63",0.91372549f,0.11764706f,0.38823529f);
        putColor("#9c27b0",0.61176471f,0.15294118f,0.69019608f);
        putColor("#673ab7",0.40392157f,0.22745098f,0.71764706f);
        putColor("#3f51b5",0.24705882f,0.31764706f,0.70980392f);
        putColor("#5677fc",0.3372549f,0.4666666f,0.98823529f);
        putColor("#0288d1",0.007843137f,0.533333f,0.81960784f);
        putColor("#0097a7",0.0f,0.59215686f,0.65490196f);
        putColor("#009688",0.0f,0.58823529f,0.34509804f);
        putColor("#259b24",0.14509804f,0.60784314f,0.14117647f);
        putColor("#8bc34a",0.54509804f,0.76470588f,0.29019608f);
        putColor("#ffa000",1.0f,0.62745098f,0.0f);
        putColor("#f57c00",0.96078431f,0.48627451f,0.0f);
        putColor("#e64a19",0.90196078f,0.29019608f,0.09803922f);
        putColor("#795548",0.4745098f,0.3333f,0.28235294f);
        putColor("#212121",0.12941176f,0.12941176f,0.12941176f);
        putColor("#607d8b",0.37647059f,0.49019608f,0.54509804f);

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
        ImageButton imageButton = (ImageButton) row.findViewById(R.id.spinnerButton);
        if(items.get(position).equals("/"))
            textView.setText(R.string.rootdirectory);
        else
        textView.setText(new File(items.get(position)).getName());
        imageButton.setVisibility(View.VISIBLE);
        if (position == tabFragment.mViewPager.getCurrentItem()) {
            float[] src = {

                    color[0], 0, 0, 0, 0,
                    0, color[1], 0, 0, 0,
                    0, 0,  color[2],0, 0,
                    0, 0, 0, 1, 0
            };
            ColorMatrix colorMatrix = new ColorMatrix(src);
            ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
            imageButton.setColorFilter(colorMatrixColorFilter);
            textView.setTextColor(Color.parseColor(skin));
            textView.setTypeface(null, Typeface.BOLD);

        }else imageButton.setVisibility(View.GONE);

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
