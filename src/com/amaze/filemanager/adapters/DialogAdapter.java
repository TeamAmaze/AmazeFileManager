package com.amaze.filemanager.adapters;


import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.utils.Shortcuts;

import java.io.File;
import java.util.ArrayList;

public class DialogAdapter extends ArrayAdapter<File> {
    Shortcuts s = new Shortcuts();
    Activity context;
    public ArrayList<File> items;
    ///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();

    public DialogAdapter(Activity context, int resourceId, ArrayList<File> items) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;


    }


    private class ViewHolder {
        ImageButton image;
        TextView txtTitle;
        TextView txtDesc;

    }

    public View getView(int position, View convertView, ViewGroup parent) {
        File f = items.get(position);
        //final Layoutelements rowItem = getItem(position);

        View view;
        final int p = position;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.bookmarkrow, null);
            final ViewHolder vholder = new ViewHolder();
            vholder.txtTitle = (TextView) view.findViewById(R.id.text1);
            vholder.image = (ImageButton) view.findViewById(R.id.delete_button);
            vholder.image.setVisibility(View.GONE);
            vholder.txtDesc = (TextView) view.findViewById(R.id.text2);

            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtTitle.setText(f.getName());
        holder.txtDesc.setText(f.getPath());
        return view;
    }
}
