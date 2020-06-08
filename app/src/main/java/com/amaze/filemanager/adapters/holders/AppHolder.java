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
import com.amaze.filemanager.ui.views.ThemedTextView;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/** @author Emmanuel Messulam <emmanuelbendavid@gmail.com> on 10/12/2017, at 14:45. */
public class AppHolder extends RecyclerView.ViewHolder {

  public final ImageView apkIcon;
  public final ThemedTextView txtTitle;
  public final RelativeLayout rl;
  public final TextView txtDesc;
  public final ImageButton about;

  public AppHolder(View view) {
    super(view);

    txtTitle = view.findViewById(R.id.firstline);
    apkIcon = view.findViewById(R.id.apk_icon);
    rl = view.findViewById(R.id.second);
    txtDesc = view.findViewById(R.id.date);
    about = view.findViewById(R.id.properties);

    apkIcon.setVisibility(View.VISIBLE);
  }
}
