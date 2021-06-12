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

import android.app.ProgressDialog
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig
import com.amaze.filemanager.filesystem.ssh.SshClientUtils
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_CONNECT_TIMEOUT
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.PublicKey
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * [AsyncTask] to obtain SSH host fingerprint.
 *
 *
 * It works by adding a [HostKeyVerifier] that accepts all SSH host keys, then obtain the
 * key shown by server, and return to the task's caller.
 *
 *
 * [CountDownLatch] with [AtomicReference] combo is used to ensure SSH host key is
 * obtained successfully on returning to the task caller.
 *
 *
 * Mainly used by [com.amaze.filemanager.ui.dialogs.SftpConnectDialog] on saving SSH
 * connection settings.
 *
 * @see HostKeyVerifier
 *
 * @see SSHClient.addHostKeyVerifier
 * @see com.amaze.filemanager.ui.dialogs.SftpConnectDialog.onCreateDialog
 */
class GetSshHostFingerprintTask(
    private val hostname: String,
    private val port: Int,
    private val callback: AsyncTaskResult.Callback<AsyncTaskResult<PublicKey>>
) :
    AsyncTask<Void, Void, AsyncTaskResult<PublicKey>>() {
    private var progressDialog: ProgressDialog? = null

    override fun doInBackground(vararg params: Void): AsyncTaskResult<PublicKey> {
        val holder = AtomicReference<AsyncTaskResult<PublicKey>>()
        val latch = CountDownLatch(1)
        val sshClient = SshConnectionPool.sshClientFactory.create(CustomSshJConfig()).also {
            it.connectTimeout = SSH_CONNECT_TIMEOUT
            it.addHostKeyVerifier { _, _, key: PublicKey ->
                holder.set(AsyncTaskResult(key))
                latch.countDown()
                true
            }
        }
        return runCatching {
            sshClient.connect(hostname, port)
            latch.await()
            holder.get()
        }.onFailure {
            Log.e(TAG, "Unable to connect to [$hostname:$port]", it)
            latch.countDown()
        }.getOrElse {
            holder.set(AsyncTaskResult(it))
            holder.get()
        }.also {
            SshClientUtils.tryDisconnect(sshClient)
        }
    }

    override fun onPreExecute() {
        progressDialog = ProgressDialog.show(
            AppConfig.getInstance().mainActivityContext,
            "",
            AppConfig.getInstance().resources.getString(R.string.processing)
        )
    }

    override fun onPostExecute(result: AsyncTaskResult<PublicKey>) {
        progressDialog!!.dismiss()
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
                            result.exception.localizedMessage
                        ),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            callback.onResult(result)
        }
    }

    companion object {
        private val TAG = GetSshHostFingerprintTask::class.java.simpleName
    }
}
