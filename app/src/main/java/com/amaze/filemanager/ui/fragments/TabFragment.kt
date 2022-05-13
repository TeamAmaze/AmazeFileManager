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
package com.amaze.filemanager.ui.fragments

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.amaze.filemanager.R
import com.amaze.filemanager.database.TabHandler
import com.amaze.filemanager.database.models.explorer.Tab
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.ui.ColorCircleDrawable
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.drag.TabFragmentSideDragListener
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CURRENT_TAB
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SAVED_PATHS
import com.amaze.filemanager.ui.views.DisablableViewPager
import com.amaze.filemanager.ui.views.Indicator
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.MainActivityHelper
import com.amaze.filemanager.utils.PreferenceUtils.DEFAULT_CURRENT_TAB
import com.amaze.filemanager.utils.PreferenceUtils.DEFAULT_SAVED_PATHS
import com.amaze.filemanager.utils.Utils

class TabFragment : Fragment(), OnPageChangeListener {
    private var savePaths = false
    private var tabbedFragmentManager: FragmentManager? = null
    private val fragments: MutableList<Fragment> = ArrayList()
    private var sectionsPagerAdapter: ScreenSlidePagerAdapter? = null
    private var viewPager: DisablableViewPager? = null
    private var sharedPrefs: SharedPreferences? = null
    private var path: String? = null

    /** ink indicators for viewpager only for Lollipop+  */
    private var indicator: Indicator? = null

    /** views for circlular drawables below android lollipop  */
    private var circleDrawable1: ImageView? = null
    private var circleDrawable2: ImageView? = null

    /** color drawable for action bar background  */
    private val colorDrawable = ColorDrawable()

    /** colors relative to current visible tab  */
    @ColorInt
    private var startColor = 0

    @ColorInt
    private var endColor = 0
    private var rootView: ViewGroup? = null
    private val evaluator = ArgbEvaluator()
    var dragPlaceholder: ConstraintLayout? = null
        private set

    @SuppressLint("UseRequireInsteadOfGet")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.tabfragment, container, false) as ViewGroup
        tabbedFragmentManager = requireActivity().supportFragmentManager
        dragPlaceholder = rootView!!.findViewById(R.id.drag_placeholder)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            indicator = requireActivity().findViewById(R.id.indicator)
        } else {
            circleDrawable1 = requireActivity().findViewById(R.id.tab_indicator1)
            circleDrawable2 = requireActivity().findViewById(R.id.tab_indicator2)
        }
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        savePaths = sharedPrefs!!.getBoolean(PREFERENCE_SAVED_PATHS, DEFAULT_SAVED_PATHS)
        viewPager = rootView!!.findViewById(R.id.pager)
        if (arguments != null) {
            path = arguments!!.getString(KEY_PATH)
        }
        requireMainActivity().supportInvalidateOptionsMenu()
        viewPager!!.addOnPageChangeListener(this)
        sectionsPagerAdapter = ScreenSlidePagerAdapter(tabbedFragmentManager)
        if (savedInstanceState == null) {
            val lastOpenTab = sharedPrefs!!.getInt(PREFERENCE_CURRENT_TAB, DEFAULT_CURRENT_TAB)
            MainActivity.currentTab = lastOpenTab
            refactorDrawerStorages(true)
            viewPager!!.setAdapter(sectionsPagerAdapter)
            try {
                viewPager!!.setCurrentItem(lastOpenTab, true)
                if (circleDrawable1 != null && circleDrawable2 != null) {
                    updateIndicator(viewPager!!.getCurrentItem())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            fragments.clear()
            try {
                fragments.add(
                    0,
                    tabbedFragmentManager!!.getFragment(savedInstanceState, KEY_FRAGMENT_0)!!
                )
                fragments.add(
                    1,
                    tabbedFragmentManager!!.getFragment(savedInstanceState, KEY_FRAGMENT_1)!!
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sectionsPagerAdapter = ScreenSlidePagerAdapter(tabbedFragmentManager)
            viewPager!!.setAdapter(sectionsPagerAdapter)
            val pos1 = savedInstanceState.getInt(KEY_POSITION, 0)
            MainActivity.currentTab = pos1
            viewPager!!.setCurrentItem(pos1)
            sectionsPagerAdapter!!.notifyDataSetChanged()
        }
        if (indicator != null) indicator!!.setViewPager(viewPager)
        val userColorPreferences = requireMainActivity().currentColorPreference

        // color of viewpager when current tab is 0
        startColor = userColorPreferences.primaryFirstTab
        // color of viewpager when current tab is 1
        endColor = userColorPreferences.primarySecondTab

        /*
     TODO
    //update the views as there is any change in {@link MainActivity#currentTab}
    //probably due to config change
    colorDrawable.setColor(Color.parseColor(MainActivity.currentTab==1 ?
            ThemedActivity.skinTwo : ThemedActivity.skin));
    mainActivity.updateViews(colorDrawable);
    */return rootView
    }

    override fun onDestroyView() {
        indicator = null // Free the strong reference
        sharedPrefs!!.edit().putInt(PREFERENCE_CURRENT_TAB, MainActivity.currentTab).apply()
        super.onDestroyView()
    }

    fun updatePaths(pos: Int) {
        // Getting old path from database before clearing
        val tabHandler = TabHandler.getInstance()
        var i = 1
        for (fragment in fragments) {
            if (fragment is MainFragment) {
                val mainFragment = fragment
                if (mainFragment.mainFragmentViewModel != null && i - 1 == MainActivity.currentTab && i == pos) {
                    updateBottomBar(mainFragment)
                    requireMainActivity()
                        .drawer
                        .selectCorrectDrawerItemForPath(mainFragment.currentPath)
                    if (mainFragment.mainFragmentViewModel!!.openMode === OpenMode.FILE) {
                        tabHandler.update(
                            Tab(
                                i,
                                mainFragment.currentPath,
                                mainFragment.mainFragmentViewModel!!.home
                            )
                        )
                    } else {
                        tabHandler.update(
                            Tab(
                                i,
                                mainFragment.mainFragmentViewModel!!.home,
                                mainFragment.mainFragmentViewModel!!.home
                            )
                        )
                    }
                }
                i++
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (sharedPrefs != null) {
            sharedPrefs!!.edit().putInt(PREFERENCE_CURRENT_TAB, MainActivity.currentTab).apply()
        }
        if (fragments.size != 0) {
            if (tabbedFragmentManager == null) {
                return
            }
            tabbedFragmentManager!!.executePendingTransactions()
            tabbedFragmentManager!!.putFragment(outState, KEY_FRAGMENT_0, fragments[0])
            tabbedFragmentManager!!.putFragment(outState, KEY_FRAGMENT_1, fragments[1])
            outState.putInt(KEY_POSITION, viewPager!!.currentItem)
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        val mainFragment = requireMainActivity().currentMainFragment
        if (mainFragment == null || mainFragment.mainFragmentViewModel == null || mainFragment.mainActivity!!.listItemSelected) {
            return  // we do not want to update toolbar colors when ActionMode is activated
        }

        // during the config change
        @ColorInt val color =
            evaluator.evaluate(position + positionOffset, startColor, endColor) as Int
        colorDrawable.color = color
        requireMainActivity().updateViews(colorDrawable)
    }

    override fun onPageSelected(p1: Int) {
        requireMainActivity()
            .appbar
            .appbarLayout
            .animate()
            .translationY(0f)
            .setInterpolator(DecelerateInterpolator(2F))
            .start()
        MainActivity.currentTab = p1
        if (sharedPrefs != null) {
            sharedPrefs!!.edit().putInt(PREFERENCE_CURRENT_TAB, MainActivity.currentTab).apply()
        }
        val fragment = fragments[p1]
        if (fragment is MainFragment) {
            val ma = fragment
            if (ma.currentPath != null) {
                requireMainActivity().drawer.selectCorrectDrawerItemForPath(ma.currentPath)
                updateBottomBar(ma)
            }
        }
        if (circleDrawable1 != null && circleDrawable2 != null) updateIndicator(p1)
    }

    override fun onPageScrollStateChanged(state: Int) {
        // nothing to do
    }

    fun setPagingEnabled(isPaging: Boolean) {
        viewPager!!.setPagingEnabled(isPaging)
    }

    fun setCurrentItem(index: Int) {
        viewPager!!.currentItem = index
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(
        fm!!
    ) {
        override fun getItemPosition(`object`: Any): Int {
            val index = fragments.indexOf(`object`)
            return if (index == -1) POSITION_NONE else index
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItem(position: Int): Fragment {
            val f: Fragment
            f = fragments[position]
            return f
        }
    }

    private fun addNewTab(num: Int, path: String) {
        addTab(Tab(num, path, path), "")
    }

    /**
     * Fetches new storage paths from drawer and apply to tabs This method will just create tabs in UI
     * change paths in database. Calls should implement updating each tab's list for new paths.
     *
     * @param addTab whether new tabs should be added to ui or just change values in database
     */
    fun refactorDrawerStorages(addTab: Boolean) {
        val tabHandler = TabHandler.getInstance()
        val tab1 = tabHandler.findTab(1)
        val tab2 = tabHandler.findTab(2)
        val tabs = tabHandler.allTabs
        val firstTabPath = requireMainActivity().drawer.firstPath
        val secondTabPath = requireMainActivity().drawer.secondPath
        if (tabs == null || tabs.size < 1 || tab1 == null || tab2 == null) {
            // creating tabs in db for the first time, probably the first launch of
            // app, or something got corrupted
            val currentFirstTab = if (Utils.isNullOrEmpty(firstTabPath)) "/" else firstTabPath
            val currentSecondTab =
                if (Utils.isNullOrEmpty(secondTabPath)) firstTabPath else secondTabPath
            if (addTab) {
                addNewTab(1, currentSecondTab)
                addNewTab(2, currentFirstTab)
            }
            tabHandler.addTab(Tab(1, currentSecondTab, currentSecondTab)).blockingAwait()
            tabHandler.addTab(Tab(2, currentFirstTab, currentFirstTab)).blockingAwait()
            if (currentFirstTab.equals("/", ignoreCase = true)) {
                sharedPrefs!!.edit().putBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, true)
                    .apply()
            }
        } else {
            if (path != null && path!!.length != 0) {
                if (MainActivity.currentTab == 0) {
                    addTab(tab1, path)
                    addTab(tab2, "")
                }
                if (MainActivity.currentTab == 1) {
                    addTab(tab1, "")
                    addTab(tab2, path)
                }
            } else {
                addTab(tab1, "")
                addTab(tab2, "")
            }
        }
    }

    private fun addTab(tab: Tab, path: String?) {
        val main = MainFragment()
        val b = Bundle()
        if (path != null && path.length != 0) {
            b.putString("lastpath", path)
            b.putInt("openmode", OpenMode.UNKNOWN.ordinal)
        } else {
            b.putString("lastpath", tab.getOriginalPath(savePaths, requireMainActivity().prefs))
        }
        b.putString("home", tab.home)
        b.putInt("no", tab.tabNumber)
        main.arguments = b
        fragments.add(main)
        sectionsPagerAdapter!!.notifyDataSetChanged()
        viewPager!!.offscreenPageLimit = 4
    }

    val currentTabFragment: Fragment?
        get() = if (fragments.size == 2) fragments[viewPager!!.currentItem] else null

    fun getFragmentAtIndex(pos: Int): Fragment? {
        return if (fragments.size == 2 && pos < 2) fragments[pos] else null
    }

    // updating indicator color as per the current viewpager tab
    fun updateIndicator(index: Int) {
        if (index != 0 && index != 1) return
        val accentColor = requireMainActivity().accent
        circleDrawable1!!.setImageDrawable(ColorCircleDrawable(accentColor))
        circleDrawable2!!.setImageDrawable(ColorCircleDrawable(Color.GRAY))
    }

    fun updateBottomBar(mainFragment: MainFragment?) {
        if (mainFragment == null || mainFragment.mainFragmentViewModel == null) {
            Log.w(TAG, "Failed to update bottom bar: main fragment not available")
            return
        }
        requireMainActivity()
            .appbar
            .bottomBar
            .updatePath(
                mainFragment.currentPath!!,
                mainFragment.mainFragmentViewModel!!.results,
                MainActivityHelper.SEARCH_TEXT,
                mainFragment.mainFragmentViewModel!!.openMode,
                mainFragment.mainFragmentViewModel!!.folderCount,
                mainFragment.mainFragmentViewModel!!.fileCount,
                mainFragment
            )
    }

    fun initLeftRightAndTopDragListeners(destroy: Boolean, shouldInvokeLeftAndRight: Boolean) {
        if (shouldInvokeLeftAndRight) {
            initLeftAndRightDragListeners(destroy)
        }
        for (fragment in fragments) {
            if (fragment is MainFragment) {
                fragment.initTopAndEmptyAreaDragListeners(destroy)
            }
        }
    }

    private fun initLeftAndRightDragListeners(destroy: Boolean) {
        val mainFragment = requireMainActivity().currentMainFragment
        val leftPlaceholder = rootView!!.findViewById<View>(R.id.placeholder_drag_left)
        val rightPlaceholder = rootView!!.findViewById<View>(R.id.placeholder_drag_right)
        val dataUtils = DataUtils.getInstance()
        if (destroy) {
            leftPlaceholder.setOnDragListener(null)
            rightPlaceholder.setOnDragListener(null)
            leftPlaceholder.visibility = View.GONE
            rightPlaceholder.visibility = View.GONE
        } else {
            leftPlaceholder.visibility = View.VISIBLE
            rightPlaceholder.visibility = View.VISIBLE
            leftPlaceholder.setOnDragListener(
                TabFragmentSideDragListener {
                    if (viewPager!!.currentItem == 1) {
                        if (mainFragment != null) {
                            dataUtils.checkedItemsList = mainFragment.adapter.checkedItems
                            requireMainActivity().actionModeHelper.disableActionMode()
                        }
                        viewPager!!.setCurrentItem(0, true)
                    }
                })
            rightPlaceholder.setOnDragListener(
                TabFragmentSideDragListener {
                    if (viewPager!!.currentItem == 0) {
                        if (mainFragment != null) {
                            dataUtils.checkedItemsList = mainFragment.adapter.checkedItems
                            requireMainActivity().actionModeHelper.disableActionMode()
                        }
                        viewPager!!.setCurrentItem(1, true)
                    }
                })
        }
    }

    private fun requireMainActivity(): MainActivity {
        return requireActivity() as MainActivity
    }

    companion object {
        private val TAG = TabFragment::class.java.simpleName
        private const val KEY_PATH = "path"
        private const val KEY_POSITION = "pos"
        private const val KEY_FRAGMENT_0 = "tab0"
        private const val KEY_FRAGMENT_1 = "tab1"
    }
}