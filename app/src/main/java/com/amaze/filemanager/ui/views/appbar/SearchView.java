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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

/**
 * SearchView, a simple view to search
 */
public class SearchView {

  @NonNull
  private final MainActivity mainActivity;
  @NonNull
  private final AppBar appbar;

  @NonNull
  private final FloatingSearchView searchView;

  private boolean enabled = false;

  public SearchView(
          @NonNull final AppBar appbar, @NonNull final MainActivity mainActivity,
          @NonNull final SearchListener searchListener) {
    this.mainActivity = mainActivity;
    this.appbar = appbar;

    searchView = mainActivity.findViewById(R.id.floating_search_view);
    searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
      @Override
      public void onSuggestionClicked(SearchSuggestion searchSuggestion) {}

      @Override
      public void onSearchAction(String currentQuery) {
        searchListener.onSearch(currentQuery);
        hideSearchView();
      }
    });
    searchView.setOnHomeActionClickListener(this::hideSearchView);
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
      searchView.clearQuery();
      searchItem.getLocationOnScreen(searchCoords);
      animator =
          ViewAnimationUtils.createCircularReveal(
                  searchView,
              searchCoords[0] + 32,
              searchCoords[1] - 16,
              START_RADIUS,
              endRadius);
    } else {
      // TODO:ViewAnimationUtils.createCircularReveal
      animator = ObjectAnimator.ofFloat(searchView, "alpha", 0f, 1f);
    }

    mainActivity.showSmokeScreen();

    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(600);
    searchView.setVisibility(View.VISIBLE);
    animator.start();
    animator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            searchView.setSearchFocused(true);
            enabled = true;
          }
        });
  }

  /** hide search view with a circular reveal animation */
  public void hideSearchView() {
    final int END_RADIUS = 16;
    int startRadius = Math.max(searchView.getWidth(), searchView.getHeight());
    Animator animator;
    if (SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      int[] searchCoords = new int[2];
      View searchItem =
          appbar
              .getToolbar()
              .findViewById(R.id.search); // It could change position, get it every time
      searchView.clearQuery();
      searchItem.getLocationOnScreen(searchCoords);
      animator =
          ViewAnimationUtils.createCircularReveal(
                  searchView,
              searchCoords[0] + 32,
              searchCoords[1] - 16,
              startRadius,
              END_RADIUS);
    } else {
      // TODO: ViewAnimationUtils.createCircularReveal
      animator = ObjectAnimator.ofFloat(searchView, "alpha", 1f, 0f);
    }

    // removing background fade view
    mainActivity.hideSmokeScreen();
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.setDuration(600);
    animator.start();
    animator.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            searchView.setVisibility(View.GONE);
            enabled = false;
          }
        });
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isShown() {
    return searchView.isShown();
  }

  public interface SearchListener {
    void onSearch(String queue);
  }
}
