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

package com.amaze.filemanager.filesystem.files;

import static com.amaze.filemanager.asynchronous.services.EncryptService.TAG_AESCRYPT;
import static com.amaze.filemanager.filesystem.files.CryptUtil.AESCRYPT_EXTENSION;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil;
import com.amaze.filemanager.asynchronous.services.DecryptService;
import com.amaze.filemanager.asynchronous.services.EncryptService;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.database.models.explorer.EncryptedEntry;
import com.amaze.filemanager.fileoperations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.ui.activities.MainActivity;
import com.amaze.filemanager.ui.dialogs.DecryptFingerprintDialog;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.ui.fragments.MainFragment;
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants;
import com.amaze.filemanager.ui.provider.UtilitiesProvider;
import com.amaze.filemanager.utils.PasswordUtil;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.PreferenceManager;

/**
 * Provides useful interfaces and methods for encryption/decryption
 *
 * @author Emmanuel on 25/5/2017, at 16:55.
 */
public class EncryptDecryptUtils {

  public static final String DECRYPT_BROADCAST = "decrypt_broadcast";
  private static final Logger LOG = LoggerFactory.getLogger(EncryptDecryptUtils.class);
  /**
   * Queries database to map path and password. Starts the encryption process after database query
   *
   * @param path the path of file to encrypt
   * @param password the password in plaintext
   * @throws GeneralSecurityException Errors on encrypting file/folder
   * @throws IOException I/O errors on encrypting file/folder
   */
  public static void startEncryption(
      Context c, final String path, final String password, Intent intent)
      throws GeneralSecurityException, IOException {
    String destPath =
        path.substring(0, path.lastIndexOf('/') + 1)
            .concat(intent.getStringExtra(EncryptService.TAG_ENCRYPT_TARGET));

    // EncryptService.TAG_ENCRYPT_TARGET already has the .aze extension, no need to append again
    if (!intent.getBooleanExtra(TAG_AESCRYPT, false)) {
      EncryptedEntry encryptedEntry = new EncryptedEntry(destPath, password);
      CryptHandler.INSTANCE.addEntry(encryptedEntry);
    }
    // start the encryption process
    ServiceWatcherUtil.runService(c, intent);
  }

  public static void decryptFile(
      Context c,
      final MainActivity mainActivity,
      final MainFragment main,
      OpenMode openMode,
      HybridFileParcelable sourceFile,
      String decryptPath,
      UtilitiesProvider utilsProvider,
      boolean broadcastResult) {

    Intent decryptIntent = new Intent(main.getContext(), DecryptService.class);
    decryptIntent.putExtra(EncryptService.TAG_OPEN_MODE, openMode.ordinal());
    decryptIntent.putExtra(EncryptService.TAG_SOURCE, sourceFile);
    decryptIntent.putExtra(EncryptService.TAG_DECRYPT_PATH, decryptPath);
    SharedPreferences preferences1 =
        PreferenceManager.getDefaultSharedPreferences(main.getContext());

    if (sourceFile.getPath().endsWith(AESCRYPT_EXTENSION)) {
      GeneralDialogCreation.showPasswordDialog(
          c,
          mainActivity,
          utilsProvider.getAppTheme(),
          R.string.crypt_decrypt,
          R.string.authenticate_password,
          (dialog, which) -> {
            AppCompatEditText editText = dialog.getView().findViewById(R.id.singleedittext_input);
            decryptIntent.putExtra(EncryptService.TAG_PASSWORD, editText.getText().toString());
            ServiceWatcherUtil.runService(main.getContext(), decryptIntent);
            dialog.dismiss();
          },
          null);
    } else {
      EncryptedEntry encryptedEntry;

      try {
        encryptedEntry = findEncryptedEntry(sourceFile.getPath());
      } catch (GeneralSecurityException | IOException e) {
        LOG.warn("failed to find encrypted entry while decrypting", e);

        // we couldn't find any entry in database or lost the key to decipher
        Toast.makeText(
                main.getContext(),
                main.getActivity().getString(R.string.crypt_decryption_fail),
                Toast.LENGTH_LONG)
            .show();
        return;
      }

      DecryptButtonCallbackInterface decryptButtonCallbackInterface =
          new DecryptButtonCallbackInterface() {
            @Override
            public void confirm(Intent intent) {
              ServiceWatcherUtil.runService(main.getContext(), intent);
            }

            @Override
            public void failed() {
              Toast.makeText(
                      main.getContext(),
                      main.getActivity().getString(R.string.crypt_decryption_fail_password),
                      Toast.LENGTH_LONG)
                  .show();
            }
          };

      if (encryptedEntry == null && !sourceFile.getPath().endsWith(AESCRYPT_EXTENSION)) {
        // couldn't find the matching path in database, we lost the password

        Toast.makeText(
                main.getContext(),
                main.getActivity().getString(R.string.crypt_decryption_fail),
                Toast.LENGTH_LONG)
            .show();
        return;
      }

      switch (encryptedEntry.getPassword().value) {
        case PreferencesConstants.ENCRYPT_PASSWORD_FINGERPRINT:
          try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              DecryptFingerprintDialog.show(
                  c,
                  mainActivity,
                  decryptIntent,
                  utilsProvider.getAppTheme(),
                  decryptButtonCallbackInterface);
            } else throw new IllegalStateException("API < M!");
          } catch (GeneralSecurityException | IOException | IllegalStateException e) {
            LOG.warn("failed to form fingerprint dialog", e);
            Toast.makeText(
                    main.getContext(),
                    main.getString(R.string.crypt_decryption_fail),
                    Toast.LENGTH_LONG)
                .show();
          }
          break;
        case PreferencesConstants.ENCRYPT_PASSWORD_MASTER:
          try {
            GeneralDialogCreation.showDecryptDialog(
                c,
                mainActivity,
                decryptIntent,
                utilsProvider.getAppTheme(),
                PasswordUtil.INSTANCE.decryptPassword(
                    c,
                    preferences1.getString(
                        PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                        PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT),
                    Base64.DEFAULT),
                decryptButtonCallbackInterface);
          } catch (GeneralSecurityException | IOException e) {
            LOG.warn("failed to show decrypt dialog, e");
            Toast.makeText(
                    main.getContext(),
                    main.getString(R.string.crypt_decryption_fail),
                    Toast.LENGTH_LONG)
                .show();
          }
          break;
        default:
          GeneralDialogCreation.showDecryptDialog(
              c,
              mainActivity,
              decryptIntent,
              utilsProvider.getAppTheme(),
              encryptedEntry.getPassword().value,
              decryptButtonCallbackInterface);
          break;
      }
    }
  }

  /**
   * Queries database to find entry for the specific path
   *
   * @param path the path to match with
   * @return the entry
   */
  private static EncryptedEntry findEncryptedEntry(String path)
      throws GeneralSecurityException, IOException {

    CryptHandler handler = CryptHandler.INSTANCE;

    EncryptedEntry matchedEntry = null;
    // find closest path which matches with database entry
    for (EncryptedEntry encryptedEntry : handler.getAllEntries()) {
      if (path.contains(encryptedEntry.getPath())) {

        if (matchedEntry == null
            || matchedEntry.getPath().length() < encryptedEntry.getPath().length()) {
          matchedEntry = encryptedEntry;
        }
      }
    }
    return matchedEntry;
  }

  public interface EncryptButtonCallbackInterface {
    /**
     * Callback fired when user has entered a password for encryption Not called when we've a master
     * password set or enable fingerprint authentication
     *
     * @param password the password entered by user
     */
    default void onButtonPressed(@NonNull Intent intent, @NonNull String password)
        throws GeneralSecurityException, IOException {}
  }

  public interface DecryptButtonCallbackInterface {
    /** Callback fired when we've confirmed the password matches the database */
    default void confirm(@NonNull Intent intent) {}

    /** Callback fired when password doesn't match the value entered by user */
    default void failed() {}
  }
}
