package com.amaze.filemanager.utils;

import android.content.Context;
import android.text.TextUtils;

import com.amaze.filemanager.utils.files.CryptUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by Vishal on 30-05-2017.
 *
 * Class provides various utility methods for SMB client
 */

public class SmbUtil {

    /**
     * Parse path to decrypt smb password
     */
    public static String getSmbDecryptedPath(Context context, String path) throws GeneralSecurityException, IOException {
        if (!(path.contains(":") && path.contains("@"))) {
            // smb path doesn't have any credentials
            return path;
        }

        StringBuilder buffer = new StringBuilder();

        buffer.append(path.substring(0, path.indexOf(":", 4)+1));
        String encryptedPassword = path.substring(path.indexOf(":", 4)+1, path.lastIndexOf("@"));

        if (!TextUtils.isEmpty(encryptedPassword)) {
            String decryptedPassword = CryptUtil.decryptPassword(context, encryptedPassword);
            buffer.append(decryptedPassword);
        }
        buffer.append(path.substring(path.lastIndexOf("@"), path.length()));

        return buffer.toString();
    }

    /**
     * Parse path to encrypt smb password
     */
    public static String getSmbEncryptedPath(Context context, String path) throws GeneralSecurityException, IOException  {
        if (!(path.contains(":") && path.contains("@"))) {
            // smb path doesn't have any credentials
            return path;
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append(path.substring(0, path.indexOf(":", 4)+1));
        String decryptedPassword = path.substring(path.indexOf(":", 4)+1, path.lastIndexOf("@"));

        if (!TextUtils.isEmpty(decryptedPassword)) {
            String encryptPassword =  CryptUtil.encryptPassword(context, decryptedPassword);
            buffer.append(encryptPassword);
        }
        buffer.append(path.substring(path.lastIndexOf("@"), path.length()));

        return buffer.toString();
    }
}
