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

package com.amaze.filemanager.filesystem.ftp

import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("StringLiteralDuplication")
class NetCopyClientUtilTest {

    /**
     * Test [NetCopyClientUtils.deriveUriFrom].
     */
    @Test
    fun testDeriveUriFrom() {
        assertEquals(
            "ssh://user:password@127.0.0.1:22222/",
            NetCopyClientUtils.deriveUriFrom(
                prefix = "ssh://",
                username = "user",
                password = "password",
                hostname = "127.0.0.1",
                port = 22222,
                defaultPath = null
            )
        )
        assertEquals(
            "ssh://user@127.0.0.1:22222/",
            NetCopyClientUtils.deriveUriFrom(
                prefix = "ssh://",
                username = "user",
                password = null,
                hostname = "127.0.0.1",
                port = 22222,
                defaultPath = null
            )
        )
    }

    /**
     * Test [NetCopyClientUtils.extractRemotePathFrom].
     */
    @Test
    fun testExtractRemotePathFromUri() {
        assertEquals(
            "/home/user/foo/bar",
            NetCopyClientUtils.extractRemotePathFrom(
                "ssh://user:password@127.0.0.1:22/home/user/foo/bar"
            )
        )
        assertEquals(
            "/",
            NetCopyClientUtils.extractRemotePathFrom("ssh://user:password@127.0.0.1:22/")
        )
        assertEquals(
            "/",
            NetCopyClientUtils.extractRemotePathFrom("ssh://user:password@127.0.0.1:22")
        )
        assertEquals(
            "/",
            NetCopyClientUtils.extractRemotePathFrom("ssh://root:a8/875dbc-==@127.0.0.1:9899")
        )
        assertEquals(
            "/root/.config",
            NetCopyClientUtils.extractRemotePathFrom(
                "ssh://root:YTgvODc1ZGJjLT09@127.0.0.1:9899/root/.config"
            )
        )
        assertEquals(
            "/Incoming/shared",
            NetCopyClientUtils.extractRemotePathFrom("ftp://127.0.0.1:2211/Incoming/shared")
        )
        assertEquals(
            "/pub/notice.txt",
            NetCopyClientUtils.extractRemotePathFrom("ftp://127.0.0.1:2211/pub/notice.txt")
        )
    }

    /**
     * Test [NetCopyClientUtils.extractRemotePathFrom].
     */
    @Test
    fun testExtractBaseUriFromUri() {
        assertEquals(
            "ssh://root@127.0.0.1",
            NetCopyClientUtils.extractBaseUriFrom("ssh://root@127.0.0.1")
        )
        assertEquals(
            "ssh://root@127.0.0.1:2233",
            NetCopyClientUtils.extractBaseUriFrom("ssh://root@127.0.0.1:2233")
        )
        assertEquals(
            "ssh://root@127.0.0.1",
            NetCopyClientUtils.extractBaseUriFrom("ssh://root@127.0.0.1/root/.config")
        )
        assertEquals(
            "ssh://root:password@127.0.0.1",
            NetCopyClientUtils.extractBaseUriFrom("ssh://root:password@127.0.0.1")
        )
        assertEquals(
            "ssh://root:password@127.0.0.1:3456",
            NetCopyClientUtils.extractBaseUriFrom("ssh://root:password@127.0.0.1:3456/root/.config")
        )
        assertEquals(
            "ssh://root:a8/875dbc-==@127.0.0.1:9899",
            NetCopyClientUtils.extractBaseUriFrom("ssh://root:a8/875dbc-==@127.0.0.1:9899")
        )
        assertEquals(
            "ssh://root:a8/875dbc-==@127.0.0.1:9899",
            NetCopyClientUtils.extractBaseUriFrom(
                "ssh://root:a8/875dbc-==@127.0.0.1:9899/root/.config"
            )
        )
        assertEquals(
            "ftp://127.0.0.1:2211",
            NetCopyClientUtils.extractBaseUriFrom("ftp://127.0.0.1:2211")
        )
        assertEquals(
            "ftp://127.0.0.1:2211",
            NetCopyClientUtils.extractBaseUriFrom("ftp://127.0.0.1:2211/Incoming/shared")
        )
        assertEquals(
            "ftp://127.0.0.1:2211",
            NetCopyClientUtils.extractBaseUriFrom("ftp://127.0.0.1:2211/pub/notice.txt")
        )
    }
}
