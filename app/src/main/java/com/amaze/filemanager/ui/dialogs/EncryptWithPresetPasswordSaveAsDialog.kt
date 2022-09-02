/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.services.EncryptService
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_ENCRYPT_TARGET
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_PASSWORD
import com.amaze.filemanager.databinding.DialogEncryptWithMasterPasswordBinding
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.EncryptButtonCallbackInterface
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.EncryptAuthenticateDialog.createFilenameValidator
import com.amaze.filemanager.ui.dialogs.EncryptAuthenticateDialog.createUseAzeEncryptCheckboxOnCheckedChangeListener
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.ENCRYPT_PASSWORD_FINGERPRINT
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.ENCRYPT_PASSWORD_MASTER
import com.amaze.filemanager.ui.views.WarnableTextInputValidator

/**
 * Encryption save as file dialog, for us when fingerprint or master password is set.
 */
object EncryptWithPresetPasswordSaveAsDialog {

    /**
     * Displays the save as dialog.
     */
    @JvmStatic
    @SuppressLint("SetTextI18n")
    @Suppress("LongMethod")
    fun show(
        c: Context,
        intent: Intent,
        main: MainActivity,
        password: String,
        encryptButtonCallbackInterface: EncryptButtonCallbackInterface
    ) {
        intent.getParcelableExtra<HybridFileParcelable>(EncryptService.TAG_SOURCE)?.run {
            val preferences = PreferenceManager.getDefaultSharedPreferences(c)
            val accentColor = main.accent
            val vb = DialogEncryptWithMasterPasswordBinding.inflate(LayoutInflater.from(c))
            val rootView = vb.root
            val encryptSaveAsEditText = vb.editTextEncryptSaveAs.also {
                when (password) {
                    ENCRYPT_PASSWORD_FINGERPRINT -> {
                        // Fingerprint not supported for AESCrypt
                        it.setText(this.getName(c) + CryptUtil.CRYPT_EXTENSION)
                    }
                    ENCRYPT_PASSWORD_MASTER -> {
                        it.setText(this.getName(c) + CryptUtil.AESCRYPT_EXTENSION)
                    }
                    else -> {
                        throw IllegalArgumentException(
                            "Must be either " +
                                "ENCRYPT_PASSWORD_FINGERPRINT or ENCRYPT_PASSWORD_MASTER"
                        )
                    }
                }
            }
            val useAzeEncrypt = vb.checkboxUseAze
            val usageTextInfo = vb.textViewCryptInfo.apply {
                text = HtmlCompat.fromHtml(
                    main.getString(R.string.encrypt_option_use_aescrypt_desc),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            }
            if (ENCRYPT_PASSWORD_FINGERPRINT != password) {
                useAzeEncrypt.setOnCheckedChangeListener(
                    createUseAzeEncryptCheckboxOnCheckedChangeListener(
                        c,
                        this,
                        preferences,
                        main,
                        encryptSaveAsEditText,
                        usageTextInfo
                    )
                )
            } else {
                useAzeEncrypt.visibility = View.INVISIBLE
                usageTextInfo.visibility = View.INVISIBLE
            }

            val saveAsDialog = MaterialDialog.Builder(c)
                .title(
                    if (isDirectory) {
                        R.string.encrypt_folder_save_as
                    } else {
                        R.string.encrypt_file_save_as
                    }
                ).customView(rootView, true)
                .positiveColor(accentColor)
                .negativeColor(accentColor)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive { dialog, _ ->
                    intent.putExtra(TAG_ENCRYPT_TARGET, encryptSaveAsEditText.text.toString())
                    intent.putExtra(TAG_PASSWORD, password)
                    runCatching {
                        encryptButtonCallbackInterface.onButtonPressed(intent, password)
                    }.onFailure {
                        Toast.makeText(
                            c,
                            c.getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG
                        ).show()
                    }.also {
                        dialog.dismiss()
                    }
                }.build()
            WarnableTextInputValidator(
                c,
                encryptSaveAsEditText,
                vb.tilEncryptSaveAs,
                saveAsDialog.getActionButton(DialogAction.POSITIVE),
                createFilenameValidator(useAzeEncrypt)
            )
            saveAsDialog.show()
            saveAsDialog.getActionButton(DialogAction.POSITIVE).isEnabled = true
        } ?: throw IllegalArgumentException("No TAG_SOURCE parameter specified")
    }
}
