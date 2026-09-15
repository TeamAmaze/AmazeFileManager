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

package com.amaze.filemanager.ui.dialogs

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import org.junit.Assert.assertTrue
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/**
 * Base class for [SftpConnectDialog] UI level tests.
 */
abstract class AbstractSftpConnectDialogUiTests : AbstractMainActivityTestBase() {
    /**
     * Create and display [SftpConnectDialog] with Robolectric and AndroidX test.
     *
     * @param arguments [Bundle] of arguments
     * @param withDialog Lambda performing test
     */
    protected fun doTestWithDialog(
        arguments: Bundle,
        withDialog: (SftpConnectDialog, MaterialDialog) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            SftpConnectDialog().run {
                this.arguments = arguments
                this.show(activity.supportFragmentManager, SftpConnectDialog.TAG)
                ShadowLooper.runUiThreadTasks()
                assertTrue(ShadowDialog.getLatestDialog().isShowing)
                withDialog.invoke(this, ShadowDialog.getLatestDialog() as MaterialDialog)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }
}
