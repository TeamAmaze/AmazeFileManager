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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.ProgressListener;
import com.amaze.filemanager.R;
import com.amaze.filemanager.RegisterCallback;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.amaze.filemanager.ui.icons.IconUtils;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.util.ArrayList;

public class ProcessViewer extends Fragment {

    LinearLayout rootView;
    boolean mBound = false;

    // Ids to differentiate between already processed data package and new data package
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
        if (mainActivity.getAppTheme().equals(AppTheme.DARK))
            root.setBackgroundResource((R.color.cardView_background));
        rootView = (LinearLayout) root.findViewById(R.id.secondbut);
        //((MainActivity)getActivity()).getSupportActionBar().setTitle(utils.getString(getActivity(),R.string.processes));
        mainActivity.setActionBarTitle(getResources().getString(R.string.processes));
        mainActivity.floatingActionButton.hideMenuButton(true);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        icons = new IconUtils(Sp, getActivity());
        mainActivity.supportInvalidateOptionsMenu();
        return root;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RegisterCallback binder = (RegisterCallback.Stub.asInterface(service));
            mBound = true;
            try {
                for(DataPackage dataPackage:binder.getCurrent()){
                    processResults(dataPackage);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                binder.registerCallBack(new ProgressListener.Stub() {
                    @Override
                    public void onUpdate(final DataPackage dataPackage) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processResults(dataPackage);
                            }
                        });
                    }

                    @Override
                    public void refresh() {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clear();
                            }
                        });
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            /*
            for (int i : mService.hash1.keySet()) {
                processResults(mService.hash1.get(i));
            }
            mService.setProgressListener(new CopyService.ProgressListener() {
                @Override
                public void onUpdate(DataPackage dataPackage) {
                    processResults(dataPackage);
                }

                @Override
                public void refresh() {
                    clear();
                }
            });*/
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private ServiceConnection mExtractConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ExtractService.LocalBinder binder = (ExtractService.LocalBinder) service;
            ExtractService mService = binder.getService();
            mBound = true;
            for (int i : mService.hash1.keySet()) {
                processExtractResults(mService.hash1.get(i));
            }
            mService.setProgressListener(new ExtractService.ProgressListener() {
                @Override
                public void onUpdate(DataPackage dataPackage) {
                    processExtractResults(dataPackage);
                }

                @Override
                public void refresh() {
                    clear();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private ServiceConnection mCompressConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ZipTask.LocalBinder binder = (ZipTask.LocalBinder) service;
            ZipTask mService = binder.getService();
            mBound = true;
            for (int i : mService.hash1.keySet()) {
                processCompressResults(mService.hash1.get(i));
            }
            mService.setProgressListener(new ZipTask.ProgressListener() {
                @Override
                public void onUpdate(DataPackage dataPackage) {
                    processCompressResults(dataPackage);
                }

                @Override
                public void refresh() {
                    clear();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    boolean running = false;

    @Override
    public void onResume() {
        super.onResume();
        running = true;
        Intent intent = new Intent(getActivity(), CopyService.class);
        getActivity().bindService(intent, mConnection, 0);
        Intent intent1 = new Intent(getActivity(), ExtractService.class);
        getActivity().bindService(intent1, mExtractConnection, 0);
        Intent intent2 = new Intent(getActivity(), ZipTask.class);
        getActivity().bindService(intent2, mCompressConnection, 0);
    }
    void clear(){
        rootView.removeAllViewsInLayout();
        CopyIds.clear();
        CancelledCopyIds.clear();
        ExtractIds.clear();
        CancelledExtractIds.clear();
        ZipIds.clear();
        CancelledZipIds.clear();
    }
    @Override
    public void onPause() {
        super.onPause();
        running = false;
        getActivity().unbindService(mConnection);
        getActivity().unbindService(mExtractConnection);
        getActivity().unbindService(mCompressConnection);
        clear();
    }

    public void processResults(final DataPackage dataPackage) {
        if (!running) return;
        if (getResources() == null) return;
        if (dataPackage != null) {
            int id = dataPackage.getId();
            final Integer id1 = new Integer(id);

            if (!CancelledCopyIds.contains(id1)) {
                // new data package utilizing

                if (CopyIds.contains(id1)) {
                    // views have been initialized, just update the progress from new data package
                    boolean completed = dataPackage.isCompleted();
                    View process = rootView.findViewWithTag("copy" + id);
                    if (completed) {
                        try {

                            // operation completed, remove views
                            rootView.removeViewInLayout(process);
                            CopyIds.remove(CopyIds.indexOf(id1));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {

                        // update views from data package
                        String name = dataPackage.getName();
                        long total = dataPackage.getTotal();
                        long doneBytes = dataPackage.getByteProgress();

                        double progressPercent = (doneBytes/total)*100;

                        boolean move = dataPackage.isMove();
                        String text = getResources().getString(R.string.copying) + "\n" + name + "\n" + Futils.readableFileSize(doneBytes)
                                + "/" + Futils.readableFileSize(total) + "\n" + progressPercent + "%";
                        if (move) {
                            text = getResources().getString(R.string.moving) + "\n"
                                    + name + "\n"
                                    + Futils.readableFileSize(doneBytes)
                                    + "/" + Futils.readableFileSize(total) + "\n"
                                    + progressPercent + "%" + "\n"
                                    + dataPackage.getSourceProgress() + "/"
                                    + dataPackage.getSourceFiles() + "\n"
                                    + Futils.readableFileSize(dataPackage.getSpeedRaw());
                        }
                        ((TextView) process.findViewById(R.id.progressText)).setText(text);
                        ProgressBar p = (ProgressBar) process.findViewById(R.id.progressBar1);
                        p.setProgress((int) Math.round(progressPercent));
                    }
                } else {

                    // initialize views for first time
                    CardView root = (android.support.v7.widget.CardView) getActivity()
                            .getLayoutInflater().inflate(R.layout.processrow, null);
                    root.setTag("copy" + id);

                    ImageButton cancel = (ImageButton) root.findViewById(R.id.delete_button);
                    TextView progressText = (TextView) root.findViewById(R.id.progressText);

                    Drawable icon = icons.getCopyDrawable();
                    boolean move = dataPackage.isMove();
                    if (move) {
                        icon = icons.getCutDrawable();
                    }
                    if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                        cancel.setImageResource(R.drawable.ic_action_cancel);
                        root.setCardBackgroundColor(R.color.cardView_foreground);
                        root.setCardElevation(0f);
                        progressText.setTextColor(Color.WHITE);
                    } else {

                        icon.setColorFilter(Color.parseColor("#666666"), PorterDuff.Mode.SRC_ATOP);
                        progressText.setTextColor(Color.BLACK);
                    }

                    ((ImageView) root.findViewById(R.id.progressImage)).setImageDrawable(icon);
                    cancel.setOnClickListener(new View.OnClickListener() {

                        public void onClick(View p1) {
                            Toast.makeText(getActivity(), getResources().getString(R.string.stopping), Toast.LENGTH_LONG).show();
                            Intent i = new Intent("copycancel");
                            i.putExtra("id", id1);
                            getActivity().sendBroadcast(i);
                            rootView.removeView(rootView.findViewWithTag("copy" + id1));

                            CopyIds.remove(CopyIds.indexOf(id1));
                            CancelledCopyIds.add(id1);
                            // TODO: Implement this method
                        }
                    });

                    String name = dataPackage.getName();
                    long doneBytes = dataPackage.getByteProgress();
                    long totalBytes = dataPackage.getTotal();
                    double progressPercent = (doneBytes/totalBytes)*100;

                    String text = getResources().getString(R.string.copying) + "\n" + name;
                    if (move) {
                        text = getResources().getString(R.string.moving) + "\n" + name;
                    }
                    progressText.setText(text);
                    ProgressBar p = (ProgressBar) root.findViewById(R.id.progressBar1);
                    p.setProgress((int) Math.round(progressPercent));
                    CopyIds.add(id1);
                    rootView.addView(root);
                }
            }
        }
    }

    public void processExtractResults(DataPackage dataPackage) {
        if (!running) return;
        if (getResources() == null) return;
        final int id = dataPackage.getId();

        if (!CancelledExtractIds.contains(id)) {
            if (ExtractIds.contains(id)) {

                boolean completed = dataPackage.isCompleted();
                View process = rootView.findViewWithTag("extract" + id);
                if (completed) {
                    rootView.removeViewInLayout(process);
                    ExtractIds.remove(ExtractIds.indexOf(id));
                } else {
                    String name = dataPackage.getName();
                    long totalBytes = dataPackage.getTotal();
                    long doneBytes = dataPackage.getByteProgress();
                    double progressPercent = (doneBytes/totalBytes)*100;

                    ProgressBar p = (ProgressBar) process.findViewById(R.id.progressBar1);
                    if (progressPercent <= 100) {
                        ((TextView) process.findViewById(R.id.progressText)).setText(getResources().getString(R.string.extracting)
                                + "\n" + name + "\n" + progressPercent + "%" + "\n"
                                + Futils.readableFileSize(doneBytes)
                                + "/" + Futils.readableFileSize(totalBytes));

                        p.setProgress((int) Math.round(progressPercent));
                    }
                }
            } else {
                CardView root = (CardView) getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
                root.setTag("extract" + id);

                ImageView progressImage = ((ImageView) root.findViewById(R.id.progressImage));
                ImageButton cancel = (ImageButton) root.findViewById(R.id.delete_button);
                TextView progressText = (TextView) root.findViewById(R.id.progressText);

                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    root.setCardBackgroundColor(R.color.cardView_foreground);
                    root.setCardElevation(0f);
                    cancel.setImageResource(R.drawable.ic_action_cancel);
                    progressText.setTextColor(Color.WHITE);
                    progressImage.setImageResource(R.drawable.ic_doc_compressed);
                } else {

                    // cancel has default src set for light theme
                    progressText.setTextColor(Color.BLACK);
                    progressImage.setImageResource(R.drawable.ic_doc_compressed_black);
                }

                cancel.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View p1) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.stopping), Toast.LENGTH_LONG).show();
                        Intent i = new Intent("excancel");
                        i.putExtra("id", id);
                        getActivity().sendBroadcast(i);
                        rootView.removeView(rootView.findViewWithTag("extract" + id));

                        ExtractIds.remove(ExtractIds.indexOf(id));
                        CancelledExtractIds.add(id);
                        // TODO: Implement this method
                    }
                });

                String name = dataPackage.getName();
                long totalBytes = dataPackage.getTotal();
                long doneBytes = dataPackage.getByteProgress();
                double progressPercent = (doneBytes/totalBytes)*100;


                ((TextView) root.findViewById(R.id.progressText)).setText(getResources().getString(R.string.extracting) + "\n" + name);
                ProgressBar p = (ProgressBar) root.findViewById(R.id.progressBar1);
                p.setProgress((int) Math.round(progressPercent));
                ExtractIds.add(id);
                rootView.addView(root);
            }
        }
    }

    void processCompressResults(DataPackage dataPackage) {
        final int id = dataPackage.getId();

        if (!CancelledZipIds.contains(id)) {
            if (ZipIds.contains(id)) {
                boolean completed = dataPackage.isCompleted();
                View process = rootView.findViewWithTag("zip" + id);
                if (completed) {
                    rootView.removeViewInLayout(process);
                    ZipIds.remove(ZipIds.indexOf(id));
                } else {
                    String name = dataPackage.getName();
                    long totalBytes = dataPackage.getTotal();
                    long doneBytes = dataPackage.getByteProgress();
                    double progressPercent = (doneBytes/totalBytes)*100;

                    ProgressBar p = (ProgressBar) process.findViewById(R.id.progressBar1);
                    if (progressPercent <= 100) {
                        ((TextView) process.findViewById(R.id.progressText)).setText(getResources().getString(R.string.zipping)
                                + "\n" + name + "\n"
                                + progressPercent + "%");

                        p.setProgress((int) Math.round(progressPercent));
                    }
                }
            } else {
                CardView root = (CardView) getActivity().getLayoutInflater().inflate(R.layout.processrow, null);
                root.setTag("zip" + id);

                ImageView progressImage = ((ImageView) root.findViewById(R.id.progressImage));
                ImageButton cancel = (ImageButton) root.findViewById(R.id.delete_button);
                TextView progressText = (TextView) root.findViewById(R.id.progressText);

                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    root.setCardBackgroundColor(R.color.cardView_foreground);
                    root.setCardElevation(0f);
                    cancel.setImageResource(R.drawable.ic_action_cancel);
                    progressText.setTextColor(Color.WHITE);
                    progressImage.setImageResource(R.drawable.ic_doc_compressed);
                } else {

                    // cancel has default src set for light theme
                    progressText.setTextColor(Color.BLACK);
                    progressImage.setImageResource(R.drawable.ic_doc_compressed_black);
                }

                cancel.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View p1) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.stopping), Toast.LENGTH_LONG).show();
                        Intent i = new Intent("zipcancel");
                        i.putExtra("id", id);
                        getActivity().sendBroadcast(i);
                        rootView.removeView(rootView.findViewWithTag("zip" + id));

                        ZipIds.remove(ZipIds.indexOf(id));
                        CancelledZipIds.add(id);
                        // TODO: Implement this method
                    }
                });

                String name = dataPackage.getName();
                long totalBytes = dataPackage.getTotal();
                long doneBytes = dataPackage.getByteProgress();
                double progressPercent = (doneBytes/totalBytes)*100;

                ((TextView) root.findViewById(R.id.progressText)).setText(getResources().getString(R.string.zipping) + "\n" + name);
                ProgressBar p = (ProgressBar) root.findViewById(R.id.progressBar1);
                p.setProgress((int) Math.round(progressPercent));

                ZipIds.add(id);
                rootView.addView(root);
            }
        }
    }

}
