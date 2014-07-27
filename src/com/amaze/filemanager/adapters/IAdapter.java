package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.Ifragment;
import com.amaze.filemanager.utils.Futils;
import com.amaze.filemanager.utils.Layoutelements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IAdapter extends ArrayAdapter<Layoutelements> {
    Futils utils = new Futils();
    Context context;
    int dpAsPixels;

    List<Layoutelements> items;
    public SparseBooleanArray myChecked = new SparseBooleanArray();
    Ifragment frag;

    public IAdapter(Context context, int resourceId,
                    List<Layoutelements> items, Ifragment frag) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        this.frag = frag;
        dpAsPixels = frag.pix / 2;
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

    /* private view holder class */
    private class ViewHolder {
        ImageView imageView;
        TextView txtTitle;
        TextView textalbum;
        TextView textsize;
        TextView textdate;
        RelativeLayout album;
        LinearLayout iname;
        FrameLayout r;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.ifragment, null);
            final ViewHolder vholder = new ViewHolder();

            vholder.txtTitle = (TextView) view.findViewById(R.id.iline);
            vholder.imageView = (ImageView) view.findViewById(R.id.ibg);
            vholder.textalbum = (TextView) view.findViewById(R.id.ialbum);
            vholder.textsize = (TextView) view.findViewById(R.id.isize);
            vholder.textdate = (TextView) view.findViewById(R.id.idate);
            vholder.album = (RelativeLayout) view.findViewById(R.id.album);
            vholder.iname = (LinearLayout) view.findViewById(R.id.iname);
            vholder.r = (FrameLayout) view.findViewById(R.id.i);
            view.setTag(vholder);

        } else {
            view = convertView;
        }
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.txtTitle.setText(rowItem.getTitle());
        File f = new File(rowItem.getDesc());
        holder.textalbum.setText(f.getParentFile().getName());
        holder.textsize.setText(utils.getSize(f));
        holder.textdate.setText(utils.getdate(f));
        holder.r.setClickable(true);
        holder.r.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                frag.onListItemClick(p);
            }
        });
        holder.r.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View p1) {
                frag.onLongItemClick(p);
                return false;
            }
        });
        holder.imageView.setImageDrawable(rowItem.getImageId());
        final Bitmap bitmap = frag.getBitmapFromMemCache(rowItem.getDesc());
        if (bitmap != null) {
            holder.imageView.setImageBitmap(bitmap);
        } else {

            frag.loadBitmap(
                    rowItem.getDesc(),
                    holder.imageView,
                    ((BitmapDrawable) frag.getResources().getDrawable(
                            R.drawable.ic_doc_image)).getBitmap()
            );
        }
        //	view.setLayoutParams( new GridView.LayoutParams(dpAsPixels, dpAsPixels) );
        Boolean checked = myChecked.get(position);


        if (checked) {
            holder.r.setBackgroundColor(Color.parseColor("#9f33b5e5"));
        } else {
            if (frag.theme == 0) {
                holder.r.setBackgroundResource(R.drawable.bg_card);
            } else {
                holder.album.setBackgroundColor(Color.parseColor("#70000000"));
                holder.iname.setBackgroundColor(Color.parseColor("#70000000"));
                holder.r.setBackgroundResource(R.drawable.listitem);
            }
        }
        return view;
    }
}
