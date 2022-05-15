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

package com.amaze.filemanager.ui.activities

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.palette.graphics.Palette
import com.amaze.filemanager.LogHelper
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_COLORED_NAVIGATION
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.Billing
import com.amaze.filemanager.utils.PreferenceUtils.getStatusColor
import com.amaze.filemanager.utils.Utils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlin.math.abs

/**
 * Created by vishal on 27/7/16.
 * Edited by hojat72elect on 2022-05-15.
 */
class AboutActivity : ThemedActivity(), View.OnClickListener {
    private val TAG = "AboutActivity"
    private val HEADER_HEIGHT = 1024
    private val HEADER_WIDTH = 500
    private val URL_AUTHOR1_GITHUB = "https://github.com/arpitkh96"
    private val URL_AUTHOR2_GITHUB = "https://github.com/VishalNehra"
    private val URL_DEVELOPER1_GITHUB = "https://github.com/EmmanuelMess"
    private val URL_DEVELOPER2_GITHUB = "https://github.com/TranceLove"
    private val URL_REPO_CHANGELOG =
        "https://github.com/TeamAmaze/AmazeFileManager/commits/master"
    private val URL_REPO = "https://github.com/TeamAmaze/AmazeFileManager"
    private val URL_REPO_ISSUES = "https://github.com/TeamAmaze/AmazeFileManager/issues"
    private val URL_REPO_TRANSLATE = "https://www.transifex.com/amaze/amaze-file-manager/"
    val URL_REPO_XDA =
        "http://forum.xda-developers.com/" +
            "android/apps-games/app-amaze-file-managermaterial-theme-t2937314"
    val URL_REPO_RATE = "market://details?id=com.amaze.filemanager"

    private lateinit var mAppBarLayout: AppBarLayout
    private lateinit var mCollapsingToolbarLayout: CollapsingToolbarLayout
    private lateinit var mTitleTextView: TextView
    private var mAuthorsDivider: View? = null
    private var mDeveloper1Divider: View? = null
    private var billing: Billing? = null

    @SuppressLint("UseCompatLoadingForDrawables", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            when (appTheme) {
                AppTheme.DARK -> {
                    setTheme(R.style.aboutDark)
                }
                AppTheme.BLACK -> {
                    setTheme(R.style.aboutBlack)
                }
                else -> {
                    setTheme(R.style.aboutLight)
                }
            }
        }
        setContentView(R.layout.activity_about)
        mAppBarLayout = findViewById(R.id.appBarLayout)
        mCollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout)
        mTitleTextView = findViewById(R.id.text_view_title)
        mAuthorsDivider = findViewById(R.id.view_divider_authors)
        mDeveloper1Divider = findViewById(R.id.view_divider_developers_1)
        mAppBarLayout.layoutParams = calculateHeaderViewParams()
        val mToolbar = findViewById<Toolbar>(R.id.toolBar)
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(resources.getDrawable(R.drawable.md_nav_back))
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        switchIcons()
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.about_header)

        // It will generate colors based on the image in an AsyncTask.
        Palette.from(bitmap)
            .generate { palette: Palette? ->
                val mutedColor = palette!!.getMutedColor(
                    Utils.getColor(
                        this@AboutActivity,
                        R.color.primary_blue
                    )
                )
                val darkMutedColor = palette.getDarkMutedColor(
                    Utils.getColor(this@AboutActivity, R.color.primary_blue)
                )
                mCollapsingToolbarLayout.setContentScrimColor(mutedColor)
                mCollapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = darkMutedColor
                }
            }
        mAppBarLayout.addOnOffsetChangedListener(
            OnOffsetChangedListener { appBarLayout: AppBarLayout, verticalOffset: Int ->
                mTitleTextView.alpha = abs(verticalOffset / appBarLayout.totalScrollRange.toFloat())
            }
        )
        mAppBarLayout.onFocusChangeListener =
            View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                mAppBarLayout.setExpanded(
                    hasFocus,
                    true
                )
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getBoolean(PREFERENCE_COLORED_NAVIGATION)) {
                window.navigationBarColor = getStatusColor(primary)
            } else {
                when (appTheme) {
                    AppTheme.LIGHT -> {
                        window.navigationBarColor = Utils.getColor(this, android.R.color.white)
                    }
                    AppTheme.BLACK -> {
                        window.navigationBarColor = Utils.getColor(this, android.R.color.black)
                    }
                    else -> {
                        window.navigationBarColor =
                            Utils.getColor(this, R.color.holo_dark_background)
                    }
                }
            }
        }
    }

    /**
     * Calculates aspect ratio for the Amaze header
     *
     * @return the layout params with new set of width and height attribute
     */
    private fun calculateHeaderViewParams(): CoordinatorLayout.LayoutParams {
        // calculating cardview height as per the youtube video thumb aspect ratio
        val layoutParams = mAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val vidAspectRatio = HEADER_WIDTH.toFloat() / HEADER_HEIGHT.toFloat()
        Log.d(TAG, vidAspectRatio.toString() + "")
        val screenWidth = resources.displayMetrics.widthPixels
        val reqHeightAsPerAspectRatio = screenWidth.toFloat() * vidAspectRatio
        Log.d(TAG, reqHeightAsPerAspectRatio.toString() + "")
        Log.d(TAG, "new width: $screenWidth and height: $reqHeightAsPerAspectRatio")
        layoutParams.width = screenWidth
        layoutParams.height = reqHeightAsPerAspectRatio.toInt()
        return layoutParams
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Method switches icon resources as per current theme
     */
    private fun switchIcons() {
        if (appTheme == AppTheme.DARK || appTheme == AppTheme.BLACK) {
            // dark theme
            mAuthorsDivider!!.setBackgroundColor(Utils.getColor(this, R.color.divider_dark_card))
            mDeveloper1Divider!!.setBackgroundColor(Utils.getColor(this, R.color.divider_dark_card))
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.relative_layout_source -> Utils.openURL(URL_REPO, this)
            R.id.relative_layout_issues -> Utils.openURL(URL_REPO_ISSUES, this)
            R.id.relative_layout_changelog -> Utils.openURL(URL_REPO_CHANGELOG, this)
            R.id.relative_layout_licenses -> {
                val libsBuilder = LibsBuilder()
                    .withLibraries("apachemina") // Not auto-detected for some reason
                    .withActivityTitle(getString(R.string.libraries))
                    .withAboutIconShown(true)
                    .withAboutVersionShownName(true)
                    .withAboutVersionShownCode(false)
                    .withAboutDescription(getString(R.string.about_amaze))
                    .withAboutSpecial1(getString(R.string.license))
                    .withAboutSpecial1Description(getString(R.string.amaze_license))
                    .withLicenseShown(true)
                when (appTheme.getSimpleTheme(this)) {
                    AppTheme.LIGHT -> libsBuilder.withActivityStyle(
                        Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
                    )
                    AppTheme.DARK -> libsBuilder.withActivityStyle(Libs.ActivityStyle.DARK)
                    AppTheme.BLACK -> libsBuilder.withActivityTheme(
                        R.style.AboutLibrariesTheme_Black
                    )
                    else -> LogHelper.logOnProductionOrCrash(TAG, "Incorrect value for switch")
                }
                libsBuilder.start(this)
            }
            R.id.text_view_author_1_github -> Utils.openURL(URL_AUTHOR1_GITHUB, this)
            R.id.text_view_author_2_github -> Utils.openURL(URL_AUTHOR2_GITHUB, this)
            R.id.text_view_developer_1_github -> Utils.openURL(URL_DEVELOPER1_GITHUB, this)
            R.id.text_view_developer_2_github -> Utils.openURL(URL_DEVELOPER2_GITHUB, this)
            R.id.relative_layout_translate -> Utils.openURL(URL_REPO_TRANSLATE, this)
            R.id.relative_layout_xda -> Utils.openURL(URL_REPO_XDA, this)
            R.id.relative_layout_rate -> Utils.openURL(URL_REPO_RATE, this)
            R.id.relative_layout_donate -> billing = Billing(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying the manager.")
        if (billing != null) {
            billing!!.destroyBillingInstance()
        }
    }

    companion object {
        const val PACKAGE_AMAZE_UTILS = "com.amaze.fileutilities"
        val URL_AMAZE_UTILS = "market://details?id=$PACKAGE_AMAZE_UTILS"
    }
}
