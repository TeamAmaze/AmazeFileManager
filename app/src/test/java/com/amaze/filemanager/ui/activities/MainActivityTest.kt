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

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.utils.smb.SmbUtil.getSmbDecryptedPath
import com.amaze.filemanager.utils.smb.SmbUtil.getSmbEncryptedPath
import org.awaitility.Awaitility.await
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * Test [MainActivity].
 */
@Suppress("StringLiteralDuplication")
class MainActivityTest : AbstractMainActivityTestBase() {

    /**
     * Test update SMB connection should never throw [NullPointerException] i.e. the correct
     * connection is updated.
     */
    @Test
    fun testUpdateSmbExceptionShouldNotThrowNPE() {
        val scenario = ActivityScenario.launch(
            MainActivity::class.java
        )
        ShadowLooper.idleMainLooper()
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.onActivity { activity: MainActivity ->
            val path = "smb://root:toor@192.168.1.1"
            val encryptedPath = getSmbEncryptedPath(
                ApplicationProvider.getApplicationContext(),
                path
            )
            val oldName = "SMB connection"
            val newName = "root@192.168.1.1"
            try {
                activity.addConnection(
                    false,
                    oldName,
                    encryptedPath,
                    null,
                    null
                )
                activity.addConnection(
                    true,
                    newName,
                    encryptedPath,
                    oldName,
                    encryptedPath
                )
                ShadowLooper.idleMainLooper()
                await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until { AppConfig.getInstance().utilsHandler.smbList.size > 0 }
                await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until {
                        (
                            AppConfig.getInstance()
                                .utilsHandler
                                .smbList[0][0]
                                == newName
                            )
                    }
                val verify: List<Array<String>> = AppConfig.getInstance().utilsHandler.smbList
                assertEquals(1, verify.size.toLong())
                val entry = verify[0]
                assertEquals(path, getSmbDecryptedPath(AppConfig.getInstance(), entry[1]))
            } finally {
                scenario.moveToState(Lifecycle.State.DESTROYED)
                scenario.close()
            }
        }
    }
}
