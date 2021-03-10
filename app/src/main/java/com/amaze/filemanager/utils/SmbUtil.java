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

package com.amaze.filemanager.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;

import com.amaze.filemanager.filesystem.files.CryptUtil;
import com.amaze.filemanager.filesystem.smb.CifsContexts;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;

/**
 * Created by Vishal on 30-05-2017.
 *
 * <p>Class provides various utility methods for SMB client
 */
public class SmbUtil {

  public static final String PARAM_DISABLE_IPC_SIGNING_CHECK = "disableIpcSigningCheck";

  /** Parse path to decrypt smb password */
  public static String getSmbDecryptedPath(Context context, String path)
      throws GeneralSecurityException, IOException {
    if (!(path.contains(":") && path.contains("@"))) {
      // smb path doesn't have any credentials
      return path;
    }

    StringBuilder buffer = new StringBuilder();

    buffer.append(path.substring(0, path.indexOf(":", 4) + 1));
    String encryptedPassword = path.substring(path.indexOf(":", 4) + 1, path.lastIndexOf("@"));

    if (!TextUtils.isEmpty(encryptedPassword)) {
      String decryptedPassword = CryptUtil.decryptPassword(context, encryptedPassword);
      buffer.append(decryptedPassword);
    }
    buffer.append(path.substring(path.lastIndexOf("@")));

    return buffer.toString();
  }

  /** Parse path to encrypt smb password */
  public static String getSmbEncryptedPath(Context context, String path)
      throws GeneralSecurityException, IOException {
    if (!(path.contains(":") && path.contains("@"))) {
      // smb path doesn't have any credentials
      return path;
    }

    StringBuilder buffer = new StringBuilder();
    buffer.append(path.substring(0, path.indexOf(":", 4) + 1));
    String decryptedPassword = path.substring(path.indexOf(":", 4) + 1, path.lastIndexOf("@"));

    if (!TextUtils.isEmpty(decryptedPassword)) {
      String encryptPassword = CryptUtil.encryptPassword(context, decryptedPassword);
      buffer.append(encryptPassword);
    }
    buffer.append(path.substring(path.lastIndexOf("@")));

    return buffer.toString();
  }

  public static SmbFile create(String path) throws MalformedURLException {
    Uri uri = Uri.parse(path);
    boolean disableIpcSigningCheck =
        Boolean.parseBoolean(uri.getQueryParameter(PARAM_DISABLE_IPC_SIGNING_CHECK));
    String userInfo = uri.getUserInfo();
    return new SmbFile(
        path.indexOf('?') < 0 ? path : path.substring(0, path.indexOf('?')),
        CifsContexts.createWithDisableIpcSigningCheck(path, disableIpcSigningCheck)
            .withCredentials(createFrom(userInfo)));
  }

  /**
   * Create {@link NtlmPasswordAuthenticator} from given userInfo parameter.
   *
   * <p>Logic borrowed directly from jcifs-ng's own code. They should make that protected
   * constructor public...
   *
   * @param userInfo authentication string, must be already URL decoded. {@link Uri} shall do this
   *     for you already
   * @return {@link NtlmPasswordAuthenticator} instance
   */
  protected static @NonNull NtlmPasswordAuthenticator createFrom(@Nullable String userInfo) {
    if (!TextUtils.isEmpty(userInfo)) {
      String dom = null;
      String user = null;
      String pass = null;
      int i;
      int u;
      int end = userInfo.length();
      for (i = 0, u = 0; i < end; i++) {
        char c = userInfo.charAt(i);
        if (c == ';') {
          dom = userInfo.substring(0, i);
          u = i + 1;
        } else if (c == ':') {
          pass = userInfo.substring(i + 1);
          break;
        }
      }
      user = userInfo.substring(u, i);
      return new NtlmPasswordAuthenticator(dom, user, pass);
    } else {
      return new NtlmPasswordAuthenticator();
    }
  }
}
