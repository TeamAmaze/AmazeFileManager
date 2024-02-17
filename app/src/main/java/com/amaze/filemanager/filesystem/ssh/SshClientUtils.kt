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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.fileoperations.filesystem.cloud.CloudStreamer
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.extractRemotePathFrom
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.icons.MimeTypes
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

object SshClientUtils {

    @JvmStatic
    private val LOG = LoggerFactory.getLogger(SshClientUtils::class.java)

    @JvmField
    val sftpGetSize: (String) -> Long? = { path ->
        NetCopyClientUtils.execute(object : SFtpClientTemplate<Long>(path, true) {
            override fun execute(client: SFTPClient): Long {
                return client.size(extractRemotePathFrom(path))
            }
        })
    }

    /**
     * Execute the given template with SshClientTemplate.
     *
     * @param template [SshClientSessionTemplate] to execute
     * @param <T> Type of return value
     * @return Template execution results
     */
    @JvmStatic
    fun <T> execute(template: SshClientSessionTemplate<T>): T? {
        return NetCopyClientUtils.execute(
            object : SshClientTemplate<T>(template.url, false) {
                override fun executeWithSSHClient(sshClient: SSHClient): T? {
                    var session: Session? = null
                    var retval: T? = null
                    try {
                        session = sshClient.startSession()
                        retval = template.execute(session)
                    } catch (e: IOException) {
                        LOG.error("Error executing template method", e)
                    } finally {
                        if (session != null && session.isOpen) {
                            try {
                                session.close()
                            } catch (e: IOException) {
                                LOG.warn("Error closing SFTP client", e)
                            }
                        }
                    }
                    return retval
                }
            }
        )
    }

    /**
     * Execute the given template with SshClientTemplate.
     *
     * @param template [SFtpClientTemplate] to execute
     * @param <T> Type of return value
     * @return Template execution results
     */
    @JvmStatic
    fun <T> execute(template: SFtpClientTemplate<T>): T? {
        return NetCopyClientUtils.execute(template)
    }

    /**
     * Converts plain path smb://127.0.0.1/test.pdf to authorized path
     * smb://test:123@127.0.0.1/test.pdf from server list
     *
     * @param path
     * @return
     */
    @JvmStatic
    fun formatPlainServerPathToAuthorised(
        servers: ArrayList<Array<String?>>,
        path: String
    ): String {
        for (serverEntry in servers) {
            val inputUri = Uri.parse(path)
            val serverUri = Uri.parse(serverEntry[1])
            if (inputUri.scheme.equals(serverUri.scheme, ignoreCase = true) &&
                serverUri.authority!!.contains(inputUri.authority!!)
            ) {
                val output = inputUri
                    .buildUpon()
                    .encodedAuthority(serverUri.encodedAuthority)
                    .build()
                    .toString()
                LOG.info("build authorised path {} from plain path {}", output, path)
                return output
            }
        }
        return path
    }

    /**
     * Disconnects the given [SSHClient] but wrap all exceptions beneath, so callers are free
     * from the hassles of handling thrown exceptions.
     *
     * @param client [SSHClient] instance
     */
    fun tryDisconnect(client: SSHClient?) {
        if (client != null && client.isConnected) {
            try {
                client.disconnect()
            } catch (e: IOException) {
                LOG.warn("Error closing SSHClient connection", e)
            }
        }
    }

    /**
     * Open a remote SSH file on local Android device. It uses the [CloudStreamer] to stream the
     * file.
     */
    @JvmStatic
    @Suppress("Detekt.TooGenericExceptionCaught")
    fun launchFtp(baseFile: HybridFile, activity: MainActivity) {
        val streamer = CloudStreamer.getInstance()
        thread {
            try {
                val isDirectory = baseFile.isDirectory(activity)
                val fileLength = baseFile.length(activity)
                streamer.setStreamSrc(
                    baseFile.getInputStream(activity),
                    baseFile.getName(activity),
                    fileLength
                )
                activity.runOnUiThread {
                    try {
                        val file = File(
                            extractRemotePathFrom(
                                baseFile.path
                            )
                        )
                        val uri = Uri.parse(CloudStreamer.URL + Uri.fromFile(file).encodedPath)
                        val i = Intent(Intent.ACTION_VIEW)
                        i.setDataAndType(
                            uri,
                            MimeTypes.getMimeType(baseFile.path, isDirectory)
                        )
                        val packageManager = activity.packageManager
                        val resInfos = packageManager.queryIntentActivities(i, 0)
                        if (resInfos != null && resInfos.size > 0) {
                            activity.startActivity(i)
                        } else {
                            Toast.makeText(
                                activity,
                                activity.resources.getString(R.string.smb_launch_error),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    } catch (e: ActivityNotFoundException) {
                        LOG.warn("failed to launch sftp file", e)
                    }
                }
            } catch (e: Exception) {
                LOG.warn("failed to launch sftp file", e)
            }
        }
    }

    /**
     * Reads given [RemoteResourceInfo] and determines if the path it's related to is a directory.
     *
     * Will descend into corresponding target if given RemoteResourceInfo represents a symlink.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun isDirectory(client: SFTPClient, info: RemoteResourceInfo): Boolean {
        var isDirectory = info.isDirectory
        if (info.attributes.type == FileMode.Type.SYMLINK) {
            try {
                val symlinkAttrs = client.stat(info.path)
                isDirectory = symlinkAttrs.type == FileMode.Type.DIRECTORY
            } catch (ifSymlinkIsBroken: IOException) {
                LOG.warn("Symbolic link {} is broken, skipping", info.path)
                throw ifSymlinkIsBroken
            }
        }
        return isDirectory
    }
}
