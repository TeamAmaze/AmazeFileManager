/*
 * SshClientUtils.java
 *
 * Copyright © 2017 Raymond Lai <airwave209gt at gmail.com>.
 *
 * This file is part of AmazeFileManager.
 *
 * AmazeFileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AmazeFileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AmazeFileManager. If not, see <http ://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem.ssh;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.icons.MimeTypes;
import com.amaze.filemanager.utils.application.AppConfig;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.utils.cloud.CloudStreamer;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;

import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_URI_PREFIX;

public abstract class SshClientUtils
{
    private static final String TAG = "SshClientUtils";

    /**
     * Execute the given SshClientTemplate.
     *
     * This template pattern is borrowed from Spring Framework, to simplify code on operations
     * using SftpClientTemplate.
     *
     * @param template {@link SshClientTemplate} to execute
     * @param <T> Type of return value
     * @return Template execution results
     */
    public static final <T> T execute(@NonNull SshClientTemplate template) {
        SSHClient client = null;
        T retval = null;
        try {
            client = SshConnectionPool.getInstance().getConnection(template.url);
            if(client != null)
                retval = template.execute(client);
            else
                throw new RuntimeException("Unable to execute template");
        } catch(Exception e) {
            Log.e(TAG, "Error executing template method", e);
        } finally {
            if(client != null && template.closeClientOnFinish) {
                tryDisconnect(client);
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
    public static final <T> T execute(@NonNull final SshClientSessionTemplate template) {
        return execute(new SshClientTemplate(template.url, false) {
            @Override
            public T execute(SSHClient client) {
                Session session = null;
                T retval = null;
                try {
                    session = client.startSession();
                    retval = template.execute(session);
                } catch(IOException e) {
                    Log.e(TAG, "Error executing template method", e);
                } finally {
                    if(session != null && session.isOpen()) {
                        try {
                            session.close();
                        } catch(IOException e) {
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
    public static final <T> T execute(@NonNull final SFtpClientTemplate template) {
        return execute(new SshClientTemplate(template.url, false) {
            @Override
            public T execute(SSHClient client) {
                SFTPClient sftpClient = null;
                T retval = null;
                try {
                    sftpClient = client.newSFTPClient();
                    retval = template.execute(sftpClient);
                } catch(IOException e) {
                    Log.e(TAG, "Error executing template method", e);
                } finally {
                    if(sftpClient != null && template.closeClientOnFinish) {
                        try {
                            sftpClient.close();
                        } catch(IOException e) {
                            Log.w(TAG, "Error closing SFTP client", e);
                        }
                    }
                }
                return retval;
            }
        });
    }

    /**
     * Convenience method to call {@link SmbUtil#getSmbEncryptedPath(Context, String)} if the given
     * SSH URL contains the password (assuming the password is encrypted).
     *
     * @param fullUri SSH URL
     * @return SSH URL with the password (if exists) encrypted
     */
    public static final String encryptSshPathAsNecessary(@NonNull String fullUri) {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length(), fullUri.lastIndexOf('@'));
        try {
            return (uriWithoutProtocol.lastIndexOf(':') > 0) ?
                    SmbUtil.getSmbEncryptedPath(AppConfig.getInstance(), fullUri) :
                    fullUri;
        } catch(IOException | GeneralSecurityException e){
            Log.e(TAG, "Error encrypting path", e);
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
    public static final String decryptSshPathAsNecessary(@NonNull String fullUri) {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length(), fullUri.lastIndexOf('@'));
        try {
            return (uriWithoutProtocol.lastIndexOf(':') > 0) ?
                    SmbUtil.getSmbDecryptedPath(AppConfig.getInstance(), fullUri) :
                    fullUri;
        } catch(IOException | GeneralSecurityException e){
            Log.e(TAG, "Error decrypting path", e);
            return fullUri;
        }
    }

    /**
     * Convenience method to extract the Base URL from the given SSH URL.
     *
     * For example, given <code>ssh://user:password@127.0.0.1:22/home/user/foo/bar</code>, this
     * method returns <code>ssh://user:password@127.0.0.1:22</code>.
     *
     * @param fullUri Full SSH URL
     * @return The remote path part of the full SSH URL
     */
    public static final String extractBaseUriFrom(@NonNull String fullUri) {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length());
        return uriWithoutProtocol.indexOf('/') == -1 ?
            fullUri :
            fullUri.substring(0, uriWithoutProtocol.indexOf('/') + SSH_URI_PREFIX.length());
    }

    /**
     * Convenience method to extract the remote path from the given SSH URL.
     *
     * For example, given <code>ssh://user:password@127.0.0.1:22/home/user/foo/bar</code>, this
     * method returns <code>/home/user/foo/bar</code>.
     *
     * @param fullUri Full SSH URL
     * @return The remote path part of the full SSH URL
     */
    public static final String extractRemotePathFrom(@NonNull String fullUri) {
        String uriWithoutProtocol = fullUri.substring(SSH_URI_PREFIX.length());
        return uriWithoutProtocol.indexOf('/') == -1 ?
            "/" :
            uriWithoutProtocol.substring(uriWithoutProtocol.indexOf('/'));
    }

    /**
     * Disconnects the given {@link SSHClient} but wrap all exceptions beneath, so callers are free
     * from the hassles of handling thrown exceptions.
     *
     * @param client {@link SSHClient} instance
     */
    public static final void tryDisconnect(SSHClient client) {
        if(client != null && client.isConnected()){
            try {
                client.disconnect();
            } catch (IOException e) {
                Log.w(TAG, "Error closing SSHClient connection", e);
            }
        }
    }

    public static void launchSftp(final HybridFileParcelable baseFile, final MainActivity activity) {
        final CloudStreamer streamer = CloudStreamer.getInstance();

        new Thread(() -> {
            try {
                streamer.setStreamSrc(baseFile.getInputStream(activity), baseFile.getName(), baseFile.length(activity));
                activity.runOnUiThread(() -> {
                    try {
                        File file = new File(SshClientUtils.extractRemotePathFrom(baseFile.getPath()));
                        Uri uri = Uri.parse(CloudStreamer.URL + Uri.fromFile(file).getEncodedPath());
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(uri, MimeTypes.getMimeType(baseFile.getPath(), baseFile.isDirectory()));
                        PackageManager packageManager = activity.getPackageManager();
                        List<ResolveInfo> resInfos = packageManager.queryIntentActivities(i, 0);
                        if (resInfos != null && resInfos.size() > 0)
                            activity.startActivity(i);
                        else
                            Toast.makeText(activity,
                                    activity.getResources().getString(R.string.smb_launch_error),
                                    Toast.LENGTH_SHORT).show();
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {

                e.printStackTrace();
            }
        }).start();
    }

    //Decide the SSH URL depends on password/selected KeyPair
    public static String deriveSftpPathFrom(String hostname, int port, String username, String password,
                                      KeyPair selectedParsedKeyPair) {
        return (selectedParsedKeyPair != null || password == null) ?
                String.format("ssh://%s@%s:%d", username, hostname, port) :
                String.format("ssh://%s:%s@%s:%d", username, password, hostname, port);
    }
}
