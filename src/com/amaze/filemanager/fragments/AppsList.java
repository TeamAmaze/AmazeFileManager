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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.AppsAdapter;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.AppsSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconHolder;
import com.amaze.filemanager.utils.Icons;
import com.amaze.filemanager.utils.Layoutelements;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class AppsList extends ListFragment {
    Futils utils = new Futils();
    AppsList app = this;
    AppsAdapter adapter;
    public int uimode;
    SharedPreferences Sp;
    public boolean selection = false;
    public ActionMode mActionMode;
    public ArrayList<ApplicationInfo> c = new ArrayList<ApplicationInfo>();
    ListView vl;
    public IconHolder ic;
    ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        ic=new IconHolder(getActivity(),true,true);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        vl=getListView();

        //((TextView) getActivity().findViewById(R.id.title)).setText(utils.getString(getActivity(), R.string.apps));

        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        floatingActionButton.hide(true);

        //getActivity().findViewById(R.id.fab).setVisibility(View.INVISIBLE);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        ListView vl = getListView();
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int theme=Integer.parseInt(Sp.getString("theme","0"));
        int theme1 = theme;
        if (theme == 2) {
            if(hour<=6 || hour>=18) {
                theme1 = 1;
            } else
                theme1 = 0;
        }
        if(theme1==1)getActivity().getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        if (uimode == 1) {
            float scale = getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (5 * scale + 0.5f);
            vl.setPadding(dpAsPixels, 0, dpAsPixels, 0);
            vl.setDividerHeight(dpAsPixels);
        } vl.setDivider(null);
        vl.setFastScrollEnabled(true);
        if(savedInstanceState==null)new LoadListTask().execute();
        else{
        c=savedInstanceState.getParcelableArrayList("c");
        a=savedInstanceState.getParcelableArrayList("list");
            adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, a, app, c);
            setListAdapter(adapter);
            vl.setSelectionFromTop(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));

        }
    }

    public void onLongItemClick(final int position) {
        new MaterialDialog.Builder(getActivity())
                .items(new String[]{utils.getString(getActivity(), R.string.open),utils.getString(getActivity(), R.string.backup), utils.getString(getActivity(), R.string.uninstall),
                        utils.getString(getActivity(), R.string.properties),utils.getString(getActivity(), R.string.play)})
                .itemsCallback(new MaterialDialog.ListCallback() {

                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence s) {
                        switch (i) {
                            case 0:
                                Intent i1 = app.getActivity().getPackageManager().getLaunchIntentForPackage(c.get(position).packageName);
                                if (i1!= null)
                                    app.startActivity(i1);
                                else
                                    Toast.makeText(app.getActivity(),utils.getString(getActivity(),R.string.not_allowed), Toast.LENGTH_LONG).show();
                                break;

                            case 1:
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.copyingapk) + Environment.getExternalStorageDirectory().getPath() + "/app_backup", Toast.LENGTH_LONG).show();
                                ApplicationInfo info = c.get(position);
                                File f = new File(info.publicSourceDir);
                                ArrayList<String> a = new ArrayList<String>();
                                //a.add(info.publicSourceDir);
                                File dst = new File(Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                                if(!dst.exists() || !dst.isDirectory())dst.mkdirs();
                                Intent intent = new Intent(getActivity(), CopyService.class);
                                //Toast.makeText(getActivity(), f.getParent(), Toast.LENGTH_LONG).show();

                                if (Build.VERSION.SDK_INT == 21) {
                                    a.add(f.getParent());
                                } else {
                                    a.add(f.getPath());
                                }
                                intent.putExtra("FILE_PATHS", a);
                                intent.putExtra("COPY_DIRECTORY", dst.getPath());
                                getActivity().startService(intent);
                                break;
                            case 2:
                                if (!unin(c.get(position).packageName)) {
                                    ArrayList<Layoutelements> arrayList = new ArrayList<Layoutelements>();
                                    ApplicationInfo info1 = c.get(position);
                                    ArrayList<Integer> arrayList1 = new ArrayList<Integer>();
                                    arrayList1.add(position);
                                    File f1 = new File(info1.publicSourceDir);
                                    arrayList.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f1.getPath(), false), f1.getPath(), null, null, utils.getSize(f1),"", false));
                                    utils.deleteFiles(arrayList, null, arrayList1);
                                } 
                                break;
                            case 3:
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + c.get(position).packageName)));
                                break;
                            case 4:
                                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                                intent1.setData(Uri.parse("market://details?id=" + c.get(position).packageName));
                                startActivity(intent1);
                                break;
                        }
                    }
                }).title(a.get(position).getTitle()).build()
                .show();
    }

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
            try {PackageManager p=getActivity().getPackageManager();
                List<ApplicationInfo> all_apps = p.getInstalledApplications(PackageManager.GET_META_DATA);


                for (ApplicationInfo object : all_apps) {


                    c.add(object);



                }
                Collections.sort(c, new AppsSorter(p));
                for (ApplicationInfo object:c)
                a.add(new Layoutelements(getActivity().getResources().getDrawable(R.drawable.ic_doc_apk_grid), object.loadLabel(getActivity().getPackageManager()).toString(), object.publicSourceDir,"","","","",false));

            } catch (Exception e) {
                //Toast.makeText(getActivity(), "" + e, Toast.LENGTH_LONG).show();
            }//ArrayAdapter<String> b=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,a);
            // TODO: Implement this method

            return a;
        }


        public LoadListTask() {

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
}
