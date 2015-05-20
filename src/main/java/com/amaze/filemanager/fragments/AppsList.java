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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
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
import com.amaze.filemanager.utils.AppsSorter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconHolder;
import com.amaze.filemanager.utils.Layoutelements;
import com.amaze.filemanager.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppsList extends ListFragment {
    Futils utils = new Futils();
    AppsList app = this;
    AppsAdapter adapter;

    SharedPreferences Sp;
    public boolean selection = false;
    public ActionMode mActionMode;
    public ArrayList<ApplicationInfo> c = new ArrayList<ApplicationInfo>();
    ListView vl;
    public IconHolder ic;
    ArrayList<Layoutelements> a = new ArrayList<Layoutelements>();
    public int theme1;
    private MainActivity mainActivity;
    private String fabSkin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        ic=new IconHolder(getActivity(),true,true);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        mainActivity=(MainActivity)getActivity();
        mainActivity.toolbar.setTitle(utils.getString(getActivity(), R.string.apps));
        mainActivity.tabsSpinner.setVisibility(View.GONE);
        mainActivity.floatingActionButton.setVisibility(View.GONE);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);
        mainActivity.supportInvalidateOptionsMenu();
        fabSkin = mainActivity.fabskin;
        vl=getListView();
            Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        ListView vl = getListView();
        int theme=Integer.parseInt(Sp.getString("theme","0"));
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;
        vl.setDivider(null);
        if (vl.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) vl.getLayoutParams();
            p.setMargins(0, getToolbarHeight(getActivity()), 0, 0);
            vl.requestLayout();
        }
        if(theme1==1)getActivity().getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
         if(savedInstanceState==null)new LoadListTask().execute();
        else{
        c=savedInstanceState.getParcelableArrayList("c");
        a=savedInstanceState.getParcelableArrayList("list");
            adapter = new AppsAdapter(getActivity(), R.layout.rowlayout, a, app, c);
            setListAdapter(adapter);
            vl.setSelectionFromTop(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));

        }
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

    public void onLongItemClick(final int position) {
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        if(theme1==1)
            builder.theme(Theme.DARK);
        builder.items(new String[]{utils.getString(getActivity(), R.string.open),utils.getString(getActivity(), R.string.backup), utils.getString(getActivity(), R.string.uninstall),
                utils.getString(getActivity(), R.string.properties),utils.getString(getActivity(), R.string.play),utils.getString(getActivity(),R.string.share)})
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
                                ArrayList<Layoutelements> arrayList = new ArrayList<Layoutelements>();
                                ApplicationInfo info1 = c.get(position);
                                ArrayList<Integer> arrayList1 = new ArrayList<Integer>();
                                arrayList1.add(position);
                                final File f1 = new File(info1.publicSourceDir);
                                //arrayList.add(utils.newElement(Icons.loadMimeIcon(getActivity(), f1.getPath(), false), f1.getPath(), null, null, utils.getSize(f1),"", false));
                                //utils.deleteFiles(arrayList, null, arrayList1);
                                if ((info1.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                                    // system package
                                    if(Sp.getBoolean("rootmode",false)) {
                                        MaterialDialog.Builder builder1 = new MaterialDialog.Builder(getActivity());
                                        if(theme1==1)
                                            builder1.theme(Theme.DARK);
                                        builder1.content(utils.getString(getActivity(), R.string.unin_system_apk))
                                                .title(utils.getString(getActivity(), R.string.warning))
                                                .negativeText(utils.getString(getActivity(), R.string.no))
                                                .positiveText(utils.getString(getActivity(), R.string.yes))
                                                .negativeColor(Color.parseColor(fabSkin))
                                                .positiveColor(Color.parseColor(fabSkin))
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onNegative(MaterialDialog materialDialog) {

                                                        materialDialog.cancel();
                                                    }

                                                    @Override
                                                    public void onPositive(MaterialDialog materialDialog) {

                                                        ArrayList<File> files = new ArrayList<File>();
                                                        if (Build.VERSION.SDK_INT >= 21) {
                                                            String parent = f1.getParent();
                                                            if (!parent.equals("app") && !parent.equals("priv-app"))
                                                                files.add(new File(f1.getParent()));
                                                            else files.add(f1);
                                                        } else {
                                                            files.add(f1);
                                                        }
                                                        new DeleteTask(getActivity().getContentResolver(), getActivity()).execute(files);
                                                    }
                                                }).build().show();
                                    } else {
                                        Toast.makeText(getActivity(),utils.getString(getActivity(), R.string.enablerootmde),Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    unin(c.get(position).packageName);
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
                            case 5:
                                ArrayList<File> arrayList2=new ArrayList<File>();
                                arrayList2.add(new File(c.get(position).publicSourceDir));
                                utils.shareFiles(arrayList2,getActivity());
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
                a.add(new Layoutelements(getActivity().getResources().getDrawable(R.drawable.ic_doc_apk_grid), object.loadLabel(getActivity().getPackageManager()).toString(), object.publicSourceDir,"","",utils.readableFileSize(new File(object.publicSourceDir).length()),false,"",false));

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
