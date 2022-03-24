package com.amaze.filemanager.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
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
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER
import com.amaze.filemanager.ui.openKeyboard
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.amaze.filemanager.ui.views.WarnableTextInputValidator.ReturnState
import com.amaze.filemanager.ui.views.WarnableTextInputValidator.ReturnState.STATE_ERROR
import com.amaze.filemanager.utils.SimpleTextWatcher
import com.google.android.material.textfield.TextInputEditText

/**
 * Encrypt file password dialog.
 */
object EncryptAuthenticateDialog {

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
            val passwordConfirmEditText: TextInputEditText = vb.editTextDialogEncryptPasswordConfirm
            val encryptSaveAsEditText: TextInputEditText = vb.editTextEncryptSaveAs
            val useAzeEncrypt: AppCompatCheckBox = vb.checkboxUseAze
            useAzeEncrypt.setOnCheckedChangeListener(
                createUseAzeEncryptCheckboxOnCheckedChangeListener(
                    c,
                    this,
                    preferences,
                    main,
                    encryptSaveAsEditText
                )
            )
            val textInputLayoutPassword: WarnableTextInputLayout = vb.tilEncryptPassword
            val textInputLayoutPasswordConfirm: WarnableTextInputLayout = vb.tilEncryptPasswordConfirm
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
                .theme(appTheme.getMaterialDialogTheme(c))
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
                            intent, passwordEditText.text.toString()
                        )
                    }.onFailure {
                        Log.e(EncryptService.TAG, "Failed to encrypt", it)
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
            val textWatcher: TextWatcher = object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    btnOK.isEnabled =
                        encryptSaveAsEditText.text.toString().isNotEmpty() && passwordEditText.text.toString()
                        .isNotEmpty() && passwordConfirmEditText.text.toString().isNotEmpty()
                }
            }
            passwordEditText.addTextChangedListener(textWatcher)
            passwordConfirmEditText.addTextChangedListener(textWatcher)
            encryptSaveAsEditText.addTextChangedListener(textWatcher)
            createPasswordFieldValidator(
                c,
                passwordEditText,
                passwordConfirmEditText,
                textInputLayoutPassword,
                btnOK
            )
            createPasswordFieldValidator(
                c,
                passwordConfirmEditText,
                passwordEditText,
                textInputLayoutPasswordConfirm,
                btnOK
            )
            WarnableTextInputValidator(
                c,
                encryptSaveAsEditText,
                textInputLayoutEncryptSaveAs,
                btnOK,
                createFilenameValidator(useAzeEncrypt)
            )
        } ?: throw IllegalArgumentException("No TAG_SOURCE parameter specified")
    }

    private fun createPasswordFieldValidator(
        c: Context,
        passwordField: TextInputEditText,
        comparingPasswordField: TextInputEditText,
        warningTextInputLayout: WarnableTextInputLayout,
        btnOK: MDButton
    ) = WarnableTextInputValidator(
        c,
        passwordField,
        warningTextInputLayout,
        btnOK
    ) { text: String ->
        if (text.isNotEmpty() && text == comparingPasswordField.text.toString()) {
            ReturnState()
        } else if (text.isEmpty()) {
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
        encryptSaveAsEditText: TextInputEditText
    ) = { _: CompoundButton?, isChecked: Boolean ->
        if (isChecked && !preferences.getBoolean(PREFERENCE_CRYPT_WARNING_REMEMBER, false)) {
            EncryptWarningDialog.show(main, main.appTheme)
        }
        encryptSaveAsEditText.setText(
            "${file.getName(c)}${if (isChecked) {
                CRYPT_EXTENSION
            } else {
                AESCRYPT_EXTENSION
            }}"
        )
    }

    /**
     * Create a [WarnableTextInputValidator.OnTextValidate] for filename field.
     */
    @JvmStatic
    fun createFilenameValidator(useAzeEncrypt: AppCompatCheckBox) = { text: String ->
        if (text.isEmpty()) {
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
            ReturnState()
        }
    }
}
