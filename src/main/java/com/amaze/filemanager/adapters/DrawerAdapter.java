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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.ui.drawer.EntryItem;
import com.amaze.filemanager.ui.drawer.Item;
import com.amaze.filemanager.ui.icons.IconUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class DrawerAdapter extends ArrayAdapter<Item> {
    private final Context context;
    private final ArrayList<Item> values;
    private RelativeLayout l;
    MainActivity m;
    IconUtils icons;
    Float[] color;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    HashMap<String, Float[]> colors = new HashMap<String, Float[]>();

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

    void putColor(String x, float a, float b, float c) {
        colors.put(x, new Float[]{a, b, c});
    }

    void putColors() {
        putColor("#F44336", 0.956862f, 0.2627450f, 0.21176470f);
        putColor("#e91e63", 0.91372549f, 0.11764706f, 0.38823529f);
        putColor("#9c27b0", 0.61176471f, 0.15294118f, 0.69019608f);
        putColor("#673ab7", 0.40392157f, 0.22745098f, 0.71764706f);
        putColor("#3f51b5", 0.24705882f, 0.31764706f, 0.70980392f);
        putColor("#2196F3", 0.12941176f, 0.58823529f, 0.952941176470f);
        putColor("#03A9F4", 0.01176470f, 0.66274509f, 0.9568627450f);
        putColor("#00BCD4", 0.0f, 0.73725490f, 0.831372549f);
        putColor("#009688", 0.0f, 0.58823529f, 0.53333f);
        putColor("#4CAF50", 0.298039f, 0.68627450f, 0.31372549f);
        putColor("#8bc34a", 0.54509804f, 0.76470588f, 0.29019608f);
        putColor("#FFC107", 1.0f, 0.7568627450f, 0.0274509f);
        putColor("#FF9800", 1.0f, 0.596078f, 0.0f);
        putColor("#FF5722", 1.0f, 0.341176470f, 0.1333333f);
        putColor("#795548", 0.4745098f, 0.3333f, 0.28235294f);
        putColor("#212121", 0.12941176f, 0.12941176f, 0.12941176f);
        putColor("#607d8b", 0.37647059f, 0.49019608f, 0.54509804f);
        putColor("#004d40", 0.0f, 0.301960f, 0.250980f);

    }


    private class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        RelativeLayout rl;
    }
    LayoutInflater inflater;
    public DrawerAdapter(Context context, ArrayList<Item> values, MainActivity m, SharedPreferences Sp) {
        super(context, R.layout.drawerrow, values);

        this.context = context;
        this.values = values;

        for (int i = 0; i < values.size(); i++) {
            myChecked.put(i, false);
        }
        icons = new IconUtils(Sp, m);
        this.m = m;
        putColors();
        color = colors.get(m.fabskin);
        if (color == null) {
            color = colors.get("#e91e63");
        }
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (values.get(position).isSection()) {
            ImageView view = new ImageView(context);
            view.setImageResource(R.color.divider);
            view.setClickable(false);
            view.setFocusable(false);
            if(m.theme1==0)
            view.setBackgroundColor(Color.WHITE);
            else view.setBackgroundResource(R.color.background_material_dark);
            view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, m.dpToPx(17)));
            view.setPadding(0, m.dpToPx(8), 0, m.dpToPx(8));
            return view;
        } else {
            View  view = inflater.inflate(R.layout.drawerrow, parent, false);
            final TextView txtTitle=(TextView) view.findViewById(R.id.firstline);
            final ImageView imageView=(ImageView) view.findViewById(R.id.icon);
            if (m.theme1 == 0) {
                view.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {
                view.setBackgroundResource(R.drawable.safr_ripple_black);
            }
            view.setOnClickListener(new View.OnClickListener() {

                public void onClick(View p1) {
                    m.selectItem(position, false);
                }
                // TODO: Implement this method

            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    // not to remove the first bookmark (storage)
                    if (position > m.storage_count) {
                        String path=((EntryItem)getItem(position)).getPath();
                        if (!getItem(position).isSection() && path.startsWith("smb:/")) {
                            m.createSmbDialog(path, true, null);
                            return true;
                        }
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

             txtTitle.setText(((EntryItem) (values.get(position))).getTitle());

             imageView.clearColorFilter();
             imageView.setImageDrawable(getDrawable(position, myChecked.get(position)));
            if (myChecked.get(position)) {
                float[] src = {

                        color[0], 0, 0, 0, 0,
                        0, color[1], 0, 0, 0,
                        0, 0, color[2], 0, 0,
                        0, 0, 0, 1, 0
                };
                ColorMatrix colorMatrix = new ColorMatrix(src);
                ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
                if (m.theme1 == 0)
                   view.setBackgroundColor(Color.parseColor("#ffeeeeee"));
                else view.setBackgroundColor(Color.parseColor("#ff424242"));
                 imageView.setColorFilter(colorMatrixColorFilter);
                //textView.setTypeface(Typeface.DEFAULT_BOLD);
                 txtTitle.setTextColor(Color.parseColor(m.fabskin));

                //if(m.theme1==0)
            } else {
                 imageView.clearColorFilter();
                if (m.theme1 == 0) {
                     txtTitle.setTextColor(m.getResources().getColor(android.R.color.black));
                } else {
                     txtTitle.setTextColor(m.getResources().getColor(android.R.color.white));
                }
            }

            return view;
        }
    }

    Drawable getDrawable(int position,boolean isChecked){
      return   ((EntryItem)getItem(position)).getIcon(m.theme1,isChecked);
    }
}