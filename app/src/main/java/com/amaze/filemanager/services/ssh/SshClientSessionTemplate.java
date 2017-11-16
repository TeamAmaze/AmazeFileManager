package com.amaze.filemanager.services.ssh;

import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.IOException;

public abstract class SshClientSessionTemplate<T>
{
    public final String url;

    public SshClientSessionTemplate(String url)
    {
        this.url = url;
    }

    public abstract T execute(Session sshClientSession) throws IOException;
}
