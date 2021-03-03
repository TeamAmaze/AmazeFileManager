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

package com.amaze.filemanager.asynchronous.asynctasks

import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.ssh.test.MockSshConnectionPools
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.xfer.FilePermission
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class SshDeleteTaskTest : AbstractDeleteTaskTestBase() {

    /**
     * Test case to verify delete SSH file success scenario.
     *
     * @see AbstractDeleteTaskTestBase.doTestDeleteFileOk
     */
    @Test
    fun testDeleteSshFileOk() {
        MockSshConnectionPools.prepareCanDeleteScenario()
        doTestDeleteFileOk(createSshHybridFileParcelable())
    }

    /**
     * Test case to verify delete SSH file failure scenario.
     *
     * @see AbstractDeleteTaskTestBase.doTestDeleteFileAccessDenied
     */
    @Test
    fun testDeleteSshFileAccessDenied() {
        MockSshConnectionPools.prepareCannotDeleteScenario()
        doTestDeleteFileAccessDenied(createSshHybridFileParcelable())
    }

    private fun createSshHybridFileParcelable(): HybridFileParcelable {
        val ri = mock(RemoteResourceInfo::class.java).apply {
            val fa = mock(FileAttributes::class.java).apply {
                `when`(mtime).thenReturn(System.currentTimeMillis() / 1000)
                `when`(size).thenReturn(16384)
                `when`(permissions).thenReturn(
                    setOf(
                        FilePermission.USR_RWX,
                        FilePermission.GRP_RWX,
                        FilePermission.OTH_RWX
                    )
                )
            }
            `when`(name).thenReturn("test.file")
            `when`(attributes).thenReturn(fa)
        }

        return HybridFileParcelable(
            "ssh://user:password@127.0.0.1:22222",
            false, ri
        )
    }
}
