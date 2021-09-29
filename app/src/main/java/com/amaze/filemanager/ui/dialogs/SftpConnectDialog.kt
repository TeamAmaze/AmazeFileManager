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

package com.amaze.filemanager.ui.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDButton
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.asynchronous.asynctasks.ssh.GetSshHostFingerprintTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairTask
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.databinding.SftpDialogBinding
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.ssh.SshClientUtils
import com.amaze.filemanager.filesystem.ssh.SshConnectionPool
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.icons.MimeTypes
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.amaze.filemanager.utils.BookSorter
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.MinMaxInputFilter
import com.amaze.filemanager.utils.SimpleTextWatcher
import com.google.android.material.snackbar.Snackbar
import net.schmizz.sshj.common.SecurityUtils
import java.io.BufferedReader
import java.lang.ref.WeakReference
import java.security.KeyPair
import java.security.PublicKey
import java.util.Collections

/** SSH/SFTP connection setup dialog.  */
class SftpConnectDialog : DialogFragment() {

    private val TAG = SftpConnectDialog::class.java.simpleName

    companion object {
        const val ARG_NAME = "name"
        const val ARG_EDIT = "edit"
        const val ARG_ADDRESS = "address"
        const val ARG_PORT = "port"
        const val ARG_USERNAME = "username"
        const val ARG_PASSWORD = "password"
        const val ARG_DEFAULT_PATH = "defaultPath"
        const val ARG_HAS_PASSWORD = "hasPassword"
        const val ARG_KEYPAIR_NAME = "keypairName"

        private val VALID_PORT_RANGE = IntRange(1, 65535)
    }

    private var ctx: WeakReference<Context>? = null
    private var selectedPem: Uri? = null
    private var selectedParsedKeyPair: KeyPair? = null
    private var selectedParsedKeyPairName: String? = null
    private var oldPath: String? = null

    private var _binding: SftpDialogBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("ComplexMethod")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        ctx = WeakReference(activity)
        _binding = SftpDialogBinding.inflate(LayoutInflater.from(context))
        val utilsProvider: UtilitiesProvider = AppConfig.getInstance().utilsProvider
        val edit = requireArguments().getBoolean(ARG_EDIT, false)

        initForm(edit)

        val accentColor = (activity as ThemedActivity).accent

        // Use system provided action to get Uri to PEM.
        binding.selectPemBTN.setOnClickListener {
            val intent = Intent()
                .setType(MimeTypes.ALL_MIME_TYPES)
                .setAction(Intent.ACTION_GET_CONTENT)
            activityResultHandler.launch(intent)
        }

        // Define action for buttons
        val dialogBuilder = MaterialDialog.Builder(ctx!!.get()!!)
            .title(R.string.scp_connection)
            .autoDismiss(false)
            .customView(binding.root, true)
            .theme(utilsProvider.appTheme.materialDialogTheme)
            .negativeText(R.string.cancel)
            .positiveText(if (edit) R.string.update else R.string.create)
            .positiveColor(accentColor)
            .negativeColor(accentColor)
            .neutralColor(accentColor)
            .onPositive(handleOnPositiveButton(edit))
            .onNegative { dialog: MaterialDialog, _: DialogAction? ->
                dialog.dismiss()
            }

        // If we are editing connection settings, give new actions for neutral and negative buttons
        if (edit) {
            appendButtonListenersForEdit(dialogBuilder)
        }
        val dialog = dialogBuilder.build()

        // Some validations to make sure the Create/Update button is clickable only when required
        // setting values are given
        val okBTN: MDButton = dialog.getActionButton(DialogAction.POSITIVE)
        if (!edit) okBTN.isEnabled = false
        val validator: TextWatcher = createValidator(edit, okBTN)
        binding.ipET.addTextChangedListener(validator)
        binding.portET.addTextChangedListener(validator)
        binding.usernameET.addTextChangedListener(validator)
        binding.passwordET.addTextChangedListener(validator)
        return dialog
    }

    private fun initForm(edit: Boolean) = binding.run {
        portET.apply {
            filters = arrayOf(MinMaxInputFilter(VALID_PORT_RANGE))
            // For convenience, so I don't need to press backspace all the time
            onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    selectAll()
                }
            }
        }

        // If it's new connection setup, set some default values
        // Otherwise, use given Bundle instance for filling in the blanks
        if (!edit) {
            connectionET.setText(R.string.scp_connection)
            portET.setText(SshConnectionPool.SSH_DEFAULT_PORT.toString())
        } else {
            connectionET.setText(requireArguments().getString(ARG_NAME))
            ipET.setText(requireArguments().getString(ARG_ADDRESS))
            portET.setText(requireArguments().getInt(ARG_PORT).toString())
            defaultPathET.setText(requireArguments().getString(ARG_DEFAULT_PATH))
            usernameET.setText(requireArguments().getString(ARG_USERNAME))
            if (requireArguments().getBoolean(ARG_HAS_PASSWORD)) {
                passwordET.setHint(R.string.password_unchanged)
            } else {
                selectedParsedKeyPairName = requireArguments().getString(ARG_KEYPAIR_NAME)
                selectPemBTN.text = selectedParsedKeyPairName
            }
            oldPath = SshClientUtils.deriveSftpPathFrom(
                requireArguments().getString(ARG_ADDRESS)!!,
                requireArguments().getInt(ARG_PORT),
                requireArguments().getString(ARG_DEFAULT_PATH, ""),
                requireArguments().getString(ARG_USERNAME)!!,
                requireArguments().getString(ARG_PASSWORD),
                selectedParsedKeyPair
            )
        }
    }

    private fun appendButtonListenersForEdit(
        dialogBuilder: MaterialDialog.Builder
    ) {
        createConnectionSettings().run {
            dialogBuilder
                .negativeText(R.string.delete)
                .onNegative { dialog: MaterialDialog, _: DialogAction? ->
                    val path = SshClientUtils.deriveSftpPathFrom(
                        hostname,
                        port,
                        defaultPath,
                        username,
                        requireArguments().getString(ARG_PASSWORD, null),
                        selectedParsedKeyPair
                    )
                    val i = DataUtils.getInstance().containsServer(
                        arrayOf(connectionName, path)
                    )
                    if (i > -1) {
                        DataUtils.getInstance().removeServer(i)
                        AppConfig.getInstance()
                            .runInBackground {
                                AppConfig.getInstance().utilsHandler.removeFromDatabase(
                                    OperationData(
                                        UtilsHandler.Operation.SFTP,
                                        path,
                                        connectionName,
                                        null,
                                        null,
                                        null
                                    )
                                )
                            }
                        (activity as MainActivity).drawer.refreshDrawer()
                    }
                    dialog.dismiss()
                }.neutralText(R.string.cancel)
                .onNeutral { dialog: MaterialDialog, _: DialogAction? -> dialog.dismiss() }
        }
    }

    private fun createValidator(edit: Boolean, okBTN: MDButton): SimpleTextWatcher {
        return object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                val portETValue = binding.portET.text.toString()
                val port = if (portETValue.isDigitsOnly() && (portETValue.length in 1..5)) {
                    portETValue.toInt()
                } else {
                    -1
                }
                val hasCredential: Boolean = if (edit) {
                    if (true == binding.passwordET.text?.isNotEmpty() ||
                        !TextUtils.isEmpty(requireArguments().getString(ARG_PASSWORD))
                    ) {
                        true
                    } else {
                        true == selectedParsedKeyPairName?.isNotEmpty()
                    }
                } else {
                    true == binding.passwordET.text?.isNotEmpty() || selectedParsedKeyPair != null
                }
                okBTN.isEnabled = true == binding.connectionET.text?.isNotEmpty() &&
                    true == binding.ipET.text?.isNotEmpty() &&
                    port in VALID_PORT_RANGE &&
                    true == binding.usernameET.text?.isNotEmpty() &&
                    hasCredential
            }
        }
    }

    private fun handleOnPositiveButton(edit: Boolean):
        MaterialDialog.SingleButtonCallback =
            MaterialDialog.SingleButtonCallback { _, _ ->
                createConnectionSettings().run {
                    // Get original SSH host key
                    AppConfig.getInstance().utilsHandler.getSshHostKey(
                        SshClientUtils.deriveSftpPathFrom(
                            hostname,
                            port,
                            defaultPath,
                            username,
                            arguments?.getString(ARG_PASSWORD, null),
                            selectedParsedKeyPair
                        )
                    )?.let { sshHostKey ->
                        SshConnectionPool.removeConnection(
                            SshClientUtils.deriveSftpPathFrom(
                                hostname,
                                port,
                                defaultPath,
                                username,
                                password,
                                selectedParsedKeyPair
                            )
                        ) {
                            reconnectToServerToVerifyHostFingerprint(
                                this,
                                sshHostKey,
                                edit
                            )
                        }
                    } ?: firstConnectToServer(this, edit)
                }
            }

    private fun firstConnectToServer(
        connectionSettings: ConnectionSettings,
        edit: Boolean
    ) = connectionSettings.run {
        GetSshHostFingerprintTask(
            hostname,
            port
        ) { taskResult: AsyncTaskResult<PublicKey> ->
            taskResult.result?.run {
                val hostKeyFingerprint = SecurityUtils.getFingerprint(this)
                val hostAndPort = StringBuilder(hostname).also {
                    if (port != SshConnectionPool.SSH_DEFAULT_PORT && port > 0) {
                        it.append(':').append(port)
                    }
                }.toString()
                AlertDialog.Builder(ctx!!.get())
                    .setTitle(R.string.ssh_host_key_verification_prompt_title)
                    .setMessage(
                        getString(
                            R.string.ssh_host_key_verification_prompt,
                            hostAndPort,
                            algorithm,
                            hostKeyFingerprint
                        )
                    ).setCancelable(true)
                    .setPositiveButton(R.string.yes) {
                        dialog1: DialogInterface, _: Int ->
                        // This closes the host fingerprint verification dialog
                        dialog1.dismiss()
                        if (authenticateAndSaveSetup(
                                connectionSettings,
                                hostKeyFingerprint,
                                edit
                            )
                        ) {
                            dialog1.dismiss()
                            Log.d(TAG, "Saved setup")
                            dismiss()
                        }
                    }.setNegativeButton(R.string.no) {
                        dialog1: DialogInterface, _: Int ->
                        dialog1.dismiss()
                    }.show()
            }
        }.execute()
    }

    private fun reconnectToServerToVerifyHostFingerprint(
        connectionSettings: ConnectionSettings,
        sshHostKey: String,
        edit: Boolean
    ) {
        connectionSettings.run {
            GetSshHostFingerprintTask(hostname, port) {
                taskResult: AsyncTaskResult<PublicKey> ->
                taskResult.result?.let { hostKey ->
                    val hostKeyFingerprint = SecurityUtils.getFingerprint(hostKey)
                    if (hostKeyFingerprint == sshHostKey) {
                        authenticateAndSaveSetup(
                            connectionSettings,
                            sshHostKey,
                            edit
                        )
                    } else {
                        AlertDialog.Builder(ctx!!.get())
                            .setTitle(
                                R.string.ssh_connect_failed_host_key_changed_title
                            ).setMessage(
                                R.string.ssh_connect_failed_host_key_changed_prompt
                            ).setPositiveButton(
                                R.string.update_host_key
                            ) { _: DialogInterface?, _: Int ->
                                authenticateAndSaveSetup(
                                    connectionSettings,
                                    hostKeyFingerprint,
                                    edit
                                )
                            }.setNegativeButton(R.string.cancel_recommended) {
                                dialog1: DialogInterface, _: Int ->
                                dialog1.dismiss()
                            }.show()
                    }
                }
            }.execute()
        }
    }

    @Suppress("LabeledExpression")
    private val activityResultHandler = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Activity.RESULT_OK == it.resultCode) {
            it.data?.data?.run {
                selectedPem = this
                runCatching {
                    requireContext().contentResolver.openInputStream(this)?.let {
                        selectedKeyContent ->
                        PemToKeyPairTask(selectedKeyContent) { result: KeyPair? ->
                            selectedParsedKeyPair = result
                            selectedParsedKeyPairName = this
                                .lastPathSegment!!
                                .substring(
                                    this.lastPathSegment!!
                                        .indexOf('/') + 1
                                )
                            val okBTN = (dialog as MaterialDialog)
                                .getActionButton(DialogAction.POSITIVE)
                            okBTN.isEnabled = okBTN.isEnabled || true
                            binding.selectPemBTN.text = selectedParsedKeyPairName
                        }.execute()
                    }
                }.onFailure {
                    Log.e(TAG, "Error reading PEM key", it)
                }
            }
        }
    }

    private fun authenticateAndSaveSetup(
        connectionSettings: ConnectionSettings,
        hostKeyFingerprint: String,
        isEdit: Boolean
    ): Boolean = connectionSettings.run {
        val path = SshClientUtils.deriveSftpPathFrom(
            hostname,
            port,
            defaultPath,
            username,
            password,
            selectedParsedKeyPair
        )
        val encryptedPath = SshClientUtils.encryptSshPathAsNecessary(path)
        return if (!isEdit) {
            saveSshConnection(
                connectionSettings,
                hostKeyFingerprint,
                path,
                encryptedPath,
                selectedParsedKeyPairName,
                selectedParsedKeyPair
            )
        } else {
            updateSshConnection(connectionName, hostKeyFingerprint, path, encryptedPath)
        }
    }

    private fun saveSshConnection(
        connectionSettings: ConnectionSettings,
        hostKeyFingerprint: String,
        path: String,
        encryptedPath: String,
        selectedParsedKeyPairName: String?,
        selectedParsedKeyPair: KeyPair?
    ): Boolean {
        connectionSettings.run {
            return runCatching {
                SshConnectionPool.getConnection(
                    hostname,
                    port,
                    hostKeyFingerprint,
                    username,
                    password,
                    selectedParsedKeyPair
                )?.run {
                    if (DataUtils.getInstance().containsServer(path) == -1) {
                        DataUtils.getInstance().addServer(arrayOf(connectionName, path))
                        (activity as MainActivity).drawer.refreshDrawer()
                        AppConfig.getInstance().utilsHandler.saveToDatabase(
                            OperationData(
                                UtilsHandler.Operation.SFTP,
                                encryptedPath,
                                connectionName,
                                hostKeyFingerprint,
                                selectedParsedKeyPairName,
                                getPemContents()
                            )
                        )
                        val ma = (activity as MainActivity).currentMainFragment
                        ma?.loadlist(path, false, OpenMode.SFTP)
                        dismiss()
                    } else {
                        Snackbar.make(
                            requireActivity().findViewById(R.id.content_frame),
                            getString(R.string.connection_exists),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                    true
                } ?: false
            }.getOrElse {
                false
            }
        }
    }

    private fun updateSshConnection(
        connectionName: String,
        hostKeyFingerprint: String,
        path: String,
        encryptedPath: String
    ): Boolean {
        DataUtils.getInstance().removeServer(DataUtils.getInstance().containsServer(oldPath))
        DataUtils.getInstance().addServer(arrayOf(connectionName, path))
        Collections.sort(DataUtils.getInstance().servers, BookSorter())
        (activity as MainActivity).drawer.refreshDrawer()
        AppConfig.getInstance().runInBackground {
            AppConfig.getInstance().utilsHandler.updateSsh(
                connectionName,
                requireArguments().getString(ARG_NAME),
                encryptedPath,
                hostKeyFingerprint,
                selectedParsedKeyPairName,
                getPemContents()
            )
        }
        dismiss()
        return true
    }

    // Read the PEM content from InputStream to String.
    private fun getPemContents(): String? = selectedPem?.run {
        runCatching {
            requireContext().contentResolver.openInputStream(this)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
        }.getOrNull()
    }

    private data class ConnectionSettings(
        val connectionName: String,
        val hostname: String,
        val port: Int,
        val defaultPath: String? = null,
        val username: String,
        val password: String? = null,
        val selectedParsedKeyPairName: String? = null,
        val selectedParsedKeyPair: KeyPair? = null
    )

    private fun createConnectionSettings() =
        ConnectionSettings(
            connectionName = binding.connectionET.text.toString(),
            hostname = binding.ipET.text.toString(),
            port = binding.portET.text.toString().toInt(),
            defaultPath = binding.defaultPathET.text.toString(),
            username = binding.usernameET.text.toString(),
            password = if (true == binding.passwordET.text?.isEmpty()) {
                arguments?.getString(ARG_PASSWORD, null)
            } else {
                binding.passwordET.text.toString()
            },
            selectedParsedKeyPairName = this.selectedParsedKeyPairName,
            selectedParsedKeyPair = selectedParsedKeyPair
        )
}
