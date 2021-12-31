package com.amaze.filemanager.asynchronous.asynctasks.ssh.pem

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.MainThread
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.asynchronous.asynctasks.fromTask
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.ReadTextFileCallable
import com.amaze.filemanager.asynchronous.asynctasks.texteditor.read.ReadTextFileTask
import com.amaze.filemanager.exceptions.SshKeyInvalidPassphrase
import com.amaze.filemanager.ui.activities.texteditor.ReturnedValueOnReadFile
import com.amaze.filemanager.ui.views.WarnableTextInputLayout
import com.amaze.filemanager.ui.views.WarnableTextInputValidator
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.security.KeyPair

class PemToKeyPairTask(
        private val appContextWR: WeakReference<Context>,
        private val pemFile: ByteArray,
        private val passwordFinder: PasswordFinder?,
        private val callback: AsyncTaskResult.Callback<KeyPair?>?
): Task<KeyPair, PemToKeyPairCallable> {

    constructor(appContextWR: WeakReference<Context>, pemFile: InputStream, callback: AsyncTaskResult.Callback<KeyPair?>?) :
            this(appContextWR, IOUtils.readFully(pemFile).toByteArray(), null, callback)
    constructor(appContextWR: WeakReference<Context>, pemContent: String, callback: AsyncTaskResult.Callback<KeyPair?>?) :
            this(appContextWR, pemContent.toByteArray(), null, callback)

    private val task: PemToKeyPairCallable

    init {
        task = PemToKeyPairCallable(pemFile, passwordFinder)
    }

    override fun getTask(): PemToKeyPairCallable = task

    @MainThread
    override fun onError(error: Throwable) {
        if (error !is IOException) {
            return
        }

        val result = error
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
                    val passwordFinder = object : PasswordFinder {
                        override fun reqPassword(resource: Resource<*>?): CharArray {
                            return textfield.text.toString().toCharArray()
                        }

                        override fun shouldRetry(resource: Resource<*>?): Boolean {
                            return false
                        }
                    }
                    dialog.dismiss()
                    fromTask(PemToKeyPairTask(appContextWR, pemFile, passwordFinder, callback))
                }
                .negativeText(R.string.cancel)
                .onNegative { dialog: MaterialDialog, which: DialogAction? ->
                    dialog.dismiss()
                    toastOnParseError(result)
                    callback?.onResult(null)
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
                        WarnableTextInputValidator.ReturnState.STATE_ERROR, R.string.field_empty
                )
            }
            WarnableTextInputValidator.ReturnState()
        }

        val context = appContextWR.get()
        if (error is SshKeyInvalidPassphrase && context != null) {
            wilTextfield.error = context.resources.getString(R.string.ssh_key_invalid_passphrase)
            textfield.selectAll()
        }
    }

    override fun onFinish(value: KeyPair) {
        callback?.onResult(value)
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

}