package com.amaze.filemanager.utils.files;

import android.content.Context;
import android.content.Intent;

import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptUtil {
    /* rename parameter
   Renamed for understandability.
    */

    public static void startEncrpt(Context context, String filePath, String passwordInPlaintext, Intent intent) throws GeneralSecurityException, IOException {
        CryptHandler cryptHandler = new CryptHandler(context);
        EncryptedEntry encryptedEntry = new EncryptedEntry(filePath.concat(CryptUtil.CRYPT_EXTENSION), passwordInPlaintext);
        cryptHandler.addEntry(encryptedEntry);

        // start the encryption process
        ServiceWatcherUtil.runService(context, intent);
    }
}
