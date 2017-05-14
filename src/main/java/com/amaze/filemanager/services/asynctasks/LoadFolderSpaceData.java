package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.text.SpannableString;
import android.text.format.Formatter;
import android.view.View;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel
 *         on 12/5/2017, at 00:07.
 */

public class LoadFolderSpaceData extends AsyncTask<Void, Void, Pair<String, List<PieEntry>>> {

    private static final int[] COLORS = {0xFFEF5350, 0xFF2196F3, 0xFF4CAF50};

    private Context context;
    private PieChart chart;
    private BaseFile file;

    public LoadFolderSpaceData(Context c, PieChart chart, BaseFile f) {
        context = c;
        this.chart = chart;
        file = f;
    }

    @Override
    protected Pair<String, List<PieEntry>> doInBackground(Void... params) {
        final String[] LEGENDS = new String[]{context.getResources().getString(R.string.size),
                context.getString(R.string.used), context.getString(R.string.free)};

        long[] dataArray = Futils.getSpaces(file);

        if (dataArray[0] != -1 && dataArray[0] != 0) {
            long totalSpace = dataArray[0],
                    usedByFolder = dataArray[2],
                    usedByOther = dataArray[0] - dataArray[1] - dataArray[2],
                    freeSpace = dataArray[1];

            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry(usedByFolder, LEGENDS[0]));
            entries.add(new PieEntry(usedByOther, LEGENDS[1]));
            entries.add(new PieEntry(freeSpace, LEGENDS[2]));

            return new Pair<>(Formatter.formatFileSize(context, totalSpace), entries);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Pair<String, List<PieEntry>> data) {
        if(data == null) {
            chart.setVisibility(View.GONE);
            return;
        }

        PieDataSet set = new PieDataSet(data.second, null);
        set.setColors(COLORS);
        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setSliceSpace(5f);
        set.setValueLinePart2Length(1.05f);
        set.setSelectionShift(0f);

        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new SizeFormatter(context));

        chart.setCenterText(new SpannableString(context.getString(R.string.total) + "\n" + data.first));
        chart.setData(pieData);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private class SizeFormatter implements IValueFormatter {

        private Context context;

        private SizeFormatter(Context c) {
            context = c;
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                                        ViewPortHandler viewPortHandler) {
            return Formatter.formatFileSize(context, (long) value);
        }
    }

}
