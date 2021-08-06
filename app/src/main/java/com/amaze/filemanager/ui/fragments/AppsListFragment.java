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

import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_APPLIST_ISASCENDING;
import static com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_APPLIST_SORTBY;

import java.lang.ref.WeakReference;
import java.util.Objects;

import com.afollestad.materialdialogs.MaterialDialog;
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
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
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

  private SharedPreferences sharedPreferences;
  private Parcelable listViewState;
  private boolean isAscending;
  private int sortby;

  private AppsAdapterPreloadModel modelProvider;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final MainActivity mainActivity = (MainActivity) getActivity();
    Objects.requireNonNull(mainActivity);

    UtilitiesProvider utilsProvider = mainActivity.getUtilsProvider();
    updateViews(mainActivity, utilsProvider);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    isAscending = sharedPreferences.getBoolean(PREFERENCE_APPLIST_ISASCENDING, true);
    sortby = sharedPreferences.getInt(PREFERENCE_APPLIST_SORTBY, 0);

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
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case R.id.sort:
        showSortDialog(((MainActivity) requireActivity()).getAppTheme());
        return true;
      case R.id.exit:
        requireActivity().finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle b) {
    super.onSaveInstanceState(b);

    if (this.isAdded()) {
      b.putParcelable(KEY_LIST_STATE, getListView().onSaveInstanceState());
    }
  }

  private void updateViews(MainActivity mainActivity, UtilitiesProvider utilsProvider) {
    mainActivity.getAppbar().setTitle(R.string.apps);
    mainActivity.getFAB().hide();
    mainActivity.getAppbar().getBottomBar().setVisibility(View.GONE);
    mainActivity.supportInvalidateOptionsMenu();

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
    int skin_color = mainActivity.getCurrentColorPreference().getPrimaryFirstTab();
    int skinTwoColor = mainActivity.getCurrentColorPreference().getPrimarySecondTab();
    mainActivity.updateViews(
        new ColorDrawable(MainActivity.currentTab == 1 ? skinTwoColor : skin_color));
  }

  public void showSortDialog(AppTheme appTheme) {
    final MainActivity mainActivity = (MainActivity) getActivity();
    if (mainActivity == null) {
      return;
    }

    WeakReference<AppsListFragment> appsListFragment = new WeakReference<>(this);

    int accentColor = mainActivity.getAccent();
    String[] sort = getResources().getStringArray(R.array.sortbyApps);
    MaterialDialog.Builder builder =
        new MaterialDialog.Builder(mainActivity)
            .theme(
                appTheme.getMaterialDialogTheme(
                    (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES))
            .items(sort)
            .itemsCallbackSingleChoice(sortby, (dialog, view, which, text) -> true)
            .negativeText(R.string.ascending)
            .positiveColor(accentColor)
            .positiveText(R.string.descending)
            .negativeColor(accentColor)
            .onNegative(
                (dialog, which) -> {
                  final AppsListFragment $this = appsListFragment.get();
                  if ($this == null) {
                    return;
                  }

                  $this.saveAndReload(dialog.getSelectedIndex(), true);
                  dialog.dismiss();
                })
            .onPositive(
                (dialog, which) -> {
                  final AppsListFragment $this = appsListFragment.get();
                  if ($this == null) {
                    return;
                  }

                  $this.saveAndReload(dialog.getSelectedIndex(), false);
                  dialog.dismiss();
                })
            .title(R.string.sort_by);

    builder.build().show();
  }

  private void saveAndReload(int newSortby, boolean newIsAscending) {
    sortby = newSortby;
    isAscending = newIsAscending;

    sharedPreferences
        .edit()
        .putBoolean(PREFERENCE_APPLIST_ISASCENDING, newIsAscending)
        .putInt(PREFERENCE_APPLIST_SORTBY, newSortby)
        .apply();

    LoaderManager.getInstance(this).restartLoader(AppsListFragment.ID_LOADER_APP_LIST, null, this);
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
