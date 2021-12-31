package com.amaze.filemanager.asynchronous.asynctasks.ssh.auth

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.Task
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.transport.TransportException
import java.lang.ref.WeakReference
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.KeyPair

class SshAuthenticationTask(
        private val appContextWR: WeakReference<Context>,
        private val hostname: String,
        private val port: Int,
        private val hostKey: String,
        private val username: String,
        private val password: String? = null,
        private val privateKey: KeyPair? = null
): Task<SSHClient, SshAuthenticationCallable> {

    companion object {
        private val TAG = SshAuthenticationTask::class.java.simpleName
    }

    private val task: SshAuthenticationCallable

    init {
        task = SshAuthenticationCallable(hostname, port, hostKey, username, password, privateKey)
    }

    override fun getTask() = task

    override fun onError(error: Throwable) {
        Log.e(TAG, "Failure authentication ssh", error)

        val context = appContextWR.get() ?: return

        if (SocketException::class.java.isAssignableFrom(error.javaClass) ||
                SocketTimeoutException::class.java
                        .isAssignableFrom(error.javaClass)
        ) {
            Toast.makeText(
                    context,
                    context
                            .resources
                            .getString(
                                    R.string.ssh_connect_failed,
                                    hostname,
                                    port,
                                    error.localizedMessage ?: error.message
                            ),
                    Toast.LENGTH_LONG
            )
                    .show()
        } else if (TransportException::class.java
                        .isAssignableFrom(error.javaClass)
        ) {
            val disconnectReason =
                    TransportException::class.java.cast(error).disconnectReason
            if (DisconnectReason.HOST_KEY_NOT_VERIFIABLE == disconnectReason) {
                AlertDialog.Builder(context)
                        .setTitle(R.string.ssh_connect_failed_host_key_changed_title)
                        .setMessage(R.string.ssh_connect_failed_host_key_changed_message)
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
            }
        } else if (password != null) {
            Toast.makeText(
                    context,
                    R.string.ssh_authentication_failure_password,
                    Toast.LENGTH_LONG
            )
                    .show()
        } else if (privateKey != null) {
            Toast.makeText(
                    context,
                    R.string.ssh_authentication_failure_key,
                    Toast.LENGTH_LONG
            )
                    .show()
        }
    }

    override fun onFinish(value: SSHClient) {

    }
}