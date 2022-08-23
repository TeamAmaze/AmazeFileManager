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

import android.content.Intent
import android.os.Environment
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDButton
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.services.EncryptService
import com.amaze.filemanager.asynchronous.services.EncryptService.TAG_SOURCE
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.RandomPathGenerator
import com.amaze.filemanager.filesystem.files.CryptUtil.AESCRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.CryptUtil.CRYPT_EXTENSION
import com.amaze.filemanager.filesystem.files.EncryptDecryptUtils
import com.amaze.filemanager.test.getString
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.ENCRYPT_PASSWORD_FINGERPRINT
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.ENCRYPT_PASSWORD_MASTER
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_CRYPT_FINGERPRINT_DEFAULT
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.google.android.material.textfield.TextInputEditText
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowDialog
import java.io.File
import kotlin.random.Random

class EncryptWithPresetPasswordSaveAsDialogTest : AbstractEncryptDialogTests() {

    private val randomizer = Random(System.currentTimeMillis())
    private lateinit var file: File
    private lateinit var tilFileSaveAs: WarnableTextInputLayout
    private lateinit var editTextFileSaveAs: TextInputEditText
    private lateinit var checkboxUseAze: AppCompatCheckBox
    private lateinit var textViewCryptInfo: AppCompatTextView
    private lateinit var okButton: MDButton

    /**
     * MainActivity setup.
     */
    @Before
    override fun setUp() {
        super.setUp()
        file = File(
            Environment.getExternalStorageDirectory(),
            RandomPathGenerator.generateRandomPath(
                randomizer,
                16
            )
        )
    }

    /**
     * Post test cleanup.
     */
    @After
    override fun tearDown() {
        super.tearDown()
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit().putBoolean(
                PREFERENCE_CRYPT_FINGERPRINT,
                PREFERENCE_CRYPT_FINGERPRINT_DEFAULT
            ).apply()
    }

    /**
     * Test case when fingerprint encrypt option is enabled.
     *
     * Ensure optional checkbox is disabled - Fingerprint encryption cannot do AESCrypt.
     */
    @Test
    fun testWhenFingerprintOptionEnabled() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putBoolean(PREFERENCE_CRYPT_FINGERPRINT, true)
            .apply()
        performTest(
            testContent = { _, _, _ ->
                assertEquals("${file.name}$CRYPT_EXTENSION", editTextFileSaveAs.text.toString())
                assertEquals(INVISIBLE, checkboxUseAze.visibility)
                assertEquals(INVISIBLE, textViewCryptInfo.visibility)
            },
            callback = object : EncryptDecryptUtils.EncryptButtonCallbackInterface {
                override fun onButtonPressed(intent: Intent, password: String) {
                    assertEquals(ENCRYPT_PASSWORD_FINGERPRINT, password)
                    assertEquals(file.absolutePath, intent.getStringExtra(TAG_SOURCE))
                    assertFalse(intent.getBooleanExtra(EncryptService.TAG_AESCRYPT, true))
                    assertEquals(
                        "${file.name}$CRYPT_EXTENSION",
                        intent.getStringExtra(EncryptService.TAG_ENCRYPT_TARGET)
                    )
                }
            }
        )
    }

    /**
     * Test filename validation when fingerprint option enabled.
     * Shall never let but .aze go through
     */
    @Test
    fun testFilenameValidationWhenFingerprintOptionEnabled() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putBoolean(PREFERENCE_CRYPT_FINGERPRINT, true)
            .apply()
        performTest(testContent = { _, _, _ ->
            editTextFileSaveAs.setText("${file.name}.error")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.encrypt_file_must_end_with_aze),
                tilFileSaveAs.error
            )
            editTextFileSaveAs.setText("${file.name}.aes")
            assertFalse(okButton.isEnabled)
            assertEquals(
                getString(R.string.encrypt_file_must_end_with_aze),
                tilFileSaveAs.error
            )
        })
    }

    /**
     * Test filename validation when fingerprint option enabled.
     * Shall never let but .aze go through
     */
    @Test
    fun testFilenameValidationWhenFingerprintOptionDisabled() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putBoolean(PREFERENCE_CRYPT_FINGERPRINT, false)
            .apply()
        performTest(
            password = ENCRYPT_PASSWORD_MASTER,
            testContent = { _, _, _ ->
                editTextFileSaveAs.setText("${file.name}.error")
                assertFalse(okButton.isEnabled)
                assertEquals(
                    getString(R.string.encrypt_file_must_end_with_aes),
                    tilFileSaveAs.error
                )
                editTextFileSaveAs.setText("${file.name}.aze")
                assertFalse(okButton.isEnabled)
                assertEquals(
                    getString(R.string.encrypt_file_must_end_with_aes),
                    tilFileSaveAs.error
                )
                checkboxUseAze.isChecked = true
                assertTrue(okButton.isEnabled)
                assertNull(tilFileSaveAs.error)
            }
        )
    }

    /**
     * Test case when fingerprint option is disabled.
     *
     * Must be master password then. Sorry, no validation at this point - upstream is responsible
     * for that.
     */
    @Test
    fun testWhenFingerprintOptionDisabled() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putBoolean(PREFERENCE_CRYPT_FINGERPRINT, false)
            .apply()
        performTest(
            password = ENCRYPT_PASSWORD_MASTER,
            testContent = { _, _, _ ->
                assertEquals("${file.name}$AESCRYPT_EXTENSION", editTextFileSaveAs.text.toString())
                assertEquals(VISIBLE, checkboxUseAze.visibility)
                assertEquals(VISIBLE, textViewCryptInfo.visibility)
            },
            callback = object : EncryptDecryptUtils.EncryptButtonCallbackInterface {
                override fun onButtonPressed(intent: Intent, password: String) {
                    assertEquals(ENCRYPT_PASSWORD_MASTER, password)
                    assertEquals(file.absolutePath, intent.getStringExtra(TAG_SOURCE))
                    assertTrue(intent.getBooleanExtra(EncryptService.TAG_AESCRYPT, false))
                    assertEquals(
                        "${file.name}$AESCRYPT_EXTENSION",
                        intent.getStringExtra(EncryptService.TAG_ENCRYPT_TARGET)
                    )
                }
            }
        )
    }

    /**
     * Test invalid password put into the dialog argument, which shall never happen.
     */
    @Test(expected = IllegalArgumentException::class)
    fun testWithInvalidFixedPasswordArgument() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putBoolean(PREFERENCE_CRYPT_FINGERPRINT, false)
            .apply()
        performTest(
            password = "abcdefgh",
            testContent = { _, _, _ -> }
        )
    }

    /**
     * Test logic when aze encryption checkbox is ticked.
     */
    @Test
    fun testAzecryptCheckbox() {
        performTest(
            password = ENCRYPT_PASSWORD_MASTER,
            testContent = { _, _, _ ->
                checkboxUseAze.isChecked = true
                assertEquals(
                    HtmlCompat.fromHtml(
                        getString(R.string.encrypt_option_use_azecrypt_desc),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                        .toString(),
                    textViewCryptInfo.text.toString()
                )
                assertTrue(ShadowDialog.getShownDialogs().size == 2)
                assertTrue(ShadowDialog.getLatestDialog() is MaterialDialog)
                (ShadowDialog.getLatestDialog() as MaterialDialog).run {
                    assertEquals(getString(R.string.warning), titleView.text)
                    assertEquals(
                        getString(R.string.crypt_warning_key),
                        contentView?.text.toString()
                    )
                    assertEquals(
                        getString(R.string.warning_never_show),
                        getActionButton(DialogAction.NEGATIVE).text
                    )
                    assertEquals(
                        getString(R.string.warning_confirm),
                        getActionButton(DialogAction.POSITIVE).text
                    )
                    assertTrue(getActionButton(DialogAction.POSITIVE).performClick())
                }
                assertEquals(2, ShadowDialog.getShownDialogs().size)
                assertFalse(ShadowDialog.getLatestDialog().isShowing)
                assertTrue(true == editTextFileSaveAs.text?.endsWith(CRYPT_EXTENSION))
                checkboxUseAze.isChecked = false
                assertEquals(
                    HtmlCompat.fromHtml(
                        getString(R.string.encrypt_option_use_aescrypt_desc),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                        .toString(),
                    textViewCryptInfo.text.toString()
                )
                assertEquals(2, ShadowDialog.getShownDialogs().size)
                assertFalse(ShadowDialog.getLatestDialog().isShowing)
                assertTrue(true == editTextFileSaveAs.text?.endsWith(AESCRYPT_EXTENSION))
                checkboxUseAze.isChecked = true
                assertEquals(
                    HtmlCompat.fromHtml(
                        getString(R.string.encrypt_option_use_azecrypt_desc),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                        .toString(),
                    textViewCryptInfo.text.toString()
                )
                assertEquals(3, ShadowDialog.getShownDialogs().size)
                assertTrue(ShadowDialog.getLatestDialog().isShowing)
                assertTrue(true == editTextFileSaveAs.text?.endsWith(CRYPT_EXTENSION))
                (ShadowDialog.getLatestDialog() as MaterialDialog)
                    .getActionButton(DialogAction.NEGATIVE).performClick()
                assertEquals(3, ShadowDialog.getShownDialogs().size)
                assertFalse(ShadowDialog.getLatestDialog().isShowing)
                assertTrue(
                    PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
                        .getBoolean(PreferencesConstants.PREFERENCE_CRYPT_WARNING_REMEMBER, false)
                )
                checkboxUseAze.isChecked = false
                assertEquals(3, ShadowDialog.getShownDialogs().size) // no new dialog
                checkboxUseAze.isChecked = true
                assertEquals(3, ShadowDialog.getShownDialogs().size)
            }
        )
    }

    private fun performTest(
        testContent: (dialog: MaterialDialog, intent: Intent, activity: MainActivity) -> Unit,
        password: String = ENCRYPT_PASSWORD_FINGERPRINT,
        callback: EncryptDecryptUtils.EncryptButtonCallbackInterface =
            object : EncryptDecryptUtils.EncryptButtonCallbackInterface {}
    ) {
        scenario.onActivity { activity ->
            Intent().putExtra(TAG_SOURCE, HybridFileParcelable(file.absolutePath)).let { intent ->
                EncryptWithPresetPasswordSaveAsDialog.show(
                    activity,
                    intent,
                    activity,
                    password,
                    callback
                )
                ShadowDialog.getLatestDialog()?.run {
                    assertTrue(this is MaterialDialog)
                    (this as MaterialDialog).let {
                        editTextFileSaveAs = findViewById<TextInputEditText>(
                            R.id.edit_text_encrypt_save_as
                        )
                        tilFileSaveAs = findViewById<WarnableTextInputLayout>(
                            R.id.til_encrypt_save_as
                        )
                        checkboxUseAze = findViewById<AppCompatCheckBox>(R.id.checkbox_use_aze)
                        textViewCryptInfo = findViewById<AppCompatTextView>(
                            R.id.text_view_crypt_info
                        )
                        okButton = getActionButton(DialogAction.POSITIVE)
                        testContent.invoke(it, intent, activity)
                    }
                } ?: fail("Dialog cannot be seen?")
            }
        }
    }
}
