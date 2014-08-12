package com.amaze.filemanager.adapters;

import android.content.Context;
import android.content.SharedPreferences;
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

    public DrawerAdapter(Context context, String[] values, MainActivity m, SharedPreferences Sp) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;

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
        textView.setText(values[position]);
        switch (position) {
            case 0:
                imageView.setImageDrawable(icons.getSdDrawable());
                break;
            case 1:
                imageView.setImageDrawable(icons.getGridDrawable());
                break;
            case 2:
                imageView.setImageDrawable(icons.getBookDrawable1());
                break;
            case 3:
                imageView.setImageDrawable(icons.getSettingDrawable());

        }

        return rowView;
    }
}
