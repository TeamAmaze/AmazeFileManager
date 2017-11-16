package com.amaze.filemanager.services.ssh.tasks;

import android.os.AsyncTask;

import com.amaze.filemanager.services.ssh.CustomSshJConfig;
import com.amaze.filemanager.services.ssh.SshClientUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import org.bouncycastle.openssl.PEMException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SshAuthenticationTask extends AsyncTask<Void, Void, Boolean>
{
    private final String hostname;
    private final int port;
    private final String hostKey;

    private final String username;
    private final String password;
    private final KeyPair privateKey;

    public SshAuthenticationTask(String hostname, int port, String hostKey, String username, String password, KeyPair privateKey) throws PEMException
    {
        this.hostname = hostname;
        this.port = port;
        this.hostKey = hostKey;
        this.username = username;
        this.password = password;
        this.privateKey = privateKey;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        final SSHClient sshClient = new SSHClient(new CustomSshJConfig());
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
                sshClient.authPublickey(username, new KeyProvider() {
                    @Override
                    public PrivateKey getPrivate() throws IOException {
                        return privateKey.getPrivate();
                    }

                    @Override
                    public PublicKey getPublic() throws IOException {
                        return privateKey.getPublic();
                    }

                    @Override
                    public KeyType getType() throws IOException {
                        return KeyType.fromKey(getPublic());
                    }
                });
                return true;
            }

        } catch (UserAuthException e) {
            e.printStackTrace();
            return false;
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SshClientUtils.tryDisconnect(sshClient);
        }

        return false;
    }
}