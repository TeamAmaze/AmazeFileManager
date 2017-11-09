package com.amaze.filemanager.services.ssh.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.amaze.filemanager.services.ssh.SFtpClientUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;

import java.io.IOException;

public class SshAuthenticationTask extends AsyncTask<Void, Void, Boolean>
{
    private final String hostname;
    private final int port;
    private final String hostKey;

    private final String username;
    private final String password;

    public SshAuthenticationTask(String hostname, int port, String hostKey, String username, String password)
    {
        this.hostname = hostname;
        this.port = port;
        this.hostKey = hostKey;
        this.username = username;
        this.password = password;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        final SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(hostKey);

        try {
            sshClient.connect(hostname, port);
            if(password != null && !"".equals(password))
            {
                sshClient.authPassword(username, password);
                return true;
            }
            else
            {
                //Perform key-based authentication
                Log.i("DEBUG", "Do key-based auth");
            }

        } catch (UserAuthException e) {
            e.printStackTrace();
            return false;
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SFtpClientUtils.tryDisconnect(sshClient);
        }

        return false;
    }
}