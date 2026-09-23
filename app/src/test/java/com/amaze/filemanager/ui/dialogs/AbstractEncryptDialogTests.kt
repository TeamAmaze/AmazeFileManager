/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.dialogs

import android.Manifest
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.P
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.amaze.filemanager.shadows.ShadowFileUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowTabHandler
import com.amaze.filemanager.test.TestUtils.initializeInternalStorage
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Base class for various tests related to file encryption.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [
        ShadowMultiDex::class,
        ShadowTabHandler::class,
        ShadowFileUtils::class,
    ],
    sdk = [LOLLIPOP, P, Build.VERSION_CODES.R],
)
abstract class AbstractEncryptDialogTests {
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Rule
    @JvmField
    @RequiresApi(Build.VERSION_CODES.R)
    var allFilesPermissionRule =
        GrantPermissionRule
            .grant(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

    // ActivityScenarioRule launches MainActivity before @Before runs, so initialize storage here.
    private val initializeStorageRule: ExternalResource =
        object : ExternalResource() {
            override fun before() {
                if (SDK_INT >= N) {
                    initializeInternalStorage()
                }
            }
        }

    @get:Rule
    val ruleChain: TestRule =
        RuleChain.outerRule(initializeStorageRule)
            .around(allFilesPermissionRule)
            .around(activityRule)
}
