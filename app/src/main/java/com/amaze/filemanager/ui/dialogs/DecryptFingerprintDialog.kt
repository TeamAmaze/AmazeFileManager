package com.amaze.filemanager.ui.dialogs

import android.content.Context
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.DecryptButtonCallbackInterface
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.FingerprintHandler
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Decrypt dialog prompt for user fingerprint.
 */
object DecryptFingerprintDialog {

    /**
     * Display dialog prompting user for fingerprint in order to decrypt file.
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Throws(
        GeneralSecurityException::class,
        IOException::class
    )
    fun show(
        c: Context,
        main: MainActivity,
        intent: Intent,
        appTheme: AppTheme,
        decryptButtonCallbackInterface: DecryptButtonCallbackInterface
    ) {
        val accentColor = main.accent
        val builder = MaterialDialog.Builder(c)
        builder.title(c.getString(R.string.crypt_decrypt))
        val rootView = View.inflate(c, R.layout.dialog_decrypt_fingerprint_authentication, null)
        val cancelButton = rootView.findViewById<Button>(R.id.button_decrypt_fingerprint_cancel)
        cancelButton.setTextColor(accentColor)
        builder.customView(rootView, true)
        builder.canceledOnTouchOutside(false)
        builder.theme(appTheme.getMaterialDialogTheme(c))
        val dialog = builder.show()
        cancelButton.setOnClickListener { v: View? -> dialog.cancel() }
        val manager = c.getSystemService(FingerprintManager::class.java)
        val handler = FingerprintHandler(c, intent, dialog, decryptButtonCallbackInterface)
        val `object` = FingerprintManager.CryptoObject(CryptUtil.initCipher())
        handler.authenticate(manager, `object`)
    }
}