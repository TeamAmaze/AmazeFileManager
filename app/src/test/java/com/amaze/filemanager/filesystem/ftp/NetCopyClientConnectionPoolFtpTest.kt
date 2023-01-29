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

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.getConnection
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.shutdown
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.encryptFtpPathAsNecessary
import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.apache.commons.net.ftp.FTPClient
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers.not
import org.mockito.Mockito
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.`when`
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSQLiteConnection
import java.io.IOException

/**
 * Unit tests for [NetCopyClientConnectionPool] with FTP connections.
 */
@Suppress("LargeClass", "StringLiteralDuplication")
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class NetCopyClientConnectionPoolFtpTest {

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
    fun testGetConnectionWithUsernameAndPassword() {
        val mock = createFTPClient("testuser", "testpassword")
        assertNotNull(
            getConnection(
                protocol = FTP_URI_PREFIX,
                host = HOST,
                port = PORT,
                username = "testuser",
                password = "testpassword"
            )
        )
        assertNull(
            getConnection(
                protocol = FTP_URI_PREFIX,
                host = HOST,
                port = PORT,
                username = "invaliduser",
                password = "invalidpassword"
            )
        )
        verify(mock, times(2)).connect(HOST, PORT)
        verify(mock).login("testuser", "testpassword")
        verify(mock).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with an URL/URI.
     */
    @Test
    fun testGetConnectionWithUrl() {
        val validPassword = "testpassword"
        val mock = createFTPClient("testuser", validPassword)
        TestUtils.saveFtpConnectionSettings("testuser", validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://testuser:testpassword@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login("testuser", "testpassword")
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex password (case 1)
     */
    @Test
    @Throws(IOException::class)
    fun testGetConnectionWithUrlHavingComplexPassword1() {
        val validPassword = "testP@ssw0rd"
        val mock = createFTPClient("testuser", validPassword)
        TestUtils.saveFtpConnectionSettings("testuser", validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://testuser:testP@ssw0rd@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login("testuser", validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex password (case 2)
     */
    @Test
    fun testGetConnectionWithUrlHavingComplexPassword2() {
        val validPassword = "testP@##word"
        val mock = createFTPClient("testuser", validPassword)
        TestUtils.saveFtpConnectionSettings("testuser", validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://testuser:testP@##word@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login("testuser", validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 1)
     */
    @Test
    fun testGetConnectionWithUrlHavingComplexCredential1() {
        val validPassword = "testP@##word"
        val mock = createFTPClient("testuser", validPassword)
        TestUtils.saveFtpConnectionSettings("testuser", validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://testuser:testP@##word@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login("testuser", validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 2)
     */
    @Test
    fun testGetConnectionWithUrlHavingComplexCredential2() {
        val validPassword = "testP@##word"
        val mock = createFTPClient("testuser", validPassword)
        TestUtils.saveFtpConnectionSettings("testuser", validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://testuser:testP@##word@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login("testuser", validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 3)
     */
    @Test
    fun testGetConnectionWithUrlHavingComplexCredential3() {
        val validUsername = "test@example.com"
        val validPassword = "testP@ssw0rd"
        val mock = createFTPClient(validUsername, validPassword)
        TestUtils.saveFtpConnectionSettings(validUsername, validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://test@example.com:testP@ssw0rd@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login(validUsername, validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having complex credentials (case 4)
     */
    @Test
    fun testGetConnectionWithUrlHavingComplexCredential4() {
        val validUsername = "test@example.com"
        val validPassword = "testP@ssw0##$"
        val mock = createFTPClient(validUsername, validPassword)
        TestUtils.saveFtpConnectionSettings(validUsername, validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://test@example.com:testP@ssw0##$@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login(validUsername, validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 1)
     */
    @Test
    fun testGetConnectionWithUrlHavingMinusSignInPassword1() {
        val validUsername = "test@example.com"
        val validPassword = "abcd-efgh"
        val mock = createFTPClient(validUsername, validPassword)
        TestUtils.saveFtpConnectionSettings(validUsername, validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://test@example.com:abcd-efgh@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login(validUsername, validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 2)
     */
    @Test
    fun testGetConnectionWithUrlHavingMinusSignInPassword2() {
        val validUsername = "test@example.com"
        val validPassword = "---------------"
        val mock = createFTPClient(validUsername, validPassword)
        TestUtils.saveFtpConnectionSettings(validUsername, validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://test@example.com:---------------@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login(validUsername, validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 3)
     */
    @Test
    fun testGetConnectionWithUrlHavingMinusSignInPassword3() {
        val validUsername = "test@example.com"
        val validPassword = "--agdiuhdpost15"
        val mock = createFTPClient(validUsername, validPassword)
        TestUtils.saveFtpConnectionSettings(validUsername, validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://test@example.com:--agdiuhdpost15@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login(validUsername, validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    /**
     * Test getting connection with URL/URI having minus sign in password (case 4)
     */
    @Test
    fun testGetConnectionWithUrlHavingMinusSignInPassword4() {
        val validUsername = "test@example.com"
        val validPassword = "t-h-i-s-i-s-p-a-s-s-w-o-r-d-"
        val mock = createFTPClient(validUsername, validPassword)
        TestUtils.saveFtpConnectionSettings(validUsername, validPassword)
        assertNotNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://test@example.com:t-h-i-s-i-s-p-a-s-s-w-o-r-d-@127.0.0.1:22222"
                )
            )
        )
        assertNull(
            getConnection<FTPClient>(
                encryptFtpPathAsNecessary(
                    "ftp://invaliduser:invalidpassword@127.0.0.1:22222"
                )
            )
        )
        verify(mock, atLeastOnce()).connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        verify(mock, atLeastOnce()).connect(HOST, PORT)
        verify(mock).login(validUsername, validPassword)
        verify(mock, atMostOnce()).login("invaliduser", "invalidpassword")
    }

    private fun createFTPClient(validUsername: String, validPassword: String): FTPClient {
        val mock = Mockito.mock(FTPClient::class.java)
        doNothing().`when`(mock).connect(HOST, PORT)
        doNothing().`when`(mock).disconnect()
        `when`(mock.login(validUsername, validPassword)).thenReturn(true)
        `when`(
            mock.login(
                not(eq(validUsername)),
                not(
                    eq(validPassword)
                )
            )
        ).thenReturn(false)
        // reset(mock);
        NetCopyClientConnectionPool.ftpClientFactory =
            object : NetCopyClientConnectionPool.FTPClientFactory {
                override fun create(uri: String): FTPClient = mock
            }
        return mock
    }

    companion object {

        const val HOST = "127.0.0.1"
        const val PORT = 22222

        /**
         * Bootstrap the unit test
         */
        @BeforeClass
        @JvmStatic
        fun bootstrap() {
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
