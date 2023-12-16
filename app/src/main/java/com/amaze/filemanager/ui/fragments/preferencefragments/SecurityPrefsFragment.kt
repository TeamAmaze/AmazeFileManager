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

package com.amaze.filemanager.ui.fragments.preferencefragments

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.views.preference.CheckBox
import com.amaze.filemanager.utils.PasswordUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

class SecurityPrefsFragment : BasePrefsFragment() {

    private val log: Logger = LoggerFactory.getLogger(SecurityPrefsFragment::class.java)

    override val title = R.string.security

    private var masterPasswordPreference: Preference? = null
    private var keyguardManager: KeyguardManager? = null
    private var fingerprintManager: FingerprintManager? = null
    private val onClickFingerprint = Preference.OnPreferenceChangeListener { _, _ ->
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.USE_FINGERPRINT
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                activity,
                resources.getString(R.string.crypt_fingerprint_no_permission),
                Toast.LENGTH_LONG
            )
                .show()
            false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            fingerprintManager?.hasEnrolledFingerprints() == false
        ) {
            Toast.makeText(
                activity,
                resources.getString(R.string.crypt_fingerprint_not_enrolled),
                Toast.LENGTH_LONG
            )
                .show()
            false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            keyguardManager?.isKeyguardSecure == false
        ) {
            Toast.makeText(
                activity,
                resources.getString(R.string.crypt_fingerprint_no_security),
                Toast.LENGTH_LONG
            )
                .show()
            false
        } else {
            masterPasswordPreference?.isEnabled = false
            true
        }
    }
    private val onClickMasterPassword = Preference.OnPreferenceClickListener {
        val masterPasswordDialogBuilder = MaterialDialog.Builder(activity)
        masterPasswordDialogBuilder.title(
            resources.getString(R.string.crypt_pref_master_password_title)
        )

        var decryptedPassword: String? = null
        try {
            val preferencePassword = activity.prefs.getString(
                PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT
            )!!
            decryptedPassword = if (
                preferencePassword !=
                PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT
            ) {

                // password is set, try to decrypt
                PasswordUtil.decryptPassword(activity, preferencePassword)
            } else {
                // no password set in preferences, just leave the field empty
                ""
            }
        } catch (e: GeneralSecurityException) {
            log.warn("failed to decrypt master password", e)
        } catch (e: IOException) {
            log.warn("failed to decrypt master password", e)
        }

        masterPasswordDialogBuilder.input(
            resources.getString(R.string.authenticate_password),
            decryptedPassword,
            true
        ) { _, _ -> }
        masterPasswordDialogBuilder.theme(
            activity.utilsProvider.appTheme.getMaterialDialogTheme()
        )
        masterPasswordDialogBuilder.positiveText(resources.getString(R.string.ok))
        masterPasswordDialogBuilder.negativeText(resources.getString(R.string.cancel))
        masterPasswordDialogBuilder.positiveColor(activity.accent)
        masterPasswordDialogBuilder.negativeColor(activity.accent)

        masterPasswordDialogBuilder.onPositive { dialog, _ ->
            try {
                val inputText = dialog.inputEditText!!.text.toString()
                if (inputText !=
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT
                ) {
                    val editor = activity.prefs.edit()
                    editor.putString(
                        PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                        PasswordUtil.encryptPassword(
                            activity,
                            dialog.inputEditText!!.text.toString()
                        )
                    )
                    editor.apply()
                } else {
                    val editor = activity.prefs.edit()
                    editor.putString(PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD, "")
                    editor.apply()
                }
            } catch (e: GeneralSecurityException) {
                log.warn("failed to encrypt master password", e)
                val editor = activity.prefs.edit()
                editor.putString(
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT
                )
                editor.apply()
            } catch (e: IOException) {
                log.warn("failed to encrypt master password", e)
                val editor = activity.prefs.edit()
                editor.putString(
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD,
                    PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD_DEFAULT
                )
                editor.apply()
            }
        }

        masterPasswordDialogBuilder.onNegative { dialog, _ -> dialog.cancel() }

        masterPasswordDialogBuilder.build().show()

        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.security_prefs, rootKey)

        masterPasswordPreference = findPreference(
            PreferencesConstants.PREFERENCE_CRYPT_MASTER_PASSWORD
        )
        val checkBoxFingerprint = findPreference<CheckBox>(
            PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 ||
            activity.prefs.getBoolean(
                    PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT,
                    false
                )
        ) {
            // encryption feature not available
            masterPasswordPreference?.isEnabled = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // finger print sensor
            keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE)
                as KeyguardManager?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fingerprintManager = activity.getSystemService(Context.FINGERPRINT_SERVICE)
                    as FingerprintManager?
                if (fingerprintManager?.isHardwareDetected == true) {
                    checkBoxFingerprint?.isEnabled = true
                }
            }

            checkBoxFingerprint?.onPreferenceChangeListener = onClickFingerprint
        } else {
            // fingerprint manager class not defined in the framework
            checkBoxFingerprint?.isEnabled = false
        }

        masterPasswordPreference?.onPreferenceClickListener = onClickMasterPassword
    }
}
