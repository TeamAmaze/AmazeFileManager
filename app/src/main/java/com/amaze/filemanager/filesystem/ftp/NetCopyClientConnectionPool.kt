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

package com.amaze.filemanager.filesystem.ftp

import android.annotation.SuppressLint
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.ftp.auth.FtpAuthenticationTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairObservable
import com.amaze.filemanager.asynchronous.asynctasks.ssh.SshAuthenticationTask
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.extractBaseUriFrom
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable.create
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.Config
import net.schmizz.sshj.SSHClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.KeyPair
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

object NetCopyClientConnectionPool {

    const val FTP_DEFAULT_PORT = 21
    const val FTPS_DEFAULT_PORT = 990
    const val SSH_DEFAULT_PORT = 22
    const val FTP_URI_PREFIX = "ftp://"
    const val FTPS_URI_PREFIX = "ftps://"
    const val SSH_URI_PREFIX = "ssh://"
    const val CONNECT_TIMEOUT = 30000

    private var connections: MutableMap<String, NetCopyClient<*>> = ConcurrentHashMap()

    @JvmStatic
    private val LOG: Logger = LoggerFactory.getLogger(NetCopyClientConnectionPool::class.java)

    @JvmField
    var sshClientFactory: SSHClientFactory = DefaultSSHClientFactory()

    @JvmField
    var ftpClientFactory: FTPClientFactory = DefaultFTPClientFactory()

    /**
     * Obtain a [NetCopyClient] connection from the underlying connection pool.
     *
     * Beneath it will return the connection if it exists; otherwise it will create a new one and
     * put it into the connection pool.
     *
     * @param url SSH connection URL, in the form of `
     * ssh://<username>:<password>@<host>:<port>` or `
     * ssh://<username>@<host>:<port>`
     * @return [NetCopyClient] connection, already opened and authenticated
     * @throws IOException IOExceptions that occur during connection setup
     */
    fun <ClientType> getConnection(url: String): NetCopyClient<ClientType>? {
        var client = connections[url]
        if (client == null) {
            client = createNetCopyClient.invoke(url)
            if (client != null) {
                connections[extractBaseUriFrom(url)] = client
            }
        } else {
            if (!validate(client)) {
                LOG.debug("Connection no longer usable. Reconnecting...")
                expire(client)
                connections.remove(url)
                client = createNetCopyClient.invoke(url)
                if (client != null) {
                    connections[extractBaseUriFrom(url)] = client
                }
            }
        }
        return if (client != null) {
            client as NetCopyClient<ClientType>?
        } else {
            null
        }
    }

    /**
     * Obtain a [NetCopyClient] connection from the underlying connection pool.
     *
     *
     * Beneath it will return the connection if it exists; otherwise it will create a new one and
     * put it into the connection pool.
     *
     *
     * Different from [.getConnection] above, this accepts broken down parameters as
     * convenience method during setting up SCP/SFTP connection.
     *
     * @param protocol server protocol, required
     * @param host host name/IP, required
     * @param port remote server port, required
     * @param hostFingerprint expected host fingerprint, required
     * @param username username, required
     * @param password password, required if using password to authenticate
     * @param keyPair [KeyPair], required if using key-based authentication
     * @return [NetCopyClient] connection
     */
    @Suppress("LongParameterList")
    fun getConnection(
        protocol: String,
        host: String,
        port: Int,
        hostFingerprint: String? = null,
        username: String,
        password: String? = null,
        keyPair: KeyPair? = null
    ): NetCopyClient<*>? {
        val url = NetCopyClientUtils.deriveUriFrom(
            protocol,
            host,
            port,
            "",
            username,
            password
        )
        var client = connections[url]
        if (client == null) {
            client = createNetCopyClientInternal(
                protocol,
                host,
                port,
                hostFingerprint,
                username,
                password,
                keyPair
            )
            if (client != null) connections[url] = client
        } else {
            if (!validate(client)) {
                LOG.debug("Connection no longer usable. Reconnecting...")
                expire(client)
                connections.remove(url)
                client = createNetCopyClient(url)
                if (client != null) connections[url] = client
            }
        }
        return client
    }

    private val createNetCopyClient: (String) -> NetCopyClient<*>? = { url ->
        if (url.startsWith(SSH_URI_PREFIX)) {
            createSshClient(url)
        } else {
            createFtpClient(url)
        }
    }

    private val createNetCopyClientInternal: (
        String,
        String,
        Int,
        String?,
        String,
        String?,
        KeyPair?
    ) -> NetCopyClient<*>? = { protocol, host, port, hostFingerprint, username, password, keyPair ->
        if (protocol == SSH_URI_PREFIX) {
            createSshClient(host, port, hostFingerprint!!, username, password, keyPair)
        } else {
            createFtpClient(
                protocol,
                host,
                port,
                hostFingerprint?.let { JSONObject(it) },
                username,
                password
            )
        }
    }

    /**
     * Remove specified connection from connection pool. Disconnects from server before removing.
     *
     * For updating SSH/FTP connection settings.
     *
     * This method will silently end without feedback if the specified connection URI does not
     * exist in the connection pool.
     *
     * @param url SSH connection URI
     */
    @SuppressLint("CheckResult")
    fun removeConnection(url: String, callback: () -> Unit) {
        Maybe.fromCallable(AsyncRemoveConnection(url))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { callback.invoke() }
    }

    /**
     * Kill any connection that is still in place. Used by MainActivity.
     *
     * @see MainActivity.onDestroy
     * @see MainActivity.exit
     */
    fun shutdown() {
        AppConfig.getInstance().runInBackground {
            if (connections.isNotEmpty()) {
                connections.values.forEach {
                    it.expire()
                }
                connections.clear()
            }
        }
    }

    private fun validate(client: NetCopyClient<*>): Boolean {
        return Single.fromCallable {
            client.isConnectionValid()
        }.subscribeOn(NetCopyClientUtils.getScheduler(client)).blockingGet()
    }

    private fun expire(client: NetCopyClient<*>) = Flowable.fromCallable {
        client.expire()
    }.subscribeOn(NetCopyClientUtils.getScheduler(client))

    // Logic for creating SSH connection. Depends on password existence in given Uri password or
    // key-based authentication
    @Suppress("TooGenericExceptionThrown")
    private fun createSshClient(url: String): NetCopyClient<SSHClient>? {
        val connInfo = NetCopyConnectionInfo(url)
        val utilsHandler = AppConfig.getInstance().utilsHandler
        val pem = utilsHandler.getSshAuthPrivateKey(url)
        val keyPair = AtomicReference<KeyPair?>(null)
        if (true == pem?.isNotEmpty()) {
            val observable = PemToKeyPairObservable(pem)
            keyPair.set(
                create(observable)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retryWhen { exceptions ->
                        exceptions.flatMap { exception ->
                            create<Any> { subscriber ->
                                observable.displayPassphraseDialog(exception, {
                                    subscriber.onNext(Unit)
                                }, {
                                    subscriber.onError(exception)
                                })
                            }
                        }
                    }
                    .blockingFirst()
            )
        }
        val hostKey = utilsHandler.getRemoteHostKey(url) ?: return null
        return createSshClientInternal(
            connInfo.host,
            connInfo.port,
            hostKey,
            connInfo.username,
            connInfo.password,
            keyPair.get()
        )
    }

    @Suppress("LongParameterList")
    private fun createSshClient(
        host: String,
        port: Int,
        hostKey: String,
        username: String,
        password: String?,
        keyPair: KeyPair?
    ): NetCopyClient<SSHClient>? {
        return createSshClientInternal(
            host,
            port,
            hostKey,
            username,
            password,
            keyPair
        )
    }

    @Suppress("LongParameterList")
    private fun createSshClientInternal(
        host: String,
        port: Int,
        hostKey: String,
        username: String,
        password: String?,
        keyPair: KeyPair?
    ): NetCopyClient<SSHClient>? {
        val task = SshAuthenticationTask(
            hostname = host,
            port = port,
            hostKey = hostKey,
            username = username,
            password = password,
            privateKey = keyPair
        )
        val latch = CountDownLatch(1)
        var retval: SSHClient? = null
        Maybe.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .subscribe({
                retval = it
                latch.countDown()
            }, {
                latch.countDown()
                task.onError(it)
            })
        latch.await()
        return retval?.let {
            SSHClientImpl(it)
        }
    }

    private fun createFtpClient(url: String): NetCopyClient<FTPClient>? {
        NetCopyConnectionInfo(url).run {
            val certInfo = if (FTPS_URI_PREFIX == prefix) {
                AppConfig.getInstance().utilsHandler.getRemoteHostKey(url)
            } else {
                null
            }
            return createFtpClient(
                prefix,
                host,
                port,
                certInfo?.let { JSONObject(it) },
                username,
                password
            )
        }
    }

    @Suppress("LongParameterList")
    private fun createFtpClient(
        protocol: String,
        host: String,
        port: Int,
        certInfo: JSONObject?,
        username: String,
        password: String?
    ): NetCopyClient<FTPClient>? {
        val task = FtpAuthenticationTask(
            protocol,
            host,
            port,
            certInfo,
            username,
            password
        )
        val latch = CountDownLatch(1)
        var result: FTPClient? = null
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .subscribe({
                result = it
                latch.countDown()
            }, {
                latch.countDown()
                task.onError(it)
            })
        latch.await()
        return result?.let { ftpClient ->
            FTPClientImpl(ftpClient)
        }
    }

    class AsyncRemoveConnection internal constructor(
        private val url: String
    ) : Callable<Unit> {

        override fun call() {
            extractBaseUriFrom(url).run {
                if (connections.containsKey(this)) {
                    connections[this]?.expire()
                    connections.remove(this)
                }
            }
        }
    }

    /**
     * Interface defining a factory class for creating [SSHClient] instances.
     *
     * In normal usage you won't need this; will be useful however when writing tests concerning
     * SSHClient, that mocked instances can be returned so tests can be run without a real SSH
     * server.
     */
    interface SSHClientFactory {
        /**
         * Implement this to return [SSHClient] instances.
         */
        fun create(config: Config): SSHClient
    }

    /**
     * Interface defining a factory class for creating [FTPClient] instances.
     *
     * In normal usage you won't need this; will be useful however when writing tests concerning
     * FTPClient, that mocked instances can be returned so tests can be run without a real FTP
     * server.
     */
    interface FTPClientFactory {
        /**
         * Implement this to return [FTPClient] instances.
         */
        fun create(uri: String): FTPClient
    }

    /** Default [SSHClientFactory] implementation.  */
    internal class DefaultSSHClientFactory : SSHClientFactory {
        override fun create(config: Config): SSHClient {
            return SSHClient(config)
        }
    }

    internal class DefaultFTPClientFactory : FTPClientFactory {
        override fun create(uri: String): FTPClient {
            return (
                if (uri.startsWith(FTPS_URI_PREFIX)) {
                    FTPSClient("TLS", true)
                } else {
                    FTPClient()
                }
                ).also {
                it.addProtocolCommandListener(Slf4jPrintCommandListener())
                it.connectTimeout = CONNECT_TIMEOUT
                it.controlEncoding = Charsets.UTF_8.name()
            }
        }
    }
}
