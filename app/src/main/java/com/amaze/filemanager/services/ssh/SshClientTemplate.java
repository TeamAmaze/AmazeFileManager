package com.amaze.filemanager.services.ssh;

import android.support.annotation.NonNull;

import net.schmizz.sshj.SSHClient;

import java.io.IOException;

public abstract class SshClientTemplate<T>
{
    public final String url;

    public final boolean closeClientOnFinish;

    public SshClientTemplate(@NonNull String url)
    {
        this(url, true);
    }

    public SshClientTemplate(@NonNull String url, boolean closeClientOnFinish)
    {
        this.url = url;
        this.closeClientOnFinish = closeClientOnFinish;
    }

    public abstract T execute(@NonNull SSHClient client) throws IOException;
}
