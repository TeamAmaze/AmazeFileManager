package com.amaze.filemanager.filesystem.ssh;

import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_FAILED_OPS;
import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_GENERAL;
import static net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY;
import static net.schmizz.sshj.sftp.FileMode.Type.REGULAR;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amaze.filemanager.R;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import kotlin.NotImplementedError;

public class SshAmazeFilesystem extends AmazeFilesystem {
  public static final String PREFIX = "ssh:/";

  public static final SshAmazeFilesystem INSTANCE = new SshAmazeFilesystem();

  private static final String TAG = SshAmazeFilesystem.class.getSimpleName();

  static {
    AmazeFile.addFilesystem(INSTANCE);
  }

  private SshAmazeFilesystem() {}

  @Override
  public String getPrefix() {
    return PREFIX;
  }

  @NonNull
  @Override
  public String normalize(@NonNull String path) {
    return PREFIX + new File(removePrefix(path)).getAbsolutePath();
  }

  @NonNull
  @Override
  public String getHumanReadablePath(@NonNull AmazeFile f) {
    SshConnectionPool.ConnectionInfo connInfo = new SshConnectionPool.ConnectionInfo(f.getPath());
    return connInfo.toString();
  }

  @NonNull
  @Override
  public String resolve(@NotNull String parent, @NotNull String child) {
    return PREFIX + new File(removePrefix(parent), child).getAbsolutePath();
  }

  @NonNull
  @Override
  public String getDefaultParent() {
    return PREFIX + "/";
  }

  @Override
  public boolean isAbsolute(@NotNull AmazeFile f) {
    return true; // Always absolute
  }

  @NonNull
  @Override
  public String resolve(AmazeFile f) {
    return f.getPath(); // No relative paths
  }

  @NonNull
  @Override
  public String canonicalize(String path) throws IOException {
    final String returnValue =
            SshClientUtils.execute(
                    new SFtpClientTemplate<String>(path) {
                      @Override
                      public String execute(SFTPClient client) throws IOException {
                        return client.canonicalize(path);
                      }
                    });

    if (returnValue == null) {
      throw new IOException("Error canonicalizing SFTP path!");
    }

    return returnValue;
  }

  public boolean exists(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final SFtpClientTemplate<Boolean> template = new SFtpClientTemplate<Boolean>(f.getPath()) {
      @Override
      public Boolean execute(@NonNull SFTPClient client) throws IOException {
        return client.stat(SshClientUtils.extractRemotePathFrom(f.getPath())) != null;
      }
    };

    final Boolean returnValue = SshClientUtils.execute(template);

    if(returnValue == null) {
      return false;
    }

    return returnValue;
  }
  public boolean isFile(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final SFtpClientTemplate<Boolean> template = new SFtpClientTemplate<Boolean>(f.getPath()) {
      @Override
      public Boolean execute(@NonNull SFTPClient client) throws IOException {
        return client.lstat(f.getPath()).getType() == REGULAR;
      }
    };

    final Boolean returnValue = SshClientUtils.execute(template);

    if(returnValue == null) {
      return false;
    }

    return returnValue;
  }
  public boolean isDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final SFtpClientTemplate<Boolean> template = new SFtpClientTemplate<Boolean>(f.getPath()) {
      @Override
      public Boolean execute(@NonNull SFTPClient client) throws IOException {
        return client.lstat(SshClientUtils.extractRemotePathFrom(f.getPath())).getType() == DIRECTORY;
      }
    };

    final Boolean returnValue = SshClientUtils.execute(template);

    if(returnValue == null) {
      return false;
    }

    return returnValue;
  }
  public boolean isHidden(AmazeFile f) {
    return false; //Assume its not hidden
  }

  public boolean canExecute(AmazeFile f, @NonNull ContextProvider contextProvider) {
    throw new NotImplementedError();
  }
  public boolean canWrite(AmazeFile f, @NonNull ContextProvider contextProvider) {
    throw new NotImplementedError();
  }
  public boolean canRead(AmazeFile f, @NonNull ContextProvider contextProvider) {
    throw new NotImplementedError();
  }
  public boolean canAccess(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return exists(f, contextProvider);
  }

  public boolean setExecutable(AmazeFile f, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }
  public boolean setWritable(AmazeFile f, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }
  public boolean setReadable(AmazeFile f, boolean enable, boolean owneronly) {
    throw new NotImplementedError();
  }

  @Override
  public long getLastModifiedTime(AmazeFile f) {
    final Long returnValue =
            SshClientUtils.execute(
                    new SFtpClientTemplate<Long>(f.getPath()) {
                      @Override
                      public Long execute(@NonNull SFTPClient client) throws IOException {
                        return client.mtime(SshClientUtils.extractRemotePathFrom(f.getPath()));
                      }
                    });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining last modification time over SFTP");
      return 0;
    }

    return returnValue;
  }

  @NonNull
  @Override
  public String getHashMD5(@NonNull AmazeFile f, @NonNull ContextProvider contextProvider) {
    return SshClientUtils.execute(
            new SshClientSessionTemplate<String>(f.getPath()) {
              @Override
              public String execute(Session session) throws IOException {
                Session.Command cmd =
                        session.exec(
                                String.format(
                                        "md5sum -b \"%s\" | cut -c -32",
                                        SshClientUtils.extractRemotePathFrom(f.getPath())));
                String result =
                        new String(IOUtils.readFully(cmd.getInputStream()).toByteArray());
                cmd.close();
                if (cmd.getExitStatus() == 0) return result;
                else {
                  return null;
                }
              }
            });
  }

  @NonNull
  @Override
  public String getHashSHA256(@NonNull AmazeFile f, @NonNull ContextProvider contextProvider) {
    return SshClientUtils.execute(
            new SshClientSessionTemplate<String>(f.getPath()) {
              @Override
              public String execute(Session session) throws IOException {
                Session.Command cmd =
                        session.exec(
                                String.format(
                                        "sha256sum -b \"%s\" | cut -c -64",
                                        SshClientUtils.extractRemotePathFrom(f.getPath())));
                String result = IOUtils.readFully(cmd.getInputStream()).toString();
                cmd.close();
                if (cmd.getExitStatus() == 0) return result;
                else {
                  return null;
                }
              }
            });
  }

  @Override
  public long getLength(AmazeFile f, @NonNull ContextProvider contextProvider) throws IOException {
    if(f.isDirectory(contextProvider)) {
      final Long returnValue =
              SshClientUtils.<Long>execute(
                      new SFtpClientTemplate<Long>(f.getPath()) {
                        @Override
                        public Long execute(SFTPClient client) throws IOException {
                          return client.size(SshClientUtils.extractRemotePathFrom(f.getPath()));
                        }
                      });

      if (returnValue == null) {
        Log.e(TAG, "Error obtaining size of folder over SFTP");
        return 0;
      }

      return returnValue;
    }

    Long length = SshClientUtils.execute(
            new SFtpClientTemplate<Long>(f.getPath()) {
              @Override
              public Long execute(@NonNull SFTPClient client) throws IOException {
                return client.size(SshClientUtils.extractRemotePathFrom(f.getPath()));
              }
            });

    if(length == null) {
      throw new IOException("Failed getting size for ssh!");
    }

    return length;
  }

  @Override
  public boolean createFileExclusively(@NotNull String pathname) {
    return false;
  }

  @Override
  public boolean delete(AmazeFile f, @NonNull ContextProvider contextProvider) {
    Boolean retval =
            SshClientUtils.<Boolean>execute(
                    new SFtpClientTemplate(f.getPath()) {
                      @Override
                      public Boolean execute(@NonNull SFTPClient client) throws IOException {
                        String _path = SshClientUtils.extractRemotePathFrom(f.getPath());
                        if (f.isDirectory(contextProvider)) {
                          client.rmdir(_path);
                        } else {
                          client.rm(_path);
                        }
                        return client.statExistence(_path) == null;
                      }
                    });
    return retval != null && retval;
  }

  @org.jetbrains.annotations.Nullable
  @Override
  public String[] list(AmazeFile f, @NonNull ContextProvider contextProvider) {
    List<String> fileNameList = SshClientUtils.execute(
            new SFtpClientTemplate<List<String>>(f.getPath()) {
              @Override
              public List<String> execute(@NonNull SFTPClient client) {
                List<RemoteResourceInfo> infoList;
                try {
                  infoList = client.ls(SshClientUtils.extractRemotePathFrom(f.getPath()));
                } catch (IOException e) {
                  Log.w(TAG, "Failed listing files for ssh connection!", e);
                  return Collections.emptyList();
                }

                ArrayList<String> retval = new ArrayList<>(infoList.size());

                for (RemoteResourceInfo info : infoList) {
                  retval.add(resolve(f.getPath(), info.getName()));
                }

                return retval;
              }
            });

    if(fileNameList == null) {
      return null;
    }

    return fileNameList.toArray(new String[0]);
  }

  @Nullable
  @Override
  public InputStream getInputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return SshClientUtils.execute(
            new SFtpClientTemplate<InputStream>(f.getPath(), false) {
              @Override
              public InputStream execute(final SFTPClient client) throws IOException {
                final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(f.getPath()));
                return rf.new RemoteFileInputStream() {
                  @Override
                  public void close() throws IOException {
                    try {
                      super.close();
                    } finally {
                      rf.close();
                      client.close();
                    }
                  }
                };
              }
            });
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(AmazeFile f, @NonNull ContextProvider contextProvider) {
    return SshClientUtils.execute(
            new SshClientTemplate<OutputStream>(f.getPath(), false) {
              @Override
              public OutputStream execute(final SSHClient ssh) throws IOException {
                final SFTPClient client = ssh.newSFTPClient();
                final RemoteFile rf =
                        client.open(
                                SshClientUtils.extractRemotePathFrom(f.getPath()),
                                EnumSet.of(
                                        net.schmizz.sshj.sftp.OpenMode.WRITE,
                                        net.schmizz.sshj.sftp.OpenMode.CREAT));
                return rf.new RemoteFileOutputStream() {
                  @Override
                  public void close() throws IOException {
                    try {
                      super.close();
                    } finally {
                      try {
                        rf.close();
                        client.close();
                      } catch (Exception e) {
                        Log.w(TAG, "Error closing stream", e);
                      }
                    }
                  }
                };
              }
            });
  }

  @Override
  public boolean createDirectory(AmazeFile f, @NonNull ContextProvider contextProvider) {
    SshClientUtils.execute(
            new SFtpClientTemplate<Void>(f.getPath()) {
              @Override
              public Void execute(@NonNull SFTPClient client) {
                try {
                  client.mkdir(SshClientUtils.extractRemotePathFrom(f.getPath()));
                } catch (IOException e) {
                  Log.e(TAG, "Error making directory over SFTP", e);
                }
                // FIXME: anything better than throwing a null to make Rx happy?
                return null;
              }
            });
    return true;
  }

  @Override
  public boolean rename(AmazeFile file1, AmazeFile file2, @NonNull ContextProvider contextProvider) {
    @Nullable final Context context = contextProvider.getContext();

    if(context == null) {
      Log.e(TAG, "Error getting context for renaming ssh file");
      return false;
    }

    Boolean retval =
            SshClientUtils.execute(
                    new SFtpClientTemplate<Boolean>(file1.getPath()) {
                      @Override
                      public Boolean execute(@NonNull SFTPClient client) {
                        try {
                          client.rename(
                                  SshClientUtils.extractRemotePathFrom(file1.getPath()),
                                  SshClientUtils.extractRemotePathFrom(file2.getPath())
                          );
                          return true;
                        } catch (IOException e) {
                          Log.e(TAG, "Error renaming ssh file", e);
                          return false;
                        }
                      }
                    });

    if (retval == null) {
      Log.e(TAG, "Error renaming over SFTP");

      return false;
    }

    return retval;
  }

  @Override
  public boolean setLastModifiedTime(AmazeFile f, long time) {
    throw new NotImplementedError();
  }

  @Override
  public boolean setReadOnly(AmazeFile f) {
    return false;
  }

  public long getTotalSpace(AmazeFile f, @NonNull ContextProvider contextProvider) {
    final Long returnValue =
            SshClientUtils.<Long>execute(
                    new SFtpClientTemplate<Long>(f.getPath()) {
                      @Override
                      public Long execute(@NonNull SFTPClient client) throws IOException {
                        try {
                          Statvfs.Response response =
                                  new Statvfs.Response(
                                          f.getPath(),
                                          client
                                                  .getSFTPEngine()
                                                  .request(
                                                          Statvfs.request(
                                                                  client, SshClientUtils.extractRemotePathFrom(f.getPath())))
                                                  .retrieve());
                          return response.diskSize();
                        } catch (SFTPException e) {
                          Log.e(TAG, "Error querying SFTP server", e);
                          return 0L;
                        } catch (Buffer.BufferException e) {
                          Log.e(TAG, "Error parsing SFTP reply", e);
                          return 0L;
                        }
                      }
                    });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining total space over SFTP");

      return 0;
    }

    return returnValue;
  }
  public long getFreeSpace(AmazeFile f) {
    final Long returnValue =
            SshClientUtils.<Long>execute(
                    new SFtpClientTemplate<Long>(f.getPath()) {
                      @Override
                      public Long execute(@NonNull SFTPClient client) throws IOException {
                        try {
                          Statvfs.Response response =
                                  new Statvfs.Response(
                                          f.getPath(),
                                          client
                                                  .getSFTPEngine()
                                                  .request(
                                                          Statvfs.request(
                                                                  client, SshClientUtils.extractRemotePathFrom(f.getPath())))
                                                  .retrieve());
                          return response.diskFreeSpace();
                        } catch (SFTPException e) {
                          Log.e(TAG, "Error querying server", e);
                          return 0L;
                        } catch (Buffer.BufferException e) {
                          Log.e(TAG, "Error parsing reply", e);
                          return 0L;
                        }
                      }
                    });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining usable space over SFTP");
      return 0;
    }

    return returnValue;
  }
  public long getUsableSpace(AmazeFile f) {
    // TODO: Find total storage space of SFTP support is added
    throw new NotImplementedError();
  }
}
