/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
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

package com.amaze.filemanager.adapters.holders;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.ui.views.ThemedTextView;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Check RecyclerAdapter's doc. TODO load everything related to this item here instead of in
 * RecyclerAdapter.
 *
 * @author Emmanuel on 29/5/2017, at 04:19.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {
  // each data item is just a string in this case
  public final RoundedImageView pictureIcon;
  public final ImageView genericIcon, apkIcon;
  public final ImageView imageView1;
  public final ThemedTextView txtTitle;
  public final TextView txtDesc;
  public final TextView date;
  public final TextView perm;
  public final View rl;
  public final TextView genericText;
  public final ImageButton about;
  public final ImageView checkImageView;
  public final ImageView checkImageViewGrid;
  public final RelativeLayout iconLayout;

  public ItemViewHolder(View view) {
    super(view);

    txtTitle = view.findViewById(R.id.firstline);
    pictureIcon = view.findViewById(R.id.picture_icon);
    rl = view.findViewById(R.id.second);
    perm = view.findViewById(R.id.permis);
    date = view.findViewById(R.id.date);
    txtDesc = view.findViewById(R.id.secondLine);
    apkIcon = view.findViewById(R.id.apk_icon);
    genericText = view.findViewById(R.id.generictext);
    imageView1 = view.findViewById(R.id.icon_thumb);
    about = view.findViewById(R.id.properties);
    checkImageView = view.findViewById(R.id.check_icon);
    genericIcon = view.findViewById(R.id.generic_icon);
    checkImageViewGrid = view.findViewById(R.id.check_icon_grid);
    iconLayout = view.findViewById(R.id.icon_frame_grid);
  }
}
