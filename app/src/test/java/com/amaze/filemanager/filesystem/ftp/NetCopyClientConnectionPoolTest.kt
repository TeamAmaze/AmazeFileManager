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

package com.amaze.filemanager.filesystem.ftp

import org.apache.commons.net.ftp.FTPSClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NetCopyClientConnectionPool]
 */
@Suppress("StringLiteralDuplication")
class NetCopyClientConnectionPoolTest {
    /**
     * Test DefaultFTPClientFactory default behaviour with FTPClient
     */
    @Test
    fun `DefaultFTPClientFactory default behaviour with FTPClient`() {
        val factory = NetCopyClientConnectionPool.DefaultFTPClientFactory()
        val result = factory.create("ftp://127.0.0.1:2121")
        assertFalse(result is FTPSClient)
    }

    /**
     * Test DefaultFTPClientFactory default behaviour with FTPSClient
     */
    @Test
    fun `DefaultFTPClientFactory default behaviour with FTPSClient`() {
        val factory = NetCopyClientConnectionPool.DefaultFTPClientFactory()
        val result = factory.create("ftps://127.0.0.1:2121")
        assertTrue(result is FTPSClient)
        val isImplicit = FTPSClient::class.java.getDeclaredField("isImplicit")
        isImplicit.isAccessible = true
        assertTrue(isImplicit.get(result) as Boolean)
    }

    /**
     * Test DefaultFTPClientFactory with URI having tls != explicit
     */
    @Test
    fun `DefaultFTPClientFactory with URI having tls != explicit`() {
        val factory = NetCopyClientConnectionPool.DefaultFTPClientFactory()
        var result = factory.create("ftps://127.0.0.1:2121?tls=implicit")
        assertTrue(result is FTPSClient)
        val isImplicit = FTPSClient::class.java.getDeclaredField("isImplicit")
        isImplicit.isAccessible = true
        assertTrue(isImplicit.get(result) as Boolean)
        result = factory.create("ftps://127.0.0.1:2121?explicitTls=true")
        assertTrue(result is FTPSClient)
        assertTrue(isImplicit.get(result) as Boolean)
    }

    /**
     * Test DefaultFTPClientDirectory with URI having tls=explicit
     */
    @Test
    fun `DefaultFTPClientDirectory with URI having tls=explicit`() {
        val factory = NetCopyClientConnectionPool.DefaultFTPClientFactory()
        val result = factory.create("ftps://127.0.0.1:2121?tls=explicit")
        assertTrue(result is FTPSClient)
        val isImplicit = FTPSClient::class.java.getDeclaredField("isImplicit")
        isImplicit.isAccessible = true
        assertFalse(isImplicit.get(result) as Boolean)
    }
}
