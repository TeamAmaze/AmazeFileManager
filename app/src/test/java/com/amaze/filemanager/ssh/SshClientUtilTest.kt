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

package com.amaze.filemanager.ssh

import com.amaze.filemanager.filesystem.ssh.SshClientUtils
import org.junit.Assert
import org.junit.Test

class SshClientUtilTest {
    /**
     * Test [SshClientUtils.extractRemotePathFrom].
     */
    @Test
    fun testExtractRemotePathFromUri() {
        Assert.assertEquals(
            "/home/user/foo/bar",
            SshClientUtils.extractRemotePathFrom(
                "ssh://user:password@127.0.0.1:22/home/user/foo/bar"
            )
        )
        Assert.assertEquals(
            "/",
            SshClientUtils.extractRemotePathFrom("ssh://user:password@127.0.0.1:22/")
        )
        Assert.assertEquals(
            "/",
            SshClientUtils.extractRemotePathFrom("ssh://user:password@127.0.0.1:22")
        )
        Assert.assertEquals(
            "/", SshClientUtils.extractRemotePathFrom("ssh://root:a8/875dbc-==@127.0.0.1:9899")
        )
        Assert.assertEquals(
            "/root/.config",
            SshClientUtils.extractRemotePathFrom(
                "ssh://root:a8/875dbc-==@127.0.0.1:9899/root/.config"
            )
        )
    }

    /**
     * Test [SshClientUtils.extractRemotePathFrom].
     */
    @Test
    fun testExtractBaseUriFromUri() {
        Assert.assertEquals(
            "ssh://root@127.0.0.1",
            SshClientUtils.extractBaseUriFrom("ssh://root@127.0.0.1")
        )
        Assert.assertEquals(
            "ssh://root@127.0.0.1:2233",
            SshClientUtils.extractBaseUriFrom("ssh://root@127.0.0.1:2233")
        )
        Assert.assertEquals(
            "ssh://root@127.0.0.1",
            SshClientUtils.extractBaseUriFrom("ssh://root@127.0.0.1/root/.config")
        )
        Assert.assertEquals(
            "ssh://root:password@127.0.0.1",
            SshClientUtils.extractBaseUriFrom("ssh://root:password@127.0.0.1")
        )
        Assert.assertEquals(
            "ssh://root:password@127.0.0.1:3456",
            SshClientUtils.extractBaseUriFrom("ssh://root:password@127.0.0.1:3456/root/.config")
        )
        Assert.assertEquals(
            "ssh://root:a8/875dbc-==@127.0.0.1:9899",
            SshClientUtils.extractBaseUriFrom("ssh://root:a8/875dbc-==@127.0.0.1:9899")
        )
        Assert.assertEquals(
            "ssh://root:a8/875dbc-==@127.0.0.1:9899",
            SshClientUtils.extractBaseUriFrom(
                "ssh://root:a8/875dbc-==@127.0.0.1:9899/root/.config"
            )
        )
    }
}
