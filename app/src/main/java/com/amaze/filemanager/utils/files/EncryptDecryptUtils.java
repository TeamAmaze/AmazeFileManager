package com.amaze.filemanager.utils.files;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.asynchronous.services.DecryptService;
import com.amaze.filemanager.asynchronous.services.EncryptService;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.MainFragment;
import com.amaze.filemanager.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.provider.UtilitiesProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Provides useful interfaces and methods for encryption/decryption
 *
 * @author Emmanuel
 *         on 25/5/2017, at 16:55.
 */

public class EncryptDecryptUtils {

    public static final String DECRYPT_BROADCAST = "decrypt_broadcast";

    /* extract class - startEncryption and decryptFile : EncryptUtil and DecryptUtil
    Since the encryption and decryption processes are different, I have re-created the other two classes.
     */
    /**
     * Queries database to map path and password.
     * Starts the encryption process after database query
     *
     * @param path     the path of file to encrypt
     * @param password the password in plaintext
     */
    public static void startEncryption(Context c, final String path, final String password,
                                       Intent intent) throws Exception {
        EncryptUtil.startEncrpt(c, path, password, intent);
    }

    public static void decryptFile(Context c, final MainActivity mainActivity, final MainFragment main, OpenMode openMode,
                                   HybridFileParcelable sourceFile, String decryptPath,
                                   UtilitiesProvider utilsProvider,
                                   boolean broadcastResult) {

        DecryptUtil.startDecrypt(c, mainActivity, main, openMode, sourceFile, decryptPath, utilsProvider);
    }


    public interface EncryptButtonCallbackInterface {

        /**
         * Callback fired when we've just gone through warning dialog before encryption
         *
         * @param intent
         * @throws Exception
         */
        void onButtonPressed(Intent intent) throws Exception;

        /**
         * Callback fired when user has entered a password for encryption
         * Not called when we've a master password set or enable fingerprint authentication
         *
         * @param intent
         * @param password the password entered by user
         * @throws Exception
         */
        void onButtonPressed(Intent intent, String password) throws Exception;
    }

    public interface DecryptButtonCallbackInterface {
        /**
         * Callback fired when we've confirmed the password matches the database
         *
         * @param intent
         */
        void confirm(Intent intent);

        /**
         * Callback fired when password doesn't match the value entered by user
         */
        void failed();
    }

}