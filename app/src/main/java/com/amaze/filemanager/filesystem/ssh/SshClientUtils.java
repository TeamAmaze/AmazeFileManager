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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.fileoperations.filesystem.cloud.CloudStreamer;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.icons.MimeTypes;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;

public abstract class SshClientUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SshClientUtils.class);

  /**
   * Execute the given template with SshClientTemplate.
   *
   * @param template {@link SshClientSessionTemplate} to execute
   * @param <T> Type of return value
   * @return Template execution results
   */
  public static <T> T execute(@NonNull final SshClientSessionTemplate<T> template) {
    return NetCopyClientUtils.INSTANCE.execute(
        new SshClientTemplate<T>(template.url, false) {
          @Override
          public T executeWithSSHClient(@NonNull SSHClient sshClient) {
            Session session = null;
            T retval = null;
            try {
              session = sshClient.startSession();
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
    final SshClientTemplate<T> ftpClientTemplate =
        new SshClientTemplate<T>(template.url, false) {
          @Override
          @Nullable
          public T executeWithSSHClient(SSHClient sshClient) {
            SFTPClient sftpClient = null;
            T retval = null;
            try {
              sftpClient = sshClient.newSFTPClient();
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

    return NetCopyClientUtils.INSTANCE.execute(ftpClientTemplate);
  }

  /**
   * Converts plain path smb://127.0.0.1/test.pdf to authorized path
   * smb://test:123@127.0.0.1/test.pdf from server list
   *
   * @param path
   * @return
   */
  public static String formatPlainServerPathToAuthorised(ArrayList<String[]> servers, String path) {
    for (String[] serverEntry : servers) {
      Uri inputUri = Uri.parse(path);
      Uri serverUri = Uri.parse(serverEntry[1]);
      if (inputUri.getScheme().equalsIgnoreCase(serverUri.getScheme())
          && serverUri.getAuthority().contains(inputUri.getAuthority())) {
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
                boolean isDirectory = baseFile.isDirectory(activity);
                long fileLength = baseFile.length(activity);
                streamer.setStreamSrc(
                    baseFile.getInputStream(activity),
                    baseFile.getName(activity),
                    fileLength);
                activity.runOnUiThread(
                    () -> {
                      try {
                        File file =
                            new File(
                                NetCopyClientUtils.INSTANCE.extractRemotePathFrom(
                                    baseFile.getPath()));
                        Uri uri =
                            Uri.parse(CloudStreamer.URL + Uri.fromFile(file).getEncodedPath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(
                            uri,
                            MimeTypes.getMimeType(
                                baseFile.getPath(), isDirectory));
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
                        LOG.warn("failed to launch sftp file", e);
                      }
                    });
              } catch (Exception e) {
                LOG.warn("failed to launch sftp file", e);
              }
            })
        .start();
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
}
