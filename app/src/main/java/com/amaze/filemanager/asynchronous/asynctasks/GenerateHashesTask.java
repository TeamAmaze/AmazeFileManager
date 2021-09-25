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

package com.amaze.filemanager.asynchronous.asynctasks;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.amaze.filemanager.R;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.files.GenericCopyUtil;
import com.amaze.filemanager.filesystem.ssh.SshClientSessionTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

/**
 * Generates hashes from files (MD5 and SHA256)
 *
 * <p>Created by Vishal on 05-02-2015 edited by Emmanuel Messulam<emmanuelbendavid@gmail.com>
 */
public class GenerateHashesTask extends AsyncTask<Void, String, String[]> {

  private HybridFileParcelable file;
  private Context context;
  private TextView md5HashText;
  private TextView sha256Text;
  private LinearLayout mMD5LinearLayout, mSHA256LinearLayout;

  public GenerateHashesTask(HybridFileParcelable f, final Context c, final View view) {
    this.context = c;
    this.file = f;

    md5HashText = view.findViewById(R.id.t9);
    sha256Text = view.findViewById(R.id.t10);

    mMD5LinearLayout = view.findViewById(R.id.properties_dialog_md5);
    mSHA256LinearLayout = view.findViewById(R.id.properties_dialog_sha256);
  }

  @Override
  protected String[] doInBackground(Void... params) {
    String md5 = context.getString(R.string.error);
    String sha256 = context.getString(R.string.error);

    try {
      if (file.isSftp()) {
        md5 =
            SshClientUtils.execute(
                new SshClientSessionTemplate<String>(file.getPath()) {
                  @Override
                  public String execute(Session session) throws IOException {
                    Session.Command cmd =
                        session.exec(
                            String.format(
                                "md5sum -b \"%s\" | cut -c -32",
                                SshClientUtils.extractRemotePathFrom(file.getPath())));
                    String result =
                        new String(IOUtils.readFully(cmd.getInputStream()).toByteArray());
                    cmd.close();
                    if (cmd.getExitStatus() == 0) return result;
                    else {
                      return null;
                    }
                  }
                });
        sha256 =
            SshClientUtils.execute(
                new SshClientSessionTemplate<String>(file.getPath()) {
                  @Override
                  public String execute(Session session) throws IOException {
                    Session.Command cmd =
                        session.exec(
                            String.format(
                                "sha256sum -b \"%s\" | cut -c -64",
                                SshClientUtils.extractRemotePathFrom(file.getPath())));
                    String result = IOUtils.readFully(cmd.getInputStream()).toString();
                    cmd.close();
                    if (cmd.getExitStatus() == 0) return result;
                    else {
                      return null;
                    }
                  }
                });
      } else if (!file.isDirectory(context)) {
        md5 = getMD5Checksum();
        sha256 = getSHA256Checksum();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return new String[] {md5, sha256};
  }

  @Override
  protected void onPostExecute(final String[] hashes) {
    super.onPostExecute(hashes);
    if (!file.isDirectory() && file.getSize() != 0) {
      md5HashText.setText(hashes[0]);
      sha256Text.setText(hashes[1]);

      mMD5LinearLayout.setOnLongClickListener(
          v -> {
            FileUtils.copyToClipboard(context, hashes[0]);
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.md5).toUpperCase()
                        + " "
                        + context.getResources().getString(R.string.properties_copied_clipboard),
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          });
      mSHA256LinearLayout.setOnLongClickListener(
          v -> {
            FileUtils.copyToClipboard(context, hashes[1]);
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.hash_sha256)
                        + " "
                        + context.getResources().getString(R.string.properties_copied_clipboard),
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          });
    } else {
      mMD5LinearLayout.setVisibility(View.GONE);
      mSHA256LinearLayout.setVisibility(View.GONE);
    }
  }

  // see this How-to for a faster way to convert a byte array to a HEX string

  private String getMD5Checksum() throws Exception {
    byte[] b = createChecksum();
    String result = "";

    for (byte aB : b) {
      result += Integer.toString((aB & 0xff) + 0x100, 16).substring(1);
    }
    return result;
  }

  private String getSHA256Checksum() throws NoSuchAlgorithmException, IOException {
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
    byte[] input = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
    int length;
    InputStream inputStream = file.getInputStream(context);
    while ((length = inputStream.read(input)) != -1) {
      if (length > 0) messageDigest.update(input, 0, length);
    }

    byte[] hash = messageDigest.digest();

    StringBuilder hexString = new StringBuilder();

    for (byte aHash : hash) {
      // convert hash to base 16
      String hex = Integer.toHexString(0xff & aHash);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    inputStream.close();
    return hexString.toString();
  }

  private byte[] createChecksum() throws Exception {
    InputStream fis = file.getInputStream(context);

    byte[] buffer = new byte[8192];
    MessageDigest complete = MessageDigest.getInstance("MD5");
    int numRead;

    do {
      numRead = fis.read(buffer);
      if (numRead > 0) {
        complete.update(buffer, 0, numRead);
      }
    } while (numRead != -1);

    fis.close();
    return complete.digest();
  }
}
