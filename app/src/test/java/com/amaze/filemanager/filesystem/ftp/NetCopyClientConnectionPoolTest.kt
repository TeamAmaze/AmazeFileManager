package com.amaze.filemanager.filesystem.ftp

import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.P
import android.util.LruCache
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.PasswordUtil
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.password.PasswordFinder
import org.apache.commons.net.ftp.FTPSClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for [NetCopyClientConnectionPool]
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [LOLLIPOP, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
)
@Suppress("StringLiteralDuplication")
class NetCopyClientConnectionPoolTest {
    /**
     * Setup before tests.
     */
    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    /**
     * Test that NetCopyClientConnectionPool closes connections on lifecycle onDestroy.
     */
    @Test
    fun `NetCopyClientConnectionPool should close connections on lifecycle onDestroy`() {
        // Mock LifecycleOwner
        val mockLifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
        val mockLifecycle = mockk<androidx.lifecycle.Lifecycle>(relaxed = true)

        // Mock connections
        val mockConnection = mockk<NetCopyClient<*>>(relaxed = true)
        val mockLruCache = mockk<LruCache<String, NetCopyClient<*>>>(relaxed = true)

        // Replace the connections cache with a mock
        val connectionsField = NetCopyClientConnectionPool::class.java.getDeclaredField("connections")
        connectionsField.isAccessible = true
        connectionsField.set(NetCopyClientConnectionPool, mockLruCache)

        // Simulate lifecycle events
        every { mockLifecycleOwner.lifecycle } returns mockLifecycle
        every { mockLifecycle.addObserver(any()) } answers {
            val observer = it.invocation.args[0] as DefaultLifecycleObserver
            observer.onDestroy(mockLifecycleOwner)
        }

        // Trigger onDestroy
        ProcessLifecycleOwner.get().lifecycle.addObserver(NetCopyClientConnectionPool)
        mockLifecycle.addObserver(NetCopyClientConnectionPool)
        verify { mockLruCache.evictAll() }
    }

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

    /**
     * Test getConnection should return same instance for same base URIs with different
     * path suffixes.
     */
    @Test
    fun `getConnection should return same instance for same base URIs with different paths`() {
        // Given
        val prefix = "ssh://root:${PasswordUtil.encryptPassword("root")}@127.0.0.1"
        val uri1 = "$prefix/root"
        val uri2 = prefix
        val uri3 = "$prefix/usr/src/linux"
        val hostKey = "TEST_HOST_KEY"

        // Mock AppConfig singleton
        val mockAppConfig = mockk<AppConfig>()
        mockkStatic(AppConfig::class)
        every { AppConfig.getInstance() } returns mockAppConfig

        // Mock dependencies
        val mockSshClient = mockk<SSHClient>()
        val utilsHandler = mockk<UtilsHandler>()

        every { AppConfig.getInstance().utilsHandler } returns utilsHandler
        every { utilsHandler.getRemoteHostKey(any()) } returns hostKey
        every { utilsHandler.getSshAuthPrivateKey(any()) } returns null

        // Mock SSHClient behavior
        justRun { mockSshClient.addHostKeyVerifier(any<String>()) }
        justRun { mockSshClient.connect(any<String>(), any()) }
        justRun { mockSshClient.authPassword(any<String>(), any<String>()) }
        justRun { mockSshClient.authPassword(any<String>(), any<CharArray>()) }
        justRun { mockSshClient.authPassword(any<String>(), any<PasswordFinder>()) }
        justRun { mockSshClient.connectTimeout = any<Int>() }
        every { mockSshClient.isConnected } returns true
        every { mockSshClient.isAuthenticated } returns true

        // Mock SSHClientFactory
        val mockFactory = mockk<NetCopyClientConnectionPool.SSHClientFactory>()
        every { mockFactory.create(any()) } returns mockSshClient
        NetCopyClientConnectionPool.sshClientFactory = mockFactory

        // When
        val client1 = NetCopyClientConnectionPool.getConnection<SSHClient>(uri1)
        val client2 = NetCopyClientConnectionPool.getConnection<SSHClient>(uri2)
        val client3 = NetCopyClientConnectionPool.getConnection<SSHClient>(uri3)

        // Then
        assertNotNull(client1)
        assertNotNull(client2)
        assertNotNull(client3)
        assertTrue(client1 === client2)
        assertTrue(client1 === client3)
    }

    /**
     * Test getConnection should return same instance for URIs with query parameters
     * in different order (canonicalization)
     */
    @Test
    fun `getConnection should return same instance for URIs with reordered query params`() {
        // Create URIs with same query params but different order
        val uri1 = "ftps://user:pass@127.0.0.1:2121?tls=explicit&timeout=30"
        val uri2 = "ftps://user:pass@127.0.0.1:2121?timeout=30&tls=explicit"

        // Extract base URIs - should be identical after canonicalization
        val baseUri1 = NetCopyClientUtils.extractBaseUriFrom(uri1)
        val baseUri2 = NetCopyClientUtils.extractBaseUriFrom(uri2)

        assertTrue(
            "URIs with same query params in different order should produce same base URI",
            baseUri1 == baseUri2,
        )
    }
}
