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
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
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
import java.util.*

/** SSH/SFTP connection setup dialog.  */
class SftpConnectDialog : DialogFragment() {

    private val TAG = SftpConnectDialog::class.java.simpleName

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_EDIT = "edit"
        private const val ARG_ADDRESS = "address"
        private const val ARG_PORT = "port"
        private const val ARG_USERNAME = "username"
        private const val ARG_PASSWORD = "password"
        private const val ARG_DEFAULT_PATH = "defaultPath"

        private val VALID_PORT_RANGE = IntRange(1, 65535)

        // Idiotic code
        // FIXME: agree code on
        private const val SELECT_PEM_INTENT = 0x0101
    }

    private var utilsHandler: UtilsHandler? = null
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

    @Suppress("ComplexMethod", "LongMethod")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        ctx = WeakReference(activity)
        _binding = SftpDialogBinding.inflate(LayoutInflater.from(context))
        utilsHandler = AppConfig.getInstance().utilsHandler
        val utilsProvider: UtilitiesProvider = AppConfig.getInstance().utilsProvider
        val edit = arguments!!.getBoolean(ARG_EDIT, false)

        initForm(edit)

        val accentColor = (activity as ThemedActivity).accent

        // Use system provided action to get Uri to PEM.
        binding.selectPemBTN.setOnClickListener {
            val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(intent, SELECT_PEM_INTENT)
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
            .onPositive { _: MaterialDialog, _: DialogAction ->
                val connectionName = binding.connectionET.text.toString()
                val hostname = binding.ipET.text.toString()
                val port = binding.portET.text.toString().toInt()
                val defaultPath = binding.defaultPathET.text.toString()
                val username = binding.usernameET.text.toString()
                val password = if (binding.passwordET.text!!.isEmpty()) {
                    arguments!!.getString(ARG_PASSWORD, null)
                } else {
                    binding.passwordET.text.toString()
                }

                // Get original SSH host key
                utilsHandler!!.getSshHostKey(
                    SshClientUtils.deriveSftpPathFrom(
                        hostname,
                        port,
                        defaultPath,
                        username,
                        arguments!!.getString(ARG_PASSWORD, null),
                        selectedParsedKeyPair
                    )
                )?.let { sshHostKey ->
                    SshConnectionPool.getInstance()
                        .removeConnection(
                            SshClientUtils.deriveSftpPathFrom(
                                hostname,
                                port,
                                defaultPath,
                                username,
                                password,
                                selectedParsedKeyPair
                            )
                        ) {
                            GetSshHostFingerprintTask(hostname, port) {
                                taskResult: AsyncTaskResult<PublicKey?> ->
                                taskResult.result?.let { hostKey ->
                                    val hostKeyFingerprint = SecurityUtils.getFingerprint(hostKey)
                                    if (hostKeyFingerprint == sshHostKey) {
                                        authenticateAndSaveSetup(
                                            connectionName,
                                            hostname,
                                            port,
                                            defaultPath,
                                            sshHostKey,
                                            username,
                                            password,
                                            selectedParsedKeyPair,
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
                                                    connectionName,
                                                    hostname,
                                                    port,
                                                    defaultPath,
                                                    hostKeyFingerprint,
                                                    username,
                                                    password,
                                                    selectedParsedKeyPair,
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
                } ?: run {
                    GetSshHostFingerprintTask(
                        hostname,
                        port
                    ) { taskResult: AsyncTaskResult<PublicKey?> ->
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
                                            connectionName,
                                            hostname,
                                            port,
                                            defaultPath,
                                            hostKeyFingerprint,
                                            username,
                                            password,
                                            selectedParsedKeyPair,
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
            }.onNegative { dialog: MaterialDialog, _: DialogAction? ->
                dialog.dismiss()
            }

        // If we are editing connection settings, give new actions for neutral and negative buttons
        if (edit) {
            dialogBuilder
                .negativeText(R.string.delete)
                .onNegative { dialog: MaterialDialog, _: DialogAction? ->
                    val connectionName = binding.connectionET.text.toString()
                    val hostname = binding.ipET.text.toString()
                    val port = binding.portET.text.toString().toInt()
                    val defaultPath = binding.defaultPathET.text.toString()
                    val username = binding.usernameET.text.toString()
                    val path = SshClientUtils.deriveSftpPathFrom(
                        hostname,
                        port,
                        defaultPath,
                        username,
                        arguments!!.getString(ARG_PASSWORD, null),
                        selectedParsedKeyPair
                    )
                    val i = DataUtils.getInstance().containsServer(arrayOf(connectionName, path))
                    if (i > -1) {
                        DataUtils.getInstance().removeServer(i)
                        AppConfig.getInstance()
                            .runInBackground {
                                utilsHandler!!.removeFromDatabase(
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
        val dialog = dialogBuilder.build()

        // Some validations to make sure the Create/Update button is clickable only when required
        // setting values are given
        val okBTN: View = dialog.getActionButton(DialogAction.POSITIVE)
        if (!edit) okBTN.isEnabled = false
        val validator: TextWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                val portETValue = binding.portET.text.toString()
                val port = if (portETValue.isDigitsOnly() && (portETValue.length in 1..5)) {
                    portETValue.toInt()
                } else {
                    -1
                }
                val hasCredential = if (edit) {
                    if (binding.passwordET.text!!.isNotEmpty() ||
                        !TextUtils.isEmpty(arguments!!.getString(ARG_PASSWORD))
                    ) {
                        true
                    } else {
                        selectedParsedKeyPairName!!.isNotEmpty()
                    }
                } else {
                    binding.passwordET.text!!.isNotEmpty() || selectedParsedKeyPair != null
                }
                okBTN.isEnabled = binding.connectionET.text!!.isNotEmpty() &&
                    binding.ipET.text!!.isNotEmpty() &&
                    port in VALID_PORT_RANGE &&
                    binding.usernameET.text!!.isNotEmpty() &&
                    hasCredential
            }
        }
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
            connectionET.setText(arguments!!.getString(ARG_NAME))
            ipET.setText(arguments!!.getString(ARG_ADDRESS))
            portET.setText(arguments!!.getInt(ARG_PORT).toString())
            defaultPathET.setText(arguments!!.getString(ARG_DEFAULT_PATH))
            usernameET.setText(arguments!!.getString(ARG_USERNAME))
            if (arguments!!.getBoolean("hasPassword")) {
                passwordET.setHint(R.string.password_unchanged)
            } else {
                selectedParsedKeyPairName = arguments!!.getString("keypairName")
                selectPemBTN.text = selectedParsedKeyPairName
            }
            oldPath = SshClientUtils.deriveSftpPathFrom(
                arguments!!.getString(ARG_ADDRESS)!!,
                arguments!!.getInt(ARG_PORT),
                arguments!!.getString(ARG_DEFAULT_PATH, ""),
                arguments!!.getString(ARG_USERNAME)!!,
                arguments!!.getString(ARG_PASSWORD),
                selectedParsedKeyPair
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SELECT_PEM_INTENT == requestCode && Activity.RESULT_OK == resultCode) {
            selectedPem = data!!.data
            runCatching {
                ctx!!.get()!!.contentResolver.openInputStream(selectedPem!!)?.let {
                    selectedKeyContent ->
                    PemToKeyPairTask(selectedKeyContent) { result: KeyPair? ->
                        selectedParsedKeyPair = result
                        selectedParsedKeyPairName = selectedPem!!
                            .lastPathSegment!!
                            .substring(
                                selectedPem!!.lastPathSegment!!
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

    @Suppress("LongParameterList")
    private fun authenticateAndSaveSetup(
        connectionName: String,
        hostname: String,
        port: Int,
        defaultPath: String?,
        hostKeyFingerprint: String,
        username: String,
        password: String?,
        selectedParsedKeyPair: KeyPair?,
        isEdit: Boolean
    ): Boolean {
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
                connectionName,
                hostname,
                port,
                hostKeyFingerprint,
                username,
                password,
                path,
                encryptedPath
            )
        } else {
            updateSshConnection(connectionName, hostKeyFingerprint, path, encryptedPath)
        }
    }

    @Suppress("LongParameterList")
    private fun saveSshConnection(
        connectionName: String,
        hostname: String,
        port: Int,
        hostKeyFingerprint: String,
        username: String,
        password: String?,
        path: String,
        encryptedPath: String
    ): Boolean {
        return runCatching {
            SshConnectionPool.getInstance().getConnection(
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
                    utilsHandler!!.saveToDatabase(
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
                        activity!!.findViewById(R.id.content_frame),
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
            utilsHandler!!.updateSsh(
                connectionName,
                arguments!!.getString(ARG_NAME),
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
    private fun getPemContents(): String? = if (selectedPem == null) {
        null
    } else {
        runCatching {
            ctx!!.get()!!.contentResolver.openInputStream(selectedPem!!)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
        }.getOrNull()
    }
}
