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

package com.amaze.filemanager.ui.views.appbar;

import static android.os.Build.VERSION.SDK_INT;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.google.android.material.appbar.AppBarLayout;

import android.content.SharedPreferences;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

/**
 * layout_appbar.xml contains the layout for AppBar and BottomBar
 *
 * <p>This is a class containing containing methods to each section of the AppBar, creating the
 * object loads the views.
 *
 * @author Emmanuel on 2/8/2017, at 23:27.
 */
public class AppBar {

  private int TOOLBAR_START_INSET;

  private Toolbar toolbar;
  private SearchView searchView;
  private BottomBar bottomBar;

  private AppBarLayout appbarLayout;

  public AppBar(
      MainActivity a, SharedPreferences sharedPref, SearchView.SearchListener searchListener) {
    toolbar = a.findViewById(R.id.action_bar);
    searchView = new SearchView(this, a, searchListener);
    bottomBar = new BottomBar(this, a);

    appbarLayout = a.findViewById(R.id.lin);

    if (SDK_INT >= 21) toolbar.setElevation(0);
    /* For SearchView, see onCreateOptionsMenu(Menu menu)*/
    TOOLBAR_START_INSET = toolbar.getContentInsetStart();

    if (!sharedPref.getBoolean(PreferencesConstants.PREFERENCE_INTELLI_HIDE_TOOLBAR, true)) {
      AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
      params.setScrollFlags(0);
      appbarLayout.setExpanded(true, true);
    }
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  public SearchView getSearchView() {
    return searchView;
  }

  public BottomBar getBottomBar() {
    return bottomBar;
  }

  public AppBarLayout getAppbarLayout() {
    return appbarLayout;
  }

  public void setTitle(String title) {
    if (toolbar != null) toolbar.setTitle(title);
  }

  public void setTitle(@StringRes int title) {
    if (toolbar != null) toolbar.setTitle(title);
  }
}
