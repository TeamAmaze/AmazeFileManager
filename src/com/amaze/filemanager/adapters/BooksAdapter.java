/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
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

package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.BookmarksManager;
import com.amaze.filemanager.utils.Shortcuts;

import java.io.File;
import java.util.ArrayList;
public class BooksAdapter extends ArrayAdapter<File> {
    Shortcuts s;
    Activity context;
    public ArrayList<File> items;
    BookmarksManager b;
    ///	public HashMap<Integer, Boolean> myChecked = new HashMap<Integer, Boolean>();

    public BooksAdapter(Activity context, int resourceId, ArrayList<File> items, BookmarksManager b) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        this.b = b;
        s = new Shortcuts(context);
    }


    private class ViewHolder {
        ImageButton image;
        TextView txtTitle;
        TextView txtDesc;
        RelativeLayout rl;

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
            //	vholder.txtDesc = (TextView) view.findViewById(R.id.secondLine);
            vholder.txtTitle = (TextView) view.findViewById(R.id.text1);
            vholder.image = (ImageButton) view.findViewById(R.id.delete_button);
            vholder.txtDesc = (TextView) view.findViewById(R.id.text2);
            //	vholder.date = (TextView) view.findViewById(R.id.date);

            view.setTag(vholder);

        } else {
            view = convertView;

        }
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.txtTitle.setText(f.getName());
        holder.txtDesc.setText(f.getPath());
        holder.image.setImageDrawable(b.icons.getCancelDrawable());
        holder.image.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                try {
                    s.removeS(items.get(p), context);
                    items.remove(p);
                    notifyDataSetChanged();
           b.m.updateDrawer();
                } catch (Exception e) {
                    Toast.makeText(context, e + "", Toast.LENGTH_LONG).show();
                }
                // TODO: Implement this method
            }
        });
        return view;
    }
}
