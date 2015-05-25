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
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DrawerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private RelativeLayout l;
    MainActivity m;
    Futils futils=new Futils();
    IconUtils icons;
    Float[] color;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    HashMap<String,Float[]> colors=new HashMap<String,Float[]>();

    public void toggleChecked(int position) {
        toggleChecked(false);
        myChecked.put(position, true);
        notifyDataSetChanged();
    }

    public void toggleChecked(boolean b) {

        for (int i = 0; i < values.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
    }
    void putColor(String x,float a,float b,float c){colors.put(x,new Float[]{a,b,c});}
    void putColors(){
        putColor("#F44336",0.956862f,0.2627450f,0.21176470f);
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
    public DrawerAdapter(Context context, ArrayList<String> values, MainActivity m, SharedPreferences Sp) {
        super(context, R.layout.rowlayout, values);

        this.context = context;
        this.values = values;

        for (int i = 0; i < values.size(); i++) {
            myChecked.put(i, false);
        }
        icons = new IconUtils(Sp, m);
        this.m = m;
        putColors();
        color=colors.get(m.fabskin);
        if(color==null) {
            color=colors.get("#e91e63");
        }
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawerrow, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.firstline);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        l = (RelativeLayout) rowView.findViewById(R.id.second);
        if(m.theme1 == 0) {
            l.setBackgroundResource(R.drawable.safr_ripple_white);
        } else {
            l.setBackgroundResource(R.drawable.safr_ripple_black);
        }
        l.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                m.selectItem(position, false);
            }
            // TODO: Implement this method

        });
        l.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                // not to remove the first bookmark (storage)
                if (position>=m.val.size()) {

                    if (m.theme1 == 0)
                        imageView.setImageResource(R.drawable.ic_action_cancel_light);
                    else
                        imageView.setImageResource(R.drawable.ic_action_cancel);
                    imageView.setClickable(true);

                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            m.selectItem(position, true);
                        }
                    });
                }

                // return true to denote no further processing
                return true;
            }
        });

        float[] src = {

                color[0], 0, 0, 0, 0,
                0, color[1], 0, 0, 0,
                0, 0,  color[2],0, 0,
                0, 0, 0, 1, 0
        };
        ColorMatrix colorMatrix = new ColorMatrix(src);
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        if(values.get(position).equals("/storage/emulated/0"))
            textView.setText(futils.getString(context,R.string.storage));
        else if(values.get(position).equals("/")){
            textView.setText(R.string.rootdirectory);
        }
        else {
            textView.setText(new File(values.get(position)).getName());
        }

        if(myChecked.get(position)){
            if(m.theme1==0)
            rowView.setBackgroundColor(Color.parseColor("#ffeeeeee"));
            else rowView.setBackgroundColor(Color.parseColor("#ff424242"));
            imageView.setColorFilter(colorMatrixColorFilter);
            //textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextColor(Color.parseColor(m.fabskin));

            imageView.setImageResource(R.drawable.folder_drawer_white);
            //if(m.theme1==0)
            imageView.setColorFilter(colorMatrixColorFilter);
        }
        else
        {
            if(m.theme1==0) {
                textView.setTextColor(m.getResources().getColor(android.R.color.black));
            }
            else
            {
                textView.setTextColor(m.getResources().getColor(android.R.color.white));
            }
            imageView.setImageResource(R.drawable.folder_drawer);
        }

        return rowView;
    }
}