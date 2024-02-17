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
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDButton
import com.amaze.filemanager.R
import com.amaze.filemanager.asynchronous.services.EncryptService
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_AESCRYPT
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_ENCRYPT_TARGET
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_PASSWORD
import com.amaze.filemanager.databinding.DialogEncryptAuthenticateBinding
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.CryptUtil.AESCRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.CryptUtil.CRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.EncryptButtonCallbackInterface
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER
import com.amaze.filemanager.ui.openKeyboard
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.amaze.filemanager.ui.views.WarnableTextInputValidator.ReturnState
import com.amaze.filemanager.ui.views.WarnableTextInputValidator.ReturnState.STATE_ERROR
import com.google.android.material.textfield.TextInputEditText
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Encrypt file password dialog.
 */
object EncryptAuthenticateDialog {
    private val log: Logger = LoggerFactory.getLogger(EncryptAuthenticateDialog::class.java)

    /**
     * Display file encryption password dialog.
     */
    @JvmStatic
    @SuppressLint("SetTextI18n")
    @Suppress("LongMethod")
    fun show(
        c: Context,
        intent: Intent,
        main: MainActivity,
        appTheme: AppTheme,
        encryptButtonCallbackInterface: EncryptButtonCallbackInterface
    ) {
        intent.getParcelableExtra<HybridFileParcelable>(EncryptService.TAG_SOURCE)?.run {
            val preferences = PreferenceManager.getDefaultSharedPreferences(c)
            val accentColor = main.accent
            val builder = MaterialDialog.Builder(c)
            builder.title(main.getString(R.string.crypt_encrypt))
            val vb: DialogEncryptAuthenticateBinding =
                DialogEncryptAuthenticateBinding.inflate(LayoutInflater.from(c))
            val rootView: View = vb.root
            val passwordEditText: TextInputEditText = vb.editTextDialogEncryptPassword
            val passwordConfirmEditText: TextInputEditText = vb
                .editTextDialogEncryptPasswordConfirm
            val encryptSaveAsEditText: TextInputEditText = vb.editTextEncryptSaveAs
            val useAzeEncrypt: AppCompatCheckBox = vb.checkboxUseAze
            val usageTextInfo: AppCompatTextView = vb.textViewCryptInfo.apply {
                text = HtmlCompat.fromHtml(
                    main.getString(R.string.encrypt_option_use_aescrypt_desc),
                    FROM_HTML_MODE_COMPACT
                )
            }
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
            val textInputLayoutPassword: WarnableTextInputLayout = vb.tilEncryptPassword
            val textInputLayoutPasswordConfirm: WarnableTextInputLayout = vb
                .tilEncryptPasswordConfirm
            val textInputLayoutEncryptSaveAs: WarnableTextInputLayout = vb.tilEncryptSaveAs
            encryptSaveAsEditText.setText(this.getName(c) + AESCRYPT_EXTENSION)
            textInputLayoutEncryptSaveAs.hint =
                if (this.isDirectory) {
                    c.getString(R.string.encrypt_folder_save_as)
                } else {
                    c.getString(R.string.encrypt_file_save_as)
                }
            builder
                .customView(rootView, true)
                .positiveText(c.getString(R.string.ok))
                .negativeText(c.getString(R.string.cancel))
                .theme(appTheme.getMaterialDialogTheme())
                .positiveColor(accentColor)
                .negativeColor(accentColor)
                .autoDismiss(false)
                .onNegative { dialog, _ -> dialog.cancel() }
                .onPositive { dialog, _ ->
                    intent.putExtra(TAG_ENCRYPT_TARGET, encryptSaveAsEditText.text.toString())
                        .putExtra(TAG_AESCRYPT, !useAzeEncrypt.isChecked)
                        .putExtra(TAG_PASSWORD, passwordEditText.text.toString())
                    runCatching {
                        encryptButtonCallbackInterface.onButtonPressed(
                            intent,
                            passwordEditText.text.toString()
                        )
                    }.onFailure {
                        log.error("Failed to encrypt", it)
                        Toast.makeText(
                            c,
                            c.getString(R.string.crypt_encryption_fail),
                            Toast.LENGTH_LONG
                        ).show()
                    }.also {
                        dialog.dismiss()
                    }
                }
            val dialog = builder.show()
            val btnOK = dialog.getActionButton(DialogAction.POSITIVE)
            btnOK.isEnabled = false
            rootView.post { passwordEditText.openKeyboard(main.applicationContext) }
            createPasswordFieldValidator(
                c,
                passwordEditText,
                passwordConfirmEditText,
                textInputLayoutPassword,
                encryptSaveAsEditText,
                useAzeEncrypt,
                btnOK
            )
            createPasswordFieldValidator(
                c,
                passwordConfirmEditText,
                passwordEditText,
                textInputLayoutPasswordConfirm,
                encryptSaveAsEditText,
                useAzeEncrypt,
                btnOK
            )
            WarnableTextInputValidator(
                c,
                encryptSaveAsEditText,
                textInputLayoutEncryptSaveAs,
                btnOK,
                createFilenameValidator(useAzeEncrypt, extraCondition = {
                    true == passwordEditText.text?.isNotBlank() &&
                        passwordEditText.text.toString() == passwordConfirmEditText.text.toString()
                })
            )
        } ?: throw IllegalArgumentException("No TAG_SOURCE parameter specified")
    }

    private fun createPasswordFieldValidator(
        c: Context,
        passwordField: TextInputEditText,
        comparingPasswordField: TextInputEditText,
        warningTextInputLayout: WarnableTextInputLayout,
        encryptSaveAsEditText: TextInputEditText,
        useAzeEncrypt: AppCompatCheckBox,
        btnOK: MDButton
    ) = WarnableTextInputValidator(
        c,
        passwordField,
        warningTextInputLayout,
        btnOK
    ) { text: String ->
        if (text.isNotBlank() &&
            text == comparingPasswordField.text.toString() &&
            filenameIsValid(encryptSaveAsEditText.text.toString(), useAzeEncrypt)
        ) {
            ReturnState()
        } else if (text.isBlank()) {
            ReturnState(STATE_ERROR, R.string.field_empty)
        } else {
            ReturnState(STATE_ERROR, R.string.password_no_match)
        }
    }

    /**
     * Convenient method to create an [CompoundButton.OnCheckedChangeListener]
     * for the aze encryption format selection checkbox.
     */
    @JvmStatic
    @SuppressLint("SetTextI18n")
    fun createUseAzeEncryptCheckboxOnCheckedChangeListener(
        c: Context,
        file: HybridFileParcelable,
        preferences: SharedPreferences,
        main: MainActivity,
        encryptSaveAsEditText: TextInputEditText,
        usageTextInfo: AppCompatTextView
    ) = { _: CompoundButton?, isChecked: Boolean ->
        if (isChecked && !preferences.getBoolean(
                PREFERENCE_CRYPT_WARNING_REMEMBER,
                false
            )
        ) {
            EncryptWarningDialog.show(main, main.appTheme)
        }
        encryptSaveAsEditText.setText(
            "${file.getName(c)}${if (isChecked) {
                CRYPT_EXTENSION
            } else {
                AESCRYPT_EXTENSION
            }}"
        )
        usageTextInfo.text = HtmlCompat.fromHtml(
            main.getString(
                if (isChecked) {
                    R.string.encrypt_option_use_azecrypt_desc
                } else {
                    R.string.encrypt_option_use_aescrypt_desc
                }
            ),
            FROM_HTML_MODE_COMPACT
        )
    }

    /**
     * Create a [WarnableTextInputValidator.OnTextValidate] for filename field.
     */
    @JvmStatic
    fun createFilenameValidator(
        useAzeEncrypt: AppCompatCheckBox,
        extraCondition: () -> Boolean = { true }
    ) = { text: String ->
        if (text.isNotBlank() && filenameIsValid(text, useAzeEncrypt) && extraCondition.invoke()) {
            ReturnState()
        } else if (text.isBlank()) {
            ReturnState(STATE_ERROR, R.string.field_empty)
        } else if (!text.endsWith(CRYPT_EXTENSION) &&
            (useAzeEncrypt.visibility == INVISIBLE || useAzeEncrypt.isChecked)
        ) {
            ReturnState(STATE_ERROR, R.string.encrypt_file_must_end_with_aze)
        } else if (!text.endsWith(AESCRYPT_EXTENSION) &&
            useAzeEncrypt.visibility == VISIBLE && !useAzeEncrypt.isChecked
        ) {
            ReturnState(STATE_ERROR, R.string.encrypt_file_must_end_with_aes)
        } else {
            ReturnState(STATE_ERROR, R.string.empty_string)
        }
    }

    /**
     * Utility method to check if the given filename is valid as encrypted file
     */
    @JvmStatic
    fun filenameIsValid(
        filename: String?,
        useAzeEncrypt: AppCompatCheckBox
    ): Boolean {
        return (
            true == filename?.isNotBlank() && filename.endsWith(CRYPT_EXTENSION) &&
                (useAzeEncrypt.visibility == INVISIBLE || useAzeEncrypt.isChecked)
            ) || (
            true == filename?.isNotBlank() && filename.endsWith(AESCRYPT_EXTENSION) &&
                (useAzeEncrypt.visibility == VISIBLE && !useAzeEncrypt.isChecked)
            )
    }
}
