package com.amaze.filemanager.filesystem.file_types;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.filesystem.ssh.Statvfs;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.application.AppConfig;

import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

import java.io.IOException;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;


/**
 * Created by Rustam Khadipash on 6/5/2018.
 */
public class SFTPFile extends HybridFile {
    private String path;
    final private OpenMode mode = OpenMode.SFTP;
    private static final String TAG = "SFTPFile";

    public SFTPFile(OpenMode mode, String path) {
        super(mode, path);
        this.path = path;
    }

    public SFTPFile(OpenMode mode, String path, String name, boolean isDirectory) {
        super(mode, path, name, isDirectory);
        this.path = path;
    }

    @Override
    public long lastModified() {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Long execute(SFTPClient client) throws IOException {
                return client.mtime(SshClientUtils.extractRemotePathFrom(path));
            }
        });
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

    @Override
    public long length(Context context) {
        return ((HybridFileParcelable)((HybridFile)this)).getSize();
    }

    @Override
    public boolean isDirectory() {
        return isDirectory(AppConfig.getInstance());
    }

    @Override
    public boolean isDirectory(Context context) {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Boolean execute(SFTPClient client) throws IOException {
                try {
                    return client.stat(SshClientUtils.extractRemotePathFrom(path)).getType()
                            .equals(FileMode.Type.DIRECTORY);
                } catch (SFTPException notFound){
                    return false;
                }
            }
        });
    }

    @Override
    public long folderSize() {
        return folderSize(AppConfig.getInstance());
    }

    @Override
    public long folderSize(Context context) {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Long execute(SFTPClient client) throws IOException {
                return client.size(SshClientUtils.extractRemotePathFrom(path));
            }
        });
    }

    @Override
    public long getUsableSpace() {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Long execute(@NonNull SFTPClient client) throws IOException {
                try {
                    Statvfs.Response response = new Statvfs.Response(path,
                            client.getSFTPEngine().request(Statvfs.request(client, SshClientUtils.extractRemotePathFrom(path))).retrieve());
                    return response.diskFreeSpace();
                } catch (SFTPException e) {
                    Log.e(TAG, "Error querying server", e);
                } catch (Buffer.BufferException e) {
                    Log.e(TAG, "Error parsing reply", e);
                }
                return 0L;
            }
        });
    }

    @Override
    public long getTotal(Context context) {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Long execute(@NonNull SFTPClient client) throws IOException {
                try {
                    Statvfs.Response response = new Statvfs.Response(path,
                            client.getSFTPEngine().request(Statvfs.request(client, SshClientUtils.extractRemotePathFrom(path))).retrieve());
                    return response.diskSize();
                } catch (SFTPException e) {
                    Log.e(TAG, "Error querying server", e);
                } catch (Buffer.BufferException e) {
                    Log.e(TAG, "Error parsing reply", e);
                }
                return 0L;
            }
        });
    }

    @Override
    public String getReadablePath(String path) {
        if (path.contains("@"))
            return "ssh://" + path.substring(path.indexOf("@") + 1, path.length());
        else return path;
    }

    @Override
    public boolean exists() {
        return SshClientUtils.execute(new SFtpClientTemplate(path) {
            @Override
            public Boolean execute(SFTPClient client) throws IOException {
                try {
                    return client.stat(SshClientUtils.extractRemotePathFrom(path)) != null;
                } catch (SFTPException notFound){
                    return false;
                }
            }
        });
    }
}
