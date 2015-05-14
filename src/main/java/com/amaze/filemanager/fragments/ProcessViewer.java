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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.IconUtils;

import java.util.ArrayList;

public class ProcessViewer extends Fragment {

    LinearLayout rootView;
    Futils utils = new Futils();
    ArrayList<Integer> CopyIds = new ArrayList<Integer>();
    ArrayList<Integer> CancelledCopyIds = new ArrayList<Integer>();
    ArrayList<Integer> ExtractIds = new ArrayList<Integer>();
    ArrayList<Integer> CancelledExtractIds = new ArrayList<Integer>();
    ArrayList<Integer> ZipIds = new ArrayList<Integer>();
    ArrayList<Integer> CancelledZipIds = new ArrayList<Integer>();
    SharedPreferences Sp;
    IconUtils icons;
    MainActivity mainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = (ViewGroup) inflater.inflate(R.layout.processparent,
                container, false);
        setRetainInstance(false);


        mainActivity = (MainActivity) getActivity();
        if(mainActivity.theme1==1)
            root.setBackgroundResource(getResources().getColor(R.color.holo_dark_background));
        rootView = (LinearLayout) root.findViewById(R.id.secondbut);
        //((MainActivity)getActivity()).getSupportActionBar().setTitle(utils.getString(getActivity(),R.string.processes));
        mainActivity.toolbar.setTitle(utils.getString(getActivity(), R.string.processes));
        mainActivity.tabsSpinner.setVisibility(View.GONE);
        mainActivity.floatingActionButton.setVisibility(View.GONE);

        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        icons = new IconUtils(Sp, getActivity());
        mainActivity.supportInvalidateOptionsMenu();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
       (getActivity()).registerReceiver(Copy_Receiver, new IntentFilter("copy"));
       (getActivity()).registerReceiver(Extract_Receiver, new IntentFilter("EXTRACT_CONDITION"));
       (getActivity()).registerReceiver(Zip_Receiver, new IntentFilter("ZIPPING"));
    }

    @Override
    public void onPause() {
        super.onPause();
       (getActivity()).unregisterReceiver(Copy_Receiver);
       (getActivity()).unregisterReceiver(Extract_Receiver);
       (getActivity()).unregisterReceiver(Zip_Receiver);
        rootView.removeAllViewsInLayout();
        CopyIds.clear();
        CancelledCopyIds.clear();
        ExtractIds.clear();
        CancelledExtractIds.clear();
        ZipIds.clear();
        CancelledZipIds.clear();
    }

    private BroadcastReceiver Copy_Receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
          PendingResult pendingResult=  goAsync();
            Bundle b = arg1.getExtras();
            if (b != null) {
                int id = b.getInt("id");
                final Integer id1 = new Integer(id);
                if (!CancelledCopyIds.contains(id1)) {
                    if (CopyIds.contains(id1)) {

                        boolean completed = b.getBoolean("COPY_COMPLETED", false);
                        View process = rootView.findViewWithTag("copy" + id);
                        if (completed) {
                            rootView.removeViewInLayout(process);
                            CopyIds.remove(CopyIds.indexOf(id1));
                        } else {
                            String name = b.getString("name");
                            int p1 = b.getInt("p1");
                            int p2 = b.getInt("p2");
                            long total = b.getLong("total");
                            long done = b.getLong("done");
                            boolean move = b.getBoolean("move", false);
                            String text = utils.getString(getActivity(), R.string.copying) + "\n" + name + "\n" + utils.readableFileSize(done) + "/" + utils.readableFileSize(total) + "\n" + p1 + "%";
                            if (move) {
                                text = utils.getString(getActivity(), R.string.moving) + "\n" + name + "\n" + utils.readableFileSize(done) + "/" + utils.readableFileSize(total) + "\n" + p1 + "%";
                            }
                            ((TextView) process.findViewById(R.id.progressText)).setText(text);
                            ProgressBar p = (ProgressBar) process.findViewById(R.id.progressBar1);
                            p.setProgress(p1);
                            p.setSecondaryProgress(p2);
                        }
                    } else {
                        View root = getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
                        root.setTag("copy" + id);
                        ImageButton cancel = (ImageButton) root.findViewById(R.id.delete_button);
                        Drawable icon = icons.getCopyDrawable();
                        boolean move = b.getBoolean("move", false);
                        if (move) {
                            icon = icons.getCutDrawable();
                        }
                        if(mainActivity.theme1==1)cancel.setImageResource(R.drawable.ic_action_cancel);
                        ((ImageView) root.findViewById(R.id.progressImage)).setImageDrawable(icon);
                        cancel.setOnClickListener(new View.OnClickListener() {

                            public void onClick(View p1) {
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.stopping),Toast.LENGTH_LONG).show();
                                Intent i = new Intent("copycancel");
                                i.putExtra("id", id1);
                                getActivity().sendBroadcast(i);
                                rootView.removeView(rootView.findViewWithTag("copy" + id1));

                                CopyIds.remove(CopyIds.indexOf(id1));
                                CancelledCopyIds.add(id1);
                                // TODO: Implement this method
                            }
                        });

                        String name = b.getString("name");
                        int p1 = b.getInt("p1");
                        int p2 = b.getInt("p2");

                        String text = utils.getString(getActivity(), R.string.copying) + "\n" + name;
                        if (move) {
                            text = utils.getString(getActivity(), R.string.moving) + "\n" + name;
                        }
                        ((TextView) root.findViewById(R.id.progressText)).setText(text);
                        ProgressBar p = (ProgressBar) root.findViewById(R.id.progressBar1);
                        p.setProgress(p1);
                        p.setSecondaryProgress(p2);
                        CopyIds.add(id1);
                        rootView.addView(root);
                    }
                }
            pendingResult.finish();}
        }
    };
    private BroadcastReceiver Extract_Receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            PendingResult pendingResult=goAsync();
            Bundle b = arg1.getExtras();
            if (b != null) {
                final int id = b.getInt("id");

                if (!CancelledExtractIds.contains(id)) {
                    if (ExtractIds.contains(id)) {

                        boolean completed = b.getBoolean("extract_completed", false);
                        boolean indefinite=b.getBoolean("indefinite",false);
                        View process = rootView.findViewWithTag("extract" + id);
                        if (completed) {
                            rootView.removeViewInLayout(process);
                            ExtractIds.remove(ExtractIds.indexOf(id));
                        } else {
                            String name = b.getString("name");
                            int p1 = b.getInt("p1",0);
                            long p3=b.getLong("total");
                            long p2=b.getLong("done");
                            ProgressBar p = (ProgressBar) process.findViewById(R.id.progressBar1);
                            if (p1 <= 100) {
                                ((TextView) process.findViewById(R.id.progressText)).setText(utils.getString(getActivity(), R.string.extracting) + "\n" + name + "\n" + p1 + "%"+"\n"+utils.readableFileSize(p2)+"/"+utils.readableFileSize(p3));

                                p.setProgress(p1);
                                if(indefinite && !p.isIndeterminate())p.setIndeterminate(true);
                            }
                        }
                    } else {
                        View root = getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
                        root.setTag("extract" + id);
                        ((ImageView) root.findViewById(R.id.progressImage)).setImageDrawable(getResources().getDrawable(R.drawable.ic_doc_compressed_black));
                        ImageButton cancel = (ImageButton) root.findViewById(R.id.delete_button);
                        if(mainActivity.theme1==1)cancel.setImageResource(R.drawable.ic_action_cancel);
                        cancel.setOnClickListener(new View.OnClickListener() {

                            public void onClick(View p1) {
                              Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.stopping), Toast.LENGTH_LONG).show();
                                Intent i = new Intent("excancel");
                                i.putExtra("id", id);
                                getActivity().sendBroadcast(i);
                                rootView.removeView(rootView.findViewWithTag("extract" + id));

                                ExtractIds.remove(ExtractIds.indexOf(id));
                                CancelledExtractIds.add(id);
                                // TODO: Implement this method
                            }
                        });

                        String name = b.getString("name");
                        int p1 = b.getInt("p1",0);


                        ((TextView) root.findViewById(R.id.progressText)).setText(utils.getString(getActivity(), R.string.extracting) + "\n" + name);
                        ProgressBar p = (ProgressBar) root.findViewById(R.id.progressBar1);
                        p.setProgress(p1);
                        ExtractIds.add(id);
                        rootView.addView(root);
                    }
                }
            pendingResult.finish();}
        }
    };
    private BroadcastReceiver Zip_Receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            PendingResult pendingResult=goAsync();
            Bundle b = arg1.getExtras();

            if (b != null) {
                final int id = b.getInt("id");

                if (!CancelledZipIds.contains(id)) {
                    if (ZipIds.contains(id)) {

                        boolean completed = b.getBoolean("ZIP_COMPLETED", false);
                        View process = rootView.findViewWithTag("zip" + id);
                        if (completed) {
                            rootView.removeViewInLayout(process);
                            ZipIds.remove(ZipIds.indexOf(id));
                        } else {
                            String name = b.getString("name");
                            int p1 = b.getInt("ZIP_PROGRESS");

                            ProgressBar p = (ProgressBar) process.findViewById(R.id.progressBar1);
                            if (p1 <= 100) {
                                ((TextView) process.findViewById(R.id.progressText)).setText(utils.getString(getActivity(), R.string.zipping) + "\n" + name + "\n" + p1 + "%");

                                p.setProgress(p1);
                            }
                        }
                    } else {
                        View root = getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
                        root.setTag("zip" + id);
                        ((ImageView) root.findViewById(R.id.progressImage)).setImageDrawable(getResources().getDrawable(R.drawable.ic_doc_compressed_black));
                        ImageButton cancel = (ImageButton) root.findViewById(R.id.delete_button);
                        if(mainActivity.theme1==1)cancel.setImageResource(R.drawable.ic_action_cancel);
                        cancel.setOnClickListener(new View.OnClickListener() {

                            public void onClick(View p1) {
                                Toast.makeText(getActivity(), utils.getString(getActivity(), R.string.stopping), Toast.LENGTH_LONG).show();
                                Intent i = new Intent("zipcancel");
                                i.putExtra("id", id);
                                getActivity().sendBroadcast(i);
                                rootView.removeView(rootView.findViewWithTag("zip" + id));

                                ZipIds.remove(ZipIds.indexOf(id));
                                CancelledZipIds.add(id);
                                // TODO: Implement this method
                            }
                        });

                        String name = b.getString("name");
                        int p1 = b.getInt("ZIP_PROGRESS");


                        ((TextView) root.findViewById(R.id.progressText)).setText(utils.getString(getActivity(), R.string.zipping) + "\n" + name);
                        ProgressBar p = (ProgressBar) root.findViewById(R.id.progressBar1);
                        p.setProgress(p1);

                        ZipIds.add(id);
                        rootView.addView(root);
                    }
                }
          pendingResult.finish();  }
        }
    };
}
