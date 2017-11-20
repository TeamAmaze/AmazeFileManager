package com.amaze.filemanager.services.ssh;

import android.support.annotation.NonNull;

import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;

public abstract class SFtpClientTemplate<T>
{
    public final String url;

    public final boolean closeClientOnFinish;

    public SFtpClientTemplate(@NonNull String url)
    {
        this(url, true);
    }

    public SFtpClientTemplate(@NonNull String url, boolean closeClientOnFinish)
    {
        this.url = url;
        this.closeClientOnFinish = closeClientOnFinish;
    }

    public abstract T execute(@NonNull SFTPClient client) throws IOException;
}
