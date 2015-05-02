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
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fragments.BookmarksManager;
import com.amaze.filemanager.utils.Shortcuts;

import java.io.File;
import java.util.ArrayList;
public class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.ViewHolder> {
    Shortcuts s;
    Context context;
    public ArrayList<File> items;
    BookmarksManager b;

    public BooksAdapter(Context context, int resourceId, ArrayList<File> items, BookmarksManager b) {
        this.context = context;
        this.items = items;
        this.b = b;
        s = new Shortcuts(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
       View view = mInflater.inflate(R.layout.bookmarkrow, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,final int p) {
        final ViewHolder holder=(ViewHolder)viewHolder;
        File f = items.get(p);
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a stri
        public ImageView image;
        public TextView txtTitle;
        public TextView txtDesc;

        public ViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.text1);
            image = (ImageButton) view.findViewById(R.id.delete_button);
            txtDesc = (TextView) view.findViewById(R.id.text2);

        }
    }

}
