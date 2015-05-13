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
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppsAdapter extends ArrayAdapter<Layoutelements> {
    Context context;
    List<Layoutelements> items;
    public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();
    AppsList app;
    ArrayList<ApplicationInfo> c = new ArrayList<ApplicationInfo>();

    public AppsAdapter(Context context, int resourceId,
                       List<Layoutelements> items, AppsList app, ArrayList<ApplicationInfo> c) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        this.app = app;
        this.c = c;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
        }
    }

    public void toggleChecked(int position) {
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();

    }

    public void toggleChecked(boolean b) {

        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();


    }

    public List<Integer> getCheckedItemPositions() {
        List<Integer> checkedItemPositions = new ArrayList<Integer>();

        for (int i = 0; i < myChecked.size(); i++) {
            if (myChecked.get(i)) {
                (checkedItemPositions).add(i);
            }
        }

        return checkedItemPositions;
    }

    public boolean areAllChecked() {
        boolean b = true;
        for (int i = 0; i < myChecked.size(); i++) {
            if (!myChecked.get(i)) {
                b = false;
            }
        }
        return b;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        RelativeLayout rl;
        TextView txtDesc;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.rowlayout, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            if (app.theme1!=0)
                vholder.txtTitle.setTextColor(Color.WHITE);
            vholder.imageView = (ImageView) view.findViewById(R.id.bicon);
            vholder.rl = (RelativeLayout) view.findViewById(R.id.second);
            vholder.txtDesc= (TextView) view.findViewById(R.id.date);
            vholder.imageView.setVisibility(View.VISIBLE);
            view.findViewById(R.id.icon).setVisibility(View.GONE);
            view.findViewById(R.id.cicon).setVisibility(View.GONE);
            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.imageView.setImageDrawable(rowItem.getImageId());
        app.ic.cancelLoad(holder.imageView);
        app.ic.loadDrawable(holder.imageView,new File(rowItem.getDesc()),null);

        holder.txtTitle.setText(rowItem.getTitle());
        //	File f = new File(rowItem.getDesc());
        holder.txtDesc.setText(rowItem.getSize());
        holder.rl.setClickable(true);
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if (app.selection == true) {
                    toggleChecked(p);
                    app.mActionMode.invalidate();
                } else {
app.onLongItemClick(p);   }
                // TODO: Implement this method
            }
        });
        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View p1) {
                app.onLongItemClick(p);
                // TODO: Implement this method
                return false;
            }
        });


        Boolean checked = myChecked.get(position);
        if (checked != null) {

            if (checked) {
                holder.rl.setBackgroundColor(Color.parseColor("#5f33b5e5"));
            } else {
                    if (app.theme1 == 0) {

                        holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
                    } else {

                        holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
                    }

            }
        }
        return view;
    }

}
