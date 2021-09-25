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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.GlideApp;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.AppsRecyclerAdapter;
import com.amaze.filemanager.adapters.data.AppDataParcelable;
import com.amaze.filemanager.adapters.glide.AppsAdapterPreloadModel;
import com.amaze.filemanager.adapters.holders.AppHolder;
import com.amaze.filemanager.asynchronous.loaders.AppListLoader;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.GlideConstants;
import com.amaze.filemanager.utils.Utils;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.ViewPreloadSizeProvider;

import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class AppsListFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<AppListLoader.AppsDataPair>,
        AdjustListViewForTv<AppHolder> {

  public static final int ID_LOADER_APP_LIST = 0;

  private AppsRecyclerAdapter adapter;
  private SharedPreferences sharedPreferences;
  private boolean isAscending;
  private int sortby;
  private View rootView;
  private AppsAdapterPreloadModel modelProvider;
  private LinearLayoutManager linearLayoutManager;
  private RecyclerViewPreloader<String> preloader;
  private List<AppDataParcelable> appDataParcelableList;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.fragment_app_list, container, false);
    return rootView;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    final MainActivity mainActivity = (MainActivity) getActivity();
    Objects.requireNonNull(mainActivity);

    UtilitiesProvider utilsProvider = mainActivity.getUtilsProvider();
    modelProvider = new AppsAdapterPreloadModel(this, false);
    ViewPreloadSizeProvider<String> sizeProvider = new ViewPreloadSizeProvider<>();
    preloader =
        new RecyclerViewPreloader<>(
            GlideApp.with(this),
            modelProvider,
            sizeProvider,
            GlideConstants.MAX_PRELOAD_APPSADAPTER);
    linearLayoutManager = new LinearLayoutManager(getContext());
    updateViews(mainActivity, utilsProvider);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    isAscending = sharedPreferences.getBoolean(PREFERENCE_APPLIST_ISASCENDING, true);
    sortby = sharedPreferences.getInt(PREFERENCE_APPLIST_SORTBY, 0);

    LoaderManager.getInstance(this).initLoader(ID_LOADER_APP_LIST, null, this);
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    requireActivity().getMenuInflater().inflate(R.menu.app_menu, menu);
    menu.findItem(R.id.checkbox_system_apps).setChecked(true);
    super.onCreateOptionsMenu(menu, inflater);
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
      case R.id.checkbox_system_apps:
        adapter.setData(appDataParcelableList, !item.isChecked());
        item.setChecked(!item.isChecked());
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void updateViews(MainActivity mainActivity, UtilitiesProvider utilsProvider) {
    mainActivity.getAppbar().setTitle(R.string.apps);
    mainActivity.getFAB().hide();
    mainActivity.getAppbar().getBottomBar().setVisibility(View.GONE);
    mainActivity.supportInvalidateOptionsMenu();

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

    getRecyclerView().addOnScrollListener(preloader);
    getRecyclerView().setLayoutManager(linearLayoutManager);
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
            .theme(appTheme.getMaterialDialogTheme())
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
    getSpinner().setVisibility(View.GONE);
    if (data.first.isEmpty()) {
      getRecyclerView().setVisibility(View.GONE);
      rootView.findViewById(R.id.empty_text_view).setVisibility(View.VISIBLE);
    } else {
      modelProvider.setItemList(data.second);
      appDataParcelableList = new ArrayList<>(data.first);
      adapter = new AppsRecyclerAdapter(this, modelProvider, false, this, data.first);
      getRecyclerView().setVisibility(View.VISIBLE);
      getRecyclerView().setAdapter(adapter);
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<AppListLoader.AppsDataPair> loader) {
    adapter.setData(Collections.emptyList(), true);
  }

  @Override
  public void adjustListViewForTv(
      @NonNull AppHolder viewHolder, @NonNull MainActivity mainActivity) {
    try {
      int[] location = new int[2];
      viewHolder.rl.getLocationOnScreen(location);
      Log.i(getClass().getSimpleName(), "Current x and y " + location[0] + " " + location[1]);
      if (location[1] < mainActivity.getAppbar().getAppbarLayout().getHeight()) {
        getRecyclerView().scrollToPosition(Math.max(viewHolder.getAdapterPosition() - 5, 0));
      } else if (location[1] + viewHolder.rl.getHeight()
          >= getContext().getResources().getDisplayMetrics().heightPixels) {
        getRecyclerView()
            .scrollToPosition(
                Math.min(viewHolder.getAdapterPosition() + 5, adapter.getItemCount() - 1));
      }
    } catch (IndexOutOfBoundsException e) {
      Log.w(getClass().getSimpleName(), "Failed to adjust scrollview for tv", e);
    }
  }

  private RecyclerView getRecyclerView() {
    return rootView.findViewById(R.id.list_view);
  }

  private MaterialProgressBar getSpinner() {
    return rootView.findViewById(R.id.loading_spinner);
  }
}
