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

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/** Created by Arpit on 10/18/2015 edited by Emmanuel Messulam <emmanuelbendavid@gmail.com> */
public class CheckBox extends SwitchPreference {

  public CheckBox(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBindView(View view) {
    // Clean listener before invoke SwitchPreference.onBindView
    clearListenerInViewGroup((ViewGroup) view);
    super.onBindView(view);
  }

  /**
   * Clear listener in Switch for specify ViewGroup.
   *
   * @param viewGroup The ViewGroup that will need to clear the listener.
   */
  private void clearListenerInViewGroup(ViewGroup viewGroup) {
    if (null == viewGroup) {
      return;
    }

    int count = viewGroup.getChildCount();
    for (int n = 0; n < count; ++n) {
      View childView = viewGroup.getChildAt(n);
      if (childView instanceof Switch) {
        final Switch switchView = (Switch) childView;
        switchView.setOnCheckedChangeListener(null);
        return;
      } else if (childView instanceof ViewGroup) {
        ViewGroup childGroup = (ViewGroup) childView;
        clearListenerInViewGroup(childGroup);
      }
    }
  }
}
