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

package com.amaze.filemanager.services.ssh;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.utils.AppConfig;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final String TAG = "SshConnectionPool";

    private static SshConnectionPool sInstance = null;

    private final Map<String, SSHClient> mConnections;

    private SshConnectionPool()
    {
        mConnections = new ConcurrentHashMap<String, SSHClient>();
    }

    /**
     * Use this to obtain SshConnectionPool instance singleton.
     *
     * @return {@link SshConnectionPool} instance
     */
    public static final SshConnectionPool getInstance() {
        if(sInstance == null)
            sInstance = new SshConnectionPool();

        return sInstance;
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
        Log.d(TAG, "Opening connection for " + url);

        SSHClient client = mConnections.get(url);
        if(client == null) {
            client = create(url);
        } else {
            if(!validate(client)) {
                Log.d(TAG, "Connection no longer usable. Reconnecting...");
                expire(client);
                mConnections.remove(url);
                client = create(url);
            }
        }
        mConnections.put(url, client);
        return client;
    }

    /**
     * Kill any connection that is still in place. Used by {@link com.amaze.filemanager.activities.MainActivity}.
     *
     * @see MainActivity#onDestroy()
     * @see MainActivity#exit()
     */
    public void expungeAllConnections() {
        if(!mConnections.isEmpty()) {
            for(SSHClient connection : mConnections.values()) {
                SshClientUtils.tryDisconnect(connection);
            }
            mConnections.clear();
        }
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

        SSHClient client = new SSHClient(new CustomSshJConfig());
        UtilsHandler utilsHandler = AppConfig.getInstance().getUtilsHandler();
        client.addHostKeyVerifier(utilsHandler.getSshHostKey(uri.toString()));
        client.connect(host, port);
        if(password != null)
            client.authPassword(username, password);
        else
            client.authPublickey(username, createKeyProviderFrom(utilsHandler.getSshAuthPrivateKey(uri.toString())));
        return client;
    }

    private boolean validate(@NonNull SSHClient client) {
        return client.isConnected() && client.isAuthenticated();
    }

    private void expire(@NonNull SSHClient client) {
        SshClientUtils.tryDisconnect(client);
    }

    //Create the KeyProvider object with given PEM contents required by sshj
    private KeyProvider createKeyProviderFrom(@NonNull String pemContents) throws IOException {
        Reader reader = new StringReader(pemContents);
        PEMParser pemParser = new PEMParser(reader);

        PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        final KeyPair retval = converter.getKeyPair(keyPair);

        return new KeyProvider() {
            @Override
            public PrivateKey getPrivate() throws IOException {
                return retval.getPrivate();
            }

            @Override
            public PublicKey getPublic() throws IOException {
                return retval.getPublic();
            }

            @Override
            public KeyType getType() throws IOException {
                return KeyType.fromKey(getPublic());
            }
        };
    }
}
