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

import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * This is the ViewHolder that formats the hidden files as defined in bookmarkrow.xml.
 *
 * @author Emmanuel on 20/11/2017, at 18:38.
 * @author Bowie Chen on 2019-10-26.
 * @see com.amaze.filemanager.adapters.HiddenAdapter
 */
public class HiddenViewHolder extends RecyclerView.ViewHolder {

  public final ImageButton deleteButton;
  public final TextView textTitle;
  public final TextView textDescription;
  public final LinearLayout row;

  public HiddenViewHolder(View view) {
    super(view);

    textTitle = view.findViewById(R.id.filename);
    deleteButton = view.findViewById(R.id.delete_button);
    textDescription = view.findViewById(R.id.file_path);
    row = view.findViewById(R.id.bookmarkrow);
  }
}
