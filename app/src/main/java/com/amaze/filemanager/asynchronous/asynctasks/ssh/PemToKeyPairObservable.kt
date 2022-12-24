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

package com.amaze.filemanager.asynchronous.asynctasks.ssh

import android.text.InputType
import android.view.LayoutInflater
import android.widget.Toast
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.DialogSingleedittextBinding
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.security.KeyPair

class PemToKeyPairObservable(private val pemFile: ByteArray) : ObservableOnSubscribe<KeyPair> {

    private val converters = arrayOf(
        JcaPemToKeyPairConverter(),
        OpenSshPemToKeyPairConverter(),
        OpenSshV1PemToKeyPairConverter(),
        PuttyPrivateKeyToKeyPairConverter()
    )
    private var passwordFinder: PasswordFinder? = null
    private var errorMessage: String? = null

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PemToKeyPairObservable::class.java)
    }

    constructor(pemFile: InputStream) : this(pemFile.readBytes())
    constructor(pemContent: String) : this(pemContent.toByteArray())

    override fun subscribe(emitter: ObservableEmitter<KeyPair>) {
        for (converter in converters) {
            val keyPair = converter.convert(String(pemFile))
            if (keyPair != null) {
                emitter.onNext(keyPair)
                emitter.onComplete()
                return
            }
        }
        if (passwordFinder != null) {
            errorMessage = AppConfig
                .getInstance()
                .getString(R.string.ssh_key_invalid_passphrase)
        } else {
            errorMessage = AppConfig
                .getInstance()
                .getString(R.string.ssh_key_no_decoder_decrypt)
        }
        emitter.onError(IOException(errorMessage))
    }

    /**
     * For generating the callback when decoding the PEM failed. Opens dialog and prompt for
     * password.
     */
    fun displayPassphraseDialog(
        exception: Throwable,
        positiveCallback: (() -> Unit),
        negativeCallback: (() -> Unit)
    ) {
        val builder = MaterialDialog.Builder(
            AppConfig.getInstance().mainActivityContext!!
        )
        val dialogLayout = DialogSingleedittextBinding.inflate(
            LayoutInflater.from(AppConfig.getInstance().mainActivityContext)
        )
        val wilTextfield: WarnableTextInputLayout =
            dialogLayout.singleedittextWarnabletextinputlayout
        val textfield = dialogLayout.singleedittextInput
        textfield.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder
            .customView(dialogLayout.root, false)
            .autoDismiss(false)
            .title(R.string.ssh_key_prompt_passphrase)
            .positiveText(R.string.ok)
            .onPositive { dialog: MaterialDialog, which: DialogAction? ->
                passwordFinder = object : PasswordFinder {
                    override fun reqPassword(resource: Resource<*>?): CharArray {
                        return textfield.text.toString().toCharArray()
                    }
                    override fun shouldRetry(resource: Resource<*>?): Boolean {
                        return false
                    }
                }
                dialog.dismiss()
                positiveCallback.invoke()
            }
            .negativeText(R.string.cancel)
            .onNegative { dialog: MaterialDialog, which: DialogAction? ->
                dialog.dismiss()
                toastOnParseError(exception)
                negativeCallback.invoke()
            }
        val dialog = builder.show()
        WarnableTextInputValidator(
            AppConfig.getInstance().mainActivityContext,
            textfield,
            wilTextfield,
            dialog.getActionButton(DialogAction.POSITIVE)
        ) { text: String ->
            if (text.isEmpty()) {
                WarnableTextInputValidator.ReturnState(
                    WarnableTextInputValidator.ReturnState.STATE_ERROR,
                    R.string.field_empty
                )
            }
            WarnableTextInputValidator.ReturnState()
        }
        if (errorMessage != null) {
            wilTextfield.error = errorMessage
            textfield.selectAll()
        }
    }

    private fun toastOnParseError(result: Throwable) {
        Toast.makeText(
            AppConfig.getInstance().mainActivityContext,
            AppConfig.getInstance()
                .resources
                .getString(R.string.ssh_pem_key_parse_error, result.localizedMessage),
            Toast.LENGTH_LONG
        )
            .show()
    }

    private abstract inner class PemToKeyPairConverter {
        fun convert(source: String): KeyPair? = runCatching {
            throwingConvert(source)
        }.onFailure {
            log.warn("failed to convert pem to keypair", it)
        }.getOrNull()

        protected abstract fun throwingConvert(source: String?): KeyPair?
    }

    private inner class JcaPemToKeyPairConverter : PemToKeyPairConverter() {
        override fun throwingConvert(source: String?): KeyPair? {
            val pemParser = PEMParser(StringReader(source))
            val keyPair = pemParser.readObject() as PEMKeyPair?
            val converter = JcaPEMKeyConverter()
            return converter.getKeyPair(keyPair)
        }
    }

    private inner class OpenSshPemToKeyPairConverter : PemToKeyPairConverter() {
        override fun throwingConvert(source: String?): KeyPair {
            val converter = OpenSSHKeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }

    private inner class OpenSshV1PemToKeyPairConverter : PemToKeyPairConverter() {
        override fun throwingConvert(source: String?): KeyPair {
            val converter = OpenSSHKeyV1KeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }

    private inner class PuttyPrivateKeyToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        public override fun throwingConvert(source: String?): KeyPair {
            val converter = PuTTYKeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }
}
