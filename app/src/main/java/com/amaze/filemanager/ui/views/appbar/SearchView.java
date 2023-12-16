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

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.SearchRecyclerViewAdapter;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileListSorter;
import com.amaze.filemanager.filesystem.files.sort.DirSortBy;
import com.amaze.filemanager.filesystem.files.sort.SortBy;
import com.amaze.filemanager.filesystem.files.sort.SortOrder;
import com.amaze.filemanager.filesystem.files.sort.SortType;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * SearchView, a simple view to search
 *
 * @author Emmanuel on 2/8/2017, at 23:30.
 */
public class SearchView {

  private final MainActivity mainActivity;
  private final AppBar appbar;

  private final NestedScrollView searchViewLayout;
  private final AppCompatEditText searchViewEditText;

  private final AppCompatImageView clearImageView;
  private final AppCompatImageView backImageView;

  private final AppCompatTextView recentHintTV;
  private final AppCompatTextView searchResultsHintTV;
  private final AppCompatTextView deepSearchTV;

  private final ChipGroup recentChipGroup;
  private final RecyclerView recyclerView;

  private final SearchRecyclerViewAdapter searchRecyclerViewAdapter;

  /** Text to describe {@link SearchView#searchResultsSortButton} */
  private final AppCompatTextView searchResultsSortHintTV;

  /** The button to select how the results should be sorted */
  private final AppCompatButton searchResultsSortButton;

  /** The drawable used to indicate that the search results are sorted ascending */
  private final Drawable searchResultsSortAscDrawable;

  /** The drawable used to indicate that the search results are sorted descending */
  private final Drawable searchResultsSortDescDrawable;

  // 0 -> Basic Search
  // 1 -> Indexed Search
  // 2 -> Deep Search
  private int searchMode;

  private boolean enabled = false;

  private final SortType defaultSortType = new SortType(SortBy.RELEVANCE, SortOrder.ASC);

  /** The selected sort type for the search results */
  private SortType sortType = defaultSortType;

  @SuppressWarnings("ConstantConditions")
  @SuppressLint("NotifyDataSetChanged")
  public SearchView(final AppBar appbar, MainActivity mainActivity, SearchListener searchListener) {

    this.mainActivity = mainActivity;
    this.appbar = appbar;

    searchViewLayout = mainActivity.findViewById(R.id.search_view);
    searchViewEditText = mainActivity.findViewById(R.id.search_edit_text);
    clearImageView = mainActivity.findViewById(R.id.search_close_btn);
    backImageView = mainActivity.findViewById(R.id.img_view_back);
    recentChipGroup = mainActivity.findViewById(R.id.searchRecentItemsChipGroup);
    recentHintTV = mainActivity.findViewById(R.id.searchRecentHintTV);
    searchResultsHintTV = mainActivity.findViewById(R.id.searchResultsHintTV);
    deepSearchTV = mainActivity.findViewById(R.id.searchDeepSearchTV);
    recyclerView = mainActivity.findViewById(R.id.searchRecyclerView);
    searchResultsSortHintTV = mainActivity.findViewById(R.id.searchResultsSortHintTV);
    searchResultsSortButton = mainActivity.findViewById(R.id.searchResultsSortButton);
    searchResultsSortAscDrawable =
        ResourcesCompat.getDrawable(
            mainActivity.getResources(),
            R.drawable.baseline_sort_24_asc_white,
            mainActivity.getTheme());
    searchResultsSortDescDrawable =
        ResourcesCompat.getDrawable(
            mainActivity.getResources(),
            R.drawable.baseline_sort_24_desc_white,
            mainActivity.getTheme());

    setUpSearchResultsSortButton();

    initRecentSearches(mainActivity);

    searchRecyclerViewAdapter = new SearchRecyclerViewAdapter();
    recyclerView.setAdapter(searchRecyclerViewAdapter);

    clearImageView.setOnClickListener(
        v -> {
          searchViewEditText.setText("");
          clearRecyclerView();
        });

    backImageView.setOnClickListener(v -> appbar.getSearchView().hideSearchView());

    searchViewEditText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (count > 0) searchViewEditText.setError(null);

            if (count >= 3) onSearch(false);
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    searchViewEditText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {

            Utils.hideKeyboard(mainActivity);

            return onSearch(true);
          }

          return false;
        });

    deepSearchTV.setOnClickListener(
        v -> {
          String s = getSearchTerm();

          if (searchMode == 1) {

            saveRecentPreference(s);

            mainActivity
                .getCurrentMainFragment()
                .getMainActivityViewModel()
                .indexedSearch(mainActivity, s)
                .observe(
                    mainActivity.getCurrentMainFragment().getViewLifecycleOwner(),
                    hybridFileParcelables -> updateResultList(hybridFileParcelables, s));

            searchMode = 2;

            deepSearchTV.setText(
                getSpannableText(
                    mainActivity.getString(R.string.not_finding_what_you_re_looking_for),
                    mainActivity.getString(R.string.try_deep_search)));

          } else if (searchMode == 2) {

            searchListener.onSearch(s);
            appbar.getSearchView().hideSearchView();

            deepSearchTV.setVisibility(View.GONE);
          }
        });

    initSearchViewColor(mainActivity);
  }

  @SuppressWarnings("ConstantConditions")
  private boolean onSearch(boolean shouldSave) {

    String s = getSearchTerm();

    if (s.isEmpty()) {
      searchViewEditText.setError(mainActivity.getString(R.string.field_empty));
      searchViewEditText.requestFocus();
      return false;
    }

    basicSearch(s);

    if (shouldSave) saveRecentPreference(s);

    return true;
  }

  private void basicSearch(String s) {

    clearRecyclerView();

    searchResultsHintTV.setVisibility(View.VISIBLE);
    searchResultsSortButton.setVisibility(View.VISIBLE);
    searchResultsSortHintTV.setVisibility(View.VISIBLE);
    deepSearchTV.setVisibility(View.VISIBLE);
    searchMode = 1;
    deepSearchTV.setText(
        getSpannableText(
            mainActivity.getString(R.string.not_finding_what_you_re_looking_for),
            mainActivity.getString(R.string.try_indexed_search)));

    mainActivity
        .getCurrentMainFragment()
        .getMainActivityViewModel()
        .basicSearch(mainActivity, s)
        .observe(
            mainActivity.getCurrentMainFragment().getViewLifecycleOwner(),
            hybridFileParcelables -> updateResultList(hybridFileParcelables, s));
  }

  private void saveRecentPreference(String s) {

    String preferenceString =
        PreferenceManager.getDefaultSharedPreferences(mainActivity)
            .getString(PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, null);

    ArrayList<String> recentSearches =
        preferenceString != null
            ? new Gson().fromJson(preferenceString, new TypeToken<ArrayList<String>>() {}.getType())
            : new ArrayList<>();

    if (s.isEmpty() || recentSearches.contains(s)) return;

    recentSearches.add(s);

    if (recentSearches.size() > 5) recentSearches.remove(0);

    PreferenceManager.getDefaultSharedPreferences(mainActivity)
        .edit()
        .putString(
            PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, new Gson().toJson(recentSearches))
        .apply();

    initRecentSearches(mainActivity);
  }

  private void initRecentSearches(Context context) {

    String preferenceString =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, null);

    if (preferenceString == null) {
      recentHintTV.setVisibility(View.GONE);
      recentChipGroup.setVisibility(View.GONE);
      return;
    }

    recentHintTV.setVisibility(View.VISIBLE);
    recentChipGroup.setVisibility(View.VISIBLE);

    recentChipGroup.removeAllViews();

    ArrayList<String> recentSearches =
        new Gson().fromJson(preferenceString, new TypeToken<ArrayList<String>>() {}.getType());

    for (String string : recentSearches) {
      Chip chip = new Chip(new ContextThemeWrapper(context, R.style.ChipStyle));

      chip.setText(string);

      recentChipGroup.addView(chip);

      chip.setOnClickListener(
          v -> {
            String s = ((Chip) v).getText().toString();

            searchViewEditText.setText(s);

            Utils.hideKeyboard(mainActivity);

            basicSearch(s);
          });
    }
  }

  private void resetSearchMode() {
    searchMode = 0;
    deepSearchTV.setText(
        getSpannableText(
            mainActivity.getString(R.string.not_finding_what_you_re_looking_for),
            mainActivity.getString(R.string.try_indexed_search)));
    deepSearchTV.setVisibility(View.GONE);
  }

  /**
   * Updates the list of results displayed in {@link SearchView#searchRecyclerViewAdapter} sorted
   * according to the current {@link SearchView#sortType}
   *
   * @param newResults The list of results that should be displayed
   * @param searchTerm The search term that resulted in the search results
   */
  private void updateResultList(List<HybridFileParcelable> newResults, String searchTerm) {
    ArrayList<HybridFileParcelable> items = new ArrayList<>(newResults);
    Collections.sort(items, new FileListSorter(DirSortBy.NONE_ON_TOP, sortType, searchTerm));
    searchRecyclerViewAdapter.submitList(items);
    searchRecyclerViewAdapter.notifyDataSetChanged();
  }

  /** show search view with a circular reveal animation */
  public void revealSearchView() {
    final int START_RADIUS = 16;
    int endRadius = Math.max(appbar.getToolbar().getWidth(), appbar.getToolbar().getHeight());

    resetSearchMode();
    resetSearchResultsSortButton();
    clearRecyclerView();

    Animator animator;
    if (SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      int[] searchCoords = new int[2];
      View searchItem =
          appbar
              .getToolbar()
              .findViewById(R.id.search); // It could change position, get it every time
      searchViewEditText.setText("");
      searchItem.getLocationOnScreen(searchCoords);
      animator =
          ViewAnimationUtils.createCircularReveal(
              searchViewLayout,
              searchCoords[0] + 32,
              searchCoords[1] - 16,
              START_RADIUS,
              endRadius);
    } else {
      // TODO:ViewAnimationUtils.createCircularReveal
      animator = ObjectAnimator.ofFloat(searchViewLayout, "alpha", 0f, 1f);
    }

    mainActivity.showSmokeScreen();

    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(600);
    searchViewLayout.setVisibility(View.VISIBLE);
    animator.start();
    animator.addListener(
        new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animation) {}

          @Override
          public void onAnimationEnd(Animator animation) {
            searchViewEditText.requestFocus();
            InputMethodManager imm =
                (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchViewEditText, InputMethodManager.SHOW_IMPLICIT);
            enabled = true;
          }

          @Override
          public void onAnimationCancel(Animator animation) {}

          @Override
          public void onAnimationRepeat(Animator animation) {}
        });
  }

  /**
   * Sets up the {@link SearchView#searchResultsSortButton} to show a dialog when it is clicked. The
   * text and icon of {@link SearchView#searchResultsSortButton} is also set to the current {@link
   * SearchView#sortType}
   */
  private void setUpSearchResultsSortButton() {
    searchResultsSortButton.setOnClickListener(v -> showSearchResultsSortDialog());
    updateSearchResultsSortButtonDisplay();
  }

  /** Builds and shows a dialog for selection which sort should be applied for the search results */
  private void showSearchResultsSortDialog() {
    int accentColor = mainActivity.getAccent();
    new MaterialDialog.Builder(mainActivity)
        .items(R.array.sortbySearch)
        .itemsCallbackSingleChoice(
            sortType.getSortBy().getIndex(), (dialog, itemView, which, text) -> true)
        .negativeText(R.string.ascending)
        .positiveColor(accentColor)
        .onNegative(
            (dialog, which) -> onSortTypeSelected(dialog, dialog.getSelectedIndex(), SortOrder.ASC))
        .positiveText(R.string.descending)
        .negativeColor(accentColor)
        .onPositive(
            (dialog, which) ->
                onSortTypeSelected(dialog, dialog.getSelectedIndex(), SortOrder.DESC))
        .title(R.string.sort_by)
        .build()
        .show();
  }

  private void onSortTypeSelected(MaterialDialog dialog, int index, SortOrder sortOrder) {
    this.sortType = new SortType(SortBy.getSortBy(index), sortOrder);
    dialog.dismiss();
    updateSearchResultsSortButtonDisplay();
    updateResultList(searchRecyclerViewAdapter.getCurrentList(), getSearchTerm());
  }

  private void resetSearchResultsSortButton() {
    sortType = defaultSortType;
    updateSearchResultsSortButtonDisplay();
  }

  /** Updates the text and icon of {@link SearchView#searchResultsSortButton} */
  private void updateSearchResultsSortButtonDisplay() {
    searchResultsSortButton.setText(sortType.getSortBy().toResourceString(mainActivity));
    setSearchResultSortOrderIcon();
  }

  /**
   * Updates the icon of {@link SearchView#searchResultsSortButton} and colors it to fit the text
   * color
   */
  private void setSearchResultSortOrderIcon() {
    Drawable orderDrawable;
    switch (sortType.getSortOrder()) {
      default:
      case ASC:
        orderDrawable = searchResultsSortAscDrawable;
        break;
      case DESC:
        orderDrawable = searchResultsSortDescDrawable;
        break;
    }

    orderDrawable.setColorFilter(
        new PorterDuffColorFilter(
            mainActivity.getResources().getColor(R.color.accent_material_light),
            PorterDuff.Mode.SRC_ATOP));
    searchResultsSortButton.setCompoundDrawablesWithIntrinsicBounds(
        null, null, orderDrawable, null);
  }

  /** hide search view with a circular reveal animation */
  public void hideSearchView() {
    final int END_RADIUS = 16;
    int startRadius = Math.max(searchViewLayout.getWidth(), searchViewLayout.getHeight());
    Animator animator;
    if (SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      int[] searchCoords = new int[2];
      View searchItem =
          appbar
              .getToolbar()
              .findViewById(R.id.search); // It could change position, get it every time
      searchViewEditText.setText("");
      searchItem.getLocationOnScreen(searchCoords);
      animator =
          ViewAnimationUtils.createCircularReveal(
              searchViewLayout,
              searchCoords[0] + 32,
              searchCoords[1] - 16,
              startRadius,
              END_RADIUS);
    } else {
      // TODO: ViewAnimationUtils.createCircularReveal
      animator = ObjectAnimator.ofFloat(searchViewLayout, "alpha", 1f, 0f);
    }

    clearRecyclerView();

    // removing background fade view
    mainActivity.hideSmokeScreen();
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(600);
    animator.start();
    animator.addListener(
        new Animator.AnimatorListener() {
          @Override
          public void onAnimationStart(Animator animation) {}

          @Override
          public void onAnimationEnd(Animator animation) {
            searchViewLayout.setVisibility(View.GONE);
            enabled = false;
            InputMethodManager inputMethodManager =
                (InputMethodManager) mainActivity.getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                searchViewEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
          }

          @Override
          public void onAnimationCancel(Animator animation) {}

          @Override
          public void onAnimationRepeat(Animator animation) {}
        });
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isShown() {
    return searchViewLayout.isShown();
  }

  private void initSearchViewColor(MainActivity a) {
    AppTheme theme = a.getAppTheme();
    switch (theme) {
      case LIGHT:
        searchViewLayout.setBackgroundResource(R.drawable.search_view_shape);
        searchViewEditText.setTextColor(Utils.getColor(a, android.R.color.black));
        clearImageView.setColorFilter(
            ContextCompat.getColor(a, android.R.color.black), PorterDuff.Mode.SRC_ATOP);
        backImageView.setColorFilter(
            ContextCompat.getColor(a, android.R.color.black), PorterDuff.Mode.SRC_ATOP);
        break;
      case DARK:
      case BLACK:
        if (theme == AppTheme.DARK) {
          searchViewLayout.setBackgroundResource(R.drawable.search_view_shape_holo_dark);
        } else {
          searchViewLayout.setBackgroundResource(R.drawable.search_view_shape_black);
        }
        searchViewEditText.setTextColor(Utils.getColor(a, android.R.color.white));
        clearImageView.setColorFilter(
            ContextCompat.getColor(a, android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        backImageView.setColorFilter(
            ContextCompat.getColor(a, android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        break;
      default:
        break;
    }
  }

  @SuppressLint("NotifyDataSetChanged")
  private void clearRecyclerView() {
    searchRecyclerViewAdapter.submitList(new ArrayList<>());
    searchRecyclerViewAdapter.notifyDataSetChanged();

    searchResultsHintTV.setVisibility(View.GONE);
    searchResultsSortHintTV.setVisibility(View.GONE);
    searchResultsSortButton.setVisibility(View.GONE);
  }

  private SpannableString getSpannableText(String s1, String s2) {

    SpannableString spannableString = new SpannableString(s1 + " " + s2);

    spannableString.setSpan(
        new ForegroundColorSpan(mainActivity.getCurrentColorPreference().getAccent()),
        s1.length() + 1,
        spannableString.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannableString.setSpan(
        new StyleSpan(Typeface.BOLD),
        s1.length() + 1,
        spannableString.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return spannableString;
  }

  /**
   * Returns the current text in {@link SearchView#searchViewEditText}
   *
   * @return The current search text
   */
  private String getSearchTerm() {
    return searchViewEditText.getText().toString().trim();
  }

  public interface SearchListener {
    void onSearch(String queue);
  }
}
