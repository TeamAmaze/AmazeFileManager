/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import androidx.test.core.app.ActivityScenario
import androidx.viewpager2.widget.ViewPager2
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for [TabFragment].
 *
 * JVM generally runs faster than Dalvik on devices, hence tests here are not very meaningful.
 * Created here just for mimicking the Espresso tests.
 */
class TabFragmentRobolectricTest : AbstractMainActivityTestBase() {
    private lateinit var scenario: ActivityScenario<MainActivity>

    /**
     * Launches the [MainActivity] before each test.
     */
    @Before
    override fun setUp() {
        super.setUp()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /**
     * Post test cleanup.
     */
    @After
    override fun tearDown() {
        super.tearDown()
        scenario.close()
    }

    /**
     * Check if the fragment state is saved correctly during a configuration change
     * by rotate the screen while swiping between the tabs.
     */
    @Test
    fun testFragmentStateSavingDuringConfigChange() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.pager)
            // Switch tab
            viewPager.currentItem = 1

            // Trigger configuration change as if the screen was rotated
            activity.recreate()
        }
    }

    /**
     * Check if the fragment state is saved correctly during rapid tab swiping.
     */
    @Test
    fun testRapidTabSwitchingAndStateSaving() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.pager)

            // switch between tabs back and forth
            repeat(10) {
                viewPager.currentItem = 1
                viewPager.currentItem = 0
            }

            // Trigger configuration save
            activity.recreate()
        }
    }

    /**
     * Check if the fragment state is saved correctly when the fragment is detached.
     */
    @Test
    fun testFragmentDetachmentAndStateSaving() {
        scenario.onActivity { activity ->
            val tabFragment =
                activity.supportFragmentManager
                    .findFragmentById(R.id.content_frame) as TabFragment

            // detach fragment from FragmentManager
            activity.supportFragmentManager.beginTransaction().apply {
                tabFragment.fragments.firstOrNull()?.let { detach(it) }
                commit()
            }

            // Trigger configuration save
            activity.recreate()
        }
    }
}
