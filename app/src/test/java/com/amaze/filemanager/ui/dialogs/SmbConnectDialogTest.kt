/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog.ARG_EDIT
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog.ARG_NAME
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog.ARG_PATH
import com.amaze.filemanager.ui.dialogs.SmbConnectDialog.SmbConnectionListener
import com.amaze.filemanager.utils.smb.SmbUtil
import io.mockk.confirmVerified
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/**
 * Tests [SmbConnectDialog].
 */
class SmbConnectDialogTest : AbstractMainActivityTestBase() {

    /**
     * Test call to [SmbConnectionListener.addConnection] is encrypted path.
     */
    @Test
    fun testCallingAddConnectionIsEncryptedPath() {
        val listener = spyk<SmbConnectionListener>()
        doTestWithDialog(
            listener = listener,
            arguments = Bundle().also {
                it.putString(ARG_NAME, "")
                it.putString(ARG_PATH, "")
                it.putBoolean(ARG_EDIT, false)
            },
            withDialog = { dialog, materialDialog ->
                val encryptedPath = SmbUtil.getSmbEncryptedPath(
                    AppConfig.getInstance(),
                    "smb://user:password@127.0.0.1/"
                )
                dialog.binding.run {
                    this.connectionET.setText("SMB Connection Test")
                    this.usernameET.setText("user")
                    this.passwordET.setText("password")
                    this.ipET.setText("127.0.0.1")
                }
                assertTrue(materialDialog.getActionButton(DialogAction.POSITIVE).performClick())
                verify {
                    listener.addConnection(
                        false,
                        "SMB Connection Test",
                        encryptedPath,
                        "",
                        ""
                    )
                }
                confirmVerified(listener)
            }
        )
    }

    private fun doTestWithDialog(
        arguments: Bundle,
        listener: SmbConnectionListener,
        withDialog: (SmbConnectDialog, MaterialDialog) -> Unit
    ) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity ->
            SmbConnectDialog().run {
                this.smbConnectionListener = listener
                this.arguments = arguments
                this.show(activity.supportFragmentManager, SmbConnectDialog.TAG)
                ShadowLooper.runUiThreadTasks()
                assertTrue(ShadowDialog.getLatestDialog().isShowing)
                withDialog.invoke(this, ShadowDialog.getLatestDialog() as MaterialDialog)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            scenario.close()
        }
    }
}
