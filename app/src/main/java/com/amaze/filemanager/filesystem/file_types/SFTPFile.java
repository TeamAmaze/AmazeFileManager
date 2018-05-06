package com.amaze.filemanager.filesystem.file_types;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.utils.OpenMode;

import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class SFTPFile extends HybridFile {
    private String path;

    public SFTPFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public SFTPFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long length() {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Long execute(SFTPClient client) throws IOException {
                return client.size(SshClientUtils.extractRemotePathFrom(path));
            }
        });
    }
}
