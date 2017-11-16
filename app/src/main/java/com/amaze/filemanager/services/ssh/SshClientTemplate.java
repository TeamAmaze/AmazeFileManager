package com.amaze.filemanager.services.ssh;

import net.schmizz.sshj.SSHClient;

import java.io.IOException;

public abstract class SshClientTemplate<T>
{
    public final String url;

    public final boolean closeClientOnFinish;

    public SshClientTemplate(String url)
    {
        this(url, true);
    }

    public SshClientTemplate(String url, boolean closeClientOnFinish)
    {
        this.url = url;
        this.closeClientOnFinish = closeClientOnFinish;
    }

    public abstract T execute(SSHClient client) throws IOException;
}
