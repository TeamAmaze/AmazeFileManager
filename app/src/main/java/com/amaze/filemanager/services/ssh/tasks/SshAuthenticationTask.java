package com.amaze.filemanager.services.ssh.tasks;

import android.os.AsyncTask;
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

import org.bouncycastle.openssl.PEMException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class SshAuthenticationTask extends AsyncTask<Void, Void, Boolean>
{
    private final String mHostname;
    private final int mPort;
    private final String mHostKey;

    private final String mUsername;
    private final String mPassword;
    private final KeyPair mPrivateKey;

    public SshAuthenticationTask(String hostname,
                                 int port,
                                 String hostKey,
                                 String username,
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
    protected Boolean doInBackground(Void... voids) {

        final SSHClient sshClient = new SSHClient(new CustomSshJConfig());
        sshClient.addHostKeyVerifier(mHostKey);

        try {
            sshClient.connect(mHostname, mPort);
            if(mPassword != null && !"".equals(mPassword))
            {
                sshClient.authPassword(mUsername, mPassword);
                return true;
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

    @Override
    protected void onPostExecute(Boolean result) {
        if(!result)
        {
            if(mPassword != null)
                Toast.makeText(AppConfig.getInstance(), R.string.ssh_authentication_failure_password, Toast.LENGTH_LONG).show();
            if(mPrivateKey != null)
                Toast.makeText(AppConfig.getInstance(), R.string.ssh_authentication_failure_key, Toast.LENGTH_LONG).show();
        }
    }
}