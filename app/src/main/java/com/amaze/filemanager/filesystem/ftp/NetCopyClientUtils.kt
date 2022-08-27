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

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.DOESNT_EXIST
import com.amaze.filemanager.fileoperations.filesystem.FolderState
import com.amaze.filemanager.fileoperations.filesystem.WRITABLE_ON_REMOTE
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.AT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.COLON
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SLASH
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.getConnection
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate
import com.amaze.filemanager.utils.SmbUtil
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLDecoder
import java.security.GeneralSecurityException
import java.security.KeyPair

object NetCopyClientUtils {

    @JvmStatic
    private val LOG = LoggerFactory.getLogger(NetCopyClientUtils::class.java)

    /**
     * Lambda to determine the [Scheduler] to use.
     * Default is [Schedulers.io] while [Schedulers.single] is used when thread safety is required.
     */
    @JvmStatic
    var getScheduler: (NetCopyClient<*>) -> Scheduler = {
        if (it.isRequireThreadSafety()) {
            Schedulers.single()
        } else {
            Schedulers.io()
        }
    }
        // Allow test cases to override the Scheduler to use, or deadlocks will occur
        // because tests are run in parallel
        @VisibleForTesting set

    /**
     * Execute the given NetCopyClientTemplate.
     *
     * This template pattern is borrowed from Spring Framework, to simplify code on operations
     * using SftpClientTemplate.
     *
     * @param template [NetCopyClientTemplate] to execute
     * @param <T> Type of return value
     * @return Template execution results
     */
    @WorkerThread
    fun <ClientType, T> execute(
        template: NetCopyClientTemplate<ClientType, T>
    ): T? {
        var client = getConnection<ClientType>(extractBaseUriFrom(template.url))
        if (client == null) {
            client = getConnection(template.url)
        }
        var retval: T? = null
        if (client != null) {
            retval = runCatching {
                Maybe.fromCallable {
                    template.execute(client)
                }.subscribeOn(getScheduler.invoke(client)).blockingGet()
            }.onFailure {
                LOG.error("Error executing template method", it)
            }.also {
                if (template.closeClientOnFinish) {
                    tryDisconnect(client)
                }
            }.getOrNull()
        }
        return retval
    }

    /**
     * Convenience method to call [SmbUtil.getSmbEncryptedPath] if the given
     * SSH URL contains the password (assuming the password is encrypted).
     *
     * @param fullUri SSH URL
     * @return SSH URL with the password (if exists) encrypted
     */
    fun encryptFtpPathAsNecessary(fullUri: String): String {
        val prefix = fullUri.substring(0, fullUri.indexOf("://") + 3)
        val uriWithoutProtocol: String = fullUri.substring(prefix.length)
        return if (uriWithoutProtocol.substringBefore(AT).indexOf(COLON) > 0) {
            SmbUtil.getSmbEncryptedPath(
                AppConfig.getInstance(),
                fullUri
            ).replace("\n", "")
        } else {
            fullUri
        }
    }

    /**
     * Convenience method to call [SmbUtil.getSmbDecryptedPath] if the given
     * SSH URL contains the password (assuming the password is encrypted).
     *
     * @param fullUri SSH URL
     * @return SSH URL with the password (if exists) decrypted
     */
    fun decryptFtpPathAsNecessary(fullUri: String): String? {
        val prefix = fullUri.substring(0, fullUri.indexOf("://") + 3)
        val uriWithoutProtocol: String = fullUri.substring(prefix.length)
        return try {
            if (uriWithoutProtocol.lastIndexOf(COLON) > 0) SmbUtil.getSmbDecryptedPath(
                AppConfig.getInstance(),
                fullUri
            ) else fullUri
        } catch (e: IOException) {
            LOG.error("Error decrypting path", e)
            fullUri
        } catch (e: GeneralSecurityException) {
            LOG.error("Error decrypting path", e)
            fullUri
        }
    }

    /**
     * Convenience method to extract the Base URL from the given SSH URL.
     *
     *
     * For example, given `ssh://user:password@127.0.0.1:22/home/user/foo/bar`, this
     * method returns `ssh://user:password@127.0.0.1:22`.
     *
     * @param fullUri Full SSH URL
     * @return The remote path part of the full SSH URL
     */
    fun extractBaseUriFrom(fullUri: String): String {
        val prefix = fullUri.substring(0, fullUri.indexOf("://") + 3)
        val uriWithoutProtocol: String = fullUri.substring(prefix.length)
        val credentials: String
        val hostAndPath: String
        if (uriWithoutProtocol.contains(AT)) {
            credentials = uriWithoutProtocol.substring(0, uriWithoutProtocol.lastIndexOf(AT))
            hostAndPath = uriWithoutProtocol.substring(uriWithoutProtocol.lastIndexOf(AT) + 1)
        } else {
            credentials = ""
            hostAndPath = uriWithoutProtocol
        }
        return if (hostAndPath.indexOf(SLASH) == -1) {
            fullUri
        } else {
            val host = hostAndPath.substring(0, hostAndPath.indexOf(SLASH))
            val credentialsLen = if (credentials == "") {
                0
            } else {
                credentials.length + 1
            }
            fullUri.substring(
                0,
                prefix.length + credentialsLen + host.length
            )
        }
    }

    /**
     * Convenience method to extract the remote path from the given SSH URL.
     *
     *
     * For example, given `ssh://user:password@127.0.0.1:22/home/user/foo/bar`, this
     * method returns `/home/user/foo/bar`.
     *
     * @param fullUri Full SSH URL
     * @return The remote path part of the full SSH URL
     */
    fun extractRemotePathFrom(fullUri: String): String {
        if (fullUri.contains(AT)) {
            val hostPath = fullUri.substring(fullUri.lastIndexOf(AT))
            return if (hostPath.indexOf(SLASH) == -1) {
                SLASH.toString()
            } else {
                URLDecoder.decode(
                    hostPath.substring(hostPath.indexOf(SLASH)),
                    Charsets.UTF_8.name()
                )
            }
        } else {
            val hostAndPath = fullUri.substringAfter("://")
            return if (hostAndPath.contains(SLASH)) {
                hostAndPath.substring(hostAndPath.indexOf(SLASH))
            } else {
                SLASH.toString()
            }
        }
    }

    /**
     * Disconnects the given [NetCopyClient] but wrap all exceptions beneath, so callers are free
     * from the hassles of handling thrown exceptions.
     *
     * @param client [NetCopyClient] instance
     */
    private fun tryDisconnect(client: NetCopyClient<*>) {
        if (client.isConnectionValid()) {
            client.expire()
        }
    }

    /**
     * Decide the SSH URL depends on password/selected KeyPair.
     */
    @Suppress("LongParameterList")
    fun deriveUriFrom(
        prefix: String,
        hostname: String,
        port: Int,
        defaultPath: String? = null,
        username: String,
        password: String? = null,
        selectedParsedKeyPair: KeyPair? = null
    ): String {
        // FIXME: should be caller's responsibility
        var pathSuffix = defaultPath
        if (pathSuffix == null) pathSuffix = SLASH.toString()
        return if (selectedParsedKeyPair != null) {
            "$prefix$username@$hostname:$port$pathSuffix"
        } else if (username == "" && (password == "" || password == null)) {
            "$prefix$hostname:$port$pathSuffix"
        } else {
            "$prefix$username:$password@$hostname:$port$pathSuffix"
        }
    }

    /**
     * Check folder existence on remote.
     */
    @FolderState
    fun checkFolder(path: String): Int {
        val template: NetCopyClientTemplate<*, Int> = if (path.startsWith(SSH_URI_PREFIX)) {
            object : SFtpClientTemplate<Int>(extractBaseUriFrom(path)) {
                @FolderState
                @Throws(IOException::class)
                override fun execute(client: SFTPClient): Int {
                    return if (client.statExistence(extractRemotePathFrom(path)) == null) {
                        WRITABLE_ON_REMOTE
                    } else {
                        DOESNT_EXIST
                    }
                }
            }
        } else {
            object : FtpClientTemplate<Int>(extractBaseUriFrom(path)) {
                override fun executeWithFtpClient(ftpClient: FTPClient): Int {
                    return if (ftpClient.stat(extractRemotePathFrom(path))
                        == FTPReply.DIRECTORY_STATUS
                    ) {
                        WRITABLE_ON_REMOTE
                    } else {
                        DOESNT_EXIST
                    }
                }
            }
        }
        return Single.fromCallable {
            execute(template)
        }
            .subscribeOn(Schedulers.io())
            .blockingGet() ?: DOESNT_EXIST
    }
}
