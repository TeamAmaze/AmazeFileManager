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
    LayoutInflater inflater;
    int fabskin;
    public DrawerAdapter(Context context, ArrayList<Item> values, MainActivity m, SharedPreferences Sp) {
        super(context, R.layout.drawerrow, values);

        this.context = context;
        this.values = values;

        for (int i = 0; i < values.size(); i++) {
            myChecked.put(i, false);
        }
        icons = new IconUtils(Sp, m);
        this.m = m;
        fabskin=Color.parseColor(m.fabskin);
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
                        String path = ((EntryItem) getItem(position)).getPath();
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

            imageView.setImageDrawable(getDrawable(position, myChecked.get(position)));
            imageView.setColorFilter(null);
            if (myChecked.get(position)) {
                if (m.theme1 == 0)
                    view.setBackgroundColor(Color.parseColor("#ffeeeeee"));
                else view.setBackgroundColor(Color.parseColor("#ff424242"));
                imageView.setColorFilter(fabskin);
                txtTitle.setTextColor(Color.parseColor(m.fabskin));
            } else {
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
        Drawable drawable=((EntryItem)getItem(position)).getIcon(isChecked);
        return drawable;  }
}