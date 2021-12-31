package com.amaze.filemanager.asynchronous.asynctasks.ssh.auth

import android.os.AsyncTask
import androidx.annotation.WorkerThread
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.concurrent.Callable


/**
 * [Callable] for authenticating with SSH server to verify if parameters are correct.
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
 * @param hostname hostname, required
 * @param port port, must be unsigned integer
 * @param hostKey SSH host fingerprint, required
 * @param username login username, required
 * @param password login password, required if using password authentication
 * @param privateKey login [KeyPair], required if using key-based authentication
 */
class SshAuthenticationCallable(
                                private val hostname: String,
                                private val port: Int,
                                private val hostKey: String,
                                private val username: String,
                                private val password: String? = null,
                                private val privateKey: KeyPair? = null): Callable<SSHClient> {

    @WorkerThread
    @Throws(Exception::class)
    override fun call(): SSHClient{
        val sshClient = SshConnectionPool.sshClientFactory.create(CustomSshJConfig()).also {
            it.addHostKeyVerifier(hostKey)
            it.connectTimeout = SshConnectionPool.SSH_CONNECT_TIMEOUT
        }

        sshClient.connect(hostname, port)
        if (true == password?.isNotEmpty()) {
            sshClient.authPassword(username, password)
            return sshClient
        }

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
        return sshClient
    }
}