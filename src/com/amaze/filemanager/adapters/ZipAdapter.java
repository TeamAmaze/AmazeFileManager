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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.ZipViewer;
import com.amaze.filemanager.services.asynctasks.ZipExtractTask;
import com.amaze.filemanager.services.asynctasks.ZipHelperTask;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.ZipObj;
import com.pkmmte.view.CircularImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipAdapter extends ArrayAdapter<ZipObj> {
    Context c;
    Drawable folder, unknown;
    ArrayList<ZipObj> enter;
    ZipViewer zipViewer;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    public ZipAdapter(Context c, int id, ArrayList<ZipObj> enter, ZipViewer zipViewer) {
        super(c, id, enter);
        this.enter = enter;
        for (int i = 0; i < enter.size(); i++) {
            myChecked.put(i, false);
        }
        this.c = c;
        folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
        unknown = c.getResources().getDrawable(R.drawable.ic_doc_generic_am);
        this.zipViewer = zipViewer;
    }public void toggleChecked(int position) {
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();
        if (zipViewer.selection == false || zipViewer.mActionMode == null) {
            zipViewer.selection = true;
            /*zipViewer.mActionMode = zipViewer.getActivity().startActionMode(
                   zipViewer.mActionModeCallback);*/
            zipViewer.mActionMode = zipViewer.mainActivity.toolbar.startActionMode(zipViewer.mActionModeCallback);
        }
        zipViewer.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            zipViewer.selection = false;
            zipViewer.mActionMode.finish();
            zipViewer.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b,String path) {
        int k=0;
        if(enter.get(0).getEntry()==null)k=1;
        for (int i = k; i < enter.size(); i++) {
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


    private class ViewHolder {
        CircularImageView viewmageV;
        ImageView imageView,apk;
        TextView txtTitle;
        TextView txtDesc;
        TextView date;
        TextView perm;
        View rl;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        final ZipObj rowItem = enter.get(position);

        View view = convertView;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) c
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.rowlayout, parent, false);
            final ViewHolder vholder = new ViewHolder();

            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            if (zipViewer.mainActivity.theme1==1)
                vholder.txtTitle.setTextColor(getContext().getResources().getColor(android.R.color.white));
            vholder.viewmageV = (CircularImageView) view.findViewById(R.id.cicon);
            vholder.imageView = (ImageView) view.findViewById(R.id.icon);
            vholder.rl = view.findViewById(R.id.second);
            vholder.perm = (TextView) view.findViewById(R.id.permis);
            vholder.date = (TextView) view.findViewById(R.id.date);
            vholder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
            vholder.apk=(ImageView)view.findViewById(R.id.bicon);
            view.setTag(vholder);

        }
        final ViewHolder holder = (ViewHolder) view.getTag();


        GradientDrawable gradientDrawable = (GradientDrawable) holder.imageView.getBackground();
        if(rowItem.getEntry()==null){
            holder.imageView.setImageResource(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            gradientDrawable.setColor(Color.parseColor("#757575"));
            holder.txtTitle.setText("..");
            holder.txtDesc.setText("");
            holder.date.setText(R.string.goback);
        }
        else {   holder.imageView.setImageDrawable( Icons.loadMimeIcon(zipViewer.getActivity(),rowItem.getName(),false));
            final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
            if(zipViewer.showLastModified)holder.date.setText(new Futils().getdate(rowItem.getTime(),"MMM dd, yyyy",zipViewer.year));
            if (rowItem.isDirectory()) {
            holder.imageView.setImageDrawable(folder);
            gradientDrawable.setColor(Color.parseColor(zipViewer.skin));
            stringBuilder.deleteCharAt(rowItem.getName().length() - 1);
        try {
            holder.txtTitle.setText(stringBuilder.toString().substring(stringBuilder.toString().lastIndexOf("/") + 1));
        }catch (Exception e)
        {
            holder.txtTitle.setText(rowItem.getName().substring(0, rowItem.getName().lastIndexOf("/")));
        } }else{if(zipViewer.showSize)   holder.txtDesc.setText(new Futils().readableFileSize(rowItem.getSize()));
                holder.txtTitle.setText(rowItem.getName().substring(rowItem.getName().lastIndexOf("/") + 1));
                if (zipViewer.coloriseIcons) {
                    if (Icons.isVideo(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#f06292"));
                    else if (Icons.isAudio(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#9575cd"));
                    else if (Icons.isPdf(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#da4336"));
                    else if (Icons.isCode(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#00bfa5"));
                    else if (Icons.isText(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#e06055"));
                    else if (Icons.isArchive(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#f9a825"));
                    else if (Icons.isgeneric(rowItem.getName()))
                        gradientDrawable.setColor(Color.parseColor("#9e9e9e"));
                    else gradientDrawable.setColor(Color.parseColor(zipViewer.skin));
                } else gradientDrawable.setColor(Color.parseColor(zipViewer.skin));
            }}


        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(rowItem.getEntry()!=null)  toggleChecked(p);/*
                }*/
                return false;
            }
        });holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rowItem.getEntry()!=null){
                final Animation animation = AnimationUtils.loadAnimation(zipViewer.getActivity(), R.anim.holder_anim);

                holder.imageView.setAnimation(animation);
                toggleChecked(p);}

            }
        });
        Boolean checked = myChecked.get(position);
        if (checked != null) {

            if (checked) {
                holder.imageView.setImageDrawable(zipViewer.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                gradientDrawable.setColor(Color.parseColor("#757575"));

                holder.rl.setBackgroundColor(zipViewer.skinselection);
            } else {

                    holder.rl.setBackgroundResource(R.drawable.listitem1);

            }
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if(rowItem.getEntry()==null)
                    zipViewer.goBack();
                    else{
                if(zipViewer.selection)toggleChecked(p);else {
                    final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
                    if (rowItem.isDirectory())
                        stringBuilder.deleteCharAt(rowItem.getName().length() - 1);

                        if (rowItem.isDirectory()) {

                    new ZipHelperTask(zipViewer,  stringBuilder.toString()).execute(zipViewer.f);

                } else {String x=rowItem.getName().substring(rowItem.getName().lastIndexOf("/")+1);
                    File file = new File(zipViewer.f.getParent() + "/" + x);
                    zipViewer.files.clear();
                    zipViewer.files.add(0, file);

                    try {
                        ZipFile zipFile = new ZipFile(zipViewer.f);
                     new ZipExtractTask(zipFile, zipViewer.f.getParent(), zipViewer, x,true).execute(rowItem.getEntry());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }}
            }}
        });
        return view;
    }
}
