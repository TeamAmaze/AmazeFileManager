package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.AppsList;
import com.amaze.filemanager.utils.Layoutelements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class AppsAdapter extends ArrayAdapter<Layoutelements> {
    Context context;
    List<Layoutelements> items;
    public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();
    AppsList app;

    public AppsAdapter(Context context, int resourceId,
                       List<Layoutelements> items, AppsList app) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        this.app = app;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
        }
    }

    public void toggleChecked(int position) {
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();

    }

    public void toggleChecked(boolean b) {

        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();


    }

    public List<Integer> getCheckedItemPositions() {
        List<Integer> checkedItemPositions = new ArrayList<Integer>();

        for (int i = 0; i < myChecked.size(); i++) {
            if (myChecked.get(i)) {
                (checkedItemPositions).add(i);
            }
        }

        return checkedItemPositions;
    }

    public boolean areAllChecked() {
        boolean b = true;
        for (int i = 0; i < myChecked.size(); i++) {
            if (!myChecked.get(i)) {
                b = false;
            }
        }
        return b;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        RelativeLayout rl;

    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.simplerow, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            vholder.imageView = (ImageView) view.findViewById(R.id.icon);
            vholder.rl = (RelativeLayout) view.findViewById(R.id.second);
            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.imageView.setImageDrawable(rowItem.getImageId());

        final Bitmap b = app.getBitmapFromMemCache(app.c.get(p).publicSourceDir);
        if (b != null) {
            holder.imageView.setImageBitmap(b);
        } else {
            ImageView i = holder.imageView;
            i.setTag("" + p);
            app.loadBitmap(app.c.get(p), i, ((BitmapDrawable) rowItem.getImageId()).getBitmap());
        }
        holder.txtTitle.setText(rowItem.getTitle());
        //	File f = new File(rowItem.getDesc());

        holder.rl.setClickable(true);
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if (app.selection == true) {
                    toggleChecked(p);
                    app.mActionMode.invalidate();
                } else {
                    Intent i = app.getActivity().getPackageManager().getLaunchIntentForPackage(app.c.get(p).packageName);
                    if (i != null)
                        app.startActivity(i);
                    else
                        Crouton.makeText(app.getActivity(), "Not Allowed", Style.ALERT).show();
                }
                // TODO: Implement this method
            }
        });
        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View p1) {
                app.onLongItemClick(p);
                // TODO: Implement this method
                return false;
            }
        });


        Boolean checked = myChecked.get(position);
        if (checked != null) {

            if (checked) {
                holder.rl.setBackgroundColor(Color.parseColor("#5f33b5e5"));
            } else {
                if (app.uimode == 0) {
                    holder.rl.setBackgroundResource(R.drawable.listitem1);
                } else if (app.uimode == 1) {
                    holder.rl.setBackgroundResource(R.drawable.bg_card);
                }
            }
        }
        return view;
    }
}
