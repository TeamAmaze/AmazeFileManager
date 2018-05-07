package com.amaze.filemanager.utils.files;

import android.content.Context;
import android.content.Intent;

import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.utils.ServiceWatcherUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptUtil {
    public static void startEncrpt(Context c, String path, String password, Intent intent) throws GeneralSecurityException, IOException {
        CryptHandler cryptHandler = new CryptHandler(c);
        EncryptedEntry encryptedEntry = new EncryptedEntry(path.concat(CryptUtil.CRYPT_EXTENSION),
                password);
        cryptHandler.addEntry(encryptedEntry);

        // start the encryption process
        ServiceWatcherUtil.runService(c, intent);
    }
}
