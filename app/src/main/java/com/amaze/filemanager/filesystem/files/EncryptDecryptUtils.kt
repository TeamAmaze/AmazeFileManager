/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.asynchronous.services.DecryptService
import com.amaze.filemanager.asynchronous.services.EncryptService
import com.amaze.filemanager.database.CryptHandler
import com.amaze.filemanager.database.CryptHandler.addEntry
import com.amaze.filemanager.database.models.explorer.EncryptedEntry
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.DecryptFingerprintDialog.show
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation
import com.amaze.filemanager.ui.fragments.MainFragment
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.amaze.filemanager.utils.PasswordUtil.decryptPassword
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Provides useful interfaces and methods for encryption/decryption
 *
 * @author Emmanuel on 25/5/2017, at 16:55.
 */
object EncryptDecryptUtils {
    const val DECRYPT_BROADCAST: String = "decrypt_broadcast"

    private val LOG: Logger = LoggerFactory.getLogger(EncryptDecryptUtils::class.java)

    /**
     * Queries database to map path and password. Starts the encryption process after database query
     *
     * @param path the path of file to encrypt
     * @param password the password in plaintext
     * @throws GeneralSecurityException Errors on encrypting file/folder
     * @throws IOException I/O errors on encrypting file/folder
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class, IOException::class)
    fun startEncryption(
        c: Context?,
        path: String,
        password: String?,
        intent: Intent,
    ) {
        val destPath =
            path.substring(
                0,
                path.lastIndexOf('/') + 1,
            ) + intent.getStringExtra(EncryptService.TAG_ENCRYPT_TARGET)

        // EncryptService.TAG_ENCRYPT_TARGET already has the .aze extension, no need to append again
        if (!intent.getBooleanExtra(EncryptService.TAG_AESCRYPT, false)) {
            val encryptedEntry = EncryptedEntry(destPath, password)
            addEntry(encryptedEntry)
        }
        // start the encryption process
        ServiceWatcherUtil.runService(c, intent)
    }

    /**
     * Routine to decrypt file. Include branches for AESCrypt, password and fingerprint methods.
     */
    @JvmStatic
    fun decryptFile(
        c: Context,
        mainActivity: MainActivity,
        main: MainFragment,
        openMode: OpenMode,
        sourceFile: HybridFileParcelable,
        decryptPath: String?,
        utilsProvider: UtilitiesProvider,
        broadcastResult: Boolean,
    ) {
        val decryptIntent = Intent(main.context, DecryptService::class.java)
        decryptIntent.putExtra(EncryptService.TAG_OPEN_MODE, openMode.ordinal)
        decryptIntent.putExtra(EncryptService.TAG_SOURCE, sourceFile)
        decryptIntent.putExtra(EncryptService.TAG_DECRYPT_PATH, decryptPath)
        val preferences = PreferenceManager.getDefaultSharedPreferences(main.requireContext())

        if (sourceFile.path.endsWith(CryptUtil.AESCRYPT_EXTENSION)) {
            displayDecryptDialogForAescrypt(c, mainActivity, utilsProvider, decryptIntent, main)
        } else {
            val encryptedEntry: EncryptedEntry?

            try {
                encryptedEntry = findEncryptedEntry(sourceFile.path)
            } catch (e: GeneralSecurityException) {
                LOG.warn("failed to find encrypted entry while decrypting", e)
                // we couldn't find any entry in database or lost the key to decipher
                toastDecryptionFailure(main)
                return
            } catch (e: IOException) {
                LOG.warn("failed to find encrypted entry while decrypting", e)
                toastDecryptionFailure(main)
                return
            }

            val decryptButtonCallbackInterface: DecryptButtonCallbackInterface = createCallback(main)

            if (encryptedEntry == null && !sourceFile.path.endsWith(CryptUtil.AESCRYPT_EXTENSION)) {
                // couldn't find the matching path in database, we lost the password
                toastDecryptionFailure(main)
                return
            }

            when (encryptedEntry!!.password.value) {
                PreferencesConstants.ENCRYPT_PASSWORD_FINGERPRINT ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            show(
                                c,
                                mainActivity,
                                decryptIntent,
                                decryptButtonCallbackInterface,
                            )
                        } else {
                            throw IllegalStateException("API < M!")
                        }
                    } catch (e: GeneralSecurityException) {
                        LOG.warn("failed to form fingerprint dialog", e)
                        toastDecryptionFailure(main)
                    } catch (e: IOException) {
                        LOG.warn("failed to form fingerprint dialog", e)
                        toastDecryptionFailure(main)
                    } catch (e: IllegalStateException) {
                        LOG.warn("failed to form fingerprint dialog", e)
                        toastDecryptionFailure(main)
                    }

                PreferencesConstants.ENCRYPT_PASSWORD_MASTER ->
                    try {
                        displayDecryptDialogWithMasterPassword(
                            c,
                            mainActivity,
                            decryptIntent,
                            utilsProvider,
                            preferences,
                            decryptButtonCallbackInterface,
                        )
                    } catch (e: GeneralSecurityException) {
                        LOG.warn("failed to show decrypt dialog, e")
                        toastDecryptionFailure(main)
                    } catch (e: IOException) {
                        LOG.warn("failed to show decrypt dialog, e")
                        toastDecryptionFailure(main)
                    }

                else ->
                    GeneralDialogCreation.showDecryptDialog(
                        c,
                        mainActivity,
                        decryptIntent,
                        utilsProvider.appTheme,
                        encryptedEntry.password.value,
                        decryptButtonCallbackInterface,
                    )
            }
        }
    }

    private fun displayDecryptDialogWithMasterPassword(
        c: Context,
        mainActivity: MainActivity,
        decryptIntent: Intent,
        utilsProvider: UtilitiesProvider,
        preferences: SharedPreferences,
        decryptButtonCallbackInterface: DecryptButtonCallbackInterface,
    ) {
        GeneralDialogCreation.showDecryptDialog(
            c,
            mainActivity,
            decryptIntent,
            utilsProvider.appTheme,
            decryptPassword(
                c,
                preferences.getString(
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT,
                )!!,
                Base64.DEFAULT,
            ),
            decryptButtonCallbackInterface,
        )
    }

    private fun displayDecryptDialogForAescrypt(
        c: Context?,
        mainActivity: MainActivity?,
        utilsProvider: UtilitiesProvider,
        decryptIntent: Intent,
        main: MainFragment,
    ) {
        GeneralDialogCreation.showPasswordDialog(
            c!!,
            mainActivity!!,
            utilsProvider.appTheme,
            R.string.crypt_decrypt,
            R.string.authenticate_password,
            { dialog: MaterialDialog, which: DialogAction? ->
                val editText =
                    dialog.view.findViewById<AppCompatEditText>(R.id.singleedittext_input)
                decryptIntent.putExtra(EncryptService.TAG_PASSWORD, editText.text.toString())
                ServiceWatcherUtil.runService(main.context, decryptIntent)
                dialog.dismiss()
            },
            null,
        )
    }

    private fun createCallback(main: MainFragment): DecryptButtonCallbackInterface {
        return object : DecryptButtonCallbackInterface {
            override fun confirm(intent: Intent) {
                ServiceWatcherUtil.runService(main.context, intent)
            }

            override fun failed() {
                Toast.makeText(
                    main.context,
                    main.requireMainActivity().getString(R.string.crypt_decryption_fail_password),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun toastDecryptionFailure(main: MainFragment) {
        Toast.makeText(
            main.context,
            main.requireMainActivity().getString(R.string.crypt_decryption_fail),
            Toast.LENGTH_LONG,
        ).show()
    }

    /**
     * Queries database to find entry for the specific path
     *
     * @param path the path to match with
     * @return the entry
     */
    @JvmStatic
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun findEncryptedEntry(path: String): EncryptedEntry? {
        val handler = CryptHandler

        var matchedEntry: EncryptedEntry? = null
        // find closest path which matches with database entry
        for (encryptedEntry in handler.allEntries) {
            if (path.contains(encryptedEntry.path)) {
                if (matchedEntry == null ||
                    matchedEntry.path.length < encryptedEntry.path.length
                ) {
                    matchedEntry = encryptedEntry
                }
            }
        }
        return matchedEntry
    }

    interface EncryptButtonCallbackInterface {
        /**
         * Callback fired when user has entered a password for encryption Not called when we've a master
         * password set or enable fingerprint authentication
         *
         * @param password the password entered by user
         */
        @Throws(GeneralSecurityException::class, IOException::class)
        fun onButtonPressed(
            intent: Intent,
            password: String,
        ) {
        }
    }

    interface DecryptButtonCallbackInterface {
        /** Callback fired when we've confirmed the password matches the database  */
        fun confirm(intent: Intent) {}

        /** Callback fired when password doesn't match the value entered by user  */
        fun failed() {}
    }
}
