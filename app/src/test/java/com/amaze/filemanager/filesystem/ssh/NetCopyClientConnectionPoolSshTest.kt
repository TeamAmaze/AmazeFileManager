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

import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSHClientFactory
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.getConnection
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.shutdown
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.encryptFtpPathAsNecessary
import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.Utils
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers.not
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSQLiteConnection
import java.io.IOException
import java.security.KeyPair

/**
 * Tests for [NetCopyClientConnectionPool] with SSH connections.
 */
@Suppress("LargeClass", "StringLiteralDuplication")
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [JELLY_BEAN, KITKAT, P]
)
class NetCopyClientConnectionPoolSshTest {

    /**
     * Post test cleanup.
     */
    @After
    fun tearDown() {
        shutdown()
        ShadowSQLiteConnection.reset()
    }

    /**
     * Test getting connections with username and password.
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUsernameAndPassword() {
        val mock = createSshServer("testuser", "testpassword")
        assertNotNull(
            getConnection(
                SSH_URI_PREFIX,
                HOST,
                PORT,
                SecurityUtils.getFingerprint(hostKeyPair.public),
                "testuser",
                "testpassword",
                null
            )
        )
        assertNull(
            getConnection(
                SSH_URI_PREFIX,
                HOST,
                PORT,
                SecurityUtils.getFingerprint(hostKeyPair.public),
                "invaliduser",
                "invalidpassword",
                null
            )
        )
        verify(mock, times(2))
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, times(2)).connect(HOST, PORT)
        verify(mock).authPassword("testuser", "testpassword")
        verify(mock).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connections with username and key authentication.
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUsernameAndKey() {
        val mock = createSshServer("testuser", null)
        assertNotNull(
            getConnection(
                SSH_URI_PREFIX,
                HOST,
                PORT,
                SecurityUtils.getFingerprint(hostKeyPair.public),
                "testuser",
                null,
                userKeyPair
            )
        )
        shutdown()
        assertNull(
            getConnection(
                SSH_URI_PREFIX,
                HOST,
                PORT,
                SecurityUtils.getFingerprint(hostKeyPair.public),
                "invaliduser",
                null,
                userKeyPair
            )
        )
        verify(mock, times(2))
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, times(2)).connect(HOST, PORT)
        verify(mock).authPublickey("testuser", sshKeyProvider)
        verify(mock).authPublickey("invaliduser", sshKeyProvider)
    }

    /**
     * Test getting connection with an URL/URI.
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrl() {
        val validPassword = "testpassword"
        val mock = createSshServer("testuser", validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testpassword@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword("testuser", "testpassword")
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL(URI) having path to directory. It should return the same
     * connection without path to directory.
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingSubpath() {
        val validPassword = "testpassword"
        val mock = createSshServer("testuser", validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            validPassword,
            null,
            "/home/testuser"
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testpassword@127.0.0.1:22222/home/testuser"
                )
            )
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testpassword@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222/home/testuser"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, Mockito.atLeastOnce())
            .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.public))
        verify(mock, Mockito.atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, Mockito.atLeastOnce()).connect("127.0.0.1", 22222)
        verify(mock).authPassword("testuser", "testpassword")
        // invalid username won't give host key. Should never called this
        verify(mock, Mockito.never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI using key authentication.
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlAndKeyAuth() {
        val mock = createSshServer("testuser", null)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            null,
            userKeyPair.private
        )
        assertNotNull(getConnection<SSHClient>("ssh://testuser@127.0.0.1:22222"))
        assertNull(getConnection<SSHClient>("ssh://invaliduser@127.0.0.1:22222"))
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPublickey("testuser", sshKeyProvider)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPublickey("invaliduser", sshKeyProvider)
    }

    /**
     * Test getting connection with URL(URI) having path to directory using key authentication.
     * It should return the same connection without path to directory.
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlAndKeyAuthHavingSubpath() {
        val mock = createSshServer("testuser", null)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            null,
            userKeyPair.private,
            "/home/testuser"
        )
        assertNotNull(getConnection<SSHClient>("ssh://testuser@127.0.0.1:22222/home/testuser"))
        assertNotNull(getConnection<SSHClient>("ssh://testuser@127.0.0.1:22222"))
        assertNull(getConnection<SSHClient>("ssh://invaliduser@127.0.0.1:22222/home/testuser"))
        assertNull(getConnection<SSHClient>("ssh://invaliduser@127.0.0.1:22222"))
        verify(mock, Mockito.atLeastOnce())
            .addHostKeyVerifier(SecurityUtils.getFingerprint(hostKeyPair.public))
        verify(mock, Mockito.atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, Mockito.atLeastOnce()).connect("127.0.0.1", 22222)
        verify(mock).authPublickey("testuser", sshKeyProvider)
        // invalid username won't give host key. Should never called this
        verify(mock, Mockito.never())
            .authPublickey("invaliduser", sshKeyProvider)
    }

    /**
     * Test getting connection with URL/URI having complex password (case 1)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexPassword1() {
        val validPassword = "testP@ssw0rd"
        val mock = createSshServer("testuser", validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testP@ssw0rd@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword("testuser", validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex password (case 2)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexPassword2() {
        val validPassword = "testP@##word"
        val mock = createSshServer("testuser", validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testP@##word@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword("testuser", validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 1)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexCredential1() {
        val validPassword = "testP@##word"
        val mock = createSshServer("testuser", validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testP@##word@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword("testuser", validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 2)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexCredential2() {
        val validPassword = "testP@##word"
        val mock = createSshServer("testuser", validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            "testuser",
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://testuser:testP@##word@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword("testuser", validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 3)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexCredential3() {
        val validUsername = "test@example.com"
        val validPassword = "testP@ssw0rd"
        val mock = createSshServer(validUsername, validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            validUsername,
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://test@example.com:testP@ssw0rd@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword(validUsername, validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 4)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexCredential4() {
        val validUsername = "test@example.com"
        val validPassword = "testP@ssw0##$"
        val mock = createSshServer(validUsername, validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            validUsername,
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://test@example.com:testP@ssw0##$@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword(validUsername, validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 1)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingMinusSignInPassword1() {
        val validUsername = "test@example.com"
        val validPassword = "abcd-efgh"
        val mock = createSshServer(validUsername, validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            validUsername,
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://test@example.com:abcd-efgh@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword(validUsername, validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 2)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingMinusSignInPassword2() {
        val validUsername = "test@example.com"
        val validPassword = "---------------"
        val mock = createSshServer(validUsername, validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            validUsername,
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://test@example.com:---------------@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword(validUsername, validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 3)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingMinusSignInPassword3() {
        val validUsername = "test@example.com"
        val validPassword = "--agdiuhdpost15"
        val mock = createSshServer(validUsername, validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            validUsername,
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://test@example.com:--agdiuhdpost15@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword(validUsername, validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 4)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingMinusSignInPassword4() {
        val validUsername = "test@example.com"
        val validPassword = "t-h-i-s-i-s-p-a-s-s-w-o-r-d-"
        val mock = createSshServer(validUsername, validPassword)
        TestUtils.saveSshConnectionSettings(
            hostKeyPair,
            validUsername,
            validPassword,
            null
        )
        assertNotNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://test@example.com:t-h-i-s-i-s-p-a-s-s-w-o-r-d-@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<SSHClient>(
                encryptFtpPathAsNecessary(
                    "ssh://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce())
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        verify(mock, atLeastOnce()).connectTimeout =
            NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).authPassword(validUsername, validPassword)
        // invalid username won't give host key. Should never called this
        verify(mock, never()).authPassword("invaliduser", "invalidpassword")
    }

    @Throws(IOException::class)
    private fun createSshServer(validUsername: String, validPassword: String?): SSHClient {
        val mock = Mockito.mock(SSHClient::class.java)
        doNothing().`when`(mock).connect(HOST, PORT)
        doNothing()
            .`when`(mock)
            .addHostKeyVerifier(
                SecurityUtils.getFingerprint(
                    hostKeyPair.public
                )
            )
        doNothing().`when`(mock).disconnect()
        if (!Utils.isNullOrEmpty(validPassword)) {
            doNothing().`when`(mock).authPassword(validUsername, validPassword)
            doThrow(UserAuthException("Invalid login/password"))
                .`when`(mock)
                .authPassword(
                    not(eq(validUsername)),
                    not(eq(validPassword))
                )
        } else {
            doNothing().`when`(mock).authPublickey(validUsername, sshKeyProvider)
            doThrow(UserAuthException("Invalid key"))
                .`when`(mock)
                .authPublickey(
                    not(eq(validUsername)),
                    eq(sshKeyProvider)
                )
        }
        `when`(mock.isConnected).thenReturn(true)
        `when`(mock.isAuthenticated).thenReturn(true)
        NetCopyClientConnectionPool.sshClientFactory =
            object : SSHClientFactory {
                override fun create(config: net.schmizz.sshj.Config): SSHClient {
                    return mock
                }
            }
        return mock
    }

    companion object {

        const val HOST = "127.0.0.1"
        const val PORT = 22222

        lateinit var hostKeyPair: KeyPair
        lateinit var userKeyPair: KeyPair
        lateinit var sshKeyProvider: KeyProvider

        /**
         * Bootstrap the unit test
         */
        @BeforeClass
        @JvmStatic
        fun bootstrap() {
            hostKeyPair = TestUtils.createKeyPair()
            userKeyPair = TestUtils.createKeyPair()
            sshKeyProvider = object : KeyProvider {

                override fun getPrivate() = userKeyPair.private

                override fun getPublic() = userKeyPair.public

                override fun getType() = KeyType.RSA

                override fun equals(other: Any?): Boolean {
                    return if (other !is KeyProvider) {
                        false
                    } else {
                        other.private == private && other.public == public
                    }
                }
            }
            RxJavaPlugins.reset()
            RxJavaPlugins.setIoSchedulerHandler {
                Schedulers.trampoline()
            }
            RxAndroidPlugins.reset()
            RxAndroidPlugins.setInitMainThreadSchedulerHandler {
                Schedulers.trampoline()
            }
        }
    }
}
