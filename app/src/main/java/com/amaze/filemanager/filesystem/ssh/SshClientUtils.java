/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.ssh;

import static com.amaze.filemanager.fileoperations.filesystem.FolderStateKt.DOESNT_EXIST;
import static com.amaze.filemanager.fileoperations.filesystem.FolderStateKt.WRITABLE_ON_REMOTE;
import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_URI_PREFIX;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.fileoperations.filesystem.FolderState;
import com.amaze.filemanager.fileoperations.filesystem.cloud.CloudStreamer;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.SmbUtil;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

public abstract class SshClientUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SshClientUtils.class);

  /**
   * Execute the given SshClientTemplate.
   *
   * <p>This template pattern is borrowed from Spring Framework, to simplify code on operations
   * using SftpClientTemplate.
   *
   * @param template {@link SshClientTemplate} to execute
   * @param <T> Type of return value
   * @return Template execution results
   */
  public static <T> T execute(@NonNull SshClientTemplate<T> template) {
    SSHClient client = SshConnectionPool.INSTANCE.getConnection(extractBaseUriFrom(template.url));
    if (client == null) {
      client = SshConnectionPool.INSTANCE.getConnection(template.url);
    }
    T retval = null;
    if (client != null) {
      final SSHClient _client = client;
      try {
        retval =
            Single.fromCallable(() -> template.execute(_client))
                .subscribeOn(Schedulers.io())
                .blockingGet();
      } catch (Exception e) {
        LOG.error("Error executing template method", e);
      } finally {
        if (template.closeClientOnFinish) {
          tryDisconnect(client);
        }
      }
    }
    return retval;
  }

  /**
   * Execute the given template with SshClientTemplate.
   *
   * @param template {@link SshClientSessionTemplate} to execute
   * @param <T> Type of return value
   * @return Template execution results
   */
  public static <T> T execute(@NonNull final SshClientSessionTemplate<T> template) {
    return execute(
        new SshClientTemplate<T>(template.url, false) {
          @Override
          public T execute(SSHClient client) {
            Session session = null;
            T retval = null;
            try {
              session = client.startSession();
              retval = template.execute(session);
            } catch (IOException e) {
              LOG.error("Error executing template method", e);
            } finally {
              if (session != null && session.isOpen()) {
                try {
                  session.close();
                } catch (IOException e) {
                  LOG.warn("Error closing SFTP client", e);
                }
              }
            }
            return retval;
          }
        });
  }

  /**
   * Execute the given template with SshClientTemplate.
   *
   * @param template {@link SFtpClientTemplate} to execute
   * @param <T> Type of return value
   * @return Template execution results
   */
  @Nullable
  public static <T> T execute(@NonNull final SFtpClientTemplate<T> template) {
    final SshClientTemplate<T> sshClient =
        new SshClientTemplate<T>(template.url, false) {
          @Override
          @Nullable
          public T execute(SSHClient client) {
            SFTPClient sftpClient = null;
            T retval = null;
            try {
              sftpClient = client.newSFTPClient();
              retval = template.execute(sftpClient);
            } catch (IOException e) {
              LOG.error("Error executing template method", e);
            } finally {
              if (sftpClient != null && template.closeClientOnFinish) {
                try {
                  sftpClient.close();
                } catch (IOException e) {
                  LOG.warn("Error closing SFTP client", e);
                }
              }
            }
            return retval;
          }
        };

    return execute(sshClient);
  }

  /**
   * Convenience method to call {@link SmbUtil#getSmbEncryptedPath(Context, String)} if the given
   * SSH URL contains the password (assuming the password is encrypted).
   *
   * @param fullUri SSH URL
   * @return SSH URL with the password (if exists) encrypted
   */
  public static String encryptSshPathAsNecessary(@NonNull String fullUri) {
    String uriWithoutProtocol =
        fullUri.substring(SSH_URI_PREFIX.length(), fullUri.lastIndexOf('@'));
    try {
      return (uriWithoutProtocol.lastIndexOf(':') > 0)
          ? SmbUtil.getSmbEncryptedPath(AppConfig.getInstance(), fullUri).replace("\n", "")
          : fullUri;
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("Error encrypting path", e);
      return fullUri;
    }
  }

  /**
   * Convenience method to call {@link SmbUtil#getSmbDecryptedPath(Context, String)} if the given
   * SSH URL contains the password (assuming the password is encrypted).
   *
   * @param fullUri SSH URL
   * @return SSH URL with the password (if exists) decrypted
   */
  public static String decryptSshPathAsNecessary(@NonNull String fullUri) {
    String uriWithoutProtocol =
        fullUri.substring(SSH_URI_PREFIX.length(), fullUri.lastIndexOf('@'));
    try {
      return (uriWithoutProtocol.lastIndexOf(':') > 0)
          ? SmbUtil.getSmbDecryptedPath(AppConfig.getInstance(), fullUri)
          : fullUri;
    } catch (IOException | GeneralSecurityException e) {
      LOG.error("Error decrypting path", e);
      return fullUri;
    }
  }

  /**
   * Convenience method to extract the Base URL from the given SSH URL.
   *
   * <p>For example, given <code>ssh://user:password@127.0.0.1:22/home/user/foo/bar</code>, this
   * method returns <code>ssh://user:password@127.0.0.1:22</code>.
   *
   * @param fullUri Full SSH URL
   * @return The remote path part of the full SSH URL
   */
  public static String extractBaseUriFrom(@NonNull String fullUri) {
    String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length());
    String credentials = uriWithoutProtocol.substring(0, uriWithoutProtocol.lastIndexOf('@'));
    String hostAndPath = uriWithoutProtocol.substring(uriWithoutProtocol.lastIndexOf('@') + 1);
    if (hostAndPath.indexOf('/') == -1) {
      return fullUri;
    } else {
      String host = hostAndPath.substring(0, hostAndPath.indexOf('/'));
      return fullUri.substring(
          0, SSH_URI_PREFIX.length() + credentials.length() + 1 + host.length());
    }
  }

  /**
   * Convenience method to extract the remote path from the given SSH URL.
   *
   * <p>For example, given <code>ssh://user:password@127.0.0.1:22/home/user/foo/bar</code>, this
   * method returns <code>/home/user/foo/bar</code>.
   *
   * @param fullUri Full SSH URL
   * @return The remote path part of the full SSH URL
   */
  public static String extractRemotePathFrom(@NonNull String fullUri) {
    String hostPath = fullUri.substring(fullUri.lastIndexOf('@'));
    return hostPath.indexOf('/') == -1 ? "/" : hostPath.substring(hostPath.indexOf('/'));
  }

  /**
   * Converts plain path smb://127.0.0.1/test.pdf to authorized path
   * smb://test:123@127.0.0.1/test.pdf from server list
   *
   * @param path
   * @return
   */
  public static String formatPlainServerPathToAuthorised(ArrayList<String[]> servers, String path) {
    synchronized (servers) {
      for (String[] serverEntry : servers) {
        Uri inputUri = Uri.parse(path);
        Uri serverUri = Uri.parse(serverEntry[1]);
        if (serverUri.getAuthority().contains(inputUri.getAuthority())) {
          String output =
              inputUri
                  .buildUpon()
                  .encodedAuthority(serverUri.getEncodedAuthority())
                  .build()
                  .toString();
          LOG.info("build authorised path {} from plain path {}", output, path);
          return output;
        }
      }
      return path;
    }
  }

  /**
   * Disconnects the given {@link SSHClient} but wrap all exceptions beneath, so callers are free
   * from the hassles of handling thrown exceptions.
   *
   * @param client {@link SSHClient} instance
   */
  public static void tryDisconnect(SSHClient client) {
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException e) {
        LOG.warn("Error closing SSHClient connection", e);
      }
    }
  }

  public static void launchSftp(final HybridFile baseFile, final MainActivity activity) {
    final CloudStreamer streamer = CloudStreamer.getInstance();

    new Thread(
            () -> {
              try {
                streamer.setStreamSrc(
                    baseFile.getInputStream(activity),
                    baseFile.getName(activity),
                    baseFile.length(activity));
                activity.runOnUiThread(
                    () -> {
                      try {
                        File file =
                            new File(SshClientUtils.extractRemotePathFrom(baseFile.getPath()));
                        Uri uri =
                            Uri.parse(CloudStreamer.URL + Uri.fromFile(file).getEncodedPath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(
                            uri,
                            MimeTypes.getMimeType(
                                baseFile.getPath(), baseFile.isDirectory(activity)));
                        PackageManager packageManager = activity.getPackageManager();
                        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                        if (resInfos != null && resInfos.size() > 0) activity.startActivity(i);
                        else
                          Toast.makeText(
                                  activity,
                                  activity.getResources().getString(R.string.smb_launch_error),
                                  Toast.LENGTH_SHORT)
                              .show();
                      } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                      }
                    });
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  // Decide the SSH URL depends on password/selected KeyPair
  public static String deriveSftpPathFrom(
      @NonNull String hostname,
      int port,
      @Nullable String defaultPath,
      @NonNull String username,
      @Nullable String password,
      @Nullable KeyPair selectedParsedKeyPair) {
    // FIXME: should be caller's responsibility
    String pathSuffix = defaultPath;
    if (pathSuffix == null) pathSuffix = "/";
    return (selectedParsedKeyPair != null || password == null)
        ? String.format("ssh://%s@%s:%d%s", username, hostname, port, pathSuffix)
        : String.format("ssh://%s:%s@%s:%d%s", username, password, hostname, port, pathSuffix);
  }

  public static boolean isDirectory(@NonNull SFTPClient client, @NonNull RemoteResourceInfo info)
      throws IOException {
    boolean isDirectory = info.isDirectory();
    if (info.getAttributes().getType().equals(FileMode.Type.SYMLINK)) {
      try {
        FileAttributes symlinkAttrs = client.stat(info.getPath());
        isDirectory = symlinkAttrs.getType().equals(FileMode.Type.DIRECTORY);
      } catch (IOException ifSymlinkIsBroken) {
        LOG.warn("Symbolic link {} is broken, skipping", info.getPath());
        throw ifSymlinkIsBroken;
      }
    }
    return isDirectory;
  }

  public static @FolderState int checkFolder(@NonNull String path) {
    return Single.fromCallable(
            () ->
                execute(
                    new SFtpClientTemplate<Integer>(extractBaseUriFrom(path)) {
                      @Override
                      public @FolderState Integer execute(@NonNull SFTPClient client)
                          throws IOException {
                        return (client.statExistence(extractRemotePathFrom(path)) == null)
                            ? WRITABLE_ON_REMOTE
                            : DOESNT_EXIST;
                      }
                    }))
        .subscribeOn(Schedulers.io())
        .blockingGet();
  }
}
