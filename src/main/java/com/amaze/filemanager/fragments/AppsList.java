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

package com.amaze.filemanager.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.view.ActionMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.AppsAdapter;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.utils.FileListSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.ui.icons.IconHolder;
import com.amaze.filemanager.ui.Layoutelements;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppsList extends ListFragment {
    Futils utils = new Futils();
    AppsList app = this;
    AppsAdapter adapter;

    public SharedPreferences Sp;
    public ArrayList<PackageInfo> c = new ArrayList<>();
    ListView vl;
    public IconHolder ic;
    ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
    public int theme1;
    private MainActivity mainActivity;
    public String fabSkin;
    int asc,sortby;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        ic=new IconHolder(getActivity(),true,true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        getActivity().registerReceiver(br, intentFilter);

    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity=(MainActivity)getActivity();
        mainActivity.setActionBarTitle(utils.getString(getActivity(), R.string.apps));
        mainActivity.floatingActionButton.hideMenuButton(true);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();
        fabSkin = mainActivity.fabskin;
        vl=getListView();
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        getSortModes();
        ListView vl = getListView();
        int theme=Integer.parseInt(Sp.getString("theme","0"));
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;
        vl.setDivider(null);
        if(theme1==1)getActivity().getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
         if(savedInstanceState==null)loadlist(false);
        else{
        c=savedInstanceState.getParcelableArrayList("c");
        a=savedInstanceState.getParcelableArrayList("list");
            adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, a, app, c);
            setListAdapter(adapter);
            vl.setSelectionFromTop(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));

        }
    }
    @Override
    public  void onDestroy(){
        super.onDestroy();
        getActivity().unregisterReceiver(br);
    }
    int index=0,top=0;
    public void loadlist(boolean save){
        if(save) {
            index = vl.getFirstVisiblePosition();
            View vi = vl.getChildAt(0);
            top = (vi == null) ? 0 : vi.getTop();
        } new LoadListTask(save,top,index).execute();
    }
    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }
    @Override
    public void onResume() {
        super.onResume();

    }
    BroadcastReceiver br = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent != null) {
            loadlist(true);
            }}
    };


    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if(vl!=null){
        b.putParcelableArrayList("c",c);
        b.putParcelableArrayList("list",a);
        int index = vl.getFirstVisiblePosition();
        View vi = vl.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        b.putInt("index", index);
        b.putInt("top", top);
    }}
    class LoadListTask extends AsyncTask<Void, Void, ArrayList<Layoutelements>> {

        protected ArrayList<Layoutelements> doInBackground(Void[] p1) {
            try {
                PackageManager p = getActivity().getPackageManager();
                List<PackageInfo> all_apps = p.getInstalledPackages(PackageManager.GET_META_DATA);
                a = new ArrayList<>();
                c = new ArrayList<>();
                for (PackageInfo object : all_apps) {
                    File f=new File(object.applicationInfo.publicSourceDir);
                    a.add(new Layoutelements(ContextCompat.getDrawable(getActivity(),R.drawable.ic_doc_apk_grid), object.applicationInfo.loadLabel(p).toString(), object.applicationInfo.publicSourceDir, object.packageName, object.versionName, utils.readableFileSize(f.length()),f.length(), false, f.lastModified()+"", false));

                }
                Collections.sort(a, new FileListSorter(0, sortby, asc, false));
            } catch (Exception e) {
                //Toast.makeText(getActivity(), "" + e, Toast.LENGTH_LONG).show();
            }//ArrayAdapter<String> b=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,a);
            // TODO: Implement this method

            return a;
        }

        int index,top;
        boolean save;
        public LoadListTask(boolean save,int top,int index) {
            this.save=save;
            this.index=index;
            this.top=top;
        }

        @Override
        protected void onPreExecute() {

        }


        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(ArrayList<Layoutelements> bitmap) {
            if (isCancelled()) {
                bitmap = null;

            }
            try {
                if (bitmap != null) {


                    adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, bitmap, app, c);
                    setListAdapter(adapter);
                    if(save && getListView()!=null)
                        getListView().setSelectionFromTop(index,top);
                }
            } catch (Exception e) {
            }

        }
    }  // copy the .apk file to wherever

    public boolean unin(String pkg) {

        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "" + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public void getSortModes() {
        int t = Integer.parseInt(Sp.getString("sortbyApps", "0"));
        if (t <= 2) {
            sortby = t;
            asc = 1;
        } else if (t > 2) {
            asc = -1;
            sortby = t - 3;
        }

    }}
