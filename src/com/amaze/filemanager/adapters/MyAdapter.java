package com.amaze.filemanager.adapters;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.amaze.filemanager.*;
import com.amaze.filemanager.fragments.*;
import com.amaze.filemanager.utils.*;
import java.io.*;
import java.util.*;

public class MyAdapter extends ArrayAdapter<Layoutelements> {
    Context context;
    List<Layoutelements> items;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    Main main;
    Futils utils = new Futils();
   IconHolder ic;
    boolean showThumbs;
    public MyAdapter(Context context, int resourceId,
                     List<Layoutelements> items, Main main) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        this.main = main;
        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, false);
        }
        showThumbs=main.Sp.getBoolean("showThumbs",true);
		ic=new IconHolder(context,showThumbs);
    }


    public void toggleChecked(int position) {
        if (myChecked.get(position)) {
            myChecked.put(position, false);
        } else {
            myChecked.put(position, true);
        }

        notifyDataSetChanged();
        if (main.selection == false || main.mActionMode == null) {
            main.selection = true;
            main.mActionMode = main.getActivity().startActionMode(
                    main.mActionModeCallback);
        }
    }

    public void toggleChecked(boolean b) {

        for (int i = 0; i < items.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
    }

    public ArrayList<Integer> getCheckedItemPositions() {
        ArrayList<Integer> checkedItemPositions = new ArrayList<Integer>();

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
        TextView txtDesc;
        TextView date;
        RelativeLayout rl;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Layoutelements rowItem = getItem(position);

        View view = convertView;
        final int p = position;
        if (convertView == null) {
            int i = R.layout.simplerow;
            if (main.sdetails.equals("1")) {
                i = R.layout.simplerow;
            } else if (main.sdetails.equals("0")) {
                i = R.layout.rowlayout;
            }
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(i, parent, false);
            final ViewHolder vholder = new ViewHolder();

            vholder.txtTitle = (TextView) view.findViewById(R.id.firstline);
            vholder.imageView = (ImageView) view.findViewById(R.id.icon);
			
            vholder.rl = (RelativeLayout) view.findViewById(R.id.second);
            if (main.sdetails.equals("0")) {
                vholder.date = (TextView) view.findViewById(R.id.date);
                vholder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
            }
            view.setTag(vholder);
			

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
		ic.cancelLoad(holder.imageView);
        Boolean checked = myChecked.get(position);
        if (checked != null) {

            if (checked) {
                holder.rl.setBackgroundColor(Color.parseColor("#5fcccccc"));
            } else {
                if (main.uimode == 0) {
                    holder.rl.setBackgroundResource(R.drawable.listitem1);
                } else if (main.uimode == 1) {
                    holder.rl.setBackgroundResource(R.drawable.bg_card);
                }
            }
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.onListItemClicked(p, v);
            }
        });

        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View p1) {
                if (main.results) {
                    utils.longClickSearchItem(main, rowItem.getDesc());
                } else if (!main.selection) {
                    toggleChecked(p);

                }
                // TODO: Implement this method
                return true;
            }
        });


        holder.txtTitle.setText(rowItem.getTitle());

        holder.imageView.setImageDrawable(rowItem.getImageId());
        if (Icons.isPicture((rowItem.getDesc().toLowerCase()))) {

			ic.loadDrawable(holder.imageView,new File(rowItem.getDesc()),main.getResources().getDrawable(R.drawable.ic_doc_image));
            

        }
        if (Icons.isApk((rowItem.getDesc()))) {
               
            ic.loadDrawable(holder.imageView,new File(rowItem.getDesc()),main.getResources().getDrawable(R.drawable.ic_doc_apk));
        }

        if (main.sdetails.equals("0")) {
            holder.date.setText(utils.getdate(new File(rowItem.getDesc())));
            if (new File(rowItem.getDesc()).isDirectory()) {
                holder.txtDesc.setText(utils.count(new File(rowItem.getDesc()
                        .toString())));
            } else {
                holder.txtDesc.setText(utils.getSize(new File(rowItem.getDesc()
                        .toString())));
            }
            holder.date.setVisibility(View.VISIBLE);
            holder.txtDesc.setVisibility(View.VISIBLE);
        }
        return view;
    }
}

