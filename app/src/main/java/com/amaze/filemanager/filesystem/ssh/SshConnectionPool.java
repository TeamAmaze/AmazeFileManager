/*
 * SshConnectionPool.java
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

package com.amaze.filemanager.filesystem.ssh;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult;
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairTask;
import com.amaze.filemanager.asynchronous.asynctasks.ssh.SshAuthenticationTask;
import com.amaze.filemanager.utils.application.AppConfig;

import net.schmizz.sshj.SSHClient;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Poor man's implementation of SSH connection pool.
 *
 * It uses a {@link ConcurrentHashMap} to hold the opened SSH connections; all code that uses
 * {@link SSHClient} can ask for connection here with <code>getConnection(url)</code>.
 */
public class SshConnectionPool
{
    public static final int SSH_DEFAULT_PORT = 22;

    public static final String SSH_URI_PREFIX = "ssh://";

    public static final int SSH_CONNECT_TIMEOUT = 30000;

    private static final String TAG = "SshConnectionPool";

    private static SshConnectionPool instance = null;

    private final Map<String, SSHClient> connections;

    private SshConnectionPool()
    {
        connections = new ConcurrentHashMap<String, SSHClient>();
    }

    /**
     * Use this to obtain SshConnectionPool instance singleton.
     *
     * @return {@link SshConnectionPool} instance
     */
    public static final SshConnectionPool getInstance() {
        if(instance == null)
            instance = new SshConnectionPool();

        return instance;
    }

    /**
     * Obtain a {@link SSHClient} connection from the underlying connection pool.
     *
     * Beneath it will return the connection if it exists; otherwise it will create a new one and
     * put it into the connection pool.
     *
     * @param url SSH connection URL, in the form of <code>ssh://&lt;username&gt;:&lt;password&gt;@&lt;host&gt;:&lt;port&gt;</code> or <code>ssh://&lt;username&gt;@&lt;host&gt;:&lt;port&gt;</code>
     * @return {@link SSHClient} connection, already opened and authenticated
     * @throws IOException IOExceptions that occur during connection setup
     */
    public SSHClient getConnection(@NonNull String url) throws IOException {
        url = SshClientUtils.extractBaseUriFrom(url);

        SSHClient client = connections.get(url);
        if(client == null) {
            client = create(url);
            if(client != null)
                connections.put(url, client);
        } else {
            if(!validate(client)) {
                Log.d(TAG, "Connection no longer usable. Reconnecting...");
                expire(client);
                connections.remove(url);
                client = create(url);
                if(client != null)
                    connections.put(url, client);
            }
        }
        return client;
    }

    /**
     * Kill any connection that is still in place. Used by {@link com.amaze.filemanager.activities.MainActivity}.
     *
     * @see MainActivity#onDestroy()
     * @see MainActivity#exit()
     */
    public void expungeAllConnections() {
        AppConfig.runInBackground(() -> {
            if(!connections.isEmpty()) {
                for (SSHClient connection : connections.values()) {
                    SshClientUtils.tryDisconnect(connection);
                }
                connections.clear();
            }
        });
    }

    private SSHClient create(@NonNull String url) throws IOException {
        return create(Uri.parse(url));
    }

    // Logic for creating SSH connection. Depends on password existence in given Uri password or
    // key-based authentication
    private SSHClient create(@NonNull Uri uri) throws IOException {
        String host = uri.getHost();
        int port = uri.getPort();
        //If the uri is fetched from the app's database storage, we assume it will never be empty
        String[] userInfo = uri.getUserInfo().split(":");
        String username = userInfo[0];
        String password = userInfo.length > 1 ? userInfo[1] : null;

        if(port < 0)
            port = SSH_DEFAULT_PORT;

        UtilsHandler utilsHandler = AppConfig.getInstance().getUtilsHandler();
        try {
            String pem = utilsHandler.getSshAuthPrivateKey(uri.toString());
            AtomicReference<KeyPair> keyPair = new AtomicReference<>(null);
            if(pem != null && !"".equals(pem)) {
                CountDownLatch latch = new CountDownLatch(1);
                new PemToKeyPairTask(pem, result -> {
                    if(result.result != null){
                        keyPair.set(result.result);
                        latch.countDown();
                    }
                }).execute();
                latch.await();
            }
            AsyncTaskResult<SSHClient> taskResult = new SshAuthenticationTask(host, port,
                    utilsHandler.getSshHostKey(uri.toString()),
                    username, password, keyPair.get()).execute().get();

            SSHClient client = taskResult.result;
            return client;
        } catch(InterruptedException e) {
            //FIXME: proper handling
            throw new RuntimeException(e);
        } catch(ExecutionException e) {
            //FIXME: proper handling
            throw new RuntimeException(e);
        }
    }

    private boolean validate(@NonNull SSHClient client) {
        return client.isConnected() && client.isAuthenticated();
    }

    private void expire(@NonNull SSHClient client) {
        SshClientUtils.tryDisconnect(client);
    }
}
