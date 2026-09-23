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
import android.util.LruCache
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.ftp.auth.FtpAuthenticationTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairObservable
import com.amaze.filemanager.asynchronous.asynctasks.ssh.SshAuthenticationTask
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl.Companion.ARG_TLS
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl.Companion.TLS_EXPLICIT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.extractBaseUriFrom
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.QUESTION_MARK
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
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicReference

object NetCopyClientConnectionPool : DefaultLifecycleObserver {
    const val FTP_DEFAULT_PORT = 21
    const val FTPS_DEFAULT_PORT = 990
    const val SSH_DEFAULT_PORT = 22
    const val FTP_URI_PREFIX = "ftp://"
    const val FTPS_URI_PREFIX = "ftps://"
    const val SSH_URI_PREFIX = "ssh://"
    const val CONNECT_TIMEOUT = 30000

    private var connections: LruCache<String, NetCopyClient<*>> =
        object : LruCache<String, NetCopyClient<*>>(32) {
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: NetCopyClient<*>,
                newValue: NetCopyClient<*>?,
            ) {
                super.entryRemoved(evicted, key, oldValue, newValue)
                if (evicted) {
                    oldValue.expire()
                }
            }
        }

    @JvmStatic
    private val LOG: Logger = LoggerFactory.getLogger(NetCopyClientConnectionPool::class.java)

    @JvmField
    var sshClientFactory: SSHClientFactory = DefaultSSHClientFactory()

    @JvmField
    var ftpClientFactory: FTPClientFactory = DefaultFTPClientFactory()

    init {
        // Register this object as a lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // Called when app is destroyed
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        shutdown()
    }

    private fun closeAllConnections() {
        Single.create<Unit> { emitter ->
            if (connections.size() > 0) {
                connections.snapshot().values.forEach {
                    it.expire()
                }
            }
            connections.evictAll()
            emitter.onSuccess(Unit)
        }.subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * A no-op method to force eager initialization.
     *
     * @see [AppConfig.onCreate]
     */
    fun initialize() = Unit

    /**
     * Lifecycle method called when the app is going to be destroyed.
     */
    fun shutdown() {
        closeAllConnections()
    }

    /**
     * Obtain a [NetCopyClient] connection from the underlying connection pool.
     *
     * Beneath it will return the connection if it exists; otherwise it will create a new one and
     * put it into the connection pool.
     *
     * @param url SSH connection URL, in the form of `
     * ssh://<username>:<password>@<host>:<port>[/path]` or `
     * ssh://<username>@<host>:<port>[/path]`
     * @return [NetCopyClient] connection, already opened and authenticated
     * @throws IOException IOExceptions that occur during connection setup
     */
    fun <ClientType> getConnection(url: String): NetCopyClient<ClientType>? {
        // Extract base URI first to ensure consistent cache key
        val baseUri = extractBaseUriFrom(url)
        synchronized(connections) {
            var client = connections[baseUri]
            if (client == null) {
                client = createNetCopyClient.invoke(url)
                if (client != null) {
                    connections.put(baseUri, client)
                }
            } else {
                if (!validate(client)) {
                    LOG.debug("Connection no longer usable. Reconnecting...")
                    expire(client)
                    connections.remove(baseUri)
                    client = createNetCopyClient.invoke(url)
                    if (client != null) {
                        connections.put(baseUri, client)
                    }
                }
            }
            return if (client != null) {
                client as NetCopyClient<ClientType>?
            } else {
                null
            }
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
        keyPair: KeyPair? = null,
        explicitTls: Boolean = false,
    ): NetCopyClient<*>? {
        val url =
            NetCopyClientUtils.deriveUriFrom(
                protocol,
                host,
                port,
                "",
                username,
                password,
                explicitTls,
            )
        // Extract base URI to ensure consistent cache key with getConnection(String)
        val baseUri = extractBaseUriFrom(url)
        synchronized(connections) {
            var client = connections[baseUri]
            if (client == null) {
                client =
                    createNetCopyClientInternal(
                        protocol,
                        host,
                        port,
                        hostFingerprint,
                        username,
                        password,
                        keyPair,
                        explicitTls,
                    )
                if (client != null) connections.put(baseUri, client)
            } else {
                if (!validate(client)) {
                    LOG.debug("Connection no longer usable. Reconnecting...")
                    client.expire()
                    connections.remove(baseUri)
                    client = createNetCopyClient(url)
                    if (client != null) connections.put(baseUri, client)
                }
            }
            return client
        }
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
        KeyPair?,
        Boolean,
    ) -> NetCopyClient<*>? =
        { protocol, host, port, hostFingerprint, username, password, keyPair, explicitTls ->
            if (protocol == SSH_URI_PREFIX) {
                createSshClient(host, port, hostFingerprint!!, username, password, keyPair)
            } else {
                createFtpClient(
                    protocol,
                    host,
                    port,
                    hostFingerprint?.let { JSONObject(it) },
                    username,
                    password,
                    explicitTls,
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
    fun removeConnection(
        url: String,
        callback: () -> Unit,
    ) {
        val baseUri = extractBaseUriFrom(url)
        Maybe.fromCallable(AsyncRemoveConnection(baseUri))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { callback.invoke() }
    }

    private fun validate(client: NetCopyClient<*>): Boolean {
        return Single.fromCallable {
            client.isConnectionValid()
        }.subscribeOn(NetCopyClientUtils.getScheduler(client)).blockingGet()
    }

    private fun expire(client: NetCopyClient<*>) =
        Flowable.fromCallable {
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
                    .blockingFirst(),
            )
        }
        val hostKey = utilsHandler.getRemoteHostKey(url) ?: return null
        return createSshClientInternal(
            connInfo.host,
            connInfo.port,
            hostKey,
            connInfo.username,
            connInfo.password,
            keyPair.get(),
        )
    }

    @Suppress("LongParameterList")
    private fun createSshClient(
        host: String,
        port: Int,
        hostKey: String,
        username: String,
        password: String?,
        keyPair: KeyPair?,
    ): NetCopyClient<SSHClient>? {
        return createSshClientInternal(
            host,
            port,
            hostKey,
            username,
            password,
            keyPair,
        )
    }

    @Suppress("LongParameterList", "TooGenericExceptionCaught")
    private fun createSshClientInternal(
        host: String,
        port: Int,
        hostKey: String,
        username: String,
        password: String?,
        keyPair: KeyPair?,
    ): NetCopyClient<SSHClient>? {
        val task =
            SshAuthenticationTask(
                hostname = host,
                port = port,
                hostKey = hostKey,
                username = username,
                password = password,
                privateKey = keyPair,
            )

        return runCatching {
            Single.create { emitter ->
                try {
                    val retval = task.getTask().call()
                    emitter.onSuccess(retval)
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }.map { sshClient -> SSHClientImpl(sshClient) }
                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .blockingGet()
        }.getOrNull()
    }

    private fun createFtpClient(url: String): NetCopyClient<FTPClient>? {
        NetCopyConnectionInfo(url).run {
            val certInfo =
                if (FTPS_URI_PREFIX == prefix) {
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
                password,
                true == arguments?.containsKey(ARG_TLS) &&
                    TLS_EXPLICIT == arguments?.get(ARG_TLS),
            )
        }
    }

    @Suppress("LongParameterList", "TooGenericExceptionCaught")
    private fun createFtpClient(
        protocol: String,
        host: String,
        port: Int,
        certInfo: JSONObject?,
        username: String,
        password: String?,
        explicitTls: Boolean = false,
    ): NetCopyClient<FTPClient>? {
        val task =
            FtpAuthenticationTask(
                protocol,
                host,
                port,
                certInfo,
                username,
                password,
                explicitTls,
            )

        return kotlin.runCatching {
            Single.create { emitter ->
                try {
                    val retval = task.getTask().call()
                    emitter.onSuccess(retval)
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }.map { ftpClient -> FTPClientImpl(ftpClient) }
                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .blockingGet()
        }.getOrNull()
    }

    class AsyncRemoveConnection internal constructor(
        private val baseUri: String,
    ) : Callable<Unit> {
        override fun call() {
            synchronized(connections) {
                connections[baseUri]?.apply {
                    this.expire()
                    connections.remove(baseUri)
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
                    FTPSClient(
                        "TLS",
                        !uri.contains(QUESTION_MARK) ||
                            !uri.substringAfter(QUESTION_MARK)
                                .contains("$ARG_TLS=$TLS_EXPLICIT"),
                    )
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
