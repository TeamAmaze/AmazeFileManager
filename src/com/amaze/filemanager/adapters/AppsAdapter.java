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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.Futils;
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

    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.simplerow, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            vholder.imageView = (ImageView) view.findViewById(R.id.icon);
            vholder.rl = (RelativeLayout) view.findViewById(R.id.second);
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

        holder.rl.setClickable(true);
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if (app.selection == true) {
                    toggleChecked(p);
                    app.mActionMode.invalidate();
                } else {

                    AlertDialog.Builder d = new AlertDialog.Builder(context);
                    final Futils utils = new Futils();
                    final AppsList appsList = new AppsList();
                    ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(
                            context, android.R.layout.select_dialog_item);
                    adapter1.add(utils.getString(context, R.string.open));
                    adapter1.add(utils.getString(context, R.string.backup));
                    adapter1.add(utils.getString(context, R.string.uninstall));
                    adapter1.add(utils.getString(context, R.string.properties));
                    adapter1.add(utils.getString(context, R.string.play));
                    d.setAdapter(adapter1, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface p1, int p2) {
                            switch (p2) {
                                case 0:
                                    Intent i = app.getActivity().getPackageManager().getLaunchIntentForPackage(app.c.get(p).packageName);
                                    if (i != null)
                                        app.startActivity(i);
                                    else
                                       Toast.makeText(app.getActivity(), utils.getString(context,R.string.not_allowed), Toast.LENGTH_LONG).show();
                                    break;

                                case 1:
                                    Toast.makeText(context, utils.getString(context, R.string.copyingapk) + Environment.getExternalStorageDirectory().getPath() + "/app_backup", Toast.LENGTH_LONG).show();
                                    ApplicationInfo info = c.get(p);
                                    File f = new File(info.publicSourceDir);
                                    ArrayList<String> a = new ArrayList<String>();
                                    a.add(info.publicSourceDir);
                                    File dst = new File(Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                                    if(!dst.exists() || !dst.isDirectory())dst.mkdirs();
                                    Intent intent = new Intent(context, CopyService.class);
                                    intent.putExtra("FILE_PATHS", a);
                                    intent.putExtra("COPY_DIRECTORY", dst.getPath());
                                    context.startService(intent);
                                    break;
                                case 2:
                                    unin(c.get(p).packageName);
                                    break;
                                case 3:
                                    context.startActivity(new Intent(
                                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:" + c.get(p).packageName)));
                                    break;
                                case 4:
                                    Intent intent1 = new Intent(Intent.ACTION_VIEW);
                                    intent1.setData(Uri.parse("market://details?id=" + c.get(p).packageName));
                                    context.startActivity(intent1);
                                    break;
                            }
                            // TODO: Implement this method
                        }
                    });
                    d.show();
                }
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
                if (app.uimode == 0) {
                    holder.rl.setBackgroundResource(R.drawable.listitem1);
                } else if (app.uimode == 1) {
                    holder.rl.setBackgroundResource(R.drawable.bg_card);
                }
            }
        }
        return view;
    }

    public void unin(String pkg) {

        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + pkg));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "" + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }
}
