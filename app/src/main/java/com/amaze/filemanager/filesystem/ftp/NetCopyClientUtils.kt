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
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_DEFAULT_PORT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_DEFAULT_PORT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_DEFAULT_PORT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.getConnection
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.AT
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.COLON
import com.amaze.filemanager.filesystem.ftp.NetCopyConnectionInfo.Companion.SLASH
import com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate
import com.amaze.filemanager.utils.smb.SmbUtil
import com.amaze.filemanager.utils.urlEncoded
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.slf4j.LoggerFactory
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
     * using NetCopyClientTemplate.
     *
     * FIXME: Over-simplification implementation causing unnecessarily closing SSHClient.
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
        val uriWithoutProtocol: String = fullUri.substringAfter("://")
        return if (uriWithoutProtocol.substringBefore(AT).indexOf(COLON) > 0) {
            SmbUtil.getSmbEncryptedPath(
                AppConfig.getInstance(),
                fullUri
            )
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
    fun decryptFtpPathAsNecessary(fullUri: String): String {
        return runCatching {
            val uriWithoutProtocol: String = fullUri.substringAfter("://")
            if (uriWithoutProtocol.lastIndexOf(COLON) > 0) {
                SmbUtil.getSmbDecryptedPath(
                    AppConfig.getInstance(),
                    fullUri
                )
            } else {
                fullUri
            }
        }.getOrElse { e ->
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
        return NetCopyConnectionInfo(fullUri).let {
            buildString {
                append(it.prefix)
                append(it.username.ifEmpty { "" })
                if (true == it.password?.isNotEmpty()) {
                    append(COLON).append(it.password)
                }
                if (it.username.isNotEmpty()) {
                    append(AT)
                }
                append(it.host)
                if (it.port > 0) {
                    append(COLON).append(it.port)
                }
            }
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
    @JvmStatic
    fun extractRemotePathFrom(fullUri: String): String {
        return NetCopyConnectionInfo(fullUri).let { connInfo ->
            if (true == connInfo.defaultPath?.isNotEmpty()) {
                buildString {
                    append(connInfo.defaultPath)
                    if (true == connInfo.filename?.isNotEmpty()) {
                        append(SLASH).append(connInfo.filename)
                    }
                }
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
        edit: Boolean = false
    ): String {
        // FIXME: should be caller's responsibility
        var pathSuffix = defaultPath
        if (pathSuffix == null) pathSuffix = SLASH.toString()
        val thisPassword = if (password == "" || password == null) {
            ""
        } else {
            ":${if (edit) {
                password
            } else {
                password.urlEncoded()
            }}"
        }
        return if (username == "" && (true == password?.isEmpty())) {
            "$prefix$hostname:$port$pathSuffix"
        } else {
            "$prefix$username$thisPassword@$hostname:$port$pathSuffix"
        }
    }

    /**
     * Check folder existence on remote.
     */
    @FolderState
    fun checkFolder(path: String): Int {
        val template: NetCopyClientTemplate<*, Int> = if (path.startsWith(SSH_URI_PREFIX)) {
            object : SFtpClientTemplate<Int>(extractBaseUriFrom(path), false) {
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
            object : FtpClientTemplate<Int>(extractBaseUriFrom(path), false) {
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
        return execute(template) ?: DOESNT_EXIST
    }

    /**
     * Return the default port used by different protocols.
     *
     * Reserved for future use.
     */
    fun defaultPort(prefix: String) = when (prefix) {
        SSH_URI_PREFIX -> SSH_DEFAULT_PORT
        FTPS_URI_PREFIX -> FTPS_DEFAULT_PORT
        FTP_URI_PREFIX -> FTP_DEFAULT_PORT
        SMB_URI_PREFIX -> 0 // SMB never requires explicit port number at URL
        else -> throw IllegalArgumentException("Cannot derive default port")
    }

    /**
     * Convenience method to format given UNIX timestamp to yyyyMMddHHmmss format.
     */
    @JvmStatic
    fun getTimestampForTouch(date: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        val df: DateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        df.calendar = calendar
        return df.format(calendar.time)
    }
}
