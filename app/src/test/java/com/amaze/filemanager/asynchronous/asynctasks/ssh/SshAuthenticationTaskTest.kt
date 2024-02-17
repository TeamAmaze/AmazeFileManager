/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.ssh

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.PasswordUtil
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.net.SocketException
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
@Suppress("StringLiteralDuplication")
class SshAuthenticationTaskTest {

    /**
     * Test setup
     */
    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    /**
     * Test SSH authentication with username/password success scenario
     */
    @Test
    fun testAuthenticationUsernamePasswordSuccess() {
        val sshClient = mock(SSHClient::class.java).apply {
            doNothing().`when`(this).addHostKeyVerifier(anyString())
            doNothing().`when`(this).connect(anyString(), anyInt())
            doNothing().`when`(this).authPassword(anyString(), anyString())
            doNothing().`when`(this).disconnect()
            `when`(isConnected).thenReturn(true)
            `when`(isAuthenticated).thenReturn(true)
        }
        prepareSshConnectionPool(sshClient)
        val task = SshAuthenticationTask(
            hostKey = "",
            hostname = "127.0.0.1",
            port = 22222,
            username = "user",
            password = PasswordUtil.encryptPassword(AppConfig.getInstance(), "password")
        )
        val latch = CountDownLatch(1)
        var e: Throwable? = null
        var result: SSHClient? = null
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                task.onFinish(it)
                result = it
                latch.countDown()
            }, {
                it.printStackTrace()
                task.onError(it)
                e = it
                latch.countDown()
            })
        result?.run {
            assertTrue(isAuthenticated)
            assertTrue(isConnected)
        } ?: fail("Null SSHClient")
    }

    /**
     * Test SSH authentication with username/password fail scenario
     */
    @Test
    fun testAuthenticationUsernamePasswordFail() {
        val sshClient = mock(SSHClient::class.java).apply {
            doNothing().`when`(this).addHostKeyVerifier(anyString())
            doNothing().`when`(this).connect(anyString(), anyInt())
            doThrow(UserAuthException(DisconnectReason.NO_MORE_AUTH_METHODS_AVAILABLE))
                .`when`(this).authPassword(anyString(), anyString())
            doNothing().`when`(this).disconnect()
            `when`(isConnected).thenReturn(true)
            `when`(isAuthenticated).thenReturn(true)
        }
        prepareSshConnectionPool(sshClient)
        val task = SshAuthenticationTask(
            hostKey = "",
            hostname = "127.0.0.1",
            port = 22222,
            username = "user",
            password = "password"
        )
        val latch = CountDownLatch(1)
        var e: Throwable? = null
        var result: SSHClient? = null
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                task.onFinish(it)
                result = it
                latch.countDown()
            }, {
                task.onError(it)
                e = it
                latch.countDown()
            })
        latch.await()
        assertNull(result)
        assertNotNull(e)
        assertNotNull(ShadowToast.getLatestToast())
        assertEquals(
            ApplicationProvider
                .getApplicationContext<Context>()
                .getString(R.string.ssh_authentication_failure_password),
            ShadowToast.getTextOfLatestToast()
        )
    }

    /**
     * Test SSH authentication with username/keypair success scenario
     */
    @Test
    fun testAuthenticationKeyPairSuccess() {
        val keyProvider = TestKeyProvider()
        val sshClient = mock(SSHClient::class.java).apply {
            doNothing().`when`(this).addHostKeyVerifier(anyString())
            doNothing().`when`(this).connect(anyString(), anyInt())
            doNothing().`when`(this).authPublickey(anyString(), eq(keyProvider))
            doNothing().`when`(this).disconnect()
            `when`(isConnected).thenReturn(true)
            `when`(isAuthenticated).thenReturn(true)
        }
        prepareSshConnectionPool(sshClient)
        val task = SshAuthenticationTask(
            hostKey = "",
            hostname = "127.0.0.1",
            port = 22222,
            username = "user",
            privateKey = keyProvider.keyPair
        )
        val latch = CountDownLatch(1)
        var e: Throwable? = null
        var result: SSHClient? = null
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                task.onFinish(it)
                result = it
                latch.countDown()
            }, {
                task.onError(it)
                e = it
                latch.countDown()
            })
        result?.run {
            assertTrue(isAuthenticated)
            assertTrue(isConnected)
        } ?: fail("Null SSHClient")
    }

    /**
     * Test SSH authentication with username/keypair fail scenario
     */
    @Test
    fun testAuthenticationKeyPairFail() {
        val keyProvider = TestKeyProvider()
        val sshClient = mock(SSHClient::class.java).apply {
            doNothing().`when`(this).addHostKeyVerifier(anyString())
            doNothing().`when`(this).connect(anyString(), anyInt())
            doThrow(
                UserAuthException(DisconnectReason.KEY_EXCHANGE_FAILED)
            ).`when`(this).authPublickey(anyString(), any<KeyProvider>())
            doNothing().`when`(this).disconnect()
            `when`(isConnected).thenReturn(true)
            `when`(isAuthenticated).thenReturn(true)
        }
        prepareSshConnectionPool(sshClient)
        val task = SshAuthenticationTask(
            hostKey = "",
            hostname = "127.0.0.1",
            port = 22222,
            username = "user",
            privateKey = keyProvider.keyPair
        )
        val latch = CountDownLatch(1)
        var e: Throwable? = null
        var result: SSHClient? = null
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                task.onFinish(it)
                result = it
                latch.countDown()
            }, {
                task.onError(it)
                e = it
                latch.countDown()
            })
        latch.await()
        assertNull(result)
        assertNotNull(e)
        assertNotNull(ShadowToast.getLatestToast())
        assertEquals(
            ApplicationProvider
                .getApplicationContext<Context>()
                .getString(R.string.ssh_authentication_failure_key),
            ShadowToast.getTextOfLatestToast()
        )
    }

    /**
     * Test SSH connection timeout scenario
     */
    @Test
    fun testConnectionTimeout() {
        val sshClient = mock(SSHClient::class.java).apply {
            doNothing().`when`(this).addHostKeyVerifier(anyString())
            doThrow(
                SocketException("Connection timeout")
            ).`when`(this).connect(anyString(), anyInt())
            doNothing().`when`(this).authPassword(anyString(), anyString())
            doNothing().`when`(this).disconnect()
            `when`(isConnected).thenReturn(true)
            `when`(isAuthenticated).thenReturn(true)
        }
        prepareSshConnectionPool(sshClient)
        val task = SshAuthenticationTask(
            hostKey = "",
            hostname = "127.0.0.1",
            port = 22222,
            username = "user",
            password = PasswordUtil.encryptPassword(AppConfig.getInstance(), "password")
        )
        val latch = CountDownLatch(1)
        var e: Throwable? = null
        var result: SSHClient? = null
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                task.onFinish(it)
                result = it
                latch.countDown()
            }, {
                task.onError(it)
                e = it
                latch.countDown()
            })
        assertNull(result)
        assertNotNull(e)
        assertNotNull(ShadowToast.getLatestToast())
        assertEquals(
            ApplicationProvider
                .getApplicationContext<Context>()
                .getString(
                    R.string.ssh_connect_failed,
                    "127.0.0.1",
                    22222,
                    "Connection timeout"
                ),
            ShadowToast.getTextOfLatestToast()
        )
    }

    private fun prepareSshConnectionPool(sshClient: SSHClient) {
        /*
         * We don't need to go through authentication flow here, and in fact SshAuthenticationTask
         * was not working in the case of Operations.rename() due to the threading model
         * Robolectric imposed. So we are injecting the SSHClient here by force.
         */
        NetCopyClientConnectionPool::class.java.getDeclaredField("connections").run {
            this.isAccessible = true
            this.set(
                NetCopyClientConnectionPool,
                mutableMapOf(
                    Pair("ssh://user:password@127.0.0.1:22222", sshClient)
                )
            )
        }

        NetCopyClientConnectionPool.sshClientFactory = object :
            NetCopyClientConnectionPool.SSHClientFactory {
            override fun create(config: net.schmizz.sshj.Config): SSHClient = sshClient
        }
    }
}
