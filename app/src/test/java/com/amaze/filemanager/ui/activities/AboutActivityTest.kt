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

package com.amaze.filemanager.ui.activities

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * About screen support rows keep their descriptions inside the card.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.P],
    shadows = [ShadowMultiDex::class],
)
class AboutActivityTest {
    /**
     * 540 dpi and fontScale 1.3 wrap the support summaries instead of clipping them.
     */
    @Test
    @Config(qualifiers = "w320dp-port-540dpi", fontScale = 1.3f)
    fun supportSummariesStayInsideCardAtLargeFont() {
        applyReportedConfiguration()
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        try {
            ShadowLooper.idleMainLooper()
            scenario.onActivity { activity ->
                assertEquals(1.3f, activity.resources.configuration.fontScale, 0.001f)
                assertEquals(540, activity.resources.configuration.densityDpi)
                assertSupportSummaries(activity)
            }
        } finally {
            scenario.close()
        }
    }

    /**
     * Default font scale on a wide screen still shows each summary, inside a 72dp row.
     */
    @Test
    @Config(qualifiers = "w600dp-port", fontScale = 1.0f)
    fun supportSummariesStayInsideCardAtDefaultFontScale() {
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        try {
            ShadowLooper.idleMainLooper()
            scenario.onActivity { activity ->
                assertEquals(1.0f, activity.resources.configuration.fontScale, 0.001f)
                assertSupportSummaries(activity)
            }
        } finally {
            scenario.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun applyReportedConfiguration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(context.resources.configuration)
        configuration.fontScale = 1.3f
        configuration.densityDpi = 540
        val metrics = context.resources.displayMetrics
        metrics.densityDpi = 540
        metrics.density = 540f / DisplayMetrics.DENSITY_DEFAULT
        metrics.scaledDensity = metrics.density * configuration.fontScale
        context.resources.updateConfiguration(configuration, metrics)
    }

    private fun assertSupportSummaries(activity: AboutActivity) {
        val content = activity.findViewById<View>(android.R.id.content)
        val metrics = activity.resources.displayMetrics
        content.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        content.layout(0, 0, content.measuredWidth, content.measuredHeight)

        val rows =
            listOf(
                Triple(R.id.appCompatTextView1, R.id.relative_layout_source, R.string.source_summary),
                Triple(R.id.appCompatTextView2, R.id.relative_layout_issues, R.string.report_bugs_summary),
                Triple(
                    R.id.appCompatTextView3,
                    R.id.relative_layout_share_logs,
                    R.string.share_logs_summary,
                ),
                Triple(
                    R.id.appCompatTextView4,
                    R.id.relative_layout_translate,
                    R.string.translate_summary,
                ),
                Triple(R.id.appCompatTextView5, R.id.relative_layout_xda, R.string.xda_summary),
            )
        val minRowHeightPx = (72 * metrics.density + 0.5f).toInt()
        for ((summaryId, rowId, textId) in rows) {
            val summary = activity.findViewById<TextView>(summaryId)
            val row = activity.findViewById<View>(rowId)
            val parent = summary.parent as View
            val name = activity.resources.getResourceEntryName(summaryId)
            assertEquals(activity.getString(textId), summary.text.toString())
            assertTrue(name, summary.width > 0)
            assertTrue(
                "$name right=${summary.right} parentWidth=${parent.width}",
                summary.right <= parent.width,
            )
            val textLayout = summary.layout
            assertNotNull(textLayout)
            val layout = textLayout!!
            for (line in 0 until layout.lineCount) {
                assertEquals(0, layout.getEllipsisCount(line))
            }
            assertTrue(summary.height >= layout.height)
            assertTrue(row.bottom >= summary.bottom)
            assertTrue(row.height >= minRowHeightPx)
        }
    }
}
