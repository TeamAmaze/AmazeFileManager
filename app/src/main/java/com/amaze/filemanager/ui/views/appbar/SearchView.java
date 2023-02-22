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

import com.amaze.filemanager.R;
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
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

/**
 * SearchView, a simple view to search
 *
 * @author Emmanuel on 2/8/2017, at 23:30.
 */
public class SearchView {

  private final MainActivity mainActivity;
  private final AppBar appbar;

  private final ConstraintLayout searchViewLayout;
  private final AppCompatEditText searchViewEditText;

  private final ImageView clearImageView;
  private final ImageView backImageView;

  private final TextView recentHintTV;
  private final ChipGroup recentChipGroup;

  private final SearchListener searchListener;

  private boolean enabled = false;

  public SearchView(final AppBar appbar, MainActivity mainActivity, SearchListener searchListener) {

    this.mainActivity = mainActivity;
    this.searchListener = searchListener;
    this.appbar = appbar;

    searchViewLayout = mainActivity.findViewById(R.id.search_view);
    searchViewEditText = mainActivity.findViewById(R.id.search_edit_text);
    clearImageView = mainActivity.findViewById(R.id.search_close_btn);
    backImageView = mainActivity.findViewById(R.id.img_view_back);
    recentChipGroup = mainActivity.findViewById(R.id.searchRecentItemsChipGroup);
    recentHintTV = mainActivity.findViewById(R.id.searchRecentHintTV);

    initRecentSearches(mainActivity);

    clearImageView.setOnClickListener(v -> searchViewEditText.setText(""));

    backImageView.setOnClickListener(v -> appbar.getSearchView().hideSearchView());

    searchViewEditText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {

            String s = searchViewEditText.getText().toString();

            searchListener.onSearch(s);
            appbar.getSearchView().hideSearchView();

            String preferenceString =
                PreferenceManager.getDefaultSharedPreferences(mainActivity)
                    .getString(PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS, null);

            ArrayList<String> recentSearches =
                preferenceString != null
                    ? new Gson()
                        .fromJson(preferenceString, new TypeToken<ArrayList<String>>() {}.getType())
                    : new ArrayList<>();

            if (s.isEmpty() || recentSearches.contains(s)) return false;

            recentSearches.add(s);

            if (recentSearches.size() > 5) recentSearches.remove(0);

            PreferenceManager.getDefaultSharedPreferences(mainActivity)
                .edit()
                .putString(
                    PreferencesConstants.PREFERENCE_RECENT_SEARCH_ITEMS,
                    new Gson().toJson(recentSearches))
                .apply();

            initRecentSearches(mainActivity);

            return true;
          }
          return false;
        });

    initSearchViewColor(mainActivity);
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
            searchListener.onSearch(((Chip) v).getText().toString());
            appbar.getSearchView().hideSearchView();
          });
    }
  }

  /** show search view with a circular reveal animation */
  public void revealSearchView() {
    final int START_RADIUS = 16;
    int endRadius = Math.max(appbar.getToolbar().getWidth(), appbar.getToolbar().getHeight());

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
    AppTheme theme = a.getAppTheme().getSimpleTheme(a);
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

  public interface SearchListener {
    void onSearch(String queue);
  }
}
