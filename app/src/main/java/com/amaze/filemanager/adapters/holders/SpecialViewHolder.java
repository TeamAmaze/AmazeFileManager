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
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.theme.AppTheme;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Check {@link com.amaze.filemanager.adapters.RecyclerAdapter}'s doc.
 *
 * @author Emmanuel on 29/5/2017, at 04:22.
 */
public class SpecialViewHolder extends RecyclerView.ViewHolder {
  public static final int HEADER_FILES = 0, HEADER_FOLDERS = 1;
  // each data item is just a string in this case
  public final TextView txtTitle;
  public final int type;

  public SpecialViewHolder(Context c, View view, UtilitiesProvider utilsProvider, int type) {
    super(view);

    this.type = type;
    txtTitle = view.findViewById(R.id.text);

    switch (type) {
      case HEADER_FILES:
        txtTitle.setText(R.string.files);
        break;
      case HEADER_FOLDERS:
        txtTitle.setText(R.string.folders);
        break;
      default:
        throw new IllegalStateException(": " + type);
    }

    // if(utilsProvider.getAppTheme().equals(AppTheme.DARK))
    //    view.setBackgroundResource(R.color.holo_dark_background);

    if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {
      txtTitle.setTextColor(Utils.getColor(c, R.color.text_light));
    } else {
      txtTitle.setTextColor(Utils.getColor(c, R.color.text_dark));
    }
  }
}
