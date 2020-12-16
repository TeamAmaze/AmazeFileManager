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

package com.amaze.filemanager.filesystem.ssh

import android.content.Context
import android.os.Build.VERSION_CODES.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.ssh.test.MockSshConnectionPools
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowCryptUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
    shadows = [ShadowMultiDex::class, ShadowCryptUtil::class],
    sdk = [JELLY_BEAN, KITKAT, P]
)
class SshHybridFileTest {

    private var ctx: Context? = null

    private val path: String = "ssh://user:password@127.0.0.1:22222/test.file"

    /**
     * Test case setup.
     *
     * TODO: some even more generic test case base to prevent copy-and-paste?
     */
    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    /**
     * Test case to verify delete SSH file success scenario.
     */
    @Test
    fun testCanDelete() {
        MockSshConnectionPools.prepareCanDeleteScenario()
        assertTrue(HybridFile(OpenMode.SFTP, path).delete(ctx!!, false))
    }

    /**
     * Test case to verify delete SSH file failure scenario.
     */
    @Test
    fun testCannotDelete() {
        MockSshConnectionPools.prepareCannotDeleteScenario()
        assertFalse(HybridFile(OpenMode.SFTP, path).delete(ctx!!, false))
    }
}
