package com.amaze.filemanager.services.ssh;

import android.support.annotation.NonNull;
import android.util.Log;

import com.amaze.filemanager.exceptions.CryptException;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.SmbUtil;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;

import static com.amaze.filemanager.services.ssh.SshConnectionPool.SSH_URI_PREFIX;

public abstract class SshClientUtils
{
    private static final String TAG = "SshClientUtils";

    public static final <T> T execute(@NonNull SshClientTemplate<T> template)
    {
        SSHClient client = null;
        T retval = null;
        try
        {
            client = SshConnectionPool.getInstance().getConnection(template.url);
            retval = template.execute(client);
        }
        catch(IOException e)
        {
            Log.e(TAG, "Error executing template method", e);
        }
        finally
        {
            if(client != null && template.closeClientOnFinish)
            {
                try
                {
                    client.close();
                }
                catch(IOException e)
                {
                    Log.w(TAG, "Error closing SFTP client", e);
                }
            }
        }
        return retval;
    }

    public static final <T> T execute(@NonNull final SshClientSessionTemplate<T> template)
    {
        return execute(new SshClientTemplate<T>(template.url, false) {
            @Override
            public T execute(SSHClient client) throws IOException
            {
                Session session = null;
                T retval = null;
                try
                {
                    session = client.startSession();
                    retval = template.execute(session);
                }
                catch(IOException e)
                {
                    Log.e(TAG, "Error executing template method", e);
                }
                finally
                {
                    if(session != null && session.isOpen())
                    {
                        try
                        {
                            session.close();
                        }
                        catch(IOException e)
                        {
                            Log.w(TAG, "Error closing SFTP client", e);
                        }
                    }
                }
                return retval;
            }
        });
    }

    /**
     * This template pattern is borrowed from Spring Framework, to simplify code on operations
     * using SftpClientTemplate.
     *
     * @param template SftpClientTemplate to execute
     * @param <T> Type of return value
     * @return Template execution results
     */
    public static final <T> T execute(@NonNull final SFtpClientTemplate<T> template)
    {
        return execute(new SshClientTemplate<T>(template.url, false) {
            @Override
            public T execute(SSHClient client) throws IOException
            {
                SFTPClient sftpClient = null;
                T retval = null;
                try
                {
                    sftpClient = client.newSFTPClient();
                    retval = template.execute(sftpClient);
                }
                catch(IOException e)
                {
                    Log.e(TAG, "Error executing template method", e);
                }
                finally
                {
                    if(sftpClient != null && template.closeClientOnFinish)
                    {
                        try
                        {
                            sftpClient.close();
                        }
                        catch(IOException e)
                        {
                            Log.w(TAG, "Error closing SFTP client", e);
                        }
                    }
                }
                return retval;
            }
        });
    }

    public static final String encryptSshPathAsNecessary(@NonNull String fullUri)
    {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length(), fullUri.indexOf('@'));
        try
        {
            return (uriWithoutProtocol.indexOf(':') > 0) ?
                    SmbUtil.getSmbEncryptedPath(AppConfig.getInstance(), fullUri) :
                    fullUri;
        }
        catch(CryptException e){
            Log.e(TAG, "Error encrypting path", e);
            return fullUri;
        }
    }

    public static final String decryptSshPathAsNecessary(@NonNull String fullUri)
    {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length(), fullUri.indexOf('@'));
        try
        {
            return (uriWithoutProtocol.indexOf(':') > 0) ?
                    SmbUtil.getSmbDecryptedPath(AppConfig.getInstance(), fullUri) :
                    fullUri;
        }
        catch(CryptException e){
            Log.e(TAG, "Error decrypting path", e);
            return fullUri;
        }
    }

    public static final String extractBaseUriFrom(@NonNull String fullUri)
    {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length());
        return uriWithoutProtocol.indexOf('/') == -1 ? fullUri : fullUri.substring(0, uriWithoutProtocol.indexOf('/') + SSH_URI_PREFIX.length());
    }

    public static final String extractRemotePathFrom(@NonNull String fullUri)
    {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length());
        return uriWithoutProtocol.indexOf('/') == -1 ? "/" : uriWithoutProtocol.substring(uriWithoutProtocol.indexOf('/'));
    }

    public static final void tryDisconnect(SSHClient client)
    {
        if(client != null && client.isConnected()){
            try {
                client.disconnect();
            } catch (IOException e) {
                Log.w(TAG, "Error closing SSHClient connection", e);
            }
        }
    }
}
