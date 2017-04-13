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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.BaseActivity;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.ui.drawer.EntryItem;
import com.amaze.filemanager.ui.drawer.Item;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class DrawerAdapter extends ArrayAdapter<Item> {
    private final Context context;
    private UtilitiesProviderInterface utilsProvider;
    private final ArrayList<Item> values;
    private MainActivity m;
    private Float[] color;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    //TODO queried but never updated
    private HashMap<String, Float[]> colors = new HashMap<>();

    public void toggleChecked(int position) {
        toggleChecked(false);
        myChecked.put(position, true);
        notifyDataSetChanged();
    }

    public void toggleChecked(boolean b) {
        for (int i = 0; i < values.size(); i++) {
            myChecked.put(i, b);
        }
        notifyDataSetChanged();
    }

    private LayoutInflater inflater;
    private int fabskin;

    public DrawerAdapter(Context context, UtilitiesProviderInterface utilsProvider, ArrayList<Item> values, MainActivity m, SharedPreferences Sp) {
        super(context, R.layout.drawerrow, values);
        this.utilsProvider = utilsProvider;

        this.context = context;
        this.values = values;

        for (int i = 0; i < values.size(); i++) {
            myChecked.put(i, false);
        }
        this.m = m;
        fabskin = Color.parseColor(BaseActivity.accentSkin);
        color = colors.get(BaseActivity.accentSkin);
        if (color == null) {
            color = colors.get("#e91e63");
        }
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        if (values.get(position).isSection()) {
            ImageView view = new ImageView(context);
            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                view.setImageResource(R.color.divider);
            else
                view.setImageResource(R.color.divider_dark);
            view.setClickable(false);
            view.setFocusable(false);
            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                view.setBackgroundColor(Color.WHITE);
            else view.setBackgroundResource(R.color.background_material_dark);
            view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, m.dpToPx(17)));
            view.setPadding(0, m.dpToPx(8), 0, m.dpToPx(8));
            return view;
        } else {
            View view = inflater.inflate(R.layout.drawerrow, parent, false);
            final TextView txtTitle = (TextView) view.findViewById(R.id.firstline);
            final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
                view.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {
                view.setBackgroundResource(R.drawable.safr_ripple_black);
            }
            view.setOnClickListener(new View.OnClickListener() {

                public void onClick(View p1) {
                    EntryItem item = (EntryItem) getItem(position);

                    if (DataUtils.containsBooks(new String[]{item.getTitle(), item.getPath()}) != -1) {

                        checkForPath(item.getPath());
                    }
                    m.selectItem(position);
                }
                // TODO: Implement this method

            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!getItem(position).isSection())
                        // not to remove the first bookmark (storage) and permanent bookmarks
                        if (position > m.storage_count && position < values.size() - 7) {
                            EntryItem item = (EntryItem) getItem(position);
                            String path = (item).getPath();
                            if (DataUtils.containsBooks(new String[]{item.getTitle(), path}) != -1) {
                                m.renameBookmark((item).getTitle(), path);
                            } else if (path.startsWith("smb:/")) {
                                m.showSMBDialog(item.getTitle(), path, true);
                            }
                        } else if (position < m.storage_count) {
                            String path = ((EntryItem) getItem(position)).getPath();
                            if (!path.equals("/"))
                                utilsProvider.getFutils().showProps(RootHelper.generateBaseFile(new File(path), true), m, utilsProvider.getAppTheme());
                        }

                    // return true to denote no further processing
                    return true;
                }
            });

            txtTitle.setText(((EntryItem) (values.get(position))).getTitle());
            imageView.setImageDrawable(getDrawable(position));
            imageView.clearColorFilter();
            if (myChecked.get(position)) {
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT))
                    view.setBackgroundColor(Color.parseColor("#ffeeeeee"));
                else view.setBackgroundColor(Color.parseColor("#ff424242"));
                imageView.setColorFilter(fabskin);
                txtTitle.setTextColor(Color.parseColor(BaseActivity.accentSkin));
            } else {
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
                    imageView.setColorFilter(Color.parseColor("#666666"));
                    txtTitle.setTextColor(m.getResources().getColor(android.R.color.black));
                } else {
                    imageView.setColorFilter(Color.WHITE);
                    txtTitle.setTextColor(m.getResources().getColor(android.R.color.white));
                }
            }

            return view;
        }
    }

    /**
     * Checks whether path for bookmark exists
     * If path is not found, empty directory is created
     *
     * @param path
     */
    private void checkForPath(String path) {
        // TODO: Add support for SMB and OTG in this function
        if (!new File(path).exists()) {
            Toast.makeText(getContext(), getContext().getString(R.string.bookmark_lost), Toast.LENGTH_SHORT).show();
            Operations.mkdir(RootHelper.generateBaseFile(new File(path), true), getContext(),
                    BaseActivity.rootMode, new Operations.ErrorCallBack() {
                        //TODO empty
                        @Override
                        public void exists(HFile file) {

                        }

                        @Override
                        public void launchSAF(HFile file) {

                        }

                        @Override
                        public void launchSAF(HFile file, HFile file1) {

                        }

                        @Override
                        public void done(HFile hFile, boolean b) {

                        }

                        @Override
                        public void invalidName(HFile file) {

                        }
                    });
        }
    }

    private Drawable getDrawable(int position) {
        return ((EntryItem) getItem(position)).getIcon();
    }
}