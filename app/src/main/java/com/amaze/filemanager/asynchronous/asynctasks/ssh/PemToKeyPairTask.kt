/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.os.AsyncTask
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import com.amaze.filemanager.ui.views.WarnableTextInputValidator.ReturnState
import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.security.KeyPair

/**
 * [AsyncTask] to convert given [InputStream] into [KeyPair] which is requird by
 * sshj, using [JcaPEMKeyConverter].
 *
 * @see JcaPEMKeyConverter
 *
 * @see KeyProvider
 *
 * @see OpenSSHKeyV1KeyFile
 *
 * @see PuTTYKeyFile
 *
 * @see com.amaze.filemanager.filesystem.ssh.SshConnectionPool.create
 * @see net.schmizz.sshj.SSHClient.authPublickey
 */
class PemToKeyPairTask(
    private val pemFile: ByteArray,
    private val callback: AsyncTaskResult.Callback<KeyPair?>
) :
    AsyncTask<Void, IOException?, KeyPair?>() {
    private val converters = arrayOf(
        JcaPemToKeyPairConverter(),
        OpenSshPemToKeyPairConverter(),
        OpenSshV1PemToKeyPairConverter(),
        PuttyPrivateKeyToKeyPairConverter()
    )
    private var paused = false
    private var passwordFinder: PasswordFinder? = null
    private var errorMessage: String? = null

    constructor(pemFile: InputStream, callback: AsyncTaskResult.Callback<KeyPair?>) :
        this(IOUtils.readFully(pemFile).toByteArray(), callback)
    constructor(pemContent: String, callback: AsyncTaskResult.Callback<KeyPair?>) :
        this(pemContent.toByteArray(), callback)

    override fun doInBackground(vararg voids: Void): KeyPair? {
        while (true) {
            if (isCancelled) {
                return null
            }
            if (paused) {
                continue
            }
            for (converter in converters) {
                val keyPair = converter.convert(String(pemFile))
                if (keyPair != null) {
                    paused = false
                    return keyPair
                }
            }
            if (passwordFinder != null) {
                errorMessage = AppConfig
                    .getInstance()
                    .getString(R.string.ssh_key_invalid_passphrase)
            }
            paused = true
            publishProgress(IOException("No converter available to parse selected PEM"))
        }
    }

    override fun onProgressUpdate(vararg values: IOException?) {
        super.onProgressUpdate(*values)
        if (values.isEmpty()) {
            return
        }
        val result = values[0]
        val builder = MaterialDialog.Builder(AppConfig.getInstance().mainActivityContext!!)
        val dialogLayout = View.inflate(
            AppConfig.getInstance().mainActivityContext,
            R.layout.dialog_singleedittext,
            null
        )
        val wilTextfield: WarnableTextInputLayout =
            dialogLayout.findViewById(R.id.singleedittext_warnabletextinputlayout)
        val textfield = dialogLayout.findViewById<EditText>(R.id.singleedittext_input)
        textfield.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder
            .customView(dialogLayout, false)
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
                paused = false
                dialog.dismiss()
            }
            .negativeText(R.string.cancel)
            .onNegative { dialog: MaterialDialog, which: DialogAction? ->
                dialog.dismiss()
                toastOnParseError(result!!)
                cancel(true)
            }
        val dialog = builder.show()
        WarnableTextInputValidator(
            AppConfig.getInstance().mainActivityContext,
            textfield,
            wilTextfield,
            dialog.getActionButton(DialogAction.POSITIVE)
        ) { text: String ->
            if (text.isEmpty()) {
                ReturnState(
                    ReturnState.STATE_ERROR, R.string.field_empty
                )
            }
            ReturnState()
        }
        if (errorMessage != null) {
            wilTextfield.error = errorMessage
            textfield.selectAll()
        }
    }

    override fun onPostExecute(result: KeyPair?) {
        callback.onResult(result)
    }

    private fun toastOnParseError(result: IOException) {
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
        fun convert(source: String?): KeyPair? = runCatching {
            throwingConvert(source)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()

        @Throws(Exception::class)
        protected abstract fun throwingConvert(source: String?): KeyPair?
    }

    private inner class JcaPemToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        override fun throwingConvert(source: String?): KeyPair? {
            val pemParser = PEMParser(StringReader(source))
            val keyPair = pemParser.readObject() as PEMKeyPair
            val converter = JcaPEMKeyConverter()
            return converter.getKeyPair(keyPair)
        }
    }

    private inner class OpenSshPemToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        public override fun throwingConvert(source: String?): KeyPair {
            val converter = OpenSSHKeyFile()
            converter.init(StringReader(source), passwordFinder)
            return KeyPair(converter.public, converter.private)
        }
    }

    private inner class OpenSshV1PemToKeyPairConverter : PemToKeyPairConverter() {
        @Throws(Exception::class)
        public override fun throwingConvert(source: String?): KeyPair {
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
