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
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.pkmmte.view.CircularImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends ArrayAdapter<Layoutelements> {
    Context context;
    List<Layoutelements> items;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    Main main;
    Futils utils = new Futils();
    boolean showThumbs;
    ColorMatrixColorFilter colorMatrixColorFilter;
    public MyAdapter(Context context, int resourceId,
                     List<Layoutelements> items, Main main) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        this.main = main;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
        }
        float[] src = {

                main.color[0], 0, 0, 0, 0,
                0, main.color[1], 0, 0, 0,
                0, 0,  main.color[2],0, 0,
                0, 0, 0, 1, 0
        };
        ColorMatrix colorMatrix = new ColorMatrix(src);
        colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);

    }


    public void toggleChecked(int position) {
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();
        if (main.selection == false || main.mActionMode == null) {
            main.selection = true;
            main.mActionMode = main.getActivity().startActionMode(
                    main.mActionModeCallback);
        }
    }

    public void toggleChecked(boolean b) {

        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
    }

    public ArrayList<Integer> getCheckedItemPositions() {
        ArrayList<Integer> checkedItemPositions = new ArrayList<Integer>();

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

    /* private view holder class */
    private class ViewHolder {
        CircularImageView viewmageV;
        ImageView imageView;
        ImageView imageView1;
        TextView txtTitle;
        TextView txtDesc;
        TextView date;
        TextView perm;
        View rl;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);


        if(main.aBoolean){
            View view = convertView;
            final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.rowlayout, parent, false);
            final ViewHolder vholder = new ViewHolder();

            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            vholder.viewmageV=(CircularImageView)view.findViewById(R.id.cicon);
            vholder.imageView=(ImageView)view.findViewById(R.id.icon);
            vholder.rl = view.findViewById(R.id.second);
                vholder.perm = (TextView) view.findViewById(R.id.permis);
                vholder.date = (TextView) view.findViewById(R.id.date);
                vholder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
            view.setTag(vholder);

            GradientDrawable gradientDrawable = (GradientDrawable) vholder.imageView.getBackground();
            gradientDrawable.setColor(Color.parseColor(main.skin));
        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        Boolean checked = myChecked.get(position);
        if (checked != null) {

            if (checked) {
                holder.rl.setBackgroundColor(main.skinselection);
            } else {
                if (main.uimode == 0) {
                    holder.rl.setBackgroundResource(R.drawable.listitem1);
                } else if (main.uimode == 1) {
                    holder.rl.setBackgroundResource(R.drawable.bg_card);
                }
            }
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.onListItemClicked(p, v);
            }
        });

        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View p1) {
                if (main.results) {
                    utils.longClickSearchItem(main, rowItem.getDesc());
                } else if (p!=0) {
                    toggleChecked(p);

                }
                return true;
            }
        });


        holder.txtTitle.setText(rowItem.getTitle());
        holder.imageView.setImageDrawable(rowItem.getImageId());
        holder.imageView.setVisibility(View.VISIBLE);
        holder.viewmageV.setVisibility(View.INVISIBLE);
        if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) {
            holder.imageView.setVisibility(View.INVISIBLE);
            holder.viewmageV.setVisibility(View.VISIBLE);
            holder.viewmageV.setImageDrawable(main.getResources().getDrawable(R.drawable.ic_doc_image));
            main.ic.cancelLoad(holder.viewmageV);
            main.ic.loadDrawable(holder.viewmageV,new File(rowItem.getDesc()),null);
        }
        if (Icons.isApk((rowItem.getDesc()))) {
            main.ic.cancelLoad(holder.imageView);
            main.ic.loadDrawable(holder.imageView,new File(rowItem.getDesc()),main.getResources().getDrawable(R.drawable.ic_doc_apk));
        }
            if(main.showPermissions)
            holder.perm.setText(rowItem.getPermissions());
        if(main.showLastModified)
            holder.date.setText(rowItem.getDate("MMM dd yyyy | KK:mm a"));
        if(main.showSize)
        holder.txtDesc.setText(rowItem.getSize());
            return view;}
        else{   View view;
            final int p = position;
            if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.griditem, parent, false);
            final ViewHolder vholder = new ViewHolder();
            vholder.rl=view.findViewById(R.id.frame);
            vholder.txtTitle = (TextView) view.findViewById(R.id.title);
            vholder.imageView = (ImageView) view.findViewById(R.id.icon_mime);
            vholder.imageView1 = (ImageView) view.findViewById(R.id.icon_thumb);
            vholder.date= (TextView) view.findViewById(R.id.date);
            vholder.txtDesc= (TextView) view.findViewById(R.id.size);
                vholder.perm= (TextView) view.findViewById(R.id.perm);
                if(main.theme==1)view.findViewById(R.id.icon_frame).setBackgroundColor(Color.parseColor("#00000000"));

            view.setTag(vholder);
            }else{ view = convertView;}
            final ViewHolder holder = (ViewHolder) view.getTag();
            Boolean checked = myChecked.get(position);
            if (checked != null) {

                if (checked) {
                    holder.rl.setBackgroundColor(main.skinselection);
                } else {
                    if (main.uimode == 0) {
                        if(main.theme==0)holder.rl.setBackgroundResource(R.drawable.item_doc_grid);
                        else holder.rl.setBackgroundResource(R.drawable.ic_grid_card_background_dark);
                    } else if (main.uimode == 1) {
                        holder.rl.setBackgroundResource(R.drawable.bg_card);
                    }
                }
            }
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, v);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {
                    if (main.results) {
                        utils.longClickSearchItem(main, rowItem.getDesc());
                    } else if (!main.selection) {
                        toggleChecked(p);

                    }
                    return true;
                }
            });
            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView1.setVisibility(View.INVISIBLE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setColorFilter(colorMatrixColorFilter);
            holder.imageView.setImageDrawable(rowItem.getImageId());
            if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) {
                holder.imageView.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if(main.theme==1)holder.imageView1.setBackgroundColor(Color.parseColor("#000000"));
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1,new File(rowItem.getDesc()),null);
            }
            if (Icons.isApk((rowItem.getDesc()))) {
                holder.imageView.setColorFilter(null);
                main.ic.cancelLoad(holder.imageView);
                main.ic.loadDrawable(holder.imageView,new File(rowItem.getDesc()),main.getResources().getDrawable(R.drawable.ic_doc_apk));
            }

            if(main.showLastModified)
                holder.date.setText(rowItem.getDate("dd/MM/yy"));
            if(main.showSize)
                holder.txtDesc.setText(rowItem.getSize());
            if(main.showPermissions)
                holder.perm.setText(rowItem.getPermissions());
            return view;}

    }public int calculatePx(int dp){
        return (int)(dp * (main.getResources().getDisplayMetrics().densityDpi / 160));
    }
}

