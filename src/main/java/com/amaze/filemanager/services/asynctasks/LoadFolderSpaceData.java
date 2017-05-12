package com.amaze.filemanager.services.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.text.format.Formatter;
import android.view.View;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.utils.Futils;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel
 *         on 12/5/2017, at 00:07.
 */

public class LoadFolderSpaceData extends AsyncTask<Void, Void, Pair<String, List<PieEntry>>> {

    private static final int[] COLORS = new int[]{0xFF0D47A1, 0xFFE53930, 0xFF4CAF50};

    private Context context;
    private PieChart chart;
    private Legend legend;
    private BaseFile file;

    public LoadFolderSpaceData(Context c, PieChart chart, BaseFile f) {
        context = c;
        this.chart = chart;
        legend = chart.getLegend();
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

            ArrayList<LegendEntry> legends = new ArrayList<>();

            legends.add(new LegendEntry(LEGENDS[0] + Formatter.formatFileSize(context, usedByFolder),
                    Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, COLORS[0]));
            legends.add(new LegendEntry(LEGENDS[1] + Formatter.formatFileSize(context, usedByOther),
                    Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, COLORS[1]));
            legends.add(new LegendEntry(LEGENDS[2] + Formatter.formatFileSize(context, freeSpace),
                    Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN, null, COLORS[2]));
            legend.setCustom(legends);


            List<PieEntry> entries = new ArrayList<>();
            entries.add(new PieEntry(usedByFolder));
            entries.add(new PieEntry(usedByOther));
            entries.add(new PieEntry(freeSpace));

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

        //chart.setCenterText(new SpannableString(context.getString(R.string.total) + data.first));
        chart.setData(new PieData(set));
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private float toGB(float bytes) {
        BigDecimal decimal = new BigDecimal(bytes);
        decimal = decimal.divide(new BigDecimal(1024*3), BigDecimal.ROUND_HALF_UP);
        decimal = decimal.setScale(2, RoundingMode.HALF_UP);
        return decimal.floatValue();
    }

}
