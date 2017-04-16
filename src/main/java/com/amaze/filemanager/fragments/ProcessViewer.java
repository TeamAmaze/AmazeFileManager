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
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.Spanned;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.services.EncryptService;
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
import java.util.concurrent.TimeUnit;

public class ProcessViewer extends Fragment {

    boolean isInitialized = false;
    SharedPreferences sharedPrefs;
    IconUtils icons;
    MainActivity mainActivity;
    int accentColor, primaryColor;
    ImageButton mCancelButton;
    ImageView mProgressImage;

    private View rootView;
    private CardView mCardView;
    private LineChart mLineChart;
    private LineData mLineData = new LineData();
    private long time = 0L;
    private TextView mProgressTypeText, mProgressFileNameText,
            mProgressBytesText, mProgressFileText,  mProgressSpeedText, mProgressTimer;

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
        mainActivity.setActionBarTitle(getResources().getString(R.string.process_viewer));
        mainActivity.floatingActionButton.hideMenuButton(true);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        icons = new IconUtils(sharedPrefs, getActivity());
        mainActivity.supportInvalidateOptionsMenu();

        mCardView = (CardView) rootView.findViewById(R.id.card_view);

        mLineChart = (LineChart) rootView.findViewById(R.id.progress_chart);
        mProgressImage = (ImageView) rootView.findViewById(R.id.progress_image);
        mCancelButton = (ImageButton) rootView.findViewById(R.id.delete_button);
        mProgressTypeText = (TextView) rootView.findViewById(R.id.text_view_progress_type);
        mProgressFileNameText = (TextView) rootView.findViewById(R.id.text_view_progress_file_name);
        mProgressBytesText = (TextView) rootView.findViewById(R.id.text_view_progress_bytes);
        mProgressFileText = (TextView) rootView.findViewById(R.id.text_view_progress_file);
        mProgressSpeedText = (TextView) rootView.findViewById(R.id.text_view_progress_speed);
        mProgressTimer = (TextView) rootView.findViewById(R.id.text_view_progress_timer);

        if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

            mCancelButton.setImageResource(R.drawable.ic_action_cancel);
            mCardView.setCardBackgroundColor(getResources().getColor(R.color.cardView_foreground));
            mCardView.setCardElevation(0f);
        }

        return rootView;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance

            CopyService.LocalBinder localBinder = (CopyService.LocalBinder) service;
            CopyService copyService = localBinder.getService();

            for (int i=0; i<copyService.getDataPackageSize(); i++) {

                processResults(copyService.getDataPackage(i), ServiceType.COPY);
            }

            // animate the chart a little after initial values have been applied
            mLineChart.animateXY(500, 500);

            copyService.setProgressListener(new CopyService.ProgressListener() {
                @Override
                public void onUpdate(final DataPackage dataPackage) {
                    if (getActivity() == null || getActivity().getSupportFragmentManager().
                            findFragmentByTag(MainActivity.KEY_INTENT_PROCESS_VIEWER) == null) {
                        // callback called when we're not inside the app
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            processResults(dataPackage, ServiceType.COPY);
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

            for (int i=0; i<extractService.getDataPackageSize(); i++) {

                processResults(extractService.getDataPackage(i), ServiceType.EXTRACT);
            }

            // animate the chart a little after initial values have been applied
            mLineChart.animateXY(500, 500);

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

                            processResults(dataPackage, ServiceType.EXTRACT);
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
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ZipTask.LocalBinder localBinder = (ZipTask.LocalBinder) service;
            ZipTask zipTask = localBinder.getService();

            for (int i=0; i<zipTask.getDataPackageSize(); i++) {

                processResults(zipTask.getDataPackage(i), ServiceType.COMPRESS);
            }

            // animate the chart a little after initial values have been applied
            mLineChart.animateXY(500, 500);

            zipTask.setProgressListener(new ZipTask.ProgressListener() {
                @Override
                public void onUpdate(final DataPackage dataPackage) {
                    if (getActivity() == null) {
                        // callback called when we're not inside the app
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            processResults(dataPackage, ServiceType.COMPRESS);
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

    private ServiceConnection mCryptConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            EncryptService.LocalBinder binder = (EncryptService.LocalBinder) service;
            EncryptService encryptService = binder.getService();

            for (int i=0; i<encryptService.getDataPackageSize(); i++) {
                DataPackage dataPackage = encryptService.getDataPackage(i);
                processResults(dataPackage, dataPackage.isMove() ? ServiceType.DECRYPT
                        : ServiceType.ENCRYPT);
            }

            // animate the chart a little after initial values have been applied
            mLineChart.animateXY(500, 500);

            encryptService.setProgressListener(new EncryptService.ProgressListener() {
                @Override
                public void onUpdate(final DataPackage dataPackage) {
                    if (getActivity() == null) {
                        // callback called when we're not inside the app
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            processResults(dataPackage, dataPackage.isMove() ? ServiceType.DECRYPT
                                    : ServiceType.ENCRYPT);
                        }
                    });
                }

                @Override
                public void refresh() {

                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

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

        Intent intent3 = new Intent(getActivity(), EncryptService.class);
        getActivity().bindService(intent3, mCryptConnection, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unbindService(mConnection);
        getActivity().unbindService(mExtractConnection);
        getActivity().unbindService(mCompressConnection);
        getActivity().unbindService(mCryptConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Enum helps defining the result type for {@link #processResults(DataPackage, ServiceType)}
     * to process
     */
    enum ServiceType {

        COPY, EXTRACT, COMPRESS, ENCRYPT, DECRYPT
    }

    public void processResults(final DataPackage dataPackage, ServiceType serviceType) {
        if (dataPackage != null) {
            String name = dataPackage.getName();
            long total = dataPackage.getTotal();
            long doneBytes = dataPackage.getByteProgress();
            boolean move = dataPackage.isMove();

            if (!isInitialized) {

                // initializing views for the first time
                chartInit(total);

                // setting progress image
                setupDrawables(serviceType, move);
                isInitialized = true;
            }

            addEntry(Futils.readableFileSizeFloat(doneBytes),
                    Futils.readableFileSizeFloat(dataPackage.getSpeedRaw()));

            mProgressFileNameText.setText(name);

            Spanned bytesText = Html.fromHtml(getResources().getString(R.string.written)
                    + " <font color='" + accentColor + "'><i>" + Formatter.formatFileSize(getContext(), doneBytes)
                    + " </font></i>" + getResources().getString(R.string.out_of) + " <i>"
                    + Formatter.formatFileSize(getContext(), total) + "</i>");
            mProgressBytesText.setText(bytesText);

            Spanned fileProcessedSpan = Html.fromHtml(getResources().getString(R.string.processing_file)
                    + " <font color='" + accentColor + "'><i>" + (dataPackage.getSourceProgress())
                    + " </font></i>" + getResources().getString(R.string.of) + " <i>"
                    + dataPackage.getSourceFiles() + "</i>");
            mProgressFileText.setText(fileProcessedSpan);

            Spanned speedSpan = Html.fromHtml(getResources().getString(R.string.current_speed)
                    + ": <font color='" + accentColor + "'><i>"
                    + Formatter.formatFileSize(getContext(), dataPackage.getSpeedRaw())
                    + "/s</font></i>");
            mProgressSpeedText.setText(speedSpan);

            Spanned timerSpan = Html.fromHtml(getResources().getString(R.string.service_timer)
                    + ": <font color='" + accentColor + "'><i>"
                    + formatTimer(++time)
                    + "</font></i>");

            mProgressTimer.setText(timerSpan);
        }
    }

    /**
     * Formats input to plain mm:ss format
     * @param timer
     * @return
     */
    private String formatTimer(long timer) {
        final long min = TimeUnit.SECONDS.toMinutes(timer);
        final long sec = TimeUnit.SECONDS.toSeconds(timer - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Setup drawables and click listeners based on the {@link ServiceType}
     * @param serviceType
     */
    private void setupDrawables(ServiceType serviceType, boolean isMove) {
        switch (serviceType) {
            case COPY:
                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_content_copy_white_36dp));
                } else {
                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_content_copy_grey600_36dp));
                }
                mProgressTypeText.setText(isMove ? getResources().getString(R.string.moving)
                        : getResources().getString(R.string.copying));
                cancelBroadcast(new Intent(CopyService.TAG_BROADCAST_COPY_CANCEL));
                break;
            case EXTRACT:
                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_zip_box_white_36dp));
                } else {
                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_zip_box_grey600_36dp));
                }
                mProgressTypeText.setText(getResources().getString(R.string.extracting));
                cancelBroadcast(new Intent(ExtractService.TAG_BROADCAST_EXTRACT_CANCEL));
                break;
            case COMPRESS:
                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_zip_box_white_36dp));
                } else {
                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_zip_box_grey600_36dp));
                }
                mProgressTypeText.setText(getResources().getString(R.string.compressing));
                cancelBroadcast(new Intent(ZipTask.KEY_COMPRESS_BROADCAST_CANCEL));
                break;
            case ENCRYPT:
                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_folder_lock_white_36dp));
                } else {
                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_folder_lock_grey600_36dp));
                }
                mProgressTypeText.setText(getResources().getString(R.string.crypt_encrypting));
                cancelBroadcast(new Intent(EncryptService.TAG_BROADCAST_CRYPT_CANCEL));
                break;
            case DECRYPT:
                if (mainActivity.getAppTheme().equals(AppTheme.DARK)) {

                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_folder_lock_open_white_36dp));
                } else {
                    mProgressImage.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_folder_lock_open_grey600_36dp));
                }
                mProgressTypeText.setText(getResources().getString(R.string.crypt_decrypting));
                cancelBroadcast(new Intent(EncryptService.TAG_BROADCAST_CRYPT_CANCEL));
                break;
        }
    }

    /**
     * Setup click listener to cancel button click for various intent types
     * @param intent
     */
    private void cancelBroadcast(final Intent intent) {

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(),
                        getResources().getString(R.string.stopping), Toast.LENGTH_LONG).show();
                getActivity().sendBroadcast(intent);
                mProgressTypeText.setText(getResources().getString(R.string.cancelled));
                mProgressSpeedText.setText("");
                mProgressFileText.setText("");
                mProgressBytesText.setText("");
                mProgressFileNameText.setText("");

                mProgressTypeText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        });
    }

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
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setAxisLineColor(Color.TRANSPARENT);
        yAxisLeft.setTypeface(Typeface.DEFAULT_BOLD);
        yAxisLeft.setGridColor(getResources().getColor(R.color.white_translucent));

        xAxis.setAxisMaximum(Futils.readableFileSizeFloat(totalBytes));
        xAxis.setAxisMinimum(0.0f);
        xAxis.setAxisLineColor(Color.TRANSPARENT);
        xAxis.setGridColor(Color.TRANSPARENT);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTypeface(Typeface.DEFAULT_BOLD);
        mLineChart.setData(mLineData);
        mLineChart.invalidate();
    }
}
