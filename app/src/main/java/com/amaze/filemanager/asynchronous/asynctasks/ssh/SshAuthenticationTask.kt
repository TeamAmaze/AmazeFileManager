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

import android.app.AlertDialog
import android.os.AsyncTask
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_CONNECT_TIMEOUT
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

/**
 * [AsyncTask] for authenticating with SSH server to verify if parameters are correct.
 *
 *
 * Used by [com.amaze.filemanager.ui.dialogs.SftpConnectDialog].
 *
 * @see SSHClient
 *
 * @see SSHClient.authPassword
 * @see SSHClient.authPublickey
 * @see com.amaze.filemanager.ui.dialogs.SftpConnectDialog.authenticateAndSaveSetup
 * @see com.amaze.filemanager.filesystem.ssh.SshConnectionPool.create
 */
class SshAuthenticationTask(
    /**
     * Constructor.
     *
     * @param hostname hostname, required
     * @param port port, must be unsigned integer
     * @param hostKey SSH host fingerprint, required
     * @param username login username, required
     * @param password login password, required if using password authentication
     * @param privateKey login [KeyPair], required if using key-based authentication
     */
    private val hostname: String,
    private val port: Int,
    private val hostKey: String,
    private val username: String,
    private val password: String? = null,
    private val privateKey: KeyPair? = null
) : AsyncTask<Void, Void, AsyncTaskResult<SSHClient>>() {

    override fun doInBackground(vararg params: Void): AsyncTaskResult<SSHClient> {
        val sshClient = SshConnectionPool.sshClientFactory.create(CustomSshJConfig()).also {
            it.addHostKeyVerifier(hostKey)
            it.connectTimeout = SSH_CONNECT_TIMEOUT
        }
        return runCatching {
            sshClient.connect(hostname, port)
            if (password != null && "" != password) {
                sshClient.authPassword(username, password)
                AsyncTaskResult(sshClient)
            } else {
                sshClient.authPublickey(
                    username,
                    object : KeyProvider {
                        override fun getPrivate(): PrivateKey {
                            return privateKey!!.private
                        }

                        override fun getPublic(): PublicKey {
                            return privateKey!!.public
                        }

                        override fun getType(): KeyType {
                            return KeyType.fromKey(public)
                        }
                    }
                )
                AsyncTaskResult(sshClient)
            }
        }.getOrElse {
            it.printStackTrace()
            AsyncTaskResult(it)
        }
    }

    // If authentication failed, use Toast to notify user.
    override fun onPostExecute(result: AsyncTaskResult<SSHClient>) {
        if (result.exception != null) {
            if (SocketException::class.java.isAssignableFrom(result.exception.javaClass) ||
                SocketTimeoutException::class.java
                    .isAssignableFrom(result.exception.javaClass)
            ) {
                Toast.makeText(
                    AppConfig.getInstance(),
                    AppConfig.getInstance()
                        .resources
                        .getString(
                            R.string.ssh_connect_failed,
                            hostname,
                            port,
                            result.exception.localizedMessage ?: result.exception.message
                        ),
                    Toast.LENGTH_LONG
                )
                    .show()
            } else if (TransportException::class.java
                .isAssignableFrom(result.exception.javaClass)
            ) {
                val disconnectReason =
                    TransportException::class.java.cast(result.exception)!!.disconnectReason
                if (DisconnectReason.HOST_KEY_NOT_VERIFIABLE == disconnectReason) {
                    AlertDialog.Builder(AppConfig.getInstance().mainActivityContext)
                        .setTitle(R.string.ssh_connect_failed_host_key_changed_title)
                        .setMessage(R.string.ssh_connect_failed_host_key_changed_message)
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            } else if (password != null) {
                Toast.makeText(
                    AppConfig.getInstance(),
                    R.string.ssh_authentication_failure_password,
                    Toast.LENGTH_LONG
                )
                    .show()
            } else if (privateKey != null) {
                Toast.makeText(
                    AppConfig.getInstance(),
                    R.string.ssh_authentication_failure_key,
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }
    }
}
