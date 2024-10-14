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

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import com.amaze.filemanager.R
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.DecryptButtonCallbackInterface
import com.amaze.filemanager.ui.activities.MainActivity
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
        IOException::class,
    )
    fun show(
        c: Context,
        main: MainActivity,
        intent: Intent,
        decryptButtonCallbackInterface: DecryptButtonCallbackInterface,
    ) {
        val manager = BiometricManager.from(c)
        if (manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS) {
            val promptInfo =
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(c.getString(R.string.crypt_decrypt))
                    .setDescription(c.getString(R.string.crypt_fingerprint_authenticate))
                    .setConfirmationRequired(false)
                    .setNegativeButtonText(c.getString(android.R.string.cancel))
                    .build()

            val handler = FingerprintHandler(main, intent, promptInfo, decryptButtonCallbackInterface)
            val `object` = BiometricPrompt.CryptoObject(CryptUtil.initCipher())
            handler.authenticate(`object`)
        }
    }
}
