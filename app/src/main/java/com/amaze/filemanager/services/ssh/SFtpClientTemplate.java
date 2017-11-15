package com.amaze.filemanager.services.ssh;

import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;

public abstract class SFtpClientTemplate<T>
{
    public final String url;

    public final String hostKey;

    public SFtpClientTemplate(String url, String hostKey)
    {
        this.url = url;
        this.hostKey = hostKey;
    }

    public abstract T execute(SFTPClient client) throws IOException;
}
