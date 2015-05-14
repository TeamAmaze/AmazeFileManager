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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.BooksAdapter;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.Shortcuts;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;


public class BookmarksManager extends Fragment {
    Futils utils = new Futils();
    Shortcuts s;
    BooksAdapter b;
    SharedPreferences Sp;
    public IconUtils icons;
    ArrayList<File> bx;
    public MainActivity m;
    int theme,theme1;
    View rootView;
    RecyclerView listview;
    Context c;
    SwipeRefreshLayout swipeRefreshLayout;
    LinearLayoutManager linearLayoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.bookmark_frag, container, false);
        MainActivity mainActivity=(MainActivity)getActivity();
        mainActivity.toolbar.setTitle(utils.getString(getActivity(),R.string.bookmanag));

        mainActivity.tabsSpinner.setVisibility(View.GONE);
        mainActivity.floatingActionButton.setVisibility(View.GONE);
        mainActivity.buttonBarFrame.setVisibility(View.GONE);

        listview=(RecyclerView)rootView.findViewById(R.id.listView);
        c=getActivity();
        linearLayoutManager=new LinearLayoutManager(c);
        listview.setLayoutManager(linearLayoutManager);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
        setRetainInstance(false);
        s = new Shortcuts(getActivity());
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        theme=Integer.parseInt(Sp.getString("theme","0"));
        swipeRefreshLayout=(SwipeRefreshLayout)rootView.findViewById(R.id.activity_main_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        theme1 = theme==2 ? PreferenceUtils.hourOfDay() : theme;
        if(theme1==1) {
            //getActivity().getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
            listview.setBackgroundColor(getResources().getColor(R.color.holo_dark_background));
        }
        m=(MainActivity)getActivity();
        m.supportInvalidateOptionsMenu();
        m.floatingActionButton.setVisibility(View.GONE);
        Animation animation1 = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_newtab);
        FloatingActionButton floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.fab1);
        //floatingActionButton.show(true);
        floatingActionButton.setColorNormal(Color.parseColor(((MainActivity)getActivity()).fabskin));
        floatingActionButton.setColorPressed(Color.parseColor(((MainActivity)getActivity()).fabSkinPressed));

        //floatingActionButton.setAnimation(animation1);
        //getActivity().findViewById(R.id.fab).setVisibility(View.VISIBLE);
        listview.setHasFixedSize(true);
        getActivity().findViewById(R.id.fab1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final MaterialDialog.Builder ba1 = new MaterialDialog.Builder(getActivity());
                ba1.title(utils.getString(getActivity(), R.string.addbook));
                View v = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
                final EditText edir = (EditText) v.findViewById(R.id.newname);
                edir.setHint(utils.getString(getActivity(), R.string.enterpath));
                ba1.customView(v, true);
                if(theme1==1)ba1.theme(Theme.DARK);
                ba1.negativeText(R.string.cancel);
                ba1.positiveText(R.string.create);
                String fabskin = Sp.getString("fab_skin_color", "#e91e63");
                ba1.positiveColor(Color.parseColor(fabskin));
                ba1.negativeColor(Color.parseColor(fabskin));
                ba1.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        try {
                            File a = new File(edir.getText().toString());
                            if (a.isDirectory()) {
                                s.addS(a);
                                b.items.add(a);
                                b.notifyDataSetChanged();
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.success), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.invalid_dir), Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.error), Toast.LENGTH_LONG).show();
                        }m.updateDrawer();
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {

                    }
                });
                ba1.build().show();

            }
        });

        icons = new IconUtils(Sp, getActivity());
        if (savedInstanceState == null)
            refresh();
        else {bx=utils.toFileArray(savedInstanceState.getStringArrayList("bx"));
            refresh(bx);
            linearLayoutManager.scrollToPositionWithOffset(savedInstanceState.getInt("index"), savedInstanceState.getInt("top"));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        if (listview != null) {
            b.putStringArrayList("bx", utils.toStringArray(bx));
            int index = linearLayoutManager.findFirstVisibleItemPosition();
            View vi = listview.getChildAt(0);
            int top = (vi == null) ? 0 : vi.getTop();
            b.putInt("index", index);
            b.putInt("top", top);
        }
    }

    public void refresh() {

        new LoadList().execute();
    }
    public class LoadList extends AsyncTask<Void, Void, ArrayList<File>> {

        public LoadList() {

        }

        @Override
        protected void onPreExecute() {
        }


        @Override
        // Actual download method, run in the task thread
        protected ArrayList<File> doInBackground(Void... params) {

            // params comes from the execute() call: params[0] is the url.
            try {

                bx = s.readS();
                if(bx==null || bx.size()==0){
                     s.makeS();
                    bx=s.readS();
                }
            } catch (Exception e) {
                try {
                    s.makeS();
                    bx=s.readS();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            return bx;
        }
        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(ArrayList<File> bitmap) {
            refresh(bitmap);
        }
    }

    public void refresh(ArrayList<File> f) {
        b = new BooksAdapter(c, R.layout.bookmarkrow, f, this);
        listview.setAdapter(b);
        swipeRefreshLayout.setRefreshing(false);
    }
}
