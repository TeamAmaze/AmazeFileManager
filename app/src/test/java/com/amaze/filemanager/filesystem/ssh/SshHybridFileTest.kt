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
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.ssh.test.MockSshConnectionPools
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowCryptUtil
import com.amaze.filemanager.utils.OpenMode
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAsyncTask

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(shadows = [ShadowMultiDex::class, ShadowAsyncTask::class, ShadowCryptUtil::class])
class SshHybridFileTest {

    private var ctx: Context? = null

    private val path: String = "ssh://user:password@127.0.0.1:22222/test.file"

    companion object {
        @BeforeClass
        fun bootstrap() {
            RxJavaPlugins.reset()
            RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        }
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCanDelete() {
        MockSshConnectionPools.prepareCanDeleteScenario()
        assertTrue(HybridFile(OpenMode.SFTP, path).delete(ctx!!, false))
    }

    @Test
    fun testCannotDelete() {
        MockSshConnectionPools.prepareCannotDeleteScenario()
        assertFalse(HybridFile(OpenMode.SFTP, path).delete(ctx!!, false))
    }
}
