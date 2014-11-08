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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.util.LruCache;
import android.view.ActionMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.AppsAdapter;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.AppsSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconHolder;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppsList extends ListFragment {
    ArrayList<File> mFile = new ArrayList<File>();
    Futils utils = new Futils();
    AppsList app = this;
    AppsAdapter adapter;
    public int uimode;
    SharedPreferences Sp;
    public boolean selection = false;
    public ActionMode mActionMode;
    public ArrayList<ApplicationInfo> c = new ArrayList<ApplicationInfo>();
    private LruCache<String, Bitmap> mMemoryCache;
   ListView vl;public IconHolder ic;
    ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ic=new IconHolder(getActivity(),true,true);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().findViewById(R.id.pink_icon).setVisibility(View.GONE);
        getActivity().findViewById(R.id.bookadd).setVisibility(View.GONE);
        getActivity().findViewById(R.id.action_overflow).setVisibility(View.GONE);
        getActivity().findViewById(R.id.search).setVisibility(View.GONE);
        getActivity().findViewById(R.id.paste).setVisibility(View.GONE);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        getActivity().findViewById(R.id.buttonbarframe).setVisibility(View.GONE);
        final int cacheSize = maxMemory / 4;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {};
        vl=getListView();

        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        uimode = Integer.parseInt(Sp.getString("uimode", "0"));
        ListView vl = getListView();
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
            adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, a, app);
            setListAdapter(adapter);
            vl.setSelectionFromTop(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));

        }
    }

    public void onLongItemClick(final int position) {
        AlertDialog.Builder d = new AlertDialog.Builder(getActivity());
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(
                getActivity(), android.R.layout.select_dialog_item);
        adapter1.add(utils.getString(getActivity(), R.string.backup));
        adapter1.add(utils.getString(getActivity(), R.string.uninstall));
        adapter1.add(utils.getString(getActivity(), R.string.properties));
        adapter1.add(utils.getString(getActivity(), R.string.play));
        d.setAdapter(adapter1, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface p1, int p2) {
                switch (p2) {
                    case 0:
                        Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.copyingapk) + Environment.getExternalStorageDirectory().getPath() + "/app_backup", Toast.LENGTH_LONG).show();
                        ApplicationInfo info = c.get(position);
                        File f = new File(info.publicSourceDir);
                        ArrayList<String> a = new ArrayList<String>();
                        a.add(info.publicSourceDir);
                        File dst = new File(Environment.getExternalStorageDirectory().getPath() + "/app_backup");
                        Intent intent = new Intent(getActivity(), CopyService.class);
                        intent.putExtra("FILE_PATHS", a);
                        intent.putExtra("COPY_DIRECTORY", dst.getPath());
                        getActivity().startService(intent);
                        break;
                    case 1:
                        unin(c.get(position).packageName);
                        break;
                    case 2:
                        startActivity(new Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + c.get(position).packageName)));
                        break;
                    case 3:
                        Intent intent1 = new Intent(Intent.ACTION_VIEW);
                        intent1.setData(Uri.parse("market://details?id=" + c.get(position).packageName));
                        startActivity(intent1);
                        break;
                }
                // TODO: Implement this method
            }
        });
        d.show();
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
            try {
                List<ApplicationInfo> all_apps = getActivity().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);


                for (ApplicationInfo object : all_apps) {


                    c.add(object);


                }
                Collections.sort(c, new AppsSorter(getActivity().getPackageManager()));
                for (int i = 0; i < c.size(); i++) {


                    a.add(new Layoutelements(getActivity().getResources().getDrawable(R.drawable.ic_doc_apk_grid), c.get(i).loadLabel(getActivity().getPackageManager()).toString(), c.get(i).publicSourceDir,"","","",false));

                    File file = new File(c.get(i).publicSourceDir);
                    mFile.add(file);
                }
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


                    adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, bitmap, app);
                    setListAdapter(adapter);

                }
            } catch (Exception e) {
            }

        }
    }  // copy the .apk file to wherever

    public void unin(String pkg) {

        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "" + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }
}
