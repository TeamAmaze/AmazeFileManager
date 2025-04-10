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
