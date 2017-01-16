package com.amaze.filemanager.utils.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;

import java.util.ArrayList;

/**
 * Created by Arpit on 01-07-2015.
 */

public class ShareAdapter extends ArrayAdapter<Intent> {
    private MaterialDialog dialog;
    private ArrayList<String> labels;
    private ArrayList<Drawable> arrayList;

    public void updateMatDialog(MaterialDialog b) {
        this.dialog = b;
    }

    public ShareAdapter(Context context,
                        ArrayList<Intent> arrayList,
                        ArrayList<String> labels,
                        ArrayList<Drawable> arrayList1) {
        super(context, R.layout.rowlayout, arrayList);
        this.labels = labels;
        this.arrayList = arrayList1;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.simplerow, parent, false);
        TextView a = ((TextView) rowView.findViewById(R.id.firstline));
        ImageView v = (ImageView) rowView.findViewById(R.id.icon);
        if (arrayList.get(position) != null)
            v.setImageDrawable(arrayList.get(position));
        a.setVisibility(View.VISIBLE);
        a.setText(labels.get(position));
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null && dialog.isShowing()) dialog.dismiss();
                getContext().startActivity(getItem(position));
            }
        });

        return rowView;
    }
}
