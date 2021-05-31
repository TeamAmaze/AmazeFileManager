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

package com.amaze.filemanager.ui.fragments;

import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.AppsAdapter;
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel;
import com.amaze.filemanager.asynchronous.loaders.AppListLoader;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.GlideConstants;
import com.amaze.filemanager.utils.Utils;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;

public class AppsListFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<AppListLoader.AppsDataPair> {

  public static final int ID_LOADER_APP_LIST = 0;

  private static final String KEY_LIST_STATE = "listState";

  private AppsAdapter adapter;

  public SharedPreferences sharedPreferences;
  private Parcelable listViewState;
  private int isAscending, sortby;

  private AppsAdapterPreloadModel modelProvider;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    MainActivity mainActivity = (MainActivity) getActivity();
    UtilitiesProvider utilsProvider = mainActivity.getUtilsProvider();

    mainActivity.getAppbar().setTitle(R.string.apps);
    mainActivity.getFAB().hide();
    mainActivity.getAppbar().getBottomBar().setVisibility(View.GONE);
    mainActivity.supportInvalidateOptionsMenu();

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    getSortModes();
    getListView().setDivider(null);
    if (utilsProvider.getAppTheme().equals(AppTheme.DARK)) {
      getActivity()
              .getWindow()
              .getDecorView()
              .setBackgroundColor(Utils.getColor(getContext(), R.color.holo_dark_background));
    } else if (utilsProvider.getAppTheme().equals(AppTheme.BLACK)) {
      getActivity()
              .getWindow()
              .getDecorView()
              .setBackgroundColor(Utils.getColor(getContext(), android.R.color.black));
    }

    modelProvider = new AppsAdapterPreloadModel(this, false);
    ViewPreloadSizeProvider<String> sizeProvider = new ViewPreloadSizeProvider<>();
    ListPreloader<String> preloader =
        new ListPreloader<>(
            GlideApp.with(this),
            modelProvider,
            sizeProvider,
            GlideConstants.MAX_PRELOAD_APPSADAPTER);

    adapter =
        new AppsAdapter(
            this,
            (ThemedActivity) getActivity(),
            utilsProvider,
            modelProvider,
            sizeProvider,
            R.layout.rowlayout,
                sharedPreferences,
            false);

    getListView().setOnScrollListener(preloader);
    setListAdapter(adapter);
    setListShown(false);
    setEmptyText(getString(R.string.no_applications));
    LoaderManager.getInstance(this).initLoader(ID_LOADER_APP_LIST, null, this);

    if (savedInstanceState != null) {
      listViewState = savedInstanceState.getParcelable(KEY_LIST_STATE);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle b) {
    super.onSaveInstanceState(b);

    b.putParcelable(KEY_LIST_STATE, getListView().onSaveInstanceState());
  }

  /**
   * Assigns sort modes A value from 0 to 2 defines sort mode as name||/last modified/size in
   * ascending order Values from 3 to 5 defines sort mode as name/last modified/size in descending
   * order
   *
   * <p>Final value of {@link #sortby} varies from 0 to 2
   */
  public void getSortModes() {
    int t = Integer.parseInt(sharedPreferences.getString("sortbyApps", "0"));
    if (t <= 2) {
      sortby = t;
      isAscending = 1;
    } else if (t > 2) {
      isAscending = -1;
      sortby = t - 3;
    }
  }

  @NonNull
  @Override
  public Loader<AppListLoader.AppsDataPair> onCreateLoader(int id, Bundle args) {
    return new AppListLoader(getContext(), sortby, isAscending);
  }

  @Override
  public void onLoadFinished(
          @NonNull Loader<AppListLoader.AppsDataPair> loader, AppListLoader.AppsDataPair data) {
    // set new data to adapter
    adapter.setData(data.first);
    modelProvider.setItemList(data.second);

    if (isResumed()) {
      setListShown(true);
    } else {
      setListShownNoAnimation(true);
    }

    if (listViewState != null) {
      getListView().onRestoreInstanceState(listViewState);
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<AppListLoader.AppsDataPair> loader) {
    adapter.setData(null);
  }
}
