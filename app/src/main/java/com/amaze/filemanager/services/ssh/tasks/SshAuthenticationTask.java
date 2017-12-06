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

package com.amaze.filemanager.services.ssh.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.services.ssh.CustomSshJConfig;
import com.amaze.filemanager.services.ssh.SshClientUtils;
import com.amaze.filemanager.utils.AppConfig;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.IOException;
import java.net.ConnectException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * {@link AsyncTask} for authenticating with SSH server to verify if parameters are correct.
 *
 * Used by {@link com.amaze.filemanager.ui.dialogs.SftpConnectDialog}.
 *
 * @see SSHClient
 * @see SSHClient#authPassword(String, String)
 * @see SSHClient#authPublickey(String, KeyProvider...)
 * @see com.amaze.filemanager.ui.dialogs.SftpConnectDialog#authenticateAndSaveSetup(String, String, int, String, String, String, String, KeyPair)
 */
public class SshAuthenticationTask extends AsyncTask<Void, Void, AsyncTaskResult<SSHClient>>
{
    private final String mHostname;
    private final int mPort;
    private final String mHostKey;

    private final String mUsername;
    private final String mPassword;
    private final KeyPair mPrivateKey;

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
        this.mHostname = hostname;
        this.mPort = port;
        this.mHostKey = hostKey;
        this.mUsername = username;
        this.mPassword = password;
        this.mPrivateKey = privateKey;
    }

    @Override
    protected AsyncTaskResult<SSHClient> doInBackground(Void... voids) {

        final SSHClient sshClient = new SSHClient(new CustomSshJConfig());
        sshClient.addHostKeyVerifier(mHostKey);

        try {
            sshClient.connect(mHostname, mPort);
            if(mPassword != null && !"".equals(mPassword)) {
                sshClient.authPassword(mUsername, mPassword);
                return new AsyncTaskResult<SSHClient>(sshClient);
            }
            else
            {
                sshClient.authPublickey(mUsername, new KeyProvider() {
                    @Override
                    public PrivateKey getPrivate() throws IOException {
                        return mPrivateKey.getPrivate();
                    }

                    @Override
                    public PublicKey getPublic() throws IOException {
                        return mPrivateKey.getPublic();
                    }

                    @Override
                    public KeyType getType() throws IOException {
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
        } finally {
            SshClientUtils.tryDisconnect(sshClient);
        }
    }

    //If authentication failed, use Toast to notify user.
    @Override
    protected void onPostExecute(AsyncTaskResult<SSHClient> result) {

        if(result.getException() != null && ConnectException.class.isAssignableFrom(result.getException().getClass())) {
            Toast.makeText(AppConfig.getInstance(),
                    String.format(AppConfig.getInstance().getResources().getString(R.string.ssh_connect_failed),
                            mHostname, mPort, result.getException().getLocalizedMessage()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        if(mPassword != null) {
            Toast.makeText(AppConfig.getInstance(), R.string.ssh_authentication_failure_password, Toast.LENGTH_LONG).show();
            return;
        }
        if(mPrivateKey != null) {
            Toast.makeText(AppConfig.getInstance(), R.string.ssh_authentication_failure_key, Toast.LENGTH_LONG).show();
            return;
        }
    }
}