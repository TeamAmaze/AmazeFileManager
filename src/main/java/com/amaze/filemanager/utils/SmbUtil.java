package com.amaze.filemanager.utils;

import android.content.Context;

import com.amaze.filemanager.utils.files.CryptUtil;

/**
 * Created by Vishal on 30-05-2017.
 *
 * Class provides various utility methods for SMB client
 */

public class SmbUtil {

    /**
     * Parse path to decrypt smb password
     * @return
     */
    public static String getSmbDecryptedPath(Context context, String path) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(path.substring(0, path.indexOf(":", 4)+1));
        String encryptedPassword = path.substring(path.indexOf(":", 4)+1, path.lastIndexOf("@"));

        try {

            String decryptedPassword = CryptUtil.decryptPassword(context, encryptedPassword);

            buffer.append(decryptedPassword);
            buffer.append(path.substring(path.lastIndexOf("@"), path.length()));

            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Parse path to encrypt smb password
     * @param context
     * @param path
     * @return
     */
    public static String getSmbEncryptedPath(Context context, String path) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(path.substring(0, path.indexOf(":", 4)+1));
        String decryptedPassword = path.substring(path.indexOf(":", 4)+1, path.lastIndexOf("@"));

        String encryptPassword;
        try {
            encryptPassword =  CryptUtil.encryptPassword(context, decryptedPassword);
        } catch (Exception e) {
            e.printStackTrace();
            encryptPassword = decryptedPassword;
        }

        buffer.append(encryptPassword);
        buffer.append(path.substring(path.lastIndexOf("@"), path.length()));

        return buffer.toString();
    }
}
