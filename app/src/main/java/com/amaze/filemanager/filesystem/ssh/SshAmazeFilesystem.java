package com.amaze.filemanager.filesystem.ssh;

import static net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY;
import static net.schmizz.sshj.sftp.FileMode.Type.REGULAR;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFile;
import com.amaze.filemanager.file_operations.filesystem.filetypes.AmazeFilesystem;
import com.amaze.filemanager.file_operations.filesystem.filetypes.ContextProvider;
import com.amaze.filemanager.filesystem.HybridFileParcelable;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import jcifs.SmbConstants;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
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
  public String resolve(String parent, String child) {
    return PREFIX + new File(removePrefix(parent), child).getAbsolutePath();
  }

  @NonNull
  @Override
  public String getDefaultParent() {
    return PREFIX + "/";
  }

  @Override
  public boolean isAbsolute(AmazeFile f) {
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

  @Override
  public int getBooleanAttributes(AmazeFile f) {
    final SFtpClientTemplate<Integer> template = new SFtpClientTemplate<Integer>(f.getPath()) {
      @Override
      public Integer execute(@NonNull SFTPClient client) throws IOException {
        Integer r = 0;

        if (client.statExistence(f.getPath()) != null) {
          r |= BA_EXISTS;

          if (client.lstat(f.getPath()).getType() == REGULAR) {
            r |= BA_REGULAR;
          }

          if (client.lstat(f.getPath()).getType() == DIRECTORY) {
            r |= BA_DIRECTORY;
          }

          //Assume its not hidden
        }

        return r;
      }
    };

    final Integer returnValue = SshClientUtils.execute(template);

    if(returnValue == null) {
      return 0;
    }

    return returnValue;
  }

  @Override
  public boolean checkAccess(AmazeFile f, int access) {
    throw new NotImplementedError();
  }

  @Override
  public boolean setPermission(AmazeFile f, int access, boolean enable, boolean owneronly) {
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

  @Override
  public long getLength(AmazeFile f, @NonNull ContextProvider contextProvider) throws IOException {
    if(f.isDirectory()) {
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
  public boolean createFileExclusively(String pathname) throws IOException {
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
                        if (f.isDirectory()) {
                          client.rmdir(_path);
                        } else {
                          client.rm(_path);
                        }
                        return client.statExistence(_path) == null;
                      }
                    });
    return retval != null && retval;
  }

  @Nullable
  @Override
  public String[] list(AmazeFile f) {
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
  public boolean rename(AmazeFile f1, AmazeFile f2) {
    Boolean retval =
            SshClientUtils.<Boolean>execute(
                    new SFtpClientTemplate(f1.getPath()) {
                      @Override
                      public Boolean execute(@NonNull SFTPClient client) throws IOException {
                        try {
                          client.rename(f1.getPath(), f2.getPath());
                          return true;
                        } catch (IOException e) {
                          Log.e(TAG, "Error renaming over SFTP", e);
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

  @Override
  public AmazeFile[] listRoots() {
    return new AmazeFile[] { new AmazeFile(getDefaultParent()) };
  }

  @Override
  public long getSpace(AmazeFile f, int t) {
    switch (t) {
      case SPACE_TOTAL: {
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
      case SPACE_FREE: {
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
      case SPACE_USABLE:
        // TODO: Find total storage space of SFTP support is added
        throw new NotImplementedError();
      default:
        throw new IllegalStateException();
    }
  }
}
