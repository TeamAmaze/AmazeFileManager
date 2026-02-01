/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ftpserver.service

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import com.amaze.filemanager.ftpserver.commands.AVBL
import com.amaze.filemanager.ftpserver.filesystem.AndroidFileSystemFactory
import com.amaze.filemanager.ftpserver.filesystem.RootFileSystemFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.ssl.ClientAuth
import org.apache.ftpserver.ssl.impl.DefaultSslConfiguration
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

/**
 * FTP Server engine that handles the actual server lifecycle.
 * This is the core server logic extracted from FtpService.
 */
object FtpServerEngine {
    private val log: Logger = LoggerFactory.getLogger(FtpServerEngine::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var server: FtpServer? = null
    private var serverThread: Thread? = null

    /**
     * Configuration for the FTP server
     */
    data class ServerConfig(
        val port: Int = FtpPreferences.DEFAULT_PORT,
        val timeout: Int = FtpPreferences.DEFAULT_TIMEOUT,
        val path: String,
        val username: String? = null,
        val password: String? = null,
        val isSecure: Boolean = FtpPreferences.DEFAULT_SECURE,
        val isReadOnly: Boolean = false,
        val useSafFilesystem: Boolean = false,
        val useRootFilesystem: Boolean = false,
        val keyStoreInputStream: InputStream? = null,
        val keyStorePassword: String = "",
        val errorMessageProvider: AVBL.ErrorMessageProvider? = null,
        val featResponseProvider: (() -> String)? = null
    )

    /**
     * Check if the server is currently running
     */
    fun isRunning(): Boolean {
        val server = server ?: return false
        return !server.isStopped
    }

    /**
     * Start the FTP server with the given configuration
     */
    fun start(
        context: Context,
        config: ServerConfig,
        onStarted: (Boolean) -> Unit = {}
    ) {
        if (isRunning()) {
            log.warn("FTP server already running")
            onStarted(true)
            return
        }

        serverThread = Thread {
            runServer(context, config, onStarted)
        }.apply { start() }
    }

    private fun runServer(
        context: Context,
        config: ServerConfig,
        onStarted: (Boolean) -> Unit
    ) {
        try {
            FtpServerFactory().run {
                val connectionConfigFactory = ConnectionConfigFactory()

                // Configure filesystem
                if (SDK_INT >= KITKAT && config.useSafFilesystem) {
                    fileSystem = AndroidFileSystemFactory(context) { config.path }
                } else if (config.useRootFilesystem) {
                    fileSystem = RootFileSystemFactory()
                } else {
                    fileSystem = NativeFileSystemFactory()
                }

                // Configure commands
                if (config.errorMessageProvider != null && config.featResponseProvider != null) {
                    commandFactory = FtpCommandFactoryFactory.create(
                        config.useSafFilesystem,
                        config.errorMessageProvider,
                        config.featResponseProvider
                    )
                }

                // Configure user
                val user = BaseUser()
                if (config.username.isNullOrEmpty()) {
                    user.name = "anonymous"
                    connectionConfigFactory.isAnonymousLoginEnabled = true
                } else {
                    user.name = config.username
                    user.password = config.password
                }
                user.homeDirectory = config.path

                if (!config.isReadOnly) {
                    user.authorities = listOf(WritePermission())
                }

                connectionConfig = connectionConfigFactory.createConnectionConfig()
                userManager.save(user)

                // Configure listener
                val listenerFactory = ListenerFactory()

                if (config.isSecure && config.keyStoreInputStream != null) {
                    try {
                        val keyStore = KeyStore.getInstance("BKS")
                        val keyStorePassword = config.keyStorePassword.toCharArray()
                        keyStore.load(config.keyStoreInputStream, keyStorePassword)

                        val keyManagerFactory = KeyManagerFactory
                            .getInstance(KeyManagerFactory.getDefaultAlgorithm())
                        keyManagerFactory.init(keyStore, keyStorePassword)

                        val trustManagerFactory = TrustManagerFactory
                            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                        trustManagerFactory.init(keyStore)

                        listenerFactory.sslConfiguration = DefaultSslConfiguration(
                            keyManagerFactory,
                            trustManagerFactory,
                            ClientAuth.WANT,
                            "TLS",
                            FtpCipherSuites.enabledCipherSuites,
                            "ftpserver"
                        )
                        listenerFactory.isImplicitSsl = true
                    } catch (e: GeneralSecurityException) {
                        log.error("Failed to configure SSL", e)
                    } catch (e: IOException) {
                        log.error("Failed to load keystore", e)
                    }
                }

                listenerFactory.port = config.port
                listenerFactory.idleTimeout = config.timeout

                addListener("default", listenerFactory.createListener())

                server = createServer().apply {
                    start()
                    scope.launch {
                        FtpEventBus.emit(FtpServerEvent.Started)
                    }
                    onStarted(true)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to start FTP server", e)
            scope.launch {
                FtpEventBus.emit(FtpServerEvent.FailedToStart)
            }
            onStarted(false)
        }
    }

    /**
     * Stop the FTP server
     */
    fun stop() {
        serverThread?.let { thread ->
            thread.interrupt()
            thread.join(10000)

            if (!thread.isAlive) {
                serverThread = null
            }

            server?.stop()
            server = null

            scope.launch {
                FtpEventBus.emit(FtpServerEvent.Stopped)
            }
        }
    }
}
