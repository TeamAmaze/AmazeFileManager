package com.amaze.filemanager.services.ssh;

import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;

public abstract class SFtpClientTemplate<T>
{
    public final String url;

    public SFtpClientTemplate(String url)
    {
        this.url = url;
    }

    public abstract T execute(SFTPClient client) throws IOException;
}
