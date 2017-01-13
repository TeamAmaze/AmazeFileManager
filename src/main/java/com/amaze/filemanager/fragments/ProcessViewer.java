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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.amaze.filemanager.ui.icons.IconUtils;
import com.amaze.filemanager.utils.DataPackage;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class ProcessViewer extends Fragment {

    private View rootView;
    private CardView mCardView;

    boolean isInitialized = false;
    SharedPreferences Sp;
    IconUtils icons;
    MainActivity mainActivity;
    int accentColor, primaryColor;
    ImageButton mCancelButton;
    TextView mProgressTextView;
    private ProgressBar mProgressBar;

    private LineChart mLineChart;
    private LineData mLineData = new LineData();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.processparent, container, false);
        setRetainInstance(false);

        mainActivity = (MainActivity) getActivity();

        accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);
        primaryColor = mainActivity.getColorPreference().getColor(ColorUsage.getPrimary(MainActivity.currentTab));
        if (mainActivity.getAppTheme().equals(AppTheme.DARK))
            rootView.setBackgroundResource((R.color.cardView_background));
        mainActivity.updateViews(new ColorDrawable(primaryColor));
        mainActivity.setActionBarTitle(getResources().getString(R.string.processes));
        mainActivity.floatingActionButton.hideMenuButton(true);
        Sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        icons = new IconUtils(Sp, getActivity());
        mainActivity.supportInvalidateOptionsMenu();

        mCardView = (CardView) rootView.findViewById(R.id.card_view);

        mLineChart = (LineChart) rootView.findViewById(R.id.progress_chart);

        mCancelButton = (ImageButton) rootView.findViewById(R.id.delete_button);
        mProgressTextView = (TextView) rootView.findViewById(R.id.progressText);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar1);

        Drawable icon = icons.getCopyDrawable();

        if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

            mCancelButton.setImageResource(R.drawable.ic_action_cancel);
            mCardView.setCardBackgroundColor(getResources().getColor(R.color.cardView_foreground));
            mCardView.setCardElevation(0f);
            mProgressTextView.setTextColor(Color.WHITE);
        } else {

            icon.setColorFilter(Color.parseColor("#666666"), PorterDuff.Mode.SRC_ATOP);
            mProgressTextView.setTextColor(Color.BLACK);
        }

        mCancelButton.setImageDrawable(icon);
        mCancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                Toast.makeText(getActivity(), getResources().getString(R.string.stopping), Toast.LENGTH_LONG).show();
                Intent i = new Intent(CopyService.TAG_BROADCAST_COPY_CANCEL);
                getActivity().sendBroadcast(i);
                mProgressTextView.setText(getString(R.string.cancel));
            }
        });

        return rootView;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance

            CopyService.LocalBinder localBinder = (CopyService.LocalBinder) service;
            CopyService copyService = localBinder.getService();

            ArrayList<DataPackage> dataPackages;
            try {
                dataPackages = copyService.getDataPackageList();
            } catch (ConcurrentModificationException e) {
                // array list was being modified while fetching (even after synchronization) :/
                // return for now
                return;
            }

            for (final DataPackage dataPackage : dataPackages) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        processResults(dataPackage);
                    }
                });
            }

            copyService.setProgressListener(new CopyService.ProgressListener() {
                @Override
                public void onUpdate(final DataPackage dataPackage) {
                    if (getActivity()==null) {
                        // callback called when we're not inside the app
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            processResults(dataPackage);
                        }
                    });
                }

                @Override
                public void refresh() {

                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private ServiceConnection mExtractConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ExtractService.LocalBinder localBinder = (ExtractService.LocalBinder) service;
            ExtractService extractService = localBinder.getService();

            ArrayList<DataPackage> dataPackages;
            try {
                dataPackages = extractService.getDataPackageList();
            } catch (ConcurrentModificationException e) {
                // array list was being modified while fetching (even after synchronization) :/
                // return for now
                return;
            }

            for (final DataPackage dataPackage : dataPackages) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        processResults(dataPackage);
                    }
                });
            }

            extractService.setProgressListener(new ExtractService.ProgressListener() {
                @Override
                public void onUpdate(final DataPackage dataPackage) {
                    if (getActivity()==null) {
                        // callback called when we're not inside the app
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            processResults(dataPackage);
                        }
                    });
                }

                @Override
                public void refresh() {

                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private ServiceConnection mCompressConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ZipTask.LocalBinder binder = (ZipTask.LocalBinder) service;
            ZipTask mService = binder.getService();
            for (int i : mService.hash1.keySet()) {
                //processCompressResults(mService.hash1.get(i));
            }
            mService.setProgressListener(new ZipTask.ProgressListener() {
                @Override
                public void onUpdate(DataPackage dataPackage) {
                    //processCompressResults(dataPackage);
                }

                @Override
                public void refresh() {
                    //clear();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(getActivity(), CopyService.class);
        getActivity().bindService(intent, mConnection, 0);
        Intent intent1 = new Intent(getActivity(), ExtractService.class);
        getActivity().bindService(intent1, mExtractConnection, 0);
        Intent intent2 = new Intent(getActivity(), ZipTask.class);
        getActivity().bindService(intent2, mCompressConnection, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(mConnection);
        getActivity().unbindService(mExtractConnection);
        getActivity().unbindService(mCompressConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void processResults(final DataPackage dataPackage) {
        if (dataPackage != null) {

            boolean completed = dataPackage.isCompleted();

            String name = dataPackage.getName();
            long total = dataPackage.getTotal();
            long doneBytes = dataPackage.getByteProgress();

            float progressPercent = ((float)doneBytes/total)*100;
            boolean move = dataPackage.isMove();

            if (isInitialized) {
                // views have already been initialized, process the data and set new values

                addEntry(Futils.readableFileSizeFloat(doneBytes),
                        Futils.readableFileSizeFloat(dataPackage.getSpeedRaw()));

                String text = (move ? getResources().getString(R.string.moving)
                        : getResources().getString(R.string.copying)) + "\n"
                        + name + "\n"
                        + Futils.readableFileSize(doneBytes)
                        + "/" + Futils.readableFileSize(total) + "\n"
                        + progressPercent + "%" + "\n"
                        + dataPackage.getSourceProgress() + "/"
                        + dataPackage.getSourceFiles() + "\n"
                        + Futils.readableFileSize(dataPackage.getSpeedRaw());
                mProgressTextView.setText(text);
                mProgressBar.setProgress(Math.round(progressPercent));
            } else {
                // initializing views for the first time
                chartInit(total);

                String text = getResources().getString(R.string.copying) + "\n" + name;
                if (move) {
                    text = getResources().getString(R.string.moving) + "\n" + name;
                }
                mProgressTextView.setText(text);
                mProgressBar.setProgress(Math.round(progressPercent));
                isInitialized = true;
            }
        }
    }

    /*public void processExtractResults(DataPackage dataPackage) {
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
    }*/

    /**
     * Add a new entry dynamically to the chart, initializes a {@link LineDataSet} if not done so
     * @param xValue the x-axis value, the number of bytes processed till now
     * @param yValue the y-axis value, bytes processed per sec
     */
    private void addEntry(float xValue, float yValue) {

        ILineDataSet dataSet = mLineData.getDataSetByIndex(0);
        if (dataSet==null) {
            // adding set for first time
            dataSet = createDataSet();
            mLineData.addDataSet(dataSet);
        }

        int randomDataSetIndex = (int) (Math.random() * mLineData.getDataSetCount());
        mLineData.addEntry(new Entry(xValue, yValue), randomDataSetIndex);
        mLineData.notifyDataChanged();

        // let the chart know it's data has changed
        mLineChart.notifyDataSetChanged();
        mLineChart.invalidate();
    }

    /**
     * Creates an instance for {@link LineDataSet} which will store the entries
     * @return
     */
    private LineDataSet createDataSet() {
        LineDataSet lineDataset = new LineDataSet(new ArrayList<Entry>(), null);

        lineDataset.setLineWidth(1.75f);
        lineDataset.setCircleRadius(5f);
        lineDataset.setCircleHoleRadius(2.5f);
        lineDataset.setColor(Color.WHITE);
        lineDataset.setCircleColor(Color.WHITE);
        lineDataset.setHighLightColor(Color.WHITE);
        lineDataset.setDrawValues(false);
        lineDataset.setCircleColorHole(accentColor);

        return lineDataset;
    }

    /**
     * Initialize chart for the first time
     * @param totalBytes maximum value for x-axis
     */
    private void chartInit(long totalBytes) {

        mLineChart.setBackgroundColor(accentColor);
        mLineChart.getLegend().setEnabled(false);

        // no description text
        mLineChart.getDescription().setEnabled(false);

        XAxis xAxis = mLineChart.getXAxis();
        YAxis yAxisLeft = mLineChart.getAxisLeft();
        mLineChart.getAxisRight().setEnabled(false);
        yAxisLeft.setAxisMinimum(0.0f);
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setAxisLineColor(Color.TRANSPARENT);
        yAxisLeft.setSpaceTop(40);

        float factor = 0.5f;
        int whiteTrans = ( (int) ( factor * 255.0f ) << 24 ) | ( Color.WHITE & 0x00ffffff);
        yAxisLeft.setGridColor(whiteTrans);

        xAxis.setAxisMaximum(Futils.readableFileSizeFloat(totalBytes));
        xAxis.setAxisLineColor(Color.TRANSPARENT);
        xAxis.setGridColor(Color.TRANSPARENT);
        xAxis.setTextColor(Color.WHITE);
        mLineChart.setData(mLineData);
        mLineChart.invalidate();
    }
}
