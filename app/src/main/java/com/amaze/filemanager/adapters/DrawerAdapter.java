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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.adapters.data.DrawerItem;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.theme.AppTheme;

import java.io.File;
import java.util.ArrayList;

public class DrawerAdapter extends ArrayAdapter<DrawerItem> {
    public static final int SELECTED_NONE = -1;

    private final Context context;
    private UtilitiesProvider utilsProvider;
    private final ArrayList<DrawerItem> values;
    private MainActivity m;
    private int selectedItem = SELECTED_NONE;
    private DataUtils dataUtils = DataUtils.getInstance();
    private LayoutInflater inflater;

    public DrawerAdapter(Context context, UtilitiesProvider utilsProvider,
                         ArrayList<DrawerItem> values, MainActivity m) {
        super(context, R.layout.drawerrow, values);
        this.utilsProvider = utilsProvider;

        this.context = context;
        this.values = values;

        selectedItem = SELECTED_NONE;
        this.m = m;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void toggleChecked(int position) {
        selectedItem = position;
        notifyDataSetChanged();
    }

    public void deselectEverything() {
        selectedItem = SELECTED_NONE;
        notifyDataSetChanged();
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
            else if (utilsProvider.getAppTheme().equals(AppTheme.BLACK))
                view.setBackgroundColor(Color.BLACK);
            else view.setBackgroundResource(R.color.background_material_dark);
            view.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(m, 17)));
            view.setPadding(0, Utils.dpToPx(m, 8), 0, Utils.dpToPx(m, 8));
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
            view.setOnClickListener(p1 -> {
                if(getItem(position).type == DrawerItem.ITEM_INTENT) {
                    getItem(position).onClickListener.onClick();
                } else {
                    DrawerItem drawerItem = getItem(position);

                    if (dataUtils.containsBooks(new String[]{drawerItem.title, drawerItem.path}) != -1) {

                        checkForPath(drawerItem.path);
                    }

                    if (dataUtils.getAccounts().size() > 0 && (drawerItem.path.startsWith(CloudHandler.CLOUD_PREFIX_BOX) ||
                            drawerItem.path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX) ||
                            drawerItem.path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE) ||
                            drawerItem.path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE))) {
                        // we have cloud accounts, try see if token is expired or not
                        CloudUtil.checkToken(drawerItem.path, m);
                    }
                }

                m.selectItem(position);
            });
            view.setOnLongClickListener(v -> {
                if (!getItem(position).isSection())
                    // not to remove the first bookmark (storage) and permanent bookmarks
                    if (position > m.storage_count && position < values.size() - 7) {
                        DrawerItem drawerItem = getItem(position);
                        String title = drawerItem.title;
                        String path = (drawerItem).path;
                        if (dataUtils.containsBooks(new String[]{drawerItem.title, path}) != -1) {
                            m.renameBookmark((drawerItem).title, path);
                        } else if (path.startsWith("smb:/")) {
                            m.showSMBDialog(drawerItem.title, path, true);
                        } else if (path.startsWith("ssh:/")) {
                            m.showSftpDialog(drawerItem.title, path, true);
                        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {

                            GeneralDialogCreation.showCloudDialog(m, utilsProvider.getAppTheme(), OpenMode.DROPBOX);

                        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {

                            GeneralDialogCreation.showCloudDialog(m, utilsProvider.getAppTheme(), OpenMode.GDRIVE);

                        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {

                            GeneralDialogCreation.showCloudDialog(m, utilsProvider.getAppTheme(), OpenMode.BOX);

                        } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {

                            GeneralDialogCreation.showCloudDialog(m, utilsProvider.getAppTheme(), OpenMode.ONEDRIVE);
                        }
                    } else if (position < m.storage_count) {
                        String path = getItem(position).path;
                        if (!path.equals("/"))
                            GeneralDialogCreation.showPropertiesDialogForStorage(RootHelper.generateBaseFile(new File(path), true), m, utilsProvider.getAppTheme());
                    }

                // return true to denote no further processing
                return true;
            });

            txtTitle.setText((values.get(position)).title);
            imageView.setImageDrawable(getDrawable(position));
            imageView.clearColorFilter();

            if (selectedItem == position) {
                int accentColor = m.getColorPreference().getColor(ColorUsage.ACCENT);
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
                    view.setBackgroundColor(Color.parseColor("#ffeeeeee"));
                } else {
                    view.setBackgroundColor(Color.parseColor("#ff424242"));
                }
                imageView.setColorFilter(accentColor);
                txtTitle.setTextColor(accentColor);
            } else {
                if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
                    imageView.setColorFilter(Color.parseColor("#666666"));
                    txtTitle.setTextColor(Utils.getColor(m, android.R.color.black));
                } else {
                    imageView.setColorFilter(Color.WHITE);
                    txtTitle.setTextColor(Utils.getColor(m, android.R.color.white));
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
                    ThemedActivity.rootMode, new Operations.ErrorCallBack() {
                        //TODO empty
                        @Override
                        public void exists(HybridFile file) {

                        }

                        @Override
                        public void launchSAF(HybridFile file) {

                        }

                        @Override
                        public void launchSAF(HybridFile file, HybridFile file1) {

                        }

                        @Override
                        public void done(HybridFile hFile, boolean b) {

                        }

                        @Override
                        public void invalidName(HybridFile file) {

                        }
                    });
        }
    }

    private Drawable getDrawable(int position) {
        return getItem(position).icon;
    }
}