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

import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.getConnection
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.shutdown
import com.amaze.filemanager.filesystem.ssh.test.TestKeyProvider
import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.PasswordUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.apache.sshd.common.NamedFactory
import org.apache.sshd.common.config.keys.KeyUtils
import org.apache.sshd.common.file.FileSystemFactory
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.server.Command
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator
import org.apache.sshd.server.scp.ScpCommandFactory
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.BindException
import java.net.URLEncoder.encode
import java.nio.file.Paths
import kotlin.text.Charsets.UTF_8

/**
 * Base class for all SSH server related tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [KITKAT, P, VERSION_CODES.R]
)
abstract class AbstractSftpServerTest {

    protected var encryptedPassword: String? =
        PasswordUtil.encryptPassword(AppConfig.getInstance(), PASSWORD)?.replace("\n", "")
    protected var serverPort = 0
    private lateinit var server: SshServer

    /**
     * Setup SSH server with device directory as root.
     */
    @Before
    @Throws(IOException::class)
    open fun setUp() {
        serverPort = createSshServer(
            VirtualFileSystemFactory(
                Paths.get(Environment.getExternalStorageDirectory().absolutePath)
            ),
            64000
        )
        prepareSshConnection()
        TestUtils.saveSshConnectionSettings(
            hostKeyPair = hostKeyProvider.keyPair,
            validUsername = encode(USERNAME, UTF_8.name()),
            validPassword = encryptedPassword,
            privateKey = null,
            port = 64000
        )
    }

    /**
     * Shutdown SSH server after test.
     */
    @After
    @Throws(IOException::class)
    fun tearDown() {
        shutdown()
        if (server.isOpen) {
            server.stop(true)
        }
    }

    protected fun prepareSshConnection() {
        val hostFingerprint = KeyUtils.getFingerPrint(hostKeyProvider.keyPair.public)
        getConnection(
            SSH_URI_PREFIX,
            HOST,
            serverPort,
            hostFingerprint,
            USERNAME,
            PasswordUtil.encryptPassword(AppConfig.getInstance(), PASSWORD)?.replace("\n", ""),
            null
        )
    }

    @Throws(IOException::class)
    protected fun createSshServer(fileSystemFactory: FileSystemFactory, startPort: Int): Int {
        server = SshServer.setUpDefaultServer()
        server.fileSystemFactory = fileSystemFactory
        server.publickeyAuthenticator = AcceptAllPublickeyAuthenticator.INSTANCE
        server.host = HOST
        server.keyPairProvider = hostKeyProvider
        server.commandFactory = ScpCommandFactory()
        server.subsystemFactories = listOf<NamedFactory<Command>>(SftpSubsystemFactory())
        server.passwordAuthenticator =
            PasswordAuthenticator { username: String, password: String, _: ServerSession? ->
                username == USERNAME && password == PASSWORD
            }
        return try {
            server.port = startPort
            server.start()
            startPort
        } catch (ifPortIsUnavailable: BindException) {
            createSshServer(fileSystemFactory, startPort + 1)
        }
    }

    companion object {

        @JvmStatic
        protected val HOST = "127.0.0.1"

        @JvmStatic
        protected val USERNAME = "testuser"

        @JvmStatic
        protected val PASSWORD = "testpassword"

        protected lateinit var hostKeyProvider: TestKeyProvider

        /**
         * Prepare SSH host key provider and RxJava scheduler handlers.
         */
        @BeforeClass
        @JvmStatic
        fun bootstrap() {
            hostKeyProvider = TestKeyProvider()
            RxJavaPlugins.reset()
            RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
            RxAndroidPlugins.reset()
            RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        }
    }
}
