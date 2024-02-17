/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.ui.fragments;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.services.AbstractProgressiveService;
import com.amaze.filemanager.asynchronous.services.CopyService;
import com.amaze.filemanager.asynchronous.services.DecryptService;
import com.amaze.filemanager.asynchronous.services.EncryptService;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.asynchronous.services.ZipService;
import com.amaze.filemanager.databinding.ProcessparentBinding;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.DatapointParcelable;
import com.amaze.filemanager.utils.ObtainableServiceBinder;
import com.amaze.filemanager.utils.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spanned;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

public class ProcessViewerFragment extends Fragment {

  /**
   * Helps defining the result type for {@link #processResults(DatapointParcelable, int)} to process
   */
  private static final int SERVICE_COPY = 0,
      SERVICE_EXTRACT = 1,
      SERVICE_COMPRESS = 2,
      SERVICE_ENCRYPT = 3,
      SERVICE_DECRYPT = 4;

  private boolean isInitialized = false;
  private MainActivity mainActivity;
  private int accentColor;
  private final LineData lineData = new LineData();
  private ProcessparentBinding binding = null;
  /** Time in seconds just for showing to the user. No guarantees. */
  private long looseTimeInSeconds = 0L;

  private ServiceConnection mCopyConnection,
      mExtractConnection,
      mCompressConnection,
      mEncryptConnection,
      mDecryptConnection;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = ProcessparentBinding.inflate(inflater);
    View rootView = binding.getRoot();

    mainActivity = (MainActivity) getActivity();

    accentColor = mainActivity.getAccent();
    if (mainActivity.getAppTheme().equals(AppTheme.DARK)
        || mainActivity.getAppTheme().equals(AppTheme.BLACK))
      rootView.setBackgroundResource((R.color.cardView_background));

    if (mainActivity.getAppTheme().equals(AppTheme.DARK)
        || mainActivity.getAppTheme().equals(AppTheme.BLACK)) {

      binding.deleteButton.setImageResource(R.drawable.ic_action_cancel);
      binding.cardView.setCardBackgroundColor(
          Utils.getColor(getContext(), R.color.cardView_foreground));
      binding.cardView.setCardElevation(0f);
    }

    return rootView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mCopyConnection = new CustomServiceConnection(this, binding.progressChart, SERVICE_COPY);
    mExtractConnection = new CustomServiceConnection(this, binding.progressChart, SERVICE_EXTRACT);
    mCompressConnection =
        new CustomServiceConnection(this, binding.progressChart, SERVICE_COMPRESS);
    mEncryptConnection = new CustomServiceConnection(this, binding.progressChart, SERVICE_ENCRYPT);
    mDecryptConnection = new CustomServiceConnection(this, binding.progressChart, SERVICE_DECRYPT);

    mainActivity.getAppbar().setTitle(R.string.process_viewer);
    mainActivity.hideFab();
    mainActivity.getAppbar().getBottomBar().setVisibility(View.GONE);
    mainActivity.supportInvalidateOptionsMenu();

    int skin_color = mainActivity.getCurrentColorPreference().getPrimaryFirstTab();
    int skinTwoColor = mainActivity.getCurrentColorPreference().getPrimarySecondTab();
    accentColor = mainActivity.getAccent();

    mainActivity.updateViews(
        new ColorDrawable(MainActivity.currentTab == 1 ? skinTwoColor : skin_color));
  }

  @Override
  public void onResume() {
    super.onResume();

    Intent intent = new Intent(getActivity(), CopyService.class);
    getActivity().bindService(intent, mCopyConnection, 0);

    Intent intent1 = new Intent(getActivity(), ExtractService.class);
    getActivity().bindService(intent1, mExtractConnection, 0);

    Intent intent2 = new Intent(getActivity(), ZipService.class);
    getActivity().bindService(intent2, mCompressConnection, 0);

    Intent intent3 = new Intent(getActivity(), EncryptService.class);
    getActivity().bindService(intent3, mEncryptConnection, 0);

    Intent intent4 = new Intent(getActivity(), DecryptService.class);
    getActivity().bindService(intent4, mDecryptConnection, 0);
  }

  @Override
  public void onPause() {
    super.onPause();
    getActivity().unbindService(mCopyConnection);
    getActivity().unbindService(mExtractConnection);
    getActivity().unbindService(mCompressConnection);
    getActivity().unbindService(mEncryptConnection);
    getActivity().unbindService(mDecryptConnection);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    mainActivity = null;
  }

  public void processResults(final DatapointParcelable dataPackage, int serviceType) {
    if (binding == null) return;

    if (dataPackage != null) {
      String name = dataPackage.getName();
      long total = dataPackage.getTotalSize();
      long doneBytes = dataPackage.getByteProgress();
      boolean move = dataPackage.getMove();

      if (!isInitialized) {

        // initializing views for the first time
        chartInit(total);

        // setting progress image
        setupDrawables(serviceType, move);
        isInitialized = true;
      }

      addEntry(
          FileUtils.readableFileSizeFloat(doneBytes),
          FileUtils.readableFileSizeFloat(dataPackage.getSpeedRaw()));

      binding.textViewProgressFileName.setText(name);

      Spanned bytesText =
          HtmlCompat.fromHtml(
              getResources().getString(R.string.written)
                  + " <font color='"
                  + accentColor
                  + "'><i>"
                  + Formatter.formatFileSize(getContext(), doneBytes)
                  + " </font></i>"
                  + getResources().getString(R.string.out_of)
                  + " <i>"
                  + Formatter.formatFileSize(getContext(), total)
                  + "</i>",
              FROM_HTML_MODE_COMPACT);
      binding.textViewProgressBytes.setText(bytesText);

      Spanned fileProcessedSpan =
          HtmlCompat.fromHtml(
              getResources().getString(R.string.processing_file)
                  + " <font color='"
                  + accentColor
                  + "'><i>"
                  + dataPackage.getSourceProgress()
                  + " </font></i>"
                  + getResources().getString(R.string.of)
                  + " <i>"
                  + dataPackage.getAmountOfSourceFiles()
                  + "</i>",
              FROM_HTML_MODE_COMPACT);
      binding.textViewProgressFile.setText(fileProcessedSpan);

      Spanned speedSpan =
          HtmlCompat.fromHtml(
              getResources().getString(R.string.current_speed)
                  + ": <font color='"
                  + accentColor
                  + "'><i>"
                  + Formatter.formatFileSize(getContext(), dataPackage.getSpeedRaw())
                  + "/s</font></i>",
              FROM_HTML_MODE_COMPACT);
      binding.textViewProgressSpeed.setText(speedSpan);

      Spanned timerSpan =
          HtmlCompat.fromHtml(
              getResources().getString(R.string.service_timer)
                  + ": <font color='"
                  + accentColor
                  + "'><i>"
                  + Utils.formatTimer(++looseTimeInSeconds)
                  + "</font></i>",
              FROM_HTML_MODE_COMPACT);

      binding.textViewProgressTimer.setText(timerSpan);

      if (dataPackage.getCompleted()) binding.deleteButton.setVisibility(View.GONE);
    }
  }

  /** Setup drawables and click listeners based on the SERVICE_* constants */
  private void setupDrawables(int serviceType, boolean isMove) {
    switch (serviceType) {
      case SERVICE_COPY:
        if (mainActivity.getAppTheme().equals(AppTheme.DARK)
            || mainActivity.getAppTheme().equals(AppTheme.BLACK)) {

          Drawable copyIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_content_copy_white_36dp);
          binding.progressImage.setImageDrawable(copyIcon);
        } else {
          Drawable greyCopyIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_content_copy_grey600_36dp);
          binding.progressImage.setImageDrawable(greyCopyIcon);
        }
        binding.textViewProgressType.setText(
            isMove
                ? getResources().getString(R.string.moving)
                : getResources().getString(R.string.copying));
        cancelBroadcast(new Intent(CopyService.TAG_BROADCAST_COPY_CANCEL));
        break;
      case SERVICE_EXTRACT:
        if (mainActivity.getAppTheme().equals(AppTheme.DARK)
            || mainActivity.getAppTheme().equals(AppTheme.BLACK)) {

          Drawable zipBoxIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_zip_box_white);
          binding.progressImage.setImageDrawable(zipBoxIcon);
        } else {
          Drawable greyZipBoxIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_zip_box_grey);
          binding.progressImage.setImageDrawable(greyZipBoxIcon);
        }
        binding.textViewProgressType.setText(getResources().getString(R.string.extracting));
        cancelBroadcast(new Intent(ExtractService.TAG_BROADCAST_EXTRACT_CANCEL));
        break;
      case SERVICE_COMPRESS:
        if (mainActivity.getAppTheme().equals(AppTheme.DARK)
            || mainActivity.getAppTheme().equals(AppTheme.BLACK)) {

          Drawable zipBoxIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_zip_box_white);
          binding.progressImage.setImageDrawable(zipBoxIcon);
        } else {
          Drawable greyZipBoxIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_zip_box_grey);
          binding.progressImage.setImageDrawable(greyZipBoxIcon);
        }
        binding.textViewProgressType.setText(getResources().getString(R.string.compressing));
        cancelBroadcast(new Intent(ZipService.KEY_COMPRESS_BROADCAST_CANCEL));
        break;
      case SERVICE_ENCRYPT:
        if (mainActivity.getAppTheme().equals(AppTheme.DARK)
            || mainActivity.getAppTheme().equals(AppTheme.BLACK)) {

          Drawable folderIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_folder_lock_white_36dp);
          binding.progressImage.setImageDrawable(folderIcon);
        } else {
          Drawable greyFolderIcon =
              ContextCompat.getDrawable(requireContext(), R.drawable.ic_folder_lock_grey600_36dp);
          binding.progressImage.setImageDrawable(greyFolderIcon);
        }
        binding.textViewProgressType.setText(getResources().getString(R.string.crypt_encrypting));
        cancelBroadcast(new Intent(EncryptService.TAG_BROADCAST_CRYPT_CANCEL));
        break;
      case SERVICE_DECRYPT:
        if (mainActivity.getAppTheme().equals(AppTheme.DARK)
            || mainActivity.getAppTheme().equals(AppTheme.BLACK)) {

          Drawable folderUnlockedIcon =
              ContextCompat.getDrawable(
                  requireContext(), R.drawable.ic_folder_lock_open_white_36dp);
          binding.progressImage.setImageDrawable(folderUnlockedIcon);
        } else {
          Drawable greyFolderUnlockedIcon =
              ContextCompat.getDrawable(
                  requireContext(), R.drawable.ic_folder_lock_open_grey600_36dp);
          binding.progressImage.setImageDrawable(greyFolderUnlockedIcon);
        }
        binding.textViewProgressType.setText(getResources().getString(R.string.crypt_decrypting));
        cancelBroadcast(new Intent(EncryptService.TAG_BROADCAST_CRYPT_CANCEL));
        break;
    }
  }

  /** Setup click listener to cancel button click for various intent types */
  private void cancelBroadcast(final Intent intent) {
    if (binding == null) return;

    binding.deleteButton.setOnClickListener(
        v -> {
          Toast.makeText(
                  getActivity(), getResources().getString(R.string.stopping), Toast.LENGTH_LONG)
              .show();
          getActivity().sendBroadcast(intent);
          binding.textViewProgressType.setText(getResources().getString(R.string.cancelled));
          binding.textViewProgressSpeed.setText("");
          binding.textViewProgressFile.setText("");
          binding.textViewProgressBytes.setText("");
          binding.textViewProgressFileName.setText("");

          binding.textViewProgressType.setTextColor(
              Utils.getColor(getContext(), android.R.color.holo_red_light));
        });
  }

  /**
   * Add a new entry dynamically to the chart, initializes a {@link LineDataSet} if not done so
   *
   * @param xValue the x-axis value, the number of bytes processed till now
   * @param yValue the y-axis value, bytes processed per sec
   */
  private void addEntry(float xValue, float yValue) {
    ILineDataSet dataSet = lineData.getDataSetByIndex(0);

    if (dataSet == null) { // adding set for first time
      dataSet = createDataSet();
      lineData.addDataSet(dataSet);
    }

    dataSet.addEntry(new Entry(xValue, yValue));

    lineData.notifyDataChanged();
    binding.progressChart.notifyDataSetChanged();
    binding.progressChart.invalidate();
  }

  /** Creates an instance for {@link LineDataSet} which will store the entries */
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
   *
   * @param totalBytes maximum value for x-axis
   */
  private void chartInit(long totalBytes) {
    binding.progressChart.setBackgroundColor(accentColor);
    binding.progressChart.getLegend().setEnabled(false);

    // no description text
    binding.progressChart.getDescription().setEnabled(false);

    XAxis xAxis = binding.progressChart.getXAxis();
    YAxis yAxisLeft = binding.progressChart.getAxisLeft();
    binding.progressChart.getAxisRight().setEnabled(false);
    yAxisLeft.setTextColor(Color.WHITE);
    yAxisLeft.setAxisLineColor(Color.TRANSPARENT);
    yAxisLeft.setTypeface(Typeface.DEFAULT_BOLD);
    yAxisLeft.setGridColor(Utils.getColor(getContext(), R.color.white_translucent));

    xAxis.setAxisMaximum(FileUtils.readableFileSizeFloat(totalBytes));
    xAxis.setAxisMinimum(0.0f);
    xAxis.setAxisLineColor(Color.TRANSPARENT);
    xAxis.setGridColor(Color.TRANSPARENT);
    xAxis.setTextColor(Color.WHITE);
    xAxis.setTypeface(Typeface.DEFAULT_BOLD);
    binding.progressChart.setData(lineData);
    binding.progressChart.invalidate();
  }

  private static class CustomServiceConnection implements ServiceConnection {

    private final WeakReference<ProcessViewerFragment> fragment;
    private final WeakReference<LineChart> lineChart;
    private final int serviceType;

    public CustomServiceConnection(
        ProcessViewerFragment frag, LineChart lineChart, int serviceType) {
      fragment = new WeakReference<>(frag);
      this.lineChart = new WeakReference<>(lineChart);
      this.serviceType = serviceType;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      ObtainableServiceBinder<? extends AbstractProgressiveService> binder =
          (ObtainableServiceBinder<? extends AbstractProgressiveService>) service;
      AbstractProgressiveService specificService = binder.getService();

      for (int i = 0; i < specificService.getDataPackageSize(); i++) {
        DatapointParcelable dataPackage = specificService.getDataPackage(i);
        ProcessViewerFragment processViewerFragment = fragment.get();
        if (processViewerFragment != null) {
          processViewerFragment.processResults(dataPackage, serviceType);
        }
      }

      // animate the chart a little after initial values have been applied
      LineChart chart = lineChart.get();
      if (chart != null) {
        chart.animateXY(500, 500);
      }

      specificService.setProgressListener(
          new AbstractProgressiveService.ProgressListener() {
            @Override
            public void onUpdate(final DatapointParcelable dataPackage) {
              ProcessViewerFragment processViewerFragment = fragment.get();
              if (processViewerFragment != null) {
                if (processViewerFragment.getActivity() == null) {
                  // callback called when we're not inside the app
                  return;
                }
                processViewerFragment
                    .getActivity()
                    .runOnUiThread(
                        () -> processViewerFragment.processResults(dataPackage, serviceType));
              }
            }

            @Override
            public void refresh() {}
          });
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}
  }
}
