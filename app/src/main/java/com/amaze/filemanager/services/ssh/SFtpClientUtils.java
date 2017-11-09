package com.amaze.filemanager.services.ssh;

import android.util.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;

public abstract class SFtpClientUtils
{
    private static final String TAG = "SFtpClientUtils";

    /**
     * This template pattern is borrowed from Spring Framework, to simplify code on operations
     * using SftpClientTemplate.
     *
     * @param template SftpClientTemplate to execute
     * @param <T> Type of return value
     * @return Template execution results
     */
    public static final <T> T execute(SFtpClientTemplate<T> template)
    {
        SSHClient ssh = null;
        SFTPClient client = null;
        T retval = null;
        try
        {
            ssh = SshConnectionPool.getInstance().getConnection(template.url);
            client = ssh.newSFTPClient();
            retval = template.execute(client);
        }
        catch(IOException e)
        {
            Log.e(TAG, "Error executing template method", e);
        }
        finally
        {
            if(client != null)
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
