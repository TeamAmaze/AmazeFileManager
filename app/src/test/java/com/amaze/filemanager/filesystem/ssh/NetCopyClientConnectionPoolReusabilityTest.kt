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

package com.amaze.filemanager.filesystem.ssh

import android.os.Environment
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ftp.SSHClientImpl
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Test class for verifying the reusability of connections in NetCopyClientConnectionPool.
 */
@Suppress("StringLiteralDuplication")
class NetCopyClientConnectionPoolReusabilityTest : AbstractSftpServerTest() {
    @Before
    override fun setUp() {
        super.setUp()
        for (s in arrayOf("sysroot", "srv", "var", "tmp", "bin", "lib", "usr", "sysroot+v2")) {
            File(Environment.getExternalStorageDirectory(), s).mkdir()
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()
        // Clean up the directories created for the test
        for (s in arrayOf("sysroot", "srv", "var", "tmp", "bin", "lib", "usr", "sysroot+v2")) {
            File(Environment.getExternalStorageDirectory(), s).deleteRecursively()
        }
    }

    /**
     * Test case to verify that connections are reused correctly in the NetCopyClientConnectionPool.
     */
    @Test
    fun `test connection reusability`() {
        assertNotNull(
            NetCopyClientConnectionPool.getConnection<SSHClientImpl>(
                "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort",
            ),
        )
        // Test that connections are reused correctly
        assertTrue(
            "Connections should be reused",
            NetCopyClientConnectionPool.getConnection<SSHClientImpl>(
                "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort",
            )
                ===
                NetCopyClientConnectionPool.getConnection<SSHClientImpl>(
                    "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/sysroot",
                ),
        )

        // Use reflection to access the private 'connections' field
        val connectionsField = NetCopyClientConnectionPool::class.java.getDeclaredField("connections")
        connectionsField.isAccessible = true
        val lruCache = connectionsField.get(NetCopyClientConnectionPool) as android.util.LruCache<*, *>
        assertTrue("LruCache size should be 1, but was ${lruCache.size()}", lruCache.size() == 1)
    }

    /**
     * Test case to verify that connections are reused correctly in the NetCopyClientConnectionPool
     * after clearing the previous connections.
     */
    @Test
    fun `test connection reusability from scratch`() {
        NetCopyClientConnectionPool.shutdown()

        assertNotNull(
            NetCopyClientConnectionPool.getConnection<SSHClientImpl>(
                "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort",
            ),
        )
        // Test that connections are reused correctly
        assertTrue(
            "Connections should be reused",
            NetCopyClientConnectionPool.getConnection<SSHClientImpl>(
                "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort",
            )
                ===
                NetCopyClientConnectionPool.getConnection<SSHClientImpl>(
                    "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/sysroot",
                ),
        )

        // Use reflection to access the private 'connections' field
        val connectionsField = NetCopyClientConnectionPool::class.java.getDeclaredField("connections")
        connectionsField.isAccessible = true
        val lruCache = connectionsField.get(NetCopyClientConnectionPool) as android.util.LruCache<*, *>
        assertTrue("LruCache size should be 1, but was ${lruCache.size()}", lruCache.size() == 1)
    }
}
