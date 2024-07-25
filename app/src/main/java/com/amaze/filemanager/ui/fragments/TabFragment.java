/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CURRENT_TAB;
import static com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SAVED_PATHS;
import static com.amaze.filemanager.utils.PreferenceUtils.DEFAULT_CURRENT_TAB;
import static com.amaze.filemanager.utils.PreferenceUtils.DEFAULT_SAVED_PATHS;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.database.models.explorer.Tab;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.ui.ColorCircleDrawable;
import com.amaze.filemanager.ui.ExtensionsKt;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.colors.UserColorPreferences;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.drag.DragToTrashListener;
import com.amaze.filemanager.ui.drag.TabFragmentSideDragListener;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.views.Indicator;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.Utils;

import android.animation.ArgbEvaluator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class TabFragment extends Fragment {
  private final Logger LOG = LoggerFactory.getLogger(TabFragment.class);

  private static final String KEY_PATH = "path";
  private static final String KEY_POSITION = "pos";

  private static final String KEY_FRAGMENT_0 = "tab0";
  private static final String KEY_FRAGMENT_1 = "tab1";

  private boolean savePaths;
  private FragmentManager fragmentManager;

  private final List<Fragment> fragments = new ArrayList<>();
  private ScreenSlidePagerAdapter sectionsPagerAdapter;
  private ViewPager2 viewPager;
  private SharedPreferences sharedPrefs;
  private String path;

  /** ink indicators for viewpager only for Lollipop+ */
  private Indicator indicator;

  /** views for circlular drawables below android lollipop */
  private AppCompatImageView circleDrawable1, circleDrawable2;

  /** color drawable for action bar background */
  private final ColorDrawable colorDrawable = new ColorDrawable();

  /** colors relative to current visible tab */
  private @ColorInt int startColor, endColor;

  private ViewGroup rootView;

  private final ArgbEvaluator evaluator = new ArgbEvaluator();
  private ConstraintLayout dragPlaceholder;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment, container, false);

    fragmentManager = requireActivity().getSupportFragmentManager();
    dragPlaceholder = rootView.findViewById(R.id.drag_placeholder);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      indicator = requireActivity().findViewById(R.id.indicator);
    } else {
      circleDrawable1 = requireActivity().findViewById(R.id.tab_indicator1);
      circleDrawable2 = requireActivity().findViewById(R.id.tab_indicator2);
    }

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
    savePaths = sharedPrefs.getBoolean(PREFERENCE_SAVED_PATHS, DEFAULT_SAVED_PATHS);

    viewPager = rootView.findViewById(R.id.pager);

    boolean hideFab = false;
    if (getArguments() != null) {
      path = getArguments().getString(KEY_PATH);
      hideFab = getArguments().getBoolean(MainFragment.BUNDLE_HIDE_FAB);
    }

    requireMainActivity().supportInvalidateOptionsMenu();
    viewPager.registerOnPageChangeCallback(new OnPageChangeCallbackImpl());

    sectionsPagerAdapter = new ScreenSlidePagerAdapter(requireActivity());
    if (savedInstanceState == null) {
      int lastOpenTab = sharedPrefs.getInt(PREFERENCE_CURRENT_TAB, DEFAULT_CURRENT_TAB);
      MainActivity.currentTab = lastOpenTab;

      refactorDrawerStorages(true, hideFab);

      viewPager.setAdapter(sectionsPagerAdapter);

      try {
        viewPager.setCurrentItem(lastOpenTab, true);
        if (circleDrawable1 != null && circleDrawable2 != null) {
          updateIndicator(viewPager.getCurrentItem());
        }
      } catch (Exception e) {
        LOG.warn("failed to set current viewpager item", e);
      }
    } else {
      fragments.clear();
      try {
        fragments.add(0, fragmentManager.getFragment(savedInstanceState, KEY_FRAGMENT_0));
        fragments.add(1, fragmentManager.getFragment(savedInstanceState, KEY_FRAGMENT_1));
      } catch (Exception e) {
        LOG.warn("failed to clear fragments", e);
      }

      sectionsPagerAdapter = new ScreenSlidePagerAdapter(requireActivity());

      viewPager.setAdapter(sectionsPagerAdapter);
      int pos1 = savedInstanceState.getInt(KEY_POSITION, 0);
      MainActivity.currentTab = pos1;
      viewPager.setCurrentItem(pos1);
      sectionsPagerAdapter.notifyDataSetChanged();
    }

    if (indicator != null) indicator.setViewPager(viewPager);

    UserColorPreferences userColorPreferences = requireMainActivity().getCurrentColorPreference();

    // color of viewpager when current tab is 0
    startColor = userColorPreferences.getPrimaryFirstTab();
    // color of viewpager when current tab is 1
    endColor = userColorPreferences.getPrimarySecondTab();

    /*
     TODO
    //update the views as there is any change in {@link MainActivity#currentTab}
    //probably due to config change
    colorDrawable.setColor(Color.parseColor(MainActivity.currentTab==1 ?
            ThemedActivity.skinTwo : ThemedActivity.skin));
    mainActivity.updateViews(colorDrawable);
    */

    return rootView;
  }

  @Override
  public void onDestroyView() {
    indicator = null; // Free the strong reference
    sharedPrefs.edit().putInt(PREFERENCE_CURRENT_TAB, MainActivity.currentTab).apply();
    super.onDestroyView();
  }

  public void updatePaths(int pos) {
    // Getting old path from database before clearing
    TabHandler tabHandler = TabHandler.getInstance();

    int i = 1;
    for (Fragment fragment : fragments) {
      if (fragment instanceof MainFragment) {
        MainFragment mainFragment = (MainFragment) fragment;
        if (mainFragment.getMainFragmentViewModel() != null
            && i - 1 == MainActivity.currentTab
            && i == pos) {
          updateBottomBar(mainFragment);
          requireMainActivity()
              .getDrawer()
              .selectCorrectDrawerItemForPath(mainFragment.getCurrentPath());
          if (mainFragment.getMainFragmentViewModel().getOpenMode() == OpenMode.FILE) {
            tabHandler.update(
                new Tab(
                    i,
                    mainFragment.getCurrentPath(),
                    mainFragment.getMainFragmentViewModel().getHome()));
          } else {
            tabHandler.update(
                new Tab(
                    i,
                    mainFragment.getMainFragmentViewModel().getHome(),
                    mainFragment.getMainFragmentViewModel().getHome()));
          }
        }
        i++;
      }
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    if (sharedPrefs != null) {
      sharedPrefs.edit().putInt(PREFERENCE_CURRENT_TAB, MainActivity.currentTab).apply();
    }

    if (fragments.size() != 0) {
      if (fragmentManager == null) {
        return;
      }

      fragmentManager.executePendingTransactions();
      fragmentManager.putFragment(outState, KEY_FRAGMENT_0, fragments.get(0));
      fragmentManager.putFragment(outState, KEY_FRAGMENT_1, fragments.get(1));
      outState.putInt(KEY_POSITION, viewPager.getCurrentItem());
    }
  }

  public void setPagingEnabled(boolean isPaging) {
    viewPager.setUserInputEnabled(isPaging);
  }

  public void setCurrentItem(int index) {
    viewPager.setCurrentItem(index);
  }

  private class OnPageChangeCallbackImpl extends ViewPager2.OnPageChangeCallback {

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      final MainFragment mainFragment = requireMainActivity().getCurrentMainFragment();
      if (mainFragment == null
          || mainFragment.getMainFragmentViewModel() == null
          || mainFragment.getMainActivity().getListItemSelected()) {
        return; // we do not want to update toolbar colors when ActionMode is activated
      }

      // during the config change
      @ColorInt
      int color = (int) evaluator.evaluate(position + positionOffset, startColor, endColor);

      colorDrawable.setColor(color);
      requireMainActivity().updateViews(colorDrawable);
    }

    @Override
    public void onPageSelected(int p1) {
      requireMainActivity()
          .getAppbar()
          .getAppbarLayout()
          .animate()
          .translationY(0)
          .setInterpolator(new DecelerateInterpolator(2))
          .start();

      MainActivity.currentTab = p1;

      if (sharedPrefs != null) {
        sharedPrefs.edit().putInt(PREFERENCE_CURRENT_TAB, MainActivity.currentTab).apply();
      }

      Fragment fragment = fragments.get(p1);
      if (fragment instanceof MainFragment) {
        MainFragment ma = (MainFragment) fragment;
        if (ma.getCurrentPath() != null) {
          requireMainActivity().getDrawer().selectCorrectDrawerItemForPath(ma.getCurrentPath());
          updateBottomBar(ma);
          // FAB might be hidden in the previous tab
          // so we check if it should be shown for the new tab
          requireMainActivity().showFab();
        }
      }

      if (circleDrawable1 != null && circleDrawable2 != null) updateIndicator(p1);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
      // nothing to do
    }
  }

  private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
    public ScreenSlidePagerAdapter(FragmentActivity fragmentActivity) {
      super(fragmentActivity);
    }

    @Override
    public int getItemCount() {
      return fragments.size();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      Fragment f;
      f = fragments.get(position);
      return f;
    }
  }

  private void addNewTab(int num, String path) {
    addTab(new Tab(num, path, path), "", false);
  }

  /**
   * Fetches new storage paths from drawer and apply to tabs This method will just create tabs in UI
   * change paths in database. Calls should implement updating each tab's list for new paths.
   *
   * @param addTab whether new tabs should be added to ui or just change values in database
   * @param hideFabInCurrentMainFragment whether the FAB should be hidden in the current {@link
   *     MainFragment}
   */
  public void refactorDrawerStorages(boolean addTab, boolean hideFabInCurrentMainFragment) {
    TabHandler tabHandler = TabHandler.getInstance();
    Tab tab1 = tabHandler.findTab(1);
    Tab tab2 = tabHandler.findTab(2);
    Tab[] tabs = tabHandler.getAllTabs();
    String firstTabPath = requireMainActivity().getDrawer().getFirstPath();
    String secondTabPath = requireMainActivity().getDrawer().getSecondPath();

    if (tabs == null || tabs.length < 1 || tab1 == null || tab2 == null) {
      // creating tabs in db for the first time, probably the first launch of
      // app, or something got corrupted
      String currentFirstTab = Utils.isNullOrEmpty(firstTabPath) ? "/" : firstTabPath;
      String currentSecondTab = Utils.isNullOrEmpty(secondTabPath) ? firstTabPath : secondTabPath;
      if (addTab) {
        addNewTab(1, currentSecondTab);
        addNewTab(2, currentFirstTab);
      }
      tabHandler.addTab(new Tab(1, currentSecondTab, currentSecondTab)).blockingAwait();
      tabHandler.addTab(new Tab(2, currentFirstTab, currentFirstTab)).blockingAwait();

      if (currentFirstTab.equalsIgnoreCase("/")) {
        sharedPrefs.edit().putBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, true).apply();
      }
    } else {
      if (path != null && path.length() != 0) {
        if (MainActivity.currentTab == 0) {
          addTab(tab1, path, hideFabInCurrentMainFragment);
          addTab(tab2, "", false);
        }

        if (MainActivity.currentTab == 1) {
          addTab(tab1, "", false);
          addTab(tab2, path, hideFabInCurrentMainFragment);
        }
      } else {
        addTab(tab1, "", false);
        addTab(tab2, "", false);
      }
    }
  }

  private void addTab(@NonNull Tab tab, String path, boolean hideFabInTab) {
    MainFragment main = new MainFragment();
    Bundle b = new Bundle();

    if (path != null && path.length() != 0) {
      b.putString("lastpath", path);
      b.putInt("openmode", OpenMode.UNKNOWN.ordinal());
    } else {
      b.putString("lastpath", tab.getOriginalPath(savePaths, requireMainActivity().getPrefs()));
    }

    b.putString("home", tab.home);
    b.putInt("no", tab.tabNumber);
    // specifies if the constructed MainFragment hides the FAB when it is shown
    b.putBoolean(MainFragment.BUNDLE_HIDE_FAB, hideFabInTab);
    main.setArguments(b);
    fragments.add(main);
    sectionsPagerAdapter.notifyDataSetChanged();
    viewPager.setOffscreenPageLimit(4);
  }

  public Fragment getCurrentTabFragment() {
    if (fragments.size() == 2) return fragments.get(viewPager.getCurrentItem());
    else return null;
  }

  public Fragment getFragmentAtIndex(int pos) {
    if (fragments.size() == 2 && pos < 2) return fragments.get(pos);
    else return null;
  }

  // updating indicator color as per the current viewpager tab
  void updateIndicator(int index) {
    if (index != 0 && index != 1) return;

    int accentColor = requireMainActivity().getAccent();

    circleDrawable1.setImageDrawable(new ColorCircleDrawable(accentColor));
    circleDrawable2.setImageDrawable(new ColorCircleDrawable(Color.GRAY));
  }

  public ConstraintLayout getDragPlaceholder() {
    return this.dragPlaceholder;
  }

  public void updateBottomBar(MainFragment mainFragment) {
    if (mainFragment == null || mainFragment.getMainFragmentViewModel() == null) {
      LOG.warn("Failed to update bottom bar: main fragment not available");
      return;
    }
    requireMainActivity()
        .getAppbar()
        .getBottomBar()
        .updatePath(
            mainFragment.getCurrentPath(),
            mainFragment.getMainFragmentViewModel().getOpenMode(),
            mainFragment.getMainFragmentViewModel().getFolderCount(),
            mainFragment.getMainFragmentViewModel().getFileCount(),
            mainFragment);
  }

  public void initLeftRightAndTopDragListeners(boolean destroy, boolean shouldInvokeLeftAndRight) {
    if (shouldInvokeLeftAndRight) {
      initLeftAndRightDragListeners(destroy);
    }
    for (Fragment fragment : fragments) {
      if (fragment instanceof MainFragment) {
        MainFragment m = (MainFragment) fragment;
        m.initTopAndEmptyAreaDragListeners(destroy);
      }
    }
  }

  private void initLeftAndRightDragListeners(boolean destroy) {
    final MainFragment mainFragment = requireMainActivity().getCurrentMainFragment();
    View leftPlaceholder = rootView.findViewById(R.id.placeholder_drag_left);
    View rightPlaceholder = rootView.findViewById(R.id.placeholder_drag_right);
    AppCompatImageView dragToTrash = rootView.findViewById(R.id.placeholder_trash_bottom);
    DataUtils dataUtils = DataUtils.getInstance();
    if (destroy) {
      leftPlaceholder.setOnDragListener(null);
      rightPlaceholder.setOnDragListener(null);
      dragToTrash.setOnDragListener(null);
      leftPlaceholder.setVisibility(View.GONE);
      rightPlaceholder.setVisibility(View.GONE);
      ExtensionsKt.hideFade(dragToTrash, 150);
    } else {
      leftPlaceholder.setVisibility(View.VISIBLE);
      rightPlaceholder.setVisibility(View.VISIBLE);
      ExtensionsKt.showFade(dragToTrash, 150);
      leftPlaceholder.setOnDragListener(
          new TabFragmentSideDragListener(
              () -> {
                if (viewPager.getCurrentItem() == 1) {
                  if (mainFragment != null) {
                    dataUtils.setCheckedItemsList(mainFragment.adapter.getCheckedItems());
                    requireMainActivity().getActionModeHelper().disableActionMode();
                  }
                  viewPager.setCurrentItem(0, true);
                }
                return null;
              }));
      rightPlaceholder.setOnDragListener(
          new TabFragmentSideDragListener(
              () -> {
                if (viewPager.getCurrentItem() == 0) {
                  if (mainFragment != null) {
                    dataUtils.setCheckedItemsList(mainFragment.adapter.getCheckedItems());
                    requireMainActivity().getActionModeHelper().disableActionMode();
                  }
                  viewPager.setCurrentItem(1, true);
                }
                return null;
              }));
      dragToTrash.setOnDragListener(
          new DragToTrashListener(
              () -> {
                if (mainFragment != null) {
                  GeneralDialogCreation.deleteFilesDialog(
                      requireContext(),
                      requireMainActivity(),
                      mainFragment.adapter.getCheckedItems(),
                      requireMainActivity().getAppTheme());
                } else {
                  AppConfig.toast(requireContext(), getString(R.string.operation_unsuccesful));
                }
                return null;
              },
              () -> {
                dragToTrash.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                return null;
              }));
    }
  }

  @NonNull
  private MainActivity requireMainActivity() {
    return (MainActivity) requireActivity();
  }
}
