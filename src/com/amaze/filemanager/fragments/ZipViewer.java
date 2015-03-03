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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.ZipAdapter;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.asynctasks.ZipExtractTask;
import com.amaze.filemanager.services.asynctasks.ZipHelperTask;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.ZipObj;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class ZipViewer extends Fragment {

    String s;
    public File f;
    public ArrayList<File> files;
    public Boolean results,selection=false;
    public String current;
    public Futils utils=new Futils();
    public String skin,year;public ZipAdapter zipAdapter;
    public ActionMode mActionMode;public int skinselection;
    public boolean coloriseIcons,showSize,showLastModified,gobackitem;
SharedPreferences Sp;
    ZipViewer zipViewer=this;
    public ArrayList<ZipObj> wholelist=new ArrayList<ZipObj>();
public     ArrayList<ZipObj> elements = new ArrayList<ZipObj>();
    public MainActivity mainActivity;
    public ListView listView;
    View rootView;
    public SwipeRefreshLayout swipeRefreshLayout;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.main_frag, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView);
        rootView.findViewById(R.id.activity_main_swipe_refresh_layout1).setVisibility(View.GONE);
        swipeRefreshLayout=(SwipeRefreshLayout)rootView.findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        LinearLayout pathbar = (LinearLayout) rootView.findViewById(R.id.pathbar);
        TextView textView = (TextView) rootView.findViewById(R.id.fullpath);
        rootView.findViewById(R.id.fab).setVisibility(View.GONE);
        pathbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        return rootView;
    }
        @Override
    public void onActivityCreated(Bundle savedInstanceState) {

            super.onActivityCreated(savedInstanceState);
            s = getArguments().getString("path");
            f = new File(s);
            rootView.findViewById(R.id.gridView).setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mainActivity = (MainActivity) getActivity();
            mainActivity.supportInvalidateOptionsMenu();
            if (mainActivity.theme1 == 1)
                listView.setBackgroundColor(Color.parseColor("#000000"));
            else
                listView.setBackgroundColor(Color.parseColor("#ffffff"));
            gobackitem = Sp.getBoolean("goBack_checkbox", true);
            coloriseIcons = Sp.getBoolean("coloriseIcons", false);
            Calendar calendar = Calendar.getInstance();
            showSize = Sp.getBoolean("showFileSize", false);
            showLastModified = Sp.getBoolean("showLastModified", true);
            year = ("" + calendar.get(Calendar.YEAR)).substring(2, 4);
            skin = Sp.getString("skin_color", "#03A9F4");
            rootView.findViewById(R.id.buttonbarframe).setBackgroundColor(Color.parseColor(skin));

            listView.setDivider(null);
            String x = getSelectionColor();
            skinselection = Color.parseColor(x);
            files = new ArrayList<File>();
            if (savedInstanceState == null)
                loadlist(f.getPath());
            else {
                wholelist = savedInstanceState.getParcelableArrayList("wholelist");
                elements = savedInstanceState.getParcelableArrayList("elements");
                current = savedInstanceState.getString("path");
                f = new File(savedInstanceState.getString("file"));
                createviews(elements, current);
            }
            mainActivity.tabsSpinner.setVisibility(View.GONE);
            try{mainActivity.toolbar.setTitle(f.getName());}catch (Exception e){
            mainActivity.toolbar.setTitle(getResources().getString(R.string.zip_viewer));}
            mainActivity.supportInvalidateOptionsMenu();
        }
    public String getSelectionColor(){

        String[] colors = new String[]{
                "#F44336","#74e84e40",
                "#e91e63","#74ec407a",
                "#9c27b0","#74ab47bc",
                "#673ab7","#747e57c2",
                "#3f51b5","#745c6bc0",
                "#2196F3","#74738ffe",
                "#03A9F4","#7429b6f6",
                "#00BCD4","#7426c6da",
                "#009688","#7426a69a",
                "#4CAF50","#742baf2b",
                "#8bc34a","#749ccc65",
                "#FFC107","#74ffca28",
                "#FF9800","#74ffa726",
                "#FF5722","#74ff7043",
                "#795548","#748d6e63",
                "#212121","#79bdbdbd",
                "#607d8b","#7478909c",
                "#004d40","#740E5D50"
        };
        return colors[ Arrays.asList(colors).indexOf(skin)+1];
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("wholelist",wholelist);
        outState.putParcelableArrayList("elements",elements);
        outState.putString("path",current);
        outState.putString("file",f.getPath());
    }
    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        private void hideOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(false);
        }

        private void showOption(int id, Menu menu) {
            MenuItem item = menu.findItem(id);
            item.setVisible(true);
        }
        View v;
        // called when the action mode is created; startActionMode() was called
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            v=getActivity().getLayoutInflater().inflate(R.layout.actionmode,null);
            mode.setCustomView(v);
            // assumes that you have "contexual.xml" menu resources
            inflater.inflate(R.menu.contextual, menu);
            hideOption(R.id.cpy, menu);
            hideOption(R.id.cut,menu);
            hideOption(R.id.delete,menu);
            hideOption(R.id.addshortcut,menu);
            hideOption(R.id.sethome, menu);
            hideOption(R.id.rename, menu);
            hideOption(R.id.share, menu);
            hideOption(R.id.about, menu);
            hideOption(R.id.openwith, menu);
            showOption(R.id.all,menu);
            hideOption(R.id.book, menu);
            hideOption(R.id.compress, menu);
            hideOption(R.id.permissions, menu);
            hideOption(R.id.hide, menu);
            mode.setTitle(utils.getString(getActivity(), R.string.select));
            ObjectAnimator anim = ObjectAnimator.ofInt(rootView.findViewById(R.id.buttonbarframe), "backgroundColor", Color.parseColor(skin), getResources().getColor(R.color.toolbar_cab));
            anim.setDuration(200);
            anim.setEvaluator(new ArgbEvaluator());
            anim.start();
            if (Build.VERSION.SDK_INT >= 21) {

                Window window = getActivity().getWindow();
                if(mainActivity.colourednavigation)window.setNavigationBarColor(getResources().getColor(android.R.color.black));
            }
            if(Build.VERSION.SDK_INT<19)
                getActivity().findViewById(R.id.action_bar).setVisibility(View.GONE);
            return true;
        }

        // the following method is called each time
        // the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ArrayList<Integer> positions = zipAdapter.getCheckedItemPositions();
            ((TextView) v.findViewById(R.id.item_count)).setText(positions.size() + "");

            return false; // Return false if nothing is done
        }

        // called when the user selects a contextual menu item
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.all:zipAdapter.toggleChecked(true,"");
         mode.invalidate();
                    return true;
                case R.id.ex:
                    try {Toast.makeText(getActivity(), new Futils().getString(getActivity(),R.string.extracting),Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity(), ExtractService.class);
                        ArrayList<String> a=new ArrayList<String>();
                        for(int i:zipAdapter.getCheckedItemPositions()){
                            a.add(elements.get(i).getName());
                        }
                       intent.putExtra("zip",f.getPath());
                        intent.putExtra("entries1",true);
                        intent.putExtra("entries",a);
                        getActivity().startService(intent);
                    } catch (Exception e) {
                        e.printStackTrace();}
                    mode.finish();
                    return true;}return false;}

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
        if(zipAdapter!=null)zipAdapter.toggleChecked(false,"");
            selection=false;
        ObjectAnimator anim = ObjectAnimator.ofInt(rootView.findViewById(R.id.buttonbarframe), "backgroundColor", getResources().getColor(R.color.toolbar_cab), Color.parseColor(skin));
        anim.setDuration(50);
        anim.setEvaluator(new ArgbEvaluator());
        anim.start();
        if (Build.VERSION.SDK_INT >= 21) {

            Window window = getActivity().getWindow();
            if(mainActivity.colourednavigation)window.setNavigationBarColor(mainActivity.skinStatusBar);
        }mActionMode=null;}
    };
    @Override
    public void onResume() {
        super.onResume();
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        floatingActionButton.hide(true);
        if (files.size()==1) {

            new DeleteTask(getActivity().getContentResolver(),  getActivity(), this).execute(files);
        }
    }
public boolean cangoBack(){
    if(current==null || current.trim().length()==0)
    return false;
    else return true;
}
    public void goBack() {

        new ZipHelperTask(this, new File(current).getParent()).execute(f);
    }
    void refresh(){
        new ZipHelperTask(this, current).execute(f);
    }
    public void bbar(){
        ((TextView) zipViewer.rootView.findViewById(R.id.fullpath)).setText(zipViewer.current);
        ((TextView)rootView.findViewById(R.id.pathname)).setText("");

    }
    public void createviews(ArrayList<ZipObj> zipEntries,String dir){
        zipViewer.zipAdapter = new ZipAdapter(zipViewer.getActivity(), R.layout.simplerow, zipEntries, zipViewer);
        zipViewer.listView.setAdapter(zipViewer.zipAdapter);
        zipViewer.current = dir;
        zipViewer.bbar();
        swipeRefreshLayout.setRefreshing(false);
    }
    public void loadlist(String path){
        File f=new File(path);
        new ZipHelperTask(this,"").execute(f);

    }
}
