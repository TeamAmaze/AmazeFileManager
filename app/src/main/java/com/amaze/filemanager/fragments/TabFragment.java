package com.amaze.filemanager.fragments;

import android.animation.ArgbEvaluator;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.TabHandler;
import com.amaze.filemanager.database.models.Tab;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.ColorCircleDrawable;
import com.amaze.filemanager.ui.views.DisablableViewPager;
import com.amaze.filemanager.ui.views.Indicator;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.color.ColorUsage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arpit on 15-12-2014.
 */
public class TabFragment extends android.support.v4.app.Fragment
        implements ViewPager.OnPageChangeListener {

    public List<Fragment> fragments = new ArrayList<>();
    public ScreenSlidePagerAdapter mSectionsPagerAdapter;
    public DisablableViewPager mViewPager;
    SharedPreferences sharedPrefs;
    String path;

    // current visible tab, either 0 or 1
    //public int currenttab;
    MainActivity mainActivity;
    boolean savepaths;
    FragmentManager fragmentManager;

    private static final String KEY_POSITION = "pos";

    // ink indicators for viewpager only for Lollipop+
    private Indicator indicator;

    // views for circlular drawables below android lollipop
    private ImageView circleDrawable1, circleDrawable2;
    private boolean coloredNavigation;

    // color drawable for action bar background
    private ColorDrawable colorDrawable = new ColorDrawable();

    // colors relative to current visible tab
    private String startColor, endColor;

    private TabHandler tabHandler;
    private DataUtils dataUtils = DataUtils.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.tabfragment, container, false);

        tabHandler = new TabHandler(getContext());
        fragmentManager = getActivity().getSupportFragmentManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            indicator = (Indicator) getActivity().findViewById(R.id.indicator);
        } else {
            circleDrawable1 = (ImageView) getActivity().findViewById(R.id.tab_indicator1);
            circleDrawable2 = (ImageView) getActivity().findViewById(R.id.tab_indicator2);
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        savepaths = sharedPrefs.getBoolean("savepaths", true);
        coloredNavigation = sharedPrefs.getBoolean(PreferencesConstants.PREFERENCE_COLORED_NAVIGATION, true);

        mViewPager = (DisablableViewPager) rootView.findViewById(R.id.pager);

        if (getArguments() != null) {
            path = getArguments().getString("path");
        }
        mainActivity = ((MainActivity) getActivity());
        mainActivity.supportInvalidateOptionsMenu();
        mViewPager.addOnPageChangeListener(this);

        mSectionsPagerAdapter = new ScreenSlidePagerAdapter(getActivity().getSupportFragmentManager());
        if (savedInstanceState == null) {
            int l = sharedPrefs.getInt(PreferenceUtils.KEY_CURRENT_TAB, PreferenceUtils.DEFAULT_CURRENT_TAB);
            MainActivity.currentTab = l;
            List<Tab> tabs1 = tabHandler.getAllTabs();
            int i = tabs1.size();
            if (i == 0) {
                // creating tabs in db for the first time, probably the first launch of app
                if (mainActivity.getDrawer().getStorageCount() > 1)
                    addTab(new Tab(1,  dataUtils.getDrawerItems().get(1).path, "/"), 1, "");
                else
                    addTab(new Tab(1,"/", "/"), 1, "");
                if (!dataUtils.getDrawerItems().get(0).isSection()) {
                    String pa = dataUtils.getDrawerItems().get(0).path;
                    addTab(new Tab(2, pa, pa), 2, "");
                } else
                    addTab(new Tab(2, dataUtils.getDrawerItems().get(1).path, "/"), 2, "");
            } else {
                if (path != null && path.length() != 0) {
                    if (l == 1)
                        addTab(tabHandler.findTab(1), 1, "");
                    addTab(tabHandler.findTab(l + 1), l + 1, path);
                    if (l == 0)
                        addTab(tabHandler.findTab(2), 2, "");
                } else {
                    addTab(tabHandler.findTab(1), 1, "");
                    addTab(tabHandler.findTab(2), 2, "");
                }
            }

            mViewPager.setAdapter(mSectionsPagerAdapter);

            try {
                mViewPager.setCurrentItem(l, true);
                if (circleDrawable1 != null && circleDrawable2 != null) {
                    updateIndicator(mViewPager.getCurrentItem());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            fragments.clear();
            try {
                if (fragmentManager == null)
                    fragmentManager = getActivity().getSupportFragmentManager();
                fragments.add(0, fragmentManager.getFragment(savedInstanceState, "tab" + 0));
                fragments.add(1, fragmentManager.getFragment(savedInstanceState, "tab" + 1));
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSectionsPagerAdapter = new ScreenSlidePagerAdapter(
                    getActivity().getSupportFragmentManager());

            mViewPager.setAdapter(mSectionsPagerAdapter);
            int pos1 = savedInstanceState.getInt(KEY_POSITION, 0);
            MainActivity.currentTab = pos1;
            mViewPager.setCurrentItem(pos1);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }


        if (indicator != null) indicator.setViewPager(mViewPager);

        // color of viewpager when current tab is 0
        startColor = mainActivity.getColorPreference().getColorAsString(ColorUsage.PRIMARY);
        // color of viewpager when current tab is 1
        endColor = mainActivity.getColorPreference().getColorAsString(ColorUsage.PRIMARY_TWO);

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
        sharedPrefs.edit().putInt(PreferenceUtils.KEY_CURRENT_TAB, MainActivity.currentTab).apply();
        super.onDestroyView();
        try {
            if (tabHandler != null)
                tabHandler.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatepaths(int pos) {
        if (tabHandler == null)
            tabHandler = new TabHandler(getActivity());
        int i = 1;
        ArrayList<String> items = new ArrayList<>();

        // Getting old path from database before clearing

        tabHandler.clear();
        for (Fragment fragment : fragments) {
            if (fragment instanceof MainFragment) {
                MainFragment m = (MainFragment) fragment;
                items.add(parsePathForName(m.getCurrentPath(), m.openMode));
                if (i - 1 == MainActivity.currentTab && i == pos) {
                    mainActivity.getAppbar().getBottomBar().updatePath(m.getCurrentPath(), m.results,
                            MainActivityHelper.SEARCH_TEXT, m.openMode, m.folder_count, m.file_count, m);
                    mainActivity.getDrawer().selectCorrectDrawerItemForPath(m.getCurrentPath());
                }
                if (m.openMode == OpenMode.FILE) {
                    tabHandler.addTab(new Tab(i, m.getCurrentPath(), m.home));
                } else
                    tabHandler.addTab(new Tab(i, m.home, m.home));

                i++;
            }
        }
    }

    String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }

    String parsePathForName(String path, OpenMode openmode) {
        Resources resources = getActivity().getResources();
        if ("/".equals(path))
            return resources.getString(R.string.rootdirectory);
        else if (openmode == OpenMode.SMB && path.startsWith("smb:/"))
            return (new File(parseSmbPath(path)).getName());
        else if ("/storage/emulated/0".equals(path))
            return resources.getString(R.string.storage);
        else if (openmode == OpenMode.CUSTOM)
            return new MainActivityHelper(mainActivity).getIntegralNames(path);
        else
            return new File(path).getName();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int i = 0;
        if (sharedPrefs != null)
            sharedPrefs.edit().putInt(PreferenceUtils.KEY_CURRENT_TAB, MainActivity.currentTab).commit();
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
        ArgbEvaluator evaluator = new ArgbEvaluator();

        int color = (int) evaluator.evaluate(position + positionOffset, Color.parseColor(startColor),
                Color.parseColor(endColor));

        colorDrawable.setColor(color);

        if (mainActivity.mainFragment != null & !mainActivity.mainFragment.selection) {
            // we do not want to update toolbar colors when action mode is activated
            // during the config change
            mainActivity.updateViews(colorDrawable);
        }
    }

    @Override
    public void onPageSelected(int p1) {
        mainActivity.getAppbar().getAppbarLayout().animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();

        MainActivity.currentTab = p1;
        if (sharedPrefs != null)
            sharedPrefs.edit().putInt(PreferenceUtils.KEY_CURRENT_TAB, MainActivity.currentTab).commit();
        Log.d(getClass().getSimpleName(), "Page Selected: " + MainActivity.currentTab);

        Fragment fragment = fragments.get(p1);
        if (fragment != null && fragment instanceof MainFragment) {
            MainFragment ma = (MainFragment) fragment;
            if (ma.getCurrentPath() != null) {
                mainActivity.getDrawer().selectCorrectDrawerItemForPath(ma.getCurrentPath());
                mainActivity.getAppbar().getBottomBar().updatePath(ma.getCurrentPath(),
                        ma.results, MainActivityHelper.SEARCH_TEXT, ma.openMode,
                        ma.folder_count, ma.file_count, ma);
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
            int index = fragments.indexOf((Fragment) object);
            if (index == -1)
                return POSITION_NONE;
            else
                return index;
        }

        public int getCount() {
            // TODO: Implement this method
            return fragments.size();
        }

        public ScreenSlidePagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            android.support.v4.app.Fragment f;
            f = fragments.get(position);
            return f;
        }
    }

    public void addTab(Tab tab, int pos, String path) {
        if (tab == null) return;
        android.support.v4.app.Fragment main = new MainFragment();
        Bundle b = new Bundle();
        if (path != null && path.length() != 0)
            b.putString("lastpath", path);
        else
            b.putString("lastpath", tab.getOriginalPath(savepaths, mainActivity.getPrefs()));
        b.putString("home", tab.home);
        b.putInt("no", pos);
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

        int accentColor = mainActivity.getColorPreference().getColor(ColorUsage.ACCENT);

        if (index == 0) {
            circleDrawable1.setImageDrawable(new ColorCircleDrawable(accentColor));
            circleDrawable2.setImageDrawable(new ColorCircleDrawable(Color.GRAY));
        } else {
            circleDrawable1.setImageDrawable(new ColorCircleDrawable(accentColor));
            circleDrawable2.setImageDrawable(new ColorCircleDrawable(Color.GRAY));
        }
    }

}
