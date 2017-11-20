package com.amaze.filemanager.services.ssh;

import android.support.annotation.NonNull;

import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.IOException;

public abstract class SshClientSessionTemplate<T>
{
    public final String url;

    public SshClientSessionTemplate(@NonNull String url)
    {
        this.url = url;
    }

    public abstract T execute(@NonNull Session sshClientSession) throws IOException;
}
