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
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppsAdapter extends ArrayAdapter<Layoutelements> {
    Context context;
    List<Layoutelements> items;
    public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();
    AppsList app;
    ArrayList<PackageInfo> c = new ArrayList<PackageInfo>();

    public AppsAdapter(Context context, int resourceId,
                       List<Layoutelements> items, AppsList app, ArrayList<PackageInfo> c) {
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
        ImageButton about;
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
            vholder.about=(ImageButton)view.findViewById(R.id.properties);
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
        app.ic.loadDrawable(holder.imageView,(rowItem.getDesc()),null);
        if (holder.about != null) {
            if(app.theme1==0)holder.about.setColorFilter(Color.parseColor("#ff666666"));
            showPopup(holder.about,rowItem);
        }
        holder.txtTitle.setText(rowItem.getTitle());
        //	File f = new File(rowItem.getDesc());
        holder.txtDesc.setText(rowItem.getSize());
        holder.rl.setClickable(true);
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                Intent i1 = app.getActivity().getPackageManager().getLaunchIntentForPackage(rowItem.getPermissions());
                if (i1 != null)
                    app.startActivity(i1);
                else
                    Toast.makeText(app.getActivity(), new Futils().getString(app.getActivity(), R.string.not_allowed), Toast.LENGTH_LONG).show();
                // TODO: Implement this method
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
    void showPopup(View v,final Layoutelements rowItem){
        final Futils utils=new Futils();
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(app.getActivity(), view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.open:
                                Intent i1 = app.getActivity().getPackageManager().getLaunchIntentForPackage(rowItem.getPermissions());
                                if (i1!= null)
                                    app.startActivity(i1);
                                else
                                    Toast.makeText(app.getActivity(),new Futils().getString(app.getActivity(),R.string.not_allowed), Toast.LENGTH_LONG).show();
                                return true;
                            case R.id.share:
                                ArrayList<File> arrayList2=new ArrayList<File>();
                                arrayList2.add(new File(rowItem.getDesc()));
                                int color1= Color.parseColor(PreferenceUtils.getAccentString(app.Sp));
                                utils.shareFiles(arrayList2,app.getActivity(),app.theme1,color1);
                                return true;
                            case R.id.unins:
                                final BaseFile f1 = new BaseFile(rowItem.getDesc());
                                f1.setMode(HFile.ROOT_MODE);
                                ApplicationInfo info1=null;
                                for(PackageInfo info:c){
                                    if(info.applicationInfo.publicSourceDir.equals(rowItem.getDesc()))info1=info.applicationInfo;
                                }
                                int color= Color.parseColor(PreferenceUtils.getAccentString(app.Sp));
                                //arrayList.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f1.getPath(), false), f1.getPath(), null, null, utils.getSize(f1),"", false));
                                //utils.deleteFiles(arrayList, null, arrayList1);
                                if ((info1.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                                    // system package
                                    if(app.Sp.getBoolean("rootmode",false)) {
                                        MaterialDialog.Builder builder1 = new MaterialDialog.Builder(app.getActivity());
                                        if(app.theme1==1)
                                            builder1.theme(Theme.DARK);
                                        builder1.content(utils.getString(app.getActivity(), R.string.unin_system_apk))
                                                .title(utils.getString(app.getActivity(), R.string.warning))
                                                .negativeColor(color)
                                                .positiveColor(color)
                                                .negativeText(utils.getString(app.getActivity(), R.string.no))
                                                .positiveText(utils.getString(app.getActivity(), R.string.yes))
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onNegative(MaterialDialog materialDialog) {

                                                        materialDialog.cancel();
                                                    }

                                                    @Override
                                                    public void onPositive(MaterialDialog materialDialog) {

                                                        ArrayList<BaseFile> files = new ArrayList<>();
                                                        if (Build.VERSION.SDK_INT >= 21) {
                                                            String parent = f1.getParent();
                                                            if (!parent.equals("app") && !parent.equals("priv-app")){
                                                                BaseFile baseFile=new BaseFile(f1.getParent());
                                                                baseFile.setMode(HFile.ROOT_MODE);
                                                                files.add(baseFile);
                                                            }
                                                            else files.add(f1);
                                                        } else {
                                                            files.add(f1);
                                                        }
                                                        new DeleteTask(app.getActivity().getContentResolver(), app.getActivity()).execute((files));
                                                    }
                                                }).build().show();
                                    } else {
                                        Toast.makeText(app.getActivity(),app.getResources().getString(R.string.enablerootmde),Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    app.unin(rowItem.getPermissions());
                                }
                                return true;
                            case R.id.play:
                                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                                intent1.setData(Uri.parse("market://details?id=" + rowItem.getPermissions()));
                                app.startActivity(intent1);
                                return true;
                            case R.id.properties:

                                app.startActivity(new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + rowItem.getPermissions())));
                                return true;
                            case R.id.backup:
                                Toast.makeText(app.getActivity(), new Futils().getString(app.getActivity(), R.string.copyingapk) + Environment.getExternalStorageDirectory().getPath() + "/app_backup", Toast.LENGTH_LONG).show();
                                File f = new File(rowItem.getDesc());
                                ArrayList<BaseFile> ab = new ArrayList<>();
                                File dst = new File(Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                                if(!dst.exists() || !dst.isDirectory())dst.mkdirs();
                                Intent intent = new Intent(app.getActivity(), CopyService.class);
                                BaseFile baseFile=RootHelper.generateBaseFile(f,true);
                                baseFile.setName(rowItem.getTitle() + "_" + rowItem.getSymlink() + ".apk");
                                ab.add(baseFile);
                                intent.putExtra("FILE_PATHS", ab);
                                intent.putExtra("COPY_DIRECTORY", dst.getPath());
                                intent.putExtra("MODE",0);
                                app.getActivity().startService(intent);
                                return true;

                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.app_options);
                popupMenu.show();
            }
        });

    }

}
