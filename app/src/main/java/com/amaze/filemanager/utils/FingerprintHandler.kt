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
package com.amaze.filemanager.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils.DecryptButtonCallbackInterface
import com.amaze.filemanager.ui.activities.MainActivity

/** Created by vishal on 15/4/17.  */
@RequiresApi(api = Build.VERSION_CODES.M)
class FingerprintHandler(
    private val mainActivity: MainActivity,
    private val decryptIntent: Intent,
    private val promptInfo: PromptInfo,
    private val decryptButtonCallbackInterface: DecryptButtonCallbackInterface,
) : BiometricPrompt.AuthenticationCallback() {
    /**
     * Authenticate user to perform decryption.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun authenticate(cryptoObject: BiometricPrompt.CryptoObject) {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.USE_FINGERPRINT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prompt =
            BiometricPrompt(mainActivity, ContextCompat.getMainExecutor(mainActivity), this)
        prompt.authenticate(promptInfo, cryptoObject)
    }

    override fun onAuthenticationError(
        errMsgId: Int,
        errString: CharSequence,
    ) = Unit

    override fun onAuthenticationFailed() {
        decryptButtonCallbackInterface.failed()
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        decryptButtonCallbackInterface.confirm(decryptIntent)
    }
}
