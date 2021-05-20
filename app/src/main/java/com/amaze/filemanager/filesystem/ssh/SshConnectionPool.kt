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

package com.amaze.filemanager.filesystem.ssh

import android.os.AsyncTask
import android.util.Log
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.SshAuthenticationTask
import net.schmizz.sshj.Config
import net.schmizz.sshj.SSHClient
import java.security.KeyPair
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference

/**
 * Poor man's implementation of SSH connection pool.
 *
 *
 * It uses a [ConcurrentHashMap] to hold the opened SSH connections; all code that uses
 * [SSHClient] can ask for connection here with `getConnection(url)`.
 */
object SshConnectionPool {

    const val SSH_DEFAULT_PORT = 22
    const val SSH_URI_PREFIX = "ssh://"
    const val SSH_CONNECT_TIMEOUT = 30000

    private val TAG = SshConnectionPool::class.java.simpleName

    private var connections: MutableMap<String, SSHClient> = ConcurrentHashMap()

    @JvmField
    var sshClientFactory: SSHClientFactory = DefaultSSHClientFactory()

    /**
     * Remove a SSH connection from connection pool. Disconnects from server before removing.
     *
     *
     * For updating SSH connection settings.
     *
     *
     * This method will silently end without feedback if the specified SSH connection URI does not
     * exist in the connection pool.
     *
     * @param url SSH connection URI
     */
    fun removeConnection(url: String, callback: Runnable) {
        AsyncRemoveConnection(url, callback).execute()
    }

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
    fun getConnection(url: String): SSHClient? {
        var client = connections[url]
        if (client == null) {
            client = create(url)
            if (client != null) {
                connections[url] = client
            }
        } else {
            if (!validate(client)) {
                Log.d(TAG, "Connection no longer usable. Reconnecting...")
                expire(client)
                connections.remove(url)
                client = create(url)
                if (client != null) {
                    connections[url] = client
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
        host: String,
        port: Int,
        hostFingerprint: String,
        username: String,
        password: String?,
        keyPair: KeyPair?
    ): SSHClient? {
        val url = SshClientUtils.deriveSftpPathFrom(host, port, "", username, password, keyPair)
        var client = connections[url]
        if (client == null) {
            client = create(host, port, hostFingerprint, username, password, keyPair)
            if (client != null) connections[url] = client
        } else {
            if (!validate(client)) {
                Log.d(TAG, "Connection no longer usable. Reconnecting...")
                expire(client)
                connections.remove(url)
                client = create(host, port, hostFingerprint, username, password, keyPair)
                if (client != null) connections[url] = client
            }
        }
        return client
    }

    /**
     * Kill any connection that is still in place. Used by [ ].
     *
     * @see MainActivity.onDestroy
     * @see MainActivity.exit
     */
    fun shutdown() {
        AppConfig.getInstance()
            .runInBackground {
                if (!connections.isEmpty()) {
                    for (connection in connections.values) {
                        SshClientUtils.tryDisconnect(connection)
                    }
                    connections.clear()
                }
            }
    }

    // Logic for creating SSH connection. Depends on password existence in given Uri password or
    // key-based authentication
    @Suppress("TooGenericExceptionThrown")
    private fun create(url: String): SSHClient? {
        val connInfo = ConnectionInfo(url)
        val utilsHandler = AppConfig.getInstance().utilsHandler
        val pem = utilsHandler.getSshAuthPrivateKey(url)
        val keyPair = AtomicReference<KeyPair?>(null)
        if (pem != null && !pem.isEmpty()) {
            try {
                val latch = CountDownLatch(1)
                PemToKeyPairTask(
                    pem
                ) { result: KeyPair? ->
                    keyPair.set(result)
                    latch.countDown()
                }
                    .execute()
                latch.await()
            } catch (e: InterruptedException) {
                throw RuntimeException("Error getting keypair from given PEM string", e)
            }
        }
        val hostKey = utilsHandler.getSshHostKey(url) ?: return null
        return create(
            connInfo.host,
            connInfo.port,
            hostKey,
            connInfo.username,
            connInfo.password,
            keyPair.get()
        )
    }

    @Suppress("LongParameterList")
    private fun create(
        host: String,
        port: Int,
        hostKey: String,
        username: String,
        password: String?,
        keyPair: KeyPair?
    ): SSHClient? {
        return try {
            val taskResult = SshAuthenticationTask(
                hostname = host,
                port = port,
                hostKey = hostKey,
                username = username,
                password = password,
                privateKey = keyPair
            ).execute().get()
            taskResult.result
        } catch (e: InterruptedException) {
            // FIXME: proper handling
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            // FIXME: proper handling
            throw RuntimeException(e)
        }
    }

    private fun validate(client: SSHClient): Boolean {
        return client.isConnected && client.isAuthenticated
    }

    private fun expire(client: SSHClient) {
        SshClientUtils.tryDisconnect(client)
    }

    /**
     * Container object for SSH URI, encapsulating logic for splitting information from given URI.
     * `Uri.parse()` only parse URI that is compliant to RFC2396, but we have to deal with
     * URI that is not compliant, since usernames and/or strong passwords usually have special
     * characters included, like `ssh://user@example.com:P@##w0rd@127.0.0.1:22`.
     *
     *
     * A design decision to keep database schema slim, by the way... -TranceLove
     */
    internal class ConnectionInfo(url: String) {
        val host: String
        val port: Int
        val username: String
        val password: String?
        protected var defaultPath: String? = null

        // FIXME: Crude assumption
        init {
            require(url.startsWith(SSH_URI_PREFIX)) { "Argument is not a SSH URI: $url" }
            host = url.substring(url.lastIndexOf('@') + 1, url.lastIndexOf(':'))
            val portAndPath = url.substring(url.lastIndexOf(':') + 1)
            var port = SSH_DEFAULT_PORT
            if (portAndPath.contains("/")) {
                port = portAndPath.substring(0, portAndPath.indexOf('/')).toInt()
                defaultPath = portAndPath.substring(portAndPath.indexOf('/'))
            } else {
                port = portAndPath.toInt()
                defaultPath = null
            }
            // If the uri is fetched from the app's database storage, we assume it will never be empty
            val authString = url.substring(SSH_URI_PREFIX.length, url.lastIndexOf('@'))
            val userInfo = authString.split(":").toTypedArray()
            username = userInfo[0]
            password = if (userInfo.size > 1) userInfo[1] else null
            if (port < 0) port = SSH_DEFAULT_PORT
            this.port = port
        }
    }

    class AsyncRemoveConnection internal constructor(
        private var url: String,
        private val callback: Runnable?
    ) : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg params: Unit) {
            url = SshClientUtils.extractBaseUriFrom(url)
            if (connections.containsKey(url)) {
                SshClientUtils.tryDisconnect(connections.remove(url))
            }
        }

        override fun onPostExecute(aVoid: Unit) {
            callback?.run()
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
        fun create(config: Config?): SSHClient
    }

    /** Default [SSHClientFactory] implementation.  */
    internal class DefaultSSHClientFactory : SSHClientFactory {
        override fun create(config: Config?): SSHClient {
            return SSHClient(config)
        }
    }
}
