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
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDButton
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.ftp.AbstractGetHostInfoTask
import com.amaze.filemanager.asynchronous.asynctasks.ftp.hostcert.FtpsGetHostCertificateTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.GetSshHostFingerprintTask
import com.amaze.filemanager.asynchronous.asynctasks.ssh.PemToKeyPairTask
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.databinding.SftpDialogBinding
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.activities.superclasses.ThemedActivity
import com.amaze.filemanager.ui.icons.MimeTypes
import com.amaze.filemanager.ui.provider.UtilitiesProvider
import com.amaze.filemanager.utils.BookSorter
import com.amaze.filemanager.utils.DataUtils
import com.amaze.filemanager.utils.MinMaxInputFilter
import com.amaze.filemanager.utils.SimpleTextWatcher
import com.amaze.filemanager.utils.X509CertificateUtil.FINGERPRINT
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.schmizz.sshj.common.SecurityUtils
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.lang.ref.WeakReference
import java.security.KeyPair
import java.security.PublicKey
import java.util.*
import java.util.concurrent.Callable

/** SSH/SFTP connection setup dialog.  */
class SftpConnectDialog : DialogFragment() {

    companion object {

        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(SftpConnectDialog::class.java)

        const val ARG_NAME = "name"
        const val ARG_EDIT = "edit"
        const val ARG_ADDRESS = "address"
        const val ARG_PORT = "port"
        const val ARG_PROTOCOL = "protocol"
        const val ARG_USERNAME = "username"
        const val ARG_PASSWORD = "password"
        const val ARG_DEFAULT_PATH = "defaultPath"
        const val ARG_HAS_PASSWORD = "hasPassword"
        const val ARG_KEYPAIR_NAME = "keypairName"

        private val VALID_PORT_RANGE = IntRange(1, 65535)
    }

    lateinit var ctx: WeakReference<Context>
    private var selectedPem: Uri? = null
    private var selectedParsedKeyPair: KeyPair? = null
    private var selectedParsedKeyPairName: String? = null
    private var oldPath: String? = null

    lateinit var binding: SftpDialogBinding

    @Suppress("ComplexMethod")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        ctx = WeakReference(activity)
        binding = SftpDialogBinding.inflate(LayoutInflater.from(context))
        val utilsProvider: UtilitiesProvider = AppConfig.getInstance().utilsProvider
        val edit = requireArguments().getBoolean(ARG_EDIT, false)

        initForm(edit)

        val accentColor = (activity as ThemedActivity).accent

        // Use system provided action to get Uri to PEM.
        binding.selectPemBTN.setOnClickListener {
            val intent = Intent()
                .setType(MimeTypes.ALL_MIME_TYPES)
                .setAction(Intent.ACTION_GET_CONTENT)
            activityResultHandlerForPemSelection.launch(intent)
        }

        // Define action for buttons
        val dialogBuilder = MaterialDialog.Builder(ctx.get()!!)
            .title(R.string.scp_connection)
            .autoDismiss(false)
            .customView(binding.root, true)
            .theme(utilsProvider.appTheme.getMaterialDialogTheme(context))
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
        protocolDropDown.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            requireContext().resources.getStringArray(R.array.ftpProtocols)
        )
        chkFtpAnonymous.setOnCheckedChangeListener { _, isChecked ->
            usernameET.isEnabled = !isChecked
            passwordET.isEnabled = !isChecked
            if (isChecked) {
                usernameET.setText("")
                passwordET.setText("")
            }
        }

        // If it's new connection setup, set some default values
        // Otherwise, use given Bundle instance for filling in the blanks
        if (!edit) {
            connectionET.setText(R.string.scp_connection)
            portET.setText(NetCopyClientConnectionPool.SSH_DEFAULT_PORT.toString())
            protocolDropDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    portET.setText(
                        when (position) {
                            1 -> NetCopyClientConnectionPool.FTP_DEFAULT_PORT.toString()
                            2 -> NetCopyClientConnectionPool.FTPS_DEFAULT_PORT.toString()
                            else -> NetCopyClientConnectionPool.SSH_DEFAULT_PORT.toString()
                        }
                    )
                    chkFtpAnonymous.visibility = when (position) {
                        0 -> View.GONE
                        else -> View.VISIBLE
                    }
                    if (position == 0) {
                        chkFtpAnonymous.isChecked = false
                    }
                    selectPemBTN.visibility = when (position) {
                        0 -> View.VISIBLE
                        else -> View.GONE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        } else {
            protocolDropDown.setSelection(
                when (requireArguments().getString(ARG_PROTOCOL)) {
                    FTP_URI_PREFIX -> 1
                    FTPS_URI_PREFIX -> 2
                    else -> 0
                }
            )
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
            oldPath = NetCopyClientUtils.encryptFtpPathAsNecessary(
                NetCopyClientUtils.deriveUriFrom(
                    requireArguments().getString(ARG_PROTOCOL)!!,
                    requireArguments().getString(ARG_ADDRESS)!!,
                    requireArguments().getInt(ARG_PORT),
                    requireArguments().getString(ARG_DEFAULT_PATH, ""),
                    requireArguments().getString(ARG_USERNAME)!!,
                    requireArguments().getString(ARG_PASSWORD),
                    selectedParsedKeyPair
                )
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
                    val path = NetCopyClientUtils.encryptFtpPathAsNecessary(
                        NetCopyClientUtils.deriveUriFrom(
                            getProtocolPrefixFromDropdownSelection(),
                            hostname,
                            port,
                            defaultPath,
                            username,
                            requireArguments().getString(ARG_PASSWORD, null),
                            selectedParsedKeyPair
                        )
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
                okBTN.isEnabled = (
                    true == binding.connectionET.text?.isNotEmpty() &&
                        true == binding.ipET.text?.isNotEmpty() &&
                        port in VALID_PORT_RANGE &&
                        true == binding.usernameET.text?.isNotEmpty() &&
                        hasCredential
                    ) || (
                    binding.chkFtpAnonymous.isChecked &&
                        binding.protocolDropDown.selectedItemPosition > 0
                    )
            }
        }
    }

    private fun handleOnPositiveButton(edit: Boolean):
        MaterialDialog.SingleButtonCallback =
        MaterialDialog.SingleButtonCallback { _, _ ->
            createConnectionSettings().run {
                when (prefix) {
                    FTP_URI_PREFIX -> positiveButtonForFtp(this, edit)
                    else -> positiveButtonForSftp(this, edit)
                }
            }
        }

    private fun positiveButtonForFtp(connectionSettings: ConnectionSettings, edit: Boolean) {
        connectionSettings.run {
            authenticateAndSaveSetup(connectionSettings = connectionSettings, isEdit = edit)
        }
    }

    /*
     * for SSH and FTPS, get host's cert/public key fingerprint.
     */
    private fun positiveButtonForSftp(connectionSettings: ConnectionSettings, edit: Boolean) {
        connectionSettings.run {
            // Get original SSH host key
            AppConfig.getInstance().utilsHandler.getRemoteHostKey(
                NetCopyClientUtils.deriveUriFrom(
                    prefix,
                    hostname,
                    port,
                    defaultPath,
                    username,
                    arguments?.getString(ARG_PASSWORD, null),
                    selectedParsedKeyPair
                )
            )?.let { sshHostKey ->
                NetCopyClientConnectionPool.removeConnection(
                    this.toUriString()
                ) {
                    if (prefix == FTPS_URI_PREFIX) {
                        reconnectToFtpsServerToVerifyHostFingerprint(
                            this,
                            JSONObject(sshHostKey),
                            edit
                        )
                    } else {
                        reconnectToSshServerToVerifyHostFingerprint(this, sshHostKey, edit)
                    }
                }
            } ?: run {
                if (prefix == FTPS_URI_PREFIX) {
                    firstConnectToFtpsServer(this, edit)
                } else {
                    firstConnectToSftpServer(this, edit)
                }
            }
        }
    }

    /*
     * Used by firstConnectToFtpsServer() and firstConnectToSftpServer().
     */
    private val createFirstConnectCallback:
        (Boolean, ConnectionSettings, String, String, String, JSONObject?) -> Unit = {
                edit,
                connectionSettings,
                hostAndPort,
                hostKeyAlgorithm,
                hostKeyFingerprint,
                hostInfo ->
            AlertDialog.Builder(ctx.get())
                .setTitle(R.string.ssh_host_key_verification_prompt_title)
                .setMessage(
                    getString(
                        R.string.ssh_host_key_verification_prompt,
                        hostAndPort,
                        hostKeyAlgorithm,
                        hostKeyFingerprint
                    )
                ).setCancelable(true)
                .setPositiveButton(R.string.yes) {
                        dialog1: DialogInterface, _: Int ->
                    // This closes the host fingerprint verification dialog
                    dialog1.dismiss()
                    if (authenticateAndSaveSetup(
                            connectionSettings,
                            hostInfo?.toString() ?: hostKeyFingerprint,
                            edit
                        )
                    ) {
                        dialog1.dismiss()
                        log.debug("Saved setup")
                        dismiss()
                    }
                }.setNegativeButton(R.string.no) {
                        dialog1: DialogInterface, _: Int ->
                    dialog1.dismiss()
                }.show()
        }

    private fun firstConnectToFtpsServer(
        connectionSettings: ConnectionSettings,
        edit: Boolean
    ) = connectionSettings.run {
        connectToSecureServerInternal(
            FtpsGetHostCertificateTask(
                hostname,
                port,
                requireContext()
            ) { hostInfo ->
                createFirstConnectCallback.invoke(
                    edit,
                    this,
                    StringBuilder(hostname).also {
                        if (port != NetCopyClientConnectionPool.FTPS_DEFAULT_PORT && port > 0) {
                            it.append(':').append(port)
                        }
                    }.toString(),
                    "SHA-256",
                    hostInfo.getString(FINGERPRINT),
                    hostInfo
                )
            }
        )
    }

    private fun firstConnectToSftpServer(
        connectionSettings: ConnectionSettings,
        edit: Boolean
    ) = connectionSettings.run {
        connectToSecureServerInternal(
            GetSshHostFingerprintTask(
                hostname,
                port,
                true
            ) { hostKey: PublicKey ->
                createFirstConnectCallback.invoke(
                    edit,
                    this,
                    StringBuilder(hostname).also {
                        if (port != NetCopyClientConnectionPool.SSH_DEFAULT_PORT && port > 0) {
                            it.append(':').append(port)
                        }
                    }.toString(),
                    hostKey.algorithm,
                    SecurityUtils.getFingerprint(hostKey),
                    null
                )
            }
        )
    }

    private val createReconnectSecureServerCallback:
        (ConnectionSettings, String, String, () -> Boolean, Boolean) -> Unit = {
                connectionSettings, oldHostIdentity, newHostIdentity, hostIdentityIsValid, edit ->
            if (hostIdentityIsValid.invoke()) {
                authenticateAndSaveSetup(
                    connectionSettings,
                    oldHostIdentity,
                    edit
                )
            } else {
                AlertDialog.Builder(ctx.get())
                    .setTitle(
                        R.string.ssh_connect_failed_host_key_changed_title
                    ).setMessage(
                        R.string.ssh_connect_failed_host_key_changed_prompt
                    ).setPositiveButton(
                        R.string.update_host_key
                    ) { _: DialogInterface?, _: Int ->
                        authenticateAndSaveSetup(
                            connectionSettings,
                            newHostIdentity,
                            edit
                        )
                    }.setNegativeButton(R.string.cancel_recommended) {
                            dialog1: DialogInterface, _: Int ->
                        dialog1.dismiss()
                    }.show()
            }
        }

    private fun reconnectToSshServerToVerifyHostFingerprint(
        connectionSettings: ConnectionSettings,
        sshHostKey: String,
        edit: Boolean
    ) {
        connectionSettings.run {
            connectToSecureServerInternal(
                GetSshHostFingerprintTask(hostname, port, false) {
                        currentHostKey: PublicKey ->
                    SecurityUtils.getFingerprint(currentHostKey).let {
                            currentHostKeyFingerprint ->
                        createReconnectSecureServerCallback(
                            connectionSettings,
                            sshHostKey,
                            currentHostKeyFingerprint,
                            { currentHostKeyFingerprint == sshHostKey },
                            edit
                        )
                    }
                }
            )
        }
    }

    private fun reconnectToFtpsServerToVerifyHostFingerprint(
        connectionSettings: ConnectionSettings,
        ftpsHostInfo: JSONObject,
        edit: Boolean
    ) {
        connectionSettings.run {
            connectToSecureServerInternal(
                FtpsGetHostCertificateTask(
                    hostname,
                    port,
                    requireContext()
                ) { hostInfo: JSONObject ->
                    createReconnectSecureServerCallback(
                        connectionSettings,
                        ftpsHostInfo.toString(),
                        hostInfo.toString(),
                        { ftpsHostInfo.getString(FINGERPRINT) == hostInfo.getString(FINGERPRINT) },
                        edit
                    )
                }
            )
        }
    }

    private fun <V, T : Callable<V>> connectToSecureServerInternal(
        task: AbstractGetHostInfoTask<V, T>
    ) {
        Single.fromCallable(task.getTask())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { task.onPreExecute() }
            .subscribe(task::onFinish, task::onError)
    }

    @Suppress("LabeledExpression")
    private val activityResultHandlerForPemSelection = registerForActivityResult(
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
                    log.error("Error reading PEM key", it)
                }
            }
        }
    }

    private fun authenticateAndSaveSetup(
        connectionSettings: ConnectionSettings,
        hostKeyFingerprint: String? = null,
        isEdit: Boolean
    ): Boolean = connectionSettings.run {
        val path = this.toUriString()
        val encryptedPath = NetCopyClientUtils.encryptFtpPathAsNecessary(path)
        return if (!isEdit) {
            saveFtpConnectionAndLoadlist(
                connectionSettings,
                hostKeyFingerprint,
                path,
                encryptedPath,
                selectedParsedKeyPairName,
                selectedParsedKeyPair
            )
        } else {
            updateFtpConnection(
                connectionName,
                hostKeyFingerprint,
                encryptedPath
            )
        }
    }

    @Suppress("LongParameterList")
    private fun saveFtpConnectionAndLoadlist(
        connectionSettings: ConnectionSettings,
        hostKeyFingerprint: String?,
        path: String,
        encryptedPath: String,
        selectedParsedKeyPairName: String?,
        selectedParsedKeyPair: KeyPair?
    ): Boolean {
        connectionSettings.run {
            return runCatching {
                NetCopyClientConnectionPool.getConnection(
                    prefix,
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
                        ma?.loadlist(
                            path,
                            false,
                            if (prefix == SSH_URI_PREFIX) {
                                OpenMode.SFTP
                            } else {
                                OpenMode.FTP
                            },
                            false
                        )
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
                log.warn("Problem getting connection and load file list", it)
                false
            }
        }
    }

    private fun updateFtpConnection(
        connectionName: String,
        hostKeyFingerprint: String?,
        encryptedPath: String
    ): Boolean {
        val i = DataUtils.getInstance().containsServer(oldPath)

        if (i != -1) {
            DataUtils.getInstance().removeServer(i)
        }

        DataUtils.getInstance().addServer(arrayOf(connectionName, encryptedPath))
        Collections.sort(DataUtils.getInstance().servers, BookSorter())
        (activity as MainActivity).drawer.refreshDrawer()
        AppConfig.getInstance().runInBackground {
            AppConfig.getInstance().utilsHandler.updateSsh(
                connectionName,
                requireArguments().getString(ARG_NAME)!!,
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

    private fun getProtocolPrefixFromDropdownSelection(): String {
        return when (binding.protocolDropDown.selectedItem.toString()) {
            requireContext().getString(R.string.protocol_ftp) -> FTP_URI_PREFIX
            requireContext().getString(R.string.protocol_ftps) -> FTPS_URI_PREFIX
            else -> SSH_URI_PREFIX
        }
    }

    private data class ConnectionSettings(
        val prefix: String,
        val connectionName: String,
        val hostname: String,
        val port: Int,
        val defaultPath: String? = null,
        val username: String,
        val password: String? = null,
        val selectedParsedKeyPairName: String? = null,
        val selectedParsedKeyPair: KeyPair? = null
    ) {
        fun toUriString() = NetCopyClientUtils.deriveUriFrom(
            prefix,
            hostname,
            port,
            defaultPath,
            username,
            password,
            selectedParsedKeyPair
        )
    }

    private fun createConnectionSettings() =
        ConnectionSettings(
            prefix = getProtocolPrefixFromDropdownSelection(),
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
