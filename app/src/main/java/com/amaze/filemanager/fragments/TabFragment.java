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

package com.amaze.filemanager.fragments;

import java.util.ArrayList;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.database.models.Tab;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.ColorCircleDrawable;
import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.views.DisablableViewPager;
import com.amaze.filemanager.ui.views.Indicator;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.PreferenceUtils;

import android.animation.ArgbEvaluator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

/** Created by Arpit on 15-12-2014. */
public class TabFragment extends Fragment implements ViewPager.OnPageChangeListener {

  public List<Fragment> fragments = new ArrayList<>();
  public ScreenSlidePagerAdapter mSectionsPagerAdapter;
  public DisablableViewPager mViewPager;

  // current visible tab, either 0 or 1
  // public int currenttab;
  private MainActivity mainActivity;
  private boolean savepaths;
  private FragmentManager fragmentManager;

  private static final String KEY_POSITION = "pos";

  private SharedPreferences sharedPrefs;
  private String path;

  // ink indicators for viewpager only for Lollipop+
  private Indicator indicator;

  // views for circlular drawables below android lollipop
  private ImageView circleDrawable1, circleDrawable2;

  // color drawable for action bar background
  private ColorDrawable colorDrawable = new ColorDrawable();

  // colors relative to current visible tab
  private @ColorInt int startColor, endColor;

  private TabHandler tabHandler;

  private ArgbEvaluator evaluator = new ArgbEvaluator();

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment, container, false);

    tabHandler = new TabHandler(getContext());
    fragmentManager = getActivity().getSupportFragmentManager();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      indicator = getActivity().findViewById(R.id.indicator);
    } else {
      circleDrawable1 = getActivity().findViewById(R.id.tab_indicator1);
      circleDrawable2 = getActivity().findViewById(R.id.tab_indicator2);
    }

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    savepaths = sharedPrefs.getBoolean("savepaths", true);

    mViewPager = rootView.findViewById(R.id.pager);

    if (getArguments() != null) {
      path = getArguments().getString("path");
    }
    mainActivity = ((MainActivity) getActivity());
    mainActivity.supportInvalidateOptionsMenu();
    mViewPager.addOnPageChangeListener(this);

    mSectionsPagerAdapter = new ScreenSlidePagerAdapter(getActivity().getSupportFragmentManager());
    if (savedInstanceState == null) {
      int lastOpenTab =
          sharedPrefs.getInt(
              PreferencesConstants.PREFERENCE_CURRENT_TAB, PreferenceUtils.DEFAULT_CURRENT_TAB);
      MainActivity.currentTab = lastOpenTab;
      Tab tab1 = tabHandler.findTab(1);
      Tab tab2 = tabHandler.findTab(2);

      if (tabHandler.getAllTabs().isEmpty()
          || tab1 == null
          || tab2 == null) { // creating tabs in db for the first time, probably the first launch of
        // app, or something got corrupted
        if (mainActivity.getDrawer().getFirstPath() != null) {
          addNewTab(1, mainActivity.getDrawer().getFirstPath());
        } else {
          if (mainActivity.getDrawer().getSecondPath() != null) {
            addNewTab(1, mainActivity.getDrawer().getSecondPath());
          } else {
            sharedPrefs.edit().putBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, true).apply();
            addNewTab(1, "/");
          }
        }

        if (mainActivity.getDrawer().getSecondPath() != null) {
          addNewTab(2, mainActivity.getDrawer().getSecondPath());
        } else {
          addNewTab(2, mainActivity.getDrawer().getFirstPath());
        }
      } else {
        if (path != null && path.length() != 0) {
          if (lastOpenTab == 0) {
            addTab(tab1, path);
            addTab(tab2, "");
          }

          if (lastOpenTab == 1) {
            addTab(tab1, "");
            addTab(tab2, path);
          }
        } else {
          addTab(tab1, "");
          addTab(tab2, "");
        }
      }

      mViewPager.setAdapter(mSectionsPagerAdapter);

      try {
        mViewPager.setCurrentItem(lastOpenTab, true);
        if (circleDrawable1 != null && circleDrawable2 != null) {
          updateIndicator(mViewPager.getCurrentItem());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

    } else {
      fragments.clear();
      try {
        if (fragmentManager == null) {
          fragmentManager = getActivity().getSupportFragmentManager();
        }

        fragments.add(0, fragmentManager.getFragment(savedInstanceState, "tab" + 0));
        fragments.add(1, fragmentManager.getFragment(savedInstanceState, "tab" + 1));
      } catch (Exception e) {
        e.printStackTrace();
      }

      mSectionsPagerAdapter =
          new ScreenSlidePagerAdapter(getActivity().getSupportFragmentManager());

      mViewPager.setAdapter(mSectionsPagerAdapter);
      int pos1 = savedInstanceState.getInt(KEY_POSITION, 0);
      MainActivity.currentTab = pos1;
      mViewPager.setCurrentItem(pos1);
      mSectionsPagerAdapter.notifyDataSetChanged();
    }

    if (indicator != null) indicator.setViewPager(mViewPager);

    UserColorPreferences userColorPreferences = mainActivity.getCurrentColorPreference();

    // color of viewpager when current tab is 0
    startColor = userColorPreferences.primaryFirstTab;
    // color of viewpager when current tab is 1
    endColor = userColorPreferences.primarySecondTab;

    // update the views as there is any change in {@link MainActivity#currentTab}
    // probably due to config change
    /*colorDrawable.setColor(Color.parseColor(MainActivity.currentTab==1 ?
            ThemedActivity.skinTwo : ThemedActivity.skin));
    mainActivity.updateViews(colorDrawable);*/

    mainActivity.mainFragment = (MainFragment) getCurrentTabFragment();

    return rootView;
  }

  @Override
  public void onDestroyView() {
    sharedPrefs
        .edit()
        .putInt(PreferencesConstants.PREFERENCE_CURRENT_TAB, MainActivity.currentTab)
        .apply();
    super.onDestroyView();
    try {
      if (tabHandler != null) tabHandler.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void updatepaths(int pos) {
    if (tabHandler == null) tabHandler = new TabHandler(getActivity());
    int i = 1;

    // Getting old path from database before clearing

    tabHandler.clear();
    for (Fragment fragment : fragments) {
      if (fragment instanceof MainFragment) {
        MainFragment m = (MainFragment) fragment;
        if (i - 1 == MainActivity.currentTab && i == pos) {
          mainActivity
              .getAppbar()
              .getBottomBar()
              .updatePath(
                  m.getCurrentPath(),
                  m.results,
                  MainActivityHelper.SEARCH_TEXT,
                  m.openMode,
                  m.folder_count,
                  m.file_count,
                  m);
          mainActivity.getDrawer().selectCorrectDrawerItemForPath(m.getCurrentPath());
        }
        if (m.openMode == OpenMode.FILE) {
          tabHandler.addTab(new Tab(i, m.getCurrentPath(), m.home));
        } else {
          tabHandler.addTab(new Tab(i, m.home, m.home));
        }

        i++;
      }
    }
  }

  String parseSmbPath(String a) {
    if (a.contains("@")) return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
    else return a;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    int i = 0;

    if (sharedPrefs != null) {
      sharedPrefs
          .edit()
          .putInt(PreferencesConstants.PREFERENCE_CURRENT_TAB, MainActivity.currentTab)
          .commit();
    }

    if (fragments != null && fragments.size() != 0) {
      if (fragmentManager == null) return;
      for (Fragment fragment : fragments) {
        fragmentManager.putFragment(outState, "tab" + i, fragment);
        i++;
      }
      outState.putInt(KEY_POSITION, mViewPager.getCurrentItem());
    }
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    MainFragment mainFragment = mainActivity.getCurrentMainFragment();
    if (mainFragment != null
        && !mainFragment
            .selection) { // we do not want to update toolbar colors when ActionMode is activated
      // during the config change
      @ColorInt
      int color = (int) evaluator.evaluate(position + positionOffset, startColor, endColor);

      colorDrawable.setColor(color);
      mainActivity.updateViews(colorDrawable);
    }
  }

  @Override
  public void onPageSelected(int p1) {
    mainActivity
        .getAppbar()
        .getAppbarLayout()
        .animate()
        .translationY(0)
        .setInterpolator(new DecelerateInterpolator(2))
        .start();

    MainActivity.currentTab = p1;

    if (sharedPrefs != null) {
      sharedPrefs
          .edit()
          .putInt(PreferencesConstants.PREFERENCE_CURRENT_TAB, MainActivity.currentTab)
          .commit();
    }

    Log.d(getClass().getSimpleName(), "Page Selected: " + MainActivity.currentTab);

    Fragment fragment = fragments.get(p1);
    if (fragment != null && fragment instanceof MainFragment) {
      MainFragment ma = (MainFragment) fragment;
      if (ma.getCurrentPath() != null) {
        mainActivity.getDrawer().selectCorrectDrawerItemForPath(ma.getCurrentPath());
        mainActivity
            .getAppbar()
            .getBottomBar()
            .updatePath(
                ma.getCurrentPath(),
                ma.results,
                MainActivityHelper.SEARCH_TEXT,
                ma.openMode,
                ma.folder_count,
                ma.file_count,
                ma);
      }
    }

    if (circleDrawable1 != null && circleDrawable2 != null) updateIndicator(p1);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    // nothing to do
  }

  private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    @Override
    public int getItemPosition(Object object) {
      int index = fragments.indexOf(object);
      if (index == -1) return POSITION_NONE;
      else return index;
    }

    public int getCount() {
      // TODO: Implement this method
      return fragments.size();
    }

    public ScreenSlidePagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      Fragment f;
      f = fragments.get(position);
      return f;
    }
  }

  private void addNewTab(int num, String path) {
    addTab(new Tab(num, path, path), "");
  }

  public void addTab(@NonNull Tab tab, String path) {
    Fragment main = new MainFragment();
    Bundle b = new Bundle();

    if (path != null && path.length() != 0) {
      b.putString("lastpath", path);
      b.putInt("openmode", OpenMode.UNKNOWN.ordinal());
    } else {
      b.putString("lastpath", tab.getOriginalPath(savepaths, mainActivity.getPrefs()));
    }

    b.putString("home", tab.home);
    b.putInt("no", tab.tabNumber);
    main.setArguments(b);
    fragments.add(main);
    mSectionsPagerAdapter.notifyDataSetChanged();
    mViewPager.setOffscreenPageLimit(4);
  }

  public Fragment getCurrentTabFragment() {
    if (fragments.size() == 2) return fragments.get(mViewPager.getCurrentItem());
    else return null;
  }

  public Fragment getFragmentAtIndex(int pos) {
    if (fragments.size() == 2 && pos < 2) return fragments.get(pos);
    else return null;
  }

  // updating indicator color as per the current viewpager tab
  void updateIndicator(int index) {
    if (index != 0 && index != 1) return;

    int accentColor = mainActivity.getAccent();

    if (index == 0) {
      circleDrawable1.setImageDrawable(new ColorCircleDrawable(accentColor));
      circleDrawable2.setImageDrawable(new ColorCircleDrawable(Color.GRAY));
    } else {
      circleDrawable1.setImageDrawable(new ColorCircleDrawable(accentColor));
      circleDrawable2.setImageDrawable(new ColorCircleDrawable(Color.GRAY));
    }
  }
}
