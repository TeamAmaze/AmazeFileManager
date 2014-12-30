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
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.MimeTypes;
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
        colorMatrixColorFilter=main.colorMatrixColorFilter;
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
            /*main.mActionMode = main.getActivity().startActionMode(
                    main.mActionModeCallback);*/
            main.mActionMode = main.mainActivity.toolbar.startActionMode(main.mActionModeCallback);
        }
        main.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            main.selection = false;
            main.mActionMode.finish();
            main.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b,String path) {
       int a; if(path.equals("/") || !main.gobackitem)a=0;else a=1;
        for (int i = a; i < items.size(); i++) {
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

    public boolean areAllChecked(String path) {
        boolean b = true;
        int a; if(path.equals("/") || !main.gobackitem)a=0;else a=1;
        for (int i = a; i < myChecked.size(); i++) {
            if (!myChecked.get(i)) {
                b = false;
            }
        }
        return b;
    }

    /* private view holder class */
    private class ViewHolder {
        CircularImageView viewmageV;
        ImageView imageView,apk;
        ImageView imageView1;
        TextView txtTitle;
        TextView txtDesc;
        TextView date;
        TextView perm;
        View rl;
        TextView ext;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);


        if (main.aBoolean) {
            View view = convertView;
            final int p = position;
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater) context
                        .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                view = mInflater.inflate(R.layout.rowlayout, parent, false);
                final ViewHolder vholder = new ViewHolder();

                vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
                if (main.theme1==1)
                    vholder.txtTitle.setTextColor(getContext().getResources().getColor(android.R.color.white));
                vholder.viewmageV = (CircularImageView) view.findViewById(R.id.cicon);
                vholder.imageView = (ImageView) view.findViewById(R.id.icon);
                vholder.rl = view.findViewById(R.id.second);
                vholder.perm = (TextView) view.findViewById(R.id.permis);
                vholder.date = (TextView) view.findViewById(R.id.date);
                vholder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
                vholder.apk = (ImageView) view.findViewById(R.id.bicon);
                vholder.ext = (TextView) view.findViewById(R.id.generictext);
                view.setTag(vholder);

            }
            final ViewHolder holder = (ViewHolder) view.getTag();
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, v);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {
                    if (!rowItem.getSize().equals(main.goback)) {

                        toggleChecked(p);
                    }

                    return true;
                }
            });


            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView.setImageDrawable(rowItem.getImageId());
            holder.ext.setText("");

            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Animation animation = AnimationUtils.loadAnimation(context, R.anim.holder_anim);
                    if (!rowItem.getSize().equals(main.goback)) {
                        holder.imageView.setAnimation(animation);
                        toggleChecked(p);
                    } else main.goBack();

                }
            });
            holder.viewmageV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback))
                        toggleChecked(p);
                    else main.goBack();
                }
            });
            holder.apk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!rowItem.getSize().equals(main.goback))
                        toggleChecked(p);
                    else main.goBack();
                }
            });
            holder.imageView.setVisibility(View.VISIBLE);
            holder.viewmageV.setVisibility(View.INVISIBLE);
            if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) {
                if (main.showThumbs) {
                    if (main.circularImages) {
                        holder.imageView.setVisibility(View.GONE);
                        holder.viewmageV.setVisibility(View.VISIBLE);
                        holder.viewmageV.setImageDrawable(main.darkimage);
                        main.ic.cancelLoad(holder.viewmageV);
                        main.ic.loadDrawable(holder.viewmageV, new File(rowItem.getDesc()), null);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.VISIBLE);
                        holder.apk.setImageDrawable(main.darkimage);
                        main.ic.cancelLoad(holder.apk);
                        main.ic.loadDrawable(holder.apk, new File(rowItem.getDesc()), null);
                    }
                }
            } else if (Icons.isApk((rowItem.getDesc()))) {
                if (main.showThumbs) {
                    holder.imageView.setVisibility(View.GONE);
                    holder.apk.setVisibility(View.VISIBLE);
                    holder.apk.setImageDrawable(main.apk);
                    main.ic.cancelLoad(holder.apk);
                    main.ic.loadDrawable(holder.apk, new File(rowItem.getDesc()), null);
                }

            } else if (Icons.isVideo(rowItem.getDesc())) {
                if (main.showThumbs) {
                    if (main.circularImages) {
                        holder.imageView.setVisibility(View.GONE);
                        holder.viewmageV.setVisibility(View.VISIBLE);
                        holder.viewmageV.setImageDrawable(main.darkvideo);
                        main.ic.cancelLoad(holder.viewmageV);
                        main.ic.loadDrawable(holder.viewmageV, new File(rowItem.getDesc()), null);
                    } else {
                        holder.imageView.setVisibility(View.GONE);
                        holder.apk.setVisibility(View.VISIBLE);
                        holder.apk.setImageDrawable(main.darkvideo);
                        main.ic.cancelLoad(holder.apk);
                        main.ic.loadDrawable(holder.apk, new File(rowItem.getDesc()), null);
                    }
                }
            }
        else{holder.viewmageV.setVisibility(View.GONE);
            holder.apk.setVisibility(View.GONE);}
            Boolean checked = myChecked.get(position);
            if (checked != null) {

                if (checked) {
                    holder.imageView.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                    GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
                    gradientDrawable.setColor(Color.parseColor("#757575"));
                    holder.ext.setText("");
                    if (Build.VERSION.SDK_INT >= 21) {

                        if (main.theme1==1)
                            holder.rl.setBackgroundColor(getContext().getResources().getColor(android.R.color.black));
                        else
                            holder.rl.setBackgroundColor(getContext().getResources().getColor(android.R.color.white));
                        holder.rl.setElevation(10f);
                    }
                    else
                        holder.rl.setBackgroundColor(main.skinselection);
                } else {

                    GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
                    if(main.coloriseIcons) {
                        if (rowItem.isDirectory(main.rootMode))
                            gradientDrawable.setColor(Color.parseColor(main.skin));
                        else if (Icons.isVideo(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#f06292"));
                        else if (Icons.isAudio(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#9575cd"));
                        else if (Icons.isPdf(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#da4336"));
                        else if (Icons.isCode(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#00bfa5"));
                        else if (Icons.isText(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#e06055"));
                        else if (Icons.isArchive(rowItem.getDesc()))
                            gradientDrawable.setColor(Color.parseColor("#f9a825"));
                        else if (Icons.isgeneric(rowItem.getDesc()))
                        {
                            if (rowItem.getSize().equals(main.goback))
                                gradientDrawable.setColor(Color.parseColor("#757575"));
                            else{
                                gradientDrawable.setColor(Color.parseColor("#9e9e9e"));
                                String ext=MimeTypes.getExtension(rowItem.getDesc());
                                if(ext!=null && ext.trim().length()!=0){
                                    holder.ext.setText(ext);
                                    holder.imageView.setImageDrawable(null);
                                }}
                    }else gradientDrawable.setColor(Color.parseColor(main.skin));
                    }else gradientDrawable.setColor(Color.parseColor(main.skin));
                    if (main.uimode == 0) {
                        holder.rl.setBackgroundResource(R.drawable.listitem1);
                    } else if (main.uimode == 1) {
                        holder.rl.setBackgroundResource(R.drawable.bg_card);
                    }
                }
            }
            if (main.showPermissions)
                holder.perm.setText(rowItem.getPermissions());
            if (main.showLastModified)
                holder.date.setText(rowItem.getDate("MMM dd, yyyy",main.year));
            String size=rowItem.getSize();

            if(size.equals(main.goback)){

                holder.date.setText(size);

                holder.txtDesc.setText("");}

            else if(main.showSize)

                holder.txtDesc.setText(rowItem.getSize());
            return view;
        } else{   View view;
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
            if(main.theme1==1) {
                view.findViewById(R.id.icon_frame).setBackgroundColor(Color.parseColor("#00000000"));
                vholder.txtTitle.setTextColor(Color.parseColor("#ffffff"));
                vholder.perm.setTextColor(Color.parseColor("#ffffff"));
            }

            view.setTag(vholder);
            }else{ view = convertView;}
            final ViewHolder holder = (ViewHolder) view.getTag();
            Boolean checked = myChecked.get(position);
            holder.rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    main.onListItemClicked(p, v);
                }
            });

            holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View p1) {
                    if (!rowItem.getSize().equals(main.goback)) {

                        toggleChecked(p);
                    } return true;
                }
            });
            holder.txtTitle.setText(rowItem.getTitle());
            holder.imageView1.setVisibility(View.INVISIBLE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageDrawable(rowItem.getImageId());
            if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) {
                    holder.imageView.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if(main.theme==1)holder.imageView1.setBackgroundColor(Color.parseColor("#000000"));
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1,new File(rowItem.getDesc()),null);
            }
            else if (Icons.isApk((rowItem.getDesc()))) {
                holder.imageView.setColorFilter(null);
                main.ic.cancelLoad(holder.imageView);
                main.ic.loadDrawable(holder.imageView,new File(rowItem.getDesc()),null);
            } if(Icons.isVideo(rowItem.getDesc())){
                holder.imageView.setColorFilter(null);
                holder.imageView1.setVisibility(View.VISIBLE);
                holder.imageView1.setImageDrawable(null);
                if(main.theme==1)holder.imageView1.setBackgroundColor(Color.parseColor("#000000"));
                main.ic.cancelLoad(holder.imageView1);
                main.ic.loadDrawable(holder.imageView1,new File(rowItem.getDesc()),null);

            }
            if(main.coloriseIcons){

                if(rowItem.isDirectory(main.rootMode))holder.imageView.setColorFilter(Color.parseColor(main.skin));

                else if(Icons.isVideo(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#f06292"));

                else if(Icons.isAudio(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#9575cd"));

                else if(Icons.isPdf(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#da4336"));

                else if(Icons.isCode(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#00bfa5"));

                else if(Icons.isText(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#e06055"));

                else if(Icons.isArchive(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#f9a825"));

                else if(Icons.isgeneric(rowItem.getDesc()))holder.imageView.setColorFilter(Color.parseColor("#9e9e9e"));

                else if(Icons.isApk(rowItem.getDesc()) || Icons.isPicture(rowItem.getDesc()))holder.imageView.setColorFilter(null);

                else holder.imageView.setColorFilter(Color.parseColor(main.skin));

            }else
            if(!Icons.isApk(rowItem.getDesc()) && !Icons.isPicture(rowItem.getDesc()))
                holder.imageView.setColorFilter(Color.parseColor(main.skin));
            else
                holder.imageView.setColorFilter(null);

            if (checked != null) {

                if (checked) {
                    holder.imageView.setColorFilter(Color.parseColor(main.skin));
                    holder.imageView.setImageDrawable(main.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                    holder.rl.setBackgroundColor(Color.parseColor("#9f757575"));
                } else {
                    if (main.uimode == 0) {
                        if(main.theme1==0)holder.rl.setBackgroundResource(R.drawable.item_doc_grid);
                        else holder.rl.setBackgroundResource(R.drawable.ic_grid_card_background_dark);
                    } else if (main.uimode == 1) {
                        holder.rl.setBackgroundResource(R.drawable.bg_card);
                    }
                }
            }
            if(main.showLastModified)
                holder.date.setText(rowItem.getDate("MMM dd, yyyy",main.year));
            if(rowItem.getSize().equals(main.goback)){
                holder.date.setText(rowItem.getSize());
                holder.txtDesc.setText("");
            }else
                holder.txtDesc.setText(rowItem.getSize());
            if(main.showPermissions)
                holder.perm.setText(rowItem.getPermissions());
            return view;}}


}

