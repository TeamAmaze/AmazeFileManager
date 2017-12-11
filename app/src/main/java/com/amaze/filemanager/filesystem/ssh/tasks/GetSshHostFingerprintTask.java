/*
 * VerifyHostKeyTask.java
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

package com.amaze.filemanager.filesystem.ssh.tasks;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.application.AppConfig;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.PublicKey;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link AsyncTask} to obtain SSH host fingerprint.
 *
 * It works by adding a {@link HostKeyVerifier} that accepts all SSH host keys, then obtain the
 * key shown by server, and return to the task's caller.
 *
 * {@link Semaphore} with {@link AtomicReference} combo is used to ensure SSH host key is obtained
 * successfully on returning to the task caller.
 *
 * Mainly used by {@link com.amaze.filemanager.ui.dialogs.SftpConnectDialog} on saving SSH
 * connection settings.
 *
 * @see HostKeyVerifier
 * @see SSHClient#addHostKeyVerifier(String)
 * @see com.amaze.filemanager.ui.dialogs.SftpConnectDialog#onCreateDialog(Bundle)
 */

public class GetSshHostFingerprintTask extends AsyncTask<Void, Void, AsyncTaskResult<PublicKey>>
{
    //We only looking for the host key, so we allow a smaller timeout than SshAuthenticationTask
    private static final int SSH_CONNECT_TIMEOUT = 5000;

    private final String mHostname;
    private final int mPort;

    public GetSshHostFingerprintTask(@NonNull String hostname, int port) {
        this.mHostname = hostname;
        this.mPort = port;
    }

    @Override
    protected AsyncTaskResult<PublicKey> doInBackground(Void... voids) {

        final AtomicReference<AsyncTaskResult<PublicKey>> holder = new AtomicReference<AsyncTaskResult<PublicKey>>();
        final Semaphore semaphore = new Semaphore(0);
        final SSHClient sshClient = new SSHClient(new CustomSshJConfig());
        sshClient.setConnectTimeout(SSH_CONNECT_TIMEOUT);
        sshClient.addHostKeyVerifier(new HostKeyVerifier() {
            @Override
            public boolean verify(String hostname, int port, PublicKey key) {
                holder.set(new AsyncTaskResult<PublicKey>(key));
                semaphore.release();
                return true;
            }
        });

        try {
            sshClient.connect(mHostname, mPort);
            semaphore.acquire();
        } catch(IOException e) {
            e.printStackTrace();
            holder.set(new AsyncTaskResult<PublicKey>(e));
            semaphore.release();
        } catch(InterruptedException e) {
            e.printStackTrace();
            holder.set(new AsyncTaskResult<PublicKey>(e));
            semaphore.release();
        }
        finally {
            SshClientUtils.tryDisconnect(sshClient);
            return holder.get();
        }
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<PublicKey> result) {
        if(result.exception != null) {
            if(SocketException.class.isAssignableFrom(result.exception.getClass())
                    || SocketTimeoutException.class.isAssignableFrom(result.exception.getClass())) {
                Toast.makeText(AppConfig.getInstance(),
                        String.format(AppConfig.getInstance().getResources().getString(R.string.ssh_connect_failed),
                                mHostname, mPort, result.exception.getLocalizedMessage()),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
