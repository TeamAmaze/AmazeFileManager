package com.amaze.filemanager.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.utils.IconUtils;

public class DrawerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    MainActivity m;

    IconUtils icons;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    public void toggleChecked(int position) {
        toggleChecked(false);
        myChecked.put(position, true);


        notifyDataSetChanged();
    }

    public void toggleChecked(boolean b) {

        for (int i = 0; i < values.length; i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
    }

    public DrawerAdapter(Context context, String[] values, MainActivity m, SharedPreferences Sp) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;

        for (int i = 0; i < values.length; i++) {
            myChecked.put(i, false);
        }
        icons = new IconUtils(Sp, m);
        this.m = m;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawerrow, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.firstline);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        LinearLayout l = (LinearLayout) rowView.findViewById(R.id.second);
        l.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                m.selectItem(position);
            }
            // TODO: Implement this method

        });
        float[] src = {

                0, 0, 0,0, 0,
                0, 0.58431373f, 0, 0, 0,
                0, 0,  0.52941176f,0, 0,
                0, 0, 0, 1, 0
        };
        ColorMatrix colorMatrix = new ColorMatrix(src);
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);

        textView.setText(values[position]);
        switch (position) {
            case 0:if(myChecked.get(0)){
                imageView.setImageResource(R.drawable.ic_action_sd_storage);
                imageView.setColorFilter(colorMatrixColorFilter);}
            else
                imageView.setImageDrawable(icons.getSdDrawable1());
                break;
            case 1:if(myChecked.get(1)){
                imageView.setImageResource(R.drawable.ic_action_view_as_grid);
                imageView.setColorFilter(colorMatrixColorFilter);}
            else
                imageView.setImageDrawable(icons.getGridDrawable());
                break;
            case 2:if(myChecked.get(2)){
                imageView.setImageResource(R.drawable.ic_action_not_important);
                imageView.setColorFilter(colorMatrixColorFilter);}
            else
                imageView.setImageDrawable(icons.getBookDrawable1());
                break;

        }
        if(myChecked.get(position)){
            if(m.theme==0){textView.setTypeface(Typeface.DEFAULT);}else textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setTextColor(m.getResources().getColor(R.color.theme_primary));}
        else
        if(m.theme==0)
            textView.setTextColor(m.getResources().getColor(android.R.color.black));
        else     textView.setTextColor(m.getResources().getColor(android.R.color.white));

        return rowView;
    }
}