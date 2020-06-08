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

package com.amaze.filemanager.ui.views.preference;

import com.amaze.filemanager.R;

import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;

/** @author Emmanuel on 17/4/2017, at 22:22. */
public class PathSwitchPreference extends Preference {

  public static final int EDIT = 0, DELETE = 1;

  private int lastItemClicked = -1;

  public PathSwitchPreference(Context context) {
    super(context);
  }

  @Override
  protected View onCreateView(ViewGroup parent) {
    setWidgetLayoutResource(R.layout.namepathswitch_preference);
    return super.onCreateView(parent);
  }

  @Override
  protected void onBindView(View view) {
    setListener(view, R.id.edit, EDIT);
    setListener(view, R.id.delete, DELETE);

    view.setOnClickListener(null);

    super.onBindView(view); // Keep this before things that need changing what's on screen
  }

  public int getLastItemClicked() {
    return lastItemClicked;
  }

  private View.OnClickListener setListener(final View v, @IdRes int id, final int elem) {
    final PathSwitchPreference t = this;

    View.OnClickListener l =
        view -> {
          lastItemClicked = elem;
          getOnPreferenceClickListener().onPreferenceClick(t);
        };

    v.findViewById(id).setOnClickListener(l);

    return l;
  }
}
