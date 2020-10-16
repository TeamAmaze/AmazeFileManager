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

import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.MainActivity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * SearchView, a simple view to search
 *
 * @author Emmanuel on 2/8/2017, at 23:30.
 */
public class SearchView {

  private MainActivity mainActivity;
  private AppBar appbar;

  private RelativeLayout searchViewLayout;
  private AppCompatEditText searchViewEditText;
  private ImageView clearImageView;
  private ImageView backImageView;

  private boolean enabled = false;

  public SearchView(
      final AppBar appbar, final MainActivity a, final SearchListener searchListener) {
    mainActivity = a;
    this.appbar = appbar;

    searchViewLayout = a.findViewById(R.id.search_view);
    searchViewEditText = a.findViewById(R.id.search_edit_text);
    clearImageView = a.findViewById(R.id.search_close_btn);
    backImageView = a.findViewById(R.id.img_view_back);

    clearImageView.setOnClickListener(v -> searchViewEditText.setText(""));

    backImageView.setOnClickListener(v -> appbar.getSearchView().hideSearchView());

    searchViewEditText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            searchListener.onSearch(searchViewEditText.getText().toString());
            appbar.getSearchView().hideSearchView();
            return true;
          }
          return false;
        });

    // searchViewEditText.setTextColor(Utils.getColor(this, android.R.color.black));
    // searchViewEditText.setHintTextColor(Color.parseColor(ThemedActivity.accentSkin));
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

  public interface SearchListener {
    void onSearch(String queue);
  }
}
