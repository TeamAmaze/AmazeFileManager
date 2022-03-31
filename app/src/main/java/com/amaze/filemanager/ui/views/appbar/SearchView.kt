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
package com.amaze.filemanager.ui.views.appbar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.MainActivity
import xyz.quaver.floatingsearchview.FloatingSearchView
import xyz.quaver.floatingsearchview.FloatingSearchView.OnSearchListener
import xyz.quaver.floatingsearchview.suggestions.model.SearchSuggestion
import kotlin.math.max

/**
 * SearchView, a simple view to search
 */
class SearchView(
        private val appbar: AppBar,
        private val mainActivity: MainActivity,
        onSearch: (queue: String?) -> Unit
) {

    private val searchView: FloatingSearchView = mainActivity.findViewById(R.id.floating_search_view)

    var isEnabled = false
        private set

    val isShown: Boolean
        get() = searchView.isShown

    init {
        searchView.onSearchListener = object : OnSearchListener {
            override fun onSearchAction(currentQuery: String?) {
                onSearch(currentQuery)
                hideSearchView()
            }

            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion?) = Unit
        }
        searchView.onHomeActionClickListener = { hideSearchView() }
        searchView.onFocusChangeListener = object : FloatingSearchView.OnFocusChangeListener {
            override fun onFocus() = Unit

            override fun onFocusCleared() {
                hideSearchView()
            }
        }
    }

    /** show search view with a circular reveal animation  */
    fun revealSearchView() {
        val startRadius = 16
        val endRadius = Math.max(appbar.toolbar.width, appbar.toolbar.height)
        val animator: Animator
        animator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val searchCoords = IntArray(2)
            val searchItem = appbar
                    .toolbar
                    .findViewById<View>(R.id.search) // It could change position, get it every time
            searchView.clearQuery()
            searchItem.getLocationOnScreen(searchCoords)
            ViewAnimationUtils.createCircularReveal(
                    searchView,
                    searchCoords[0] + 32,
                    searchCoords[1] - 16,
                    startRadius.toFloat(),
                    endRadius.toFloat())
        } else {
            // TODO:ViewAnimationUtils.createCircularReveal
            ObjectAnimator.ofFloat(searchView, "alpha", 0f, 1f)
        }

        mainActivity.showSmokeScreen()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 600
        searchView.visibility = View.VISIBLE
        animator.start()
        animator.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        searchView.setSearchFocused(true)
                        isEnabled = true
                    }
                })
    }

    /** hide search view with a circular reveal animation  */
    fun hideSearchView() {
        val endRadius = 16
        val startRadius = max(searchView.width, searchView.height)
        val animator: Animator
        animator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val searchCoords = IntArray(2)
            val searchItem = appbar
                    .toolbar
                    .findViewById<View>(R.id.search) // It could change position, get it every time
            searchView.clearQuery()
            searchItem.getLocationOnScreen(searchCoords)
            ViewAnimationUtils.createCircularReveal(
                    searchView,
                    searchCoords[0] + 32,
                    searchCoords[1] - 16,
                    startRadius.toFloat(),
                    endRadius.toFloat())
        } else {
            // TODO: ViewAnimationUtils.createCircularReveal
            ObjectAnimator.ofFloat(searchView, "alpha", 1f, 0f)
        }

        // removing background fade view
        mainActivity.hideSmokeScreen()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 600
        animator.start()
        animator.addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        searchView.visibility = View.GONE
                        isEnabled = false
                    }
                })
    }
}