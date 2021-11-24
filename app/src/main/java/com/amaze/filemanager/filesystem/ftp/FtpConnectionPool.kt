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
import android.util.Log
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.ftp.auth.FtpAuthenticationTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.SshAuthenticationTask
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.extractBaseUriFrom
import com.amaze.filemanager.utils.PasswordUtil
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.Config
import net.schmizz.sshj.SSHClient
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.json.JSONObject
import java.security.KeyPair
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

object FtpConnectionPool {
    const val FTP_DEFAULT_PORT = 21
    const val FTPS_DEFAULT_PORT = 990
    const val SSH_DEFAULT_PORT = 22
    const val FTP_URI_PREFIX = "ftp://"
    const val FTPS_URI_PREFIX = "ftps://"
    const val SSH_URI_PREFIX = "ssh://"
    const val CONNECT_TIMEOUT = 30000

    private val TAG = FtpConnectionPool::class.java.simpleName

    private var connections: MutableMap<String, NetCopyClient> = ConcurrentHashMap()

    @JvmField
    var sshClientFactory: SSHClientFactory = DefaultSSHClientFactory()

    @JvmField
    var ftpClientFactory: FTPClientFactory = DefaultFTPClientFactory()

    /**
     * Obtain a [SSHClient] connection from the underlying connection pool.
     *
     *
     * Beneath it will return the connection if it exists; otherwise it will create a new one and
     * put it into the connection pool.
     *
     * @param url SSH connection URL, in the form of `
     * ssh://<username>:<password>@<host>:<port>` or `
     * ssh://<username>@<host>:<port>`
     * @return [SSHClient] connection, already opened and authenticated
     * @throws IOException IOExceptions that occur during connection setup
     */
    fun getConnection(url: String): NetCopyClient? {
        var client = connections[url]
        if (client == null) {
            client = createNetCopyClient.invoke(url)
            if (client != null) {
                connections[extractBaseUriFrom(url)] = client
            }
        } else {
            if (!validate(client)) {
                Log.d(TAG, "Connection no longer usable. Reconnecting...")
                expire(client)
                connections.remove(url)
                client = createNetCopyClient.invoke(url)
                if (client != null) {
                    connections[extractBaseUriFrom(url)] = client
                }
            }
        }
        return client
    }

    /**
     * Obtain a [SSHClient] connection from the underlying connection pool.
     *
     *
     * Beneath it will return the connection if it exists; otherwise it will create a new one and
     * put it into the connection pool.
     *
     *
     * Different from [.getConnection] above, this accepts broken down parameters as
     * convenience method during setting up SCP/SFTP connection.
     *
     * @param host host name/IP, required
     * @param port SSH server port, required
     * @param hostFingerprint expected host fingerprint, required
     * @param username username, required
     * @param password password, required if using password to authenticate
     * @param keyPair [KeyPair], required if using key-based authentication
     * @return [SSHClient] connection
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
    ): NetCopyClient? {
        val url = NetCopyClientUtils.deriveUriFrom(
            protocol, host, port, "", username, password, keyPair
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
                Log.d(TAG, "Connection no longer usable. Reconnecting...")
                expire(client)
                connections.remove(url)
                client = createNetCopyClient(url)
                if (client != null) connections[url] = client
            }
        }
        return client
    }

    private val createNetCopyClient: (String) -> NetCopyClient? = { url ->
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
    ) -> NetCopyClient? = { protocol, host, port, hostFingerprint, username, password, keyPair ->
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
     * Remove a SSH connection from connection pool. Disconnects from server before removing.
     *
     * For updating SSH connection settings.
     *
     * This method will silently end without feedback if the specified SSH connection URI does not
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

    private fun validate(client: NetCopyClient): Boolean = client.isConnectionValid()

    private fun expire(client: NetCopyClient) = client.expire()

    // Logic for creating SSH connection. Depends on password existence in given Uri password or
    // key-based authentication
    @Suppress("TooGenericExceptionThrown")
    private fun createSshClient(url: String): NetCopyClient? {
        val connInfo = ConnectionInfo(url)
        val utilsHandler = AppConfig.getInstance().utilsHandler
        val pem = utilsHandler.getSshAuthPrivateKey(url)
        val keyPair = AtomicReference<KeyPair?>(null)
        if (true == pem?.isNotEmpty()) {
            keyPair.set(
                PemToKeyPairTask(
                    pem
                ) { }.execute().get()
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
    ): NetCopyClient? {
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
    ): NetCopyClient? {
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
                task.onError(it)
                latch.countDown()
            })
        latch.await()
        return retval?.let {
            SSHClientImpl(it)
        }
    }

    private fun createFtpClient(url: String): NetCopyClient? {
        ConnectionInfo(url).run {
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
    ): NetCopyClient? {
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
        Maybe.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .subscribe({
                result = it
                latch.countDown()
            }, {
                Log.e(TAG, "Error getting connection", it)
                latch.countDown()
            })
        latch.await()
        return result?.let { ftpClient ->
            FTPClientImpl(ftpClient)
        }
    }

    /**
     * Container object for SSH URI, encapsulating logic for splitting information from given URI.
     * `Uri.parse()` only parse URI that is compliant to RFC2396, but we have to deal with
     * URI that is not compliant, since usernames and/or strong passwords usually have special
     * characters included, like `ssh://user@example.com:P@##w0rd@127.0.0.1:22`.
     *
     * A design decision to keep database schema slim, by the way... -TranceLove
     */
    internal class ConnectionInfo(url: String) {
        val prefix: String
        val host: String
        val port: Int
        val username: String
        val password: String?
        var defaultPath: String? = null
        var queryString: String? = null

        // FIXME: Crude assumption
        init {
            require(
                url.startsWith(SSH_URI_PREFIX) or
                    url.startsWith(FTP_URI_PREFIX) or
                    url.startsWith(FTPS_URI_PREFIX)
            ) {
                "Argument is not a SSH URI: $url"
            }
            host = url.substring(url.lastIndexOf('@') + 1, url.lastIndexOf(':'))
            val portAndPath = url.substring(url.lastIndexOf(':') + 1)
            var port: Int
            if (portAndPath.contains("/")) {
                port = portAndPath.substring(0, portAndPath.indexOf('/')).toInt()
                defaultPath = portAndPath.substring(portAndPath.indexOf('/'))
            } else {
                port = portAndPath.toInt()
                defaultPath = null
            }
            // If the uri is fetched from the app's database storage, we assume it will never be empty
            prefix = when {
                url.startsWith(SSH_URI_PREFIX) -> SSH_URI_PREFIX
                url.startsWith(FTPS_URI_PREFIX) -> FTPS_URI_PREFIX
                else -> FTP_URI_PREFIX
            }
            val authString = url.substring(prefix.length, url.lastIndexOf('@'))
            val userInfo = authString.split(":").toTypedArray()
            username = userInfo[0]
            password = if (userInfo.size > 1) {
                runCatching {
                    PasswordUtil.decryptPassword(AppConfig.getInstance(), userInfo[1])
                }.getOrElse {
                    /* Hack. It should only happen after creating new SSH connection settings
                     * and plain text password is sent in.
                     *
                     * Possible to encrypt password there as alternate solution.
                     */
                    userInfo[1]
                }
            } else {
                null
            }
            if (port < 0) port = if (url.startsWith(SSH_URI_PREFIX)) {
                SSH_DEFAULT_PORT
            } else {
                FTP_DEFAULT_PORT
            }
            this.port = port
            this.queryString = url.substringAfter('?')
        }

        override fun toString(): String {
            return "$prefix$username@$host:$port${defaultPath ?: ""}"
        }
    }

    class AsyncRemoveConnection internal constructor(
        private val url: String
    ) : Callable<Unit> {

        override fun call() {
            NetCopyClientUtils.extractBaseUriFrom(url).run {
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
     * SSHClient, that mocked instances can be returned so tests can be run without a real SSH server.
     */
    interface SSHClientFactory {
        /**
         * Implement this to return [SSHClient] instances.
         */
        fun create(config: Config): SSHClient
    }

    interface FTPClientFactory {
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
            return if (uri.startsWith(FTPS_URI_PREFIX))
                FTPSClient("TLS", false).also {
                    it.addProtocolCommandListener(PrintCommandListener(System.out))
                }
            else
                FTPClient().also {
                    it.addProtocolCommandListener(PrintCommandListener(System.out))
                }
        }
    }
}
