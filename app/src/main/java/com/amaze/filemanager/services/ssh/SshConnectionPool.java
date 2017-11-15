package com.amaze.filemanager.services.ssh;

import android.net.Uri;
import android.util.Log;

import net.schmizz.sshj.SSHClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SshConnectionPool
{
    public static final int SSH_DEFAULT_PORT = 22;

    private static final String TAG = "SshConnectionPool";

    private static SshConnectionPool instance = null;

    private final Map<String, SSHClient> connections;

    private SshConnectionPool()
    {
        connections = new ConcurrentHashMap<String, SSHClient>();
    }

    public static final SshConnectionPool getInstance()
    {
        if(instance == null)
            instance = new SshConnectionPool();

        return instance;
    }

    public SSHClient getConnection(String url, String hostKey) throws IOException
    {
        SSHClient client = connections.get(url);
        if(client == null)
        {
            client = create(url, hostKey);
        }
        else
        {
            if(!validate(client))
            {
                Log.d(TAG, "Connection no longer usable. Reconnecting...");
                expire(client);
                connections.remove(url);
                client = create(url, hostKey);
            }
        }
        Log.d(TAG, client.toString());
        connections.put(url, client);
        return client;
    }

    private SSHClient create(String url, String hostKey) throws IOException
    {
        return create(Uri.parse(url), hostKey);
    }

    private SSHClient create(Uri uri, String hostKey) throws IOException
    {
        Log.d(TAG, "Opening connection for " + uri.toString());

        String host = uri.getHost();
        int port = uri.getPort();
        //If the uri is fetched from the app's database storage, we assume it will never be empty
        String[] userInfo = uri.getUserInfo().split(":");
        String username = userInfo[0];
        String password = userInfo.length > 1 ? userInfo[1] : "";

        if(port < 0)
            port = SSH_DEFAULT_PORT;

        SSHClient client = new SSHClient(new CustomSshJConfig());
        client.addHostKeyVerifier(hostKey);
        client.connect(host, port);
        client.authPassword(username, password);
        return client;
    }

    private boolean validate(SSHClient client)
    {
        return client.isConnected() && client.isAuthenticated();
    }

    private void expire(SSHClient client)
    {
        try
        {
            client.disconnect();
        }
        catch(IOException e)
        {
            Log.w(TAG, e);
        }
    }
}
