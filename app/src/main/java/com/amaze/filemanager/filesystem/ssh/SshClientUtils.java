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

import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.DOESNT_EXIST;
import static com.amaze.filemanager.file_operations.filesystem.FolderStateKt.WRITABLE_ON_REMOTE;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.amaze.filemanager.R;
import com.amaze.filemanager.file_operations.filesystem.FolderState;
import com.amaze.filemanager.file_operations.filesystem.cloud.CloudStreamer;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.icons.MimeTypes;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
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

  private static final String TAG = SshClientUtils.class.getSimpleName();

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
              Log.e(TAG, "Error executing template method", e);
            } finally {
              if (session != null && session.isOpen()) {
                try {
                  session.close();
                } catch (IOException e) {
                  Log.w(TAG, "Error closing SFTP client", e);
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
              Log.e(TAG, "Error executing template method", e);
            } finally {
              if (sftpClient != null && template.closeClientOnFinish) {
                try {
                  sftpClient.close();
                } catch (IOException e) {
                  Log.w(TAG, "Error closing SFTP client", e);
                }
              }
            }
            return retval;
          }
        };

    return NetCopyClientUtils.INSTANCE.execute(ftpClientTemplate);
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
        Log.w(TAG, "Error closing SSHClient connection", e);
      }
    }
  }

  public static void launchSftp(final HybridFileParcelable baseFile, final MainActivity activity) {
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
                            new File(
                                NetCopyClientUtils.INSTANCE.extractRemotePathFrom(
                                    baseFile.getPath()));
                        Uri uri =
                            Uri.parse(CloudStreamer.URL + Uri.fromFile(file).getEncodedPath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(
                            uri, MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
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

  public static boolean isDirectory(@NonNull SFTPClient client, @NonNull RemoteResourceInfo info)
      throws IOException {
    boolean isDirectory = info.isDirectory();
    if (info.getAttributes().getType().equals(FileMode.Type.SYMLINK)) {
      try {
        FileAttributes symlinkAttrs = client.stat(info.getPath());
        isDirectory = symlinkAttrs.getType().equals(FileMode.Type.DIRECTORY);
      } catch (IOException ifSymlinkIsBroken) {
        Log.w(TAG, String.format("Symbolic link %s is broken, skipping", info.getPath()));
        throw ifSymlinkIsBroken;
      }
    }
    return isDirectory;
  }

  public static @FolderState int checkFolder(@NonNull String path) {
    return Single.fromCallable(
            () ->
                execute(
                    new SFtpClientTemplate<Integer>(
                        NetCopyClientUtils.INSTANCE.extractBaseUriFrom(path)) {
                      @Override
                      public @FolderState Integer execute(@NonNull SFTPClient client)
                          throws IOException {
                        return (client.statExistence(
                                    NetCopyClientUtils.INSTANCE.extractRemotePathFrom(path))
                                == null)
                            ? WRITABLE_ON_REMOTE
                            : DOESNT_EXIST;
                      }
                    }))
        .subscribeOn(Schedulers.io())
        .blockingGet();
  }
}
