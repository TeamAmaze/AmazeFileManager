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

package com.amaze.filemanager.ui.activities;

import static com.amaze.filemanager.utils.Utils.openURL;

import com.amaze.filemanager.LogHelper;
import com.amaze.filemanager.R;
import com.amaze.filemanager.ui.activities.superclasses.BasicActivity;
import com.amaze.filemanager.ui.theme.AppTheme;
import com.amaze.filemanager.utils.Billing;
import com.amaze.filemanager.utils.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.palette.graphics.Palette;

/** Created by vishal on 27/7/16. */
public class AboutActivity extends BasicActivity implements View.OnClickListener {

  private static final String TAG = "AboutActivity";

  private static final int HEADER_HEIGHT = 1024;
  private static final int HEADER_WIDTH = 500;

  private AppBarLayout mAppBarLayout;
  private CollapsingToolbarLayout mCollapsingToolbarLayout;
  private TextView mTitleTextView;
  private View mAuthorsDivider, mDeveloper1Divider;
  private Billing billing;

  private static final String URL_AUTHOR1_GITHUB = "https://github.com/arpitkh96";
  private static final String URL_AUTHOR2_GITHUB = "https://github.com/VishalNehra";
  private static final String URL_DEVELOPER1_GITHUB = "https://github.com/EmmanuelMess";
  private static final String URL_DEVELOPER2_GITHUB = "https://github.com/TranceLove";
  private static final String URL_REPO_CHANGELOG =
      "https://github.com/TeamAmaze/AmazeFileManager/commits/master";
  private static final String URL_REPO = "https://github.com/TeamAmaze/AmazeFileManager";
  private static final String URL_REPO_ISSUES =
      "https://github.com/TeamAmaze/AmazeFileManager/issues";
  private static final String URL_REPO_TRANSLATE =
      "https://www.transifex.com/amaze/amaze-file-manager/";
  private static final String URL_REPO_XDA =
      "http://forum.xda-developers.com/android/apps-games/app-amaze-file-managermaterial-theme-t2937314";
  private static final String URL_REPO_RATE = "market://details?id=com.amaze.filemanager";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getAppTheme().equals(AppTheme.DARK)) {
      setTheme(R.style.aboutDark);
    } else if (getAppTheme().equals(AppTheme.BLACK)) {
      setTheme(R.style.aboutBlack);
    } else {
      setTheme(R.style.aboutLight);
    }

    setContentView(R.layout.activity_about);

    mAppBarLayout = findViewById(R.id.appBarLayout);
    mCollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout);
    mTitleTextView = findViewById(R.id.text_view_title);
    mAuthorsDivider = findViewById(R.id.view_divider_authors);
    mDeveloper1Divider = findViewById(R.id.view_divider_developers_1);

    mAppBarLayout.setLayoutParams(calculateHeaderViewParams());

    Toolbar mToolbar = findViewById(R.id.toolBar);
    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.md_nav_back));
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    switchIcons();

    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.about_header);

    // It will generate colors based on the image in an AsyncTask.
    Palette.from(bitmap)
        .generate(
            palette -> {
              int mutedColor =
                  palette.getMutedColor(Utils.getColor(AboutActivity.this, R.color.primary_blue));
              int darkMutedColor =
                  palette.getDarkMutedColor(
                      Utils.getColor(AboutActivity.this, R.color.primary_blue));
              mCollapsingToolbarLayout.setContentScrimColor(mutedColor);
              mCollapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor);
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(darkMutedColor);
              }
            });

    mAppBarLayout.addOnOffsetChangedListener(
        (appBarLayout, verticalOffset) -> {
          mTitleTextView.setAlpha(
              Math.abs(verticalOffset / (float) appBarLayout.getTotalScrollRange()));
        });
    mAppBarLayout.setOnFocusChangeListener(
        (v, hasFocus) -> {
          mAppBarLayout.setExpanded(hasFocus, true);
        });
  }

  /**
   * Calculates aspect ratio for the Amaze header
   *
   * @return the layout params with new set of width and height attribute
   */
  private CoordinatorLayout.LayoutParams calculateHeaderViewParams() {

    // calculating cardview height as per the youtube video thumb aspect ratio
    CoordinatorLayout.LayoutParams layoutParams =
        (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
    float vidAspectRatio = (float) HEADER_WIDTH / (float) HEADER_HEIGHT;
    Log.d(TAG, vidAspectRatio + "");
    int screenWidth = getResources().getDisplayMetrics().widthPixels;
    float reqHeightAsPerAspectRatio = (float) screenWidth * vidAspectRatio;
    Log.d(TAG, reqHeightAsPerAspectRatio + "");

    Log.d(TAG, "new width: " + screenWidth + " and height: " + reqHeightAsPerAspectRatio);

    layoutParams.width = screenWidth;
    layoutParams.height = (int) reqHeightAsPerAspectRatio;
    return layoutParams;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  /** Method switches icon resources as per current theme */
  private void switchIcons() {
    if (getAppTheme().equals(AppTheme.DARK) || getAppTheme().equals(AppTheme.BLACK)) {
      // dark theme
      mAuthorsDivider.setBackgroundColor(Utils.getColor(this, R.color.divider_dark_card));
      mDeveloper1Divider.setBackgroundColor(Utils.getColor(this, R.color.divider_dark_card));
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.relative_layout_source:
        openURL(URL_REPO, this);
        break;

      case R.id.relative_layout_issues:
        openURL(URL_REPO_ISSUES, this);
        break;

      case R.id.relative_layout_changelog:
        openURL(URL_REPO_CHANGELOG, this);
        break;

      case R.id.relative_layout_licenses:
        LibsBuilder libsBuilder =
            new LibsBuilder()
                .withLibraries("apachemina") // Not auto-detected for some reason
                .withActivityTitle(getString(R.string.libraries))
                .withAboutIconShown(true)
                .withAboutVersionShownName(true)
                .withAboutVersionShownCode(false)
                .withAboutDescription(getString(R.string.about_amaze))
                .withAboutSpecial1(getString(R.string.license))
                .withAboutSpecial1Description(getString(R.string.amaze_license))
                .withLicenseShown(true);

        switch (getAppTheme().getSimpleTheme()) {
          case LIGHT:
            libsBuilder.withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR);
            break;
          case DARK:
            libsBuilder.withActivityStyle(Libs.ActivityStyle.DARK);
            break;
          case BLACK:
            libsBuilder.withActivityTheme(R.style.AboutLibrariesTheme_Black);
            break;
          default:
            LogHelper.logOnProductionOrCrash(TAG, "Incorrect value for switch");
        }

        libsBuilder.start(this);

        break;

      case R.id.text_view_author_1_github:
        openURL(URL_AUTHOR1_GITHUB, this);
        break;

      case R.id.text_view_author_2_github:
        openURL(URL_AUTHOR2_GITHUB, this);
        break;

      case R.id.text_view_developer_1_github:
        openURL(URL_DEVELOPER1_GITHUB, this);
        break;

      case R.id.text_view_developer_2_github:
        openURL(URL_DEVELOPER2_GITHUB, this);
        break;

      case R.id.relative_layout_translate:
        openURL(URL_REPO_TRANSLATE, this);
        break;

      case R.id.relative_layout_xda:
        openURL(URL_REPO_XDA, this);
        break;

      case R.id.relative_layout_rate:
        openURL(URL_REPO_RATE, this);
        break;
      case R.id.relative_layout_donate:
        billing = new Billing(this);
        break;
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Destroying the manager.");
    if (billing != null) {
      billing.destroyBillingInstance();
    }
  }
}
