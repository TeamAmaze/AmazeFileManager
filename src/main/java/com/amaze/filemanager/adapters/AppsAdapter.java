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
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
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
import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.ui.LayoutElements;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends ArrayAdapter<LayoutElements> implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

    private UtilitiesProviderInterface utilsProvider;
    private Context context;
    private List<LayoutElements> items;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    private AppsList appsList;
    private LayoutElements rowItem;

    public AppsAdapter(Context context, UtilitiesProviderInterface utilsProvider, int resourceId,
                       AppsList appsList) {
        super(context, resourceId);
        this.utilsProvider = utilsProvider;
        this.context = context;
        this.appsList = appsList;

        /*for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
        }*/
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
        List<Integer> checkedItemPositions = new ArrayList<>();

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

    public void setData(List<LayoutElements> data) {
        clear();

        if (data != null) {
            this.items = data;
            addAll(data);
        }
    }

    private class ViewHolder {
        ImageView apkIcon;
        TextView txtTitle;
        RelativeLayout rl;
        TextView txtDesc;
        ImageButton about;
    }

    @NonNull
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        final LayoutElements rowItem = getItem(position);

        View view;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.rowlayout, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            vholder.apkIcon = (ImageView) view.findViewById(R.id.apk_icon);
            vholder.rl = (RelativeLayout) view.findViewById(R.id.second);
            vholder.txtDesc = (TextView) view.findViewById(R.id.date);
            vholder.about = (ImageButton) view.findViewById(R.id.properties);
            vholder.apkIcon.setVisibility(View.VISIBLE);
            view.findViewById(R.id.generic_icon).setVisibility(View.GONE);
            view.findViewById(R.id.picture_icon).setVisibility(View.GONE);
            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.apkIcon.setImageDrawable(rowItem.getImageId());
        appsList.ic.cancelLoad(holder.apkIcon);
        appsList.ic.loadDrawable(holder.apkIcon, (rowItem.getDesc()), null);
        if (holder.about != null) {
            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                holder.about.setColorFilter(Color.parseColor("#ff666666"));
            attachPopup(holder.about, rowItem);
        }
        holder.txtTitle.setText(rowItem.getTitle());
        //	File f = new File(rowItem.getDesc());
        holder.txtDesc.setText(rowItem.getSize());
        holder.rl.setClickable(true);
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                Intent i1 = appsList.getActivity().getPackageManager().getLaunchIntentForPackage(rowItem.getPermissions());
                if (i1 != null)
                    appsList.startActivity(i1);
                else
                    Toast.makeText(appsList.getActivity(), appsList.getResources().getString(R.string.not_allowed), Toast.LENGTH_LONG).show();
                // TODO: Implement this method
            }
        });


        Boolean checked = myChecked.get(position);
        if (checked != null) {

            if (checked) {
                holder.rl.setBackgroundColor(Color.parseColor("#5f33b5e5"));
            } else {
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
                    holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
                } else {
                    holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
                }
            }
        }
        return view;
    }

    private void attachPopup(View v, final LayoutElements rowItem) {
        this.rowItem = rowItem;
        v.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        PopupMenu popupMenu = new PopupMenu(appsList.getActivity(), view);
        popupMenu.setOnMenuItemClickListener(this);

        popupMenu.inflate(R.menu.app_options);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Futils utils = utilsProvider.getFutils();

        switch (item.getItemId()) {
            case R.id.open:
                Intent i1 = appsList.getActivity().getPackageManager()
                        .getLaunchIntentForPackage(rowItem.getPermissions());
                if (i1 != null)
                    appsList.startActivity(i1);
                else
                    Toast.makeText(appsList.getActivity(),
                            appsList.getResources().getString(R.string.not_allowed),
                            Toast.LENGTH_LONG).show();
                return true;
            case R.id.share:
                ArrayList<File> arrayList2 = new ArrayList<>();
                arrayList2.add(new File(rowItem.getDesc()));
                int color1 = Color.parseColor(PreferenceUtils.getAccentString(appsList.Sp));
                utils.shareFiles(arrayList2, appsList.getActivity(), utilsProvider.getAppTheme(), color1);
                return true;
            case R.id.unins:
                final BaseFile f1 = new BaseFile(rowItem.getDesc());
                f1.setMode(OpenMode.ROOT);
                int color = Color.parseColor(PreferenceUtils.getAccentString(appsList.Sp));

                if ((Integer.valueOf(rowItem.getSymlink().substring(0,
                        rowItem.getSymlink().indexOf("_"))) & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // system package
                    if (appsList.Sp.getBoolean("rootmode", false)) {
                        MaterialDialog.Builder builder1 = new MaterialDialog.Builder(appsList.getActivity());
                        builder1.theme(utilsProvider.getAppTheme().getMaterialDialogTheme())
                                .content(appsList.getResources().getString(R.string.unin_system_apk))
                                .title(appsList.getResources().getString(R.string.warning))
                                .negativeColor(color)
                                .positiveColor(color)
                                .negativeText(appsList.getResources().getString(R.string.no))
                                .positiveText(appsList.getResources().getString(R.string.yes))
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
                                            if (!parent.equals("appsList") && !parent.equals("priv-appsList")) {
                                                BaseFile baseFile = new BaseFile(f1.getParent());
                                                baseFile.setMode(OpenMode.ROOT);
                                                files.add(baseFile);
                                            } else files.add(f1);
                                        } else {
                                            files.add(f1);
                                        }
                                        new DeleteTask(appsList.getActivity().getContentResolver(),
                                                appsList.getActivity()).execute((files));
                                    }
                                }).build().show();
                    } else {
                        Toast.makeText(appsList.getActivity(), appsList.getResources().getString(R.string.enablerootmde), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    appsList.unin(rowItem.getPermissions());
                }
                return true;
            case R.id.play:
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                intent1.setData(Uri.parse("market://details?id=" + rowItem.getPermissions()));
                appsList.startActivity(intent1);
                return true;
            case R.id.properties:

                appsList.startActivity(new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + rowItem.getPermissions())));
                return true;
            case R.id.backup:
                Toast.makeText(appsList.getActivity(), appsList.getResources().getString(R.string.copyingapk) + Environment.getExternalStorageDirectory().getPath() + "/app_backup", Toast.LENGTH_LONG).show();
                File f = new File(rowItem.getDesc());
                ArrayList<BaseFile> ab = new ArrayList<>();
                File dst = new File(Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                if (!dst.exists() || !dst.isDirectory()) dst.mkdirs();
                Intent intent = new Intent(appsList.getActivity(), CopyService.class);
                BaseFile baseFile = RootHelper.generateBaseFile(f, true);
                baseFile.setName(rowItem.getTitle() + "_" +
                        rowItem.getSymlink().substring(rowItem.getSymlink().indexOf("_") + 1) + ".apk");
                ab.add(baseFile);

                intent.putParcelableArrayListExtra(CopyService.TAG_COPY_SOURCES, ab);
                intent.putExtra(CopyService.TAG_COPY_TARGET, dst.getPath());
                intent.putExtra(CopyService.TAG_COPY_OPEN_MODE, 0);

                ServiceWatcherUtil.runService(appsList.getActivity(), intent);
                return true;
        }
        return false;
    }

}
