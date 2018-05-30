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

public class DecryptUtil {

    /* rename parameter
   Renamed for understandability.
    */

    /* extract method - decryptFile method
    Toasts are overlap a lot. This part is extracted.
    For the understandability of decryptFile method, methods were extracted for each case.
     */
    public static void startDecrypt(Context context, MainActivity mainActivity, MainFragment mainFragment, OpenMode openMode, HybridFileParcelable sourceFile, String decryptPath, UtilitiesProvider utilsProvider) {
        Intent decryptIntent = new Intent(mainFragment.getContext(), DecryptService.class);
        decryptIntent.putExtra(EncryptService.TAG_OPEN_MODE, openMode.ordinal());
        decryptIntent.putExtra(EncryptService.TAG_SOURCE, sourceFile);
        decryptIntent.putExtra(EncryptService.TAG_DECRYPT_PATH, decryptPath);

        SharedPreferences preferences1 = PreferenceManager.getDefaultSharedPreferences(mainFragment.getContext());

        EncryptedEntry encryptedEntry = getEncryptedEntry(mainFragment, sourceFile);

        EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface = getDecryptButtonCallbackInterface(mainFragment);

        makeGeneralDialogCreationEachPasswordType(context, mainActivity, mainFragment, utilsProvider, decryptIntent, preferences1, encryptedEntry, decryptButtonCallbackInterface);
    }

    private static void makeGeneralDialogCreationEachPasswordType(Context context, MainActivity mainActivity, MainFragment mainFragment, UtilitiesProvider utilsProvider, Intent decryptIntent, SharedPreferences preferences1, EncryptedEntry encryptedEntry, EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {
        switch (encryptedEntry.getPassword()) {
            case PreferencesConstants.ENCRYPT_PASSWORD_FINGERPRINT:
                getGeneralDialogFingerprintPasswordType(context, mainActivity, mainFragment, utilsProvider, decryptIntent, decryptButtonCallbackInterface);
                break;
            case PreferencesConstants.ENCRYPT_PASSWORD_MASTER:
                getGeneralDialogMasterPasswordType(context, mainActivity, mainFragment, utilsProvider, decryptIntent, preferences1, decryptButtonCallbackInterface);
                break;
            default:
                getGeneralDialogDefaultPasswordType(context, mainActivity, utilsProvider, decryptIntent, encryptedEntry, decryptButtonCallbackInterface);
                break;
        }
    }

    private static void getGeneralDialogDefaultPasswordType(Context context, MainActivity mainActivity, UtilitiesProvider utilsProvider, Intent decryptIntent, EncryptedEntry encryptedEntry, EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {
        GeneralDialogCreation.showDecryptDialog(context, mainActivity, decryptIntent,
                utilsProvider.getAppTheme(), encryptedEntry.getPassword(),
                decryptButtonCallbackInterface);
    }

    private static void getGeneralDialogMasterPasswordType(Context context, MainActivity mainActivity, MainFragment mainFragment, UtilitiesProvider utilsProvider, Intent decryptIntent, SharedPreferences preferences1, EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {
        try {
            GeneralDialogCreation.showDecryptDialog(context,
                    mainActivity, decryptIntent, utilsProvider.getAppTheme(),
                    CryptUtil.decryptPassword(context, preferences1.getString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                            PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT)), decryptButtonCallbackInterface);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            ToastFailOutput(mainFragment.getContext(), mainFragment.getResources().getString(R.string.crypt_decryption_fail));
        }
    }

    private static void getGeneralDialogFingerprintPasswordType(Context context, MainActivity mainActivity, MainFragment mainFragment, UtilitiesProvider utilsProvider, Intent decryptIntent, EncryptDecryptUtils.DecryptButtonCallbackInterface decryptButtonCallbackInterface) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                GeneralDialogCreation.showDecryptFingerprintDialog(context,
                        mainActivity, decryptIntent, utilsProvider.getAppTheme(), decryptButtonCallbackInterface);
            } else throw new IllegalStateException("API < M!");
        } catch (GeneralSecurityException | IOException | IllegalStateException e) {
            e.printStackTrace();
            ToastFailOutput(mainFragment.getContext(), mainFragment.getResources().getString(R.string.crypt_decryption_fail));
        }
    }

    @Nullable
    private static EncryptedEntry getEncryptedEntry(MainFragment mainFragment, HybridFileParcelable sourceFile) {
        EncryptedEntry encryptedEntry = null;

        try {
            encryptedEntry = findEncryptedEntry(mainFragment.getContext(), sourceFile.getPath());
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();

            // we couldn't find any entry in database or lost the key to decipher
            ToastFailOutput(mainFragment.getContext(), mainFragment.getActivity().getResources().getString(R.string.crypt_decryption_fail));
        }

        if (encryptedEntry == null) {
            // couldn't find the matching path in database, we lost the password
            ToastFailOutput(mainFragment.getContext(), mainFragment.getActivity().getResources().getString(R.string.crypt_decryption_fail));
        }
        return encryptedEntry;
    }

    @NonNull
    private static EncryptDecryptUtils.DecryptButtonCallbackInterface getDecryptButtonCallbackInterface(MainFragment mainFragment) {
        return new EncryptDecryptUtils.DecryptButtonCallbackInterface() {
            @Override
            public void confirm(Intent intent) {
                ServiceWatcherUtil.runService(mainFragment.getContext(), intent);
            }

            @Override
            public void failed() {
                ToastFailOutput(mainFragment.getContext(), mainFragment.getActivity().getResources().getString(R.string.crypt_decryption_fail_password));
            }
        };
    }

    private static void ToastFailOutput(Context mainContext, String toastMessage) {
        Toast.makeText(mainContext, toastMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Queries database to find entry for the specific path
     *
     * @param path the path to match with
     * @return the entry
     */
    private static EncryptedEntry findEncryptedEntry(Context context, String path) throws GeneralSecurityException, IOException {

        CryptHandler cryptHandler = new CryptHandler(context);

        EncryptedEntry matchedEntry = null;
        // find closest path which matches with database entry
        for (EncryptedEntry encryptedEntry : cryptHandler.getAllEntries()) {
            if (path.contains(encryptedEntry.getPath())) {

                if (matchedEntry == null || matchedEntry.getPath().length() < encryptedEntry.getPath().length()) {
                    matchedEntry = encryptedEntry;
                }
            }
        }
        return matchedEntry;
    }
}
