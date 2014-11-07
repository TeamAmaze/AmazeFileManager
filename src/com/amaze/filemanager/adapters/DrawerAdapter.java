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
import android.graphics.Typeface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.IconUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class DrawerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    MainActivity m;

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
        color=colors.get(m.skin);
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawerrow, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.firstline);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        LinearLayout l = (LinearLayout) rowView.findViewById(R.id.second);
        l.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                m.selectItem(position);
            }
            // TODO: Implement this method

        });
        float[] src = {

                color[0], 0, 0, 0, 0,
                0, color[1], 0, 0, 0,
                0, 0,  color[2],0, 0,
                0, 0, 0, 1, 0
        };
        ColorMatrix colorMatrix = new ColorMatrix(src);
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);

        textView.setText(values.get(position));
        if (position == values.size() - 1) {
            if (myChecked.get(position)) {
                imageView.setImageResource(R.drawable.ic_action_not_important);
            } else
                imageView.setImageDrawable(icons.getBookDrawable1());
        }else if(position == values.size() - 2){
            if(myChecked.get(position)) {
                imageView.setImageResource(R.drawable.ic_action_view_as_grid);
            } else
                imageView.setImageDrawable(icons.getGridDrawable());
        }else{
            if(myChecked.get(position)){
                imageView.setImageResource(R.drawable.ic_action_sd_storage);
                imageView.setColorFilter(colorMatrixColorFilter);}
            else
                imageView.setImageDrawable(icons.getSdDrawable1());
        }
        if(myChecked.get(position)){
            imageView.setColorFilter(colorMatrixColorFilter);
           textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextColor(Color.parseColor(m.skin));}
        else
        if(m.theme==0)
            textView.setTextColor(m.getResources().getColor(android.R.color.black));
        else     textView.setTextColor(m.getResources().getColor(android.R.color.white));

        return rowView;
    }
}