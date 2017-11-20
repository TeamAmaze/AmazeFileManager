package com.amaze.filemanager.services.ssh.tasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amaze.filemanager.services.ssh.CustomSshJConfig;
import com.amaze.filemanager.services.ssh.SshClientUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import java.io.IOException;
import java.security.PublicKey;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class VerifyHostKeyTask extends AsyncTask<Void, Void, PublicKey>
{
    final String hostname;
    final int port;

    public VerifyHostKeyTask(@NonNull String hostname, int port)
    {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    protected PublicKey doInBackground(Void... voids) {

        final AtomicReference<PublicKey> holder = new AtomicReference<PublicKey>();
        final Semaphore semaphore = new Semaphore(0);
        final SSHClient sshClient = new SSHClient(new CustomSshJConfig());
        sshClient.addHostKeyVerifier(new HostKeyVerifier() {
            @Override
            public boolean verify(String hostname, int port, PublicKey key) {
                holder.set(key);
                Log.d("DEBUG", SecurityUtils.getFingerprint(key));
                Log.d("DEBUG", key.getAlgorithm());
                semaphore.release();
                return true;
            }
        });

        try {
            sshClient.connect(hostname, port);
            semaphore.acquire();
        } catch(IOException e) {
            holder.set(null);
            semaphore.release();
        } catch(InterruptedException e) {
            holder.set(null);
            semaphore.release();
        }
        finally {
            SshClientUtils.tryDisconnect(sshClient);
            return holder.get();
        }
    }
}
