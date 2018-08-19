/*
 * SshAuthenticationTask.java
 *
 * Copyright © 2017 Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.asynchronous.asynctasks.ssh;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig;
import com.amaze.filemanager.utils.application.AppConfig;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.DisconnectReason;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_CONNECT_TIMEOUT;

/**
 * {@link AsyncTask} for authenticating with SSH server to verify if parameters are correct.
 *
 * Used by {@link com.amaze.filemanager.ui.dialogs.SftpConnectDialog}.
 *
 * @see SSHClient
 * @see SSHClient#authPassword(String, String)
 * @see SSHClient#authPublickey(String, KeyProvider...)
 * @see com.amaze.filemanager.ui.dialogs.SftpConnectDialog#authenticateAndSaveSetup(String, String, int, String, String, String, String, KeyPair, boolean)
 * @see com.amaze.filemanager.filesystem.ssh.SshConnectionPool#create(Uri)
 */
public class SshAuthenticationTask extends AsyncTask<Void, Void, AsyncTaskResult<SSHClient>>
{
    private final String hostname;
    private final int port;
    private final String hostKey;

    private final String username;
    private final String password;
    private final KeyPair privateKey;

    /**
     * Constructor.
     *
     * @param hostname hostname, required
     * @param port port, must be unsigned integer
     * @param hostKey SSH host fingerprint, required
     * @param username login username, required
     * @param password login password, required if using password authentication
     * @param privateKey login {@link KeyPair}, required if using key-based authentication
     */
    public SshAuthenticationTask(@NonNull String hostname,
                                 @NonNull int port,
                                 @NonNull String hostKey,
                                 @NonNull String username,
                                 String password,
                                 KeyPair privateKey)
    {
        this.hostname = hostname;
        this.port = port;
        this.hostKey = hostKey;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
    }

    @Override
    protected AsyncTaskResult<SSHClient> doInBackground(Void... voids) {

        final SSHClient sshClient = new SSHClient(new CustomSshJConfig());
        sshClient.addHostKeyVerifier(hostKey);
        sshClient.setConnectTimeout(SSH_CONNECT_TIMEOUT);

        try {
            sshClient.connect(hostname, port);
            if(password != null && !"".equals(password)) {
                sshClient.authPassword(username, password);
                return new AsyncTaskResult<SSHClient>(sshClient);
            }
            else
            {
                sshClient.authPublickey(username, new KeyProvider() {
                    @Override
                    public PrivateKey getPrivate() {
                        return privateKey.getPrivate();
                    }

                    @Override
                    public PublicKey getPublic() {
                        return privateKey.getPublic();
                    }

                    @Override
                    public KeyType getType() {
                        return KeyType.fromKey(getPublic());
                    }
                });
                return new AsyncTaskResult<SSHClient>(sshClient);
            }

        } catch (UserAuthException e) {
            e.printStackTrace();
            return new AsyncTaskResult<SSHClient>(e);
        } catch (TransportException e) {
            e.printStackTrace();
            return new AsyncTaskResult<SSHClient>(e);
        } catch (IOException e) {
            e.printStackTrace();
            return new AsyncTaskResult<SSHClient>(e);
        }
    }

    //If authentication failed, use Toast to notify user.
    @Override
    protected void onPostExecute(AsyncTaskResult<SSHClient> result) {

        if(result.exception != null) {
            if(SocketException.class.isAssignableFrom(result.exception.getClass())
                    || SocketTimeoutException.class.isAssignableFrom(result.exception.getClass())) {
                Toast.makeText(AppConfig.getInstance(),
                        AppConfig.getInstance().getResources().getString(R.string.ssh_connect_failed,
                                hostname, port, result.exception.getLocalizedMessage()),
                        Toast.LENGTH_LONG).show();
            }
            else if(TransportException.class.isAssignableFrom(result.exception.getClass()))
            {
                DisconnectReason disconnectReason = TransportException.class.cast(result.exception).getDisconnectReason();
                if(DisconnectReason.HOST_KEY_NOT_VERIFIABLE.equals(disconnectReason)) {
                    new AlertDialog.Builder(AppConfig.getInstance().getMainActivityContext())
                            .setTitle(R.string.ssh_connect_failed_host_key_changed_title)
                            .setMessage(R.string.ssh_connect_failed_host_key_changed_message)
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss()).show();
                }
            }
            else if(password != null) {
                Toast.makeText(AppConfig.getInstance(), R.string.ssh_authentication_failure_password, Toast.LENGTH_LONG).show();
            }
            else if(privateKey != null) {
                Toast.makeText(AppConfig.getInstance(), R.string.ssh_authentication_failure_key, Toast.LENGTH_LONG).show();
            }
        }
    }
}