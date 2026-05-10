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

package com.amaze.filemanager.ui.fragments

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.O
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.DocumentsContract
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.provider.Settings
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import android.text.InputType
import android.text.Spanned
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.databinding.DialogFtpLoginBinding
import com.amaze.filemanager.databinding.FragmentFtpBinding
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.ftpserver.service.FtpEventBus
import com.amaze.filemanager.ftpserver.service.FtpPreferences
import com.amaze.filemanager.ftpserver.service.FtpServerEngine
import com.amaze.filemanager.ftpserver.service.FtpServerEvent
import com.amaze.filemanager.server.ServerRegistry
import com.amaze.filemanager.server.ServerType
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.runIfDocumentsUIExists
import com.amaze.filemanager.ui.theme.AppTheme
import com.amaze.filemanager.utils.NetworkUtil.getLocalInetAddress
import com.amaze.filemanager.utils.NetworkUtil.isConnectedToLocalNetwork
import com.amaze.filemanager.utils.NetworkUtil.isConnectedToWifi
import com.amaze.filemanager.utils.OneCharacterCharSequence
import com.amaze.filemanager.utils.PasswordUtil
import com.amaze.filemanager.utils.Utils
import com.amaze.filemanager.utils.registerReceiverCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Created by yashwanthreddyg on 10-06-2016. Edited by Luca D'Amico (Luca91) on 25 Jul 2017
 */
@Suppress("TooManyFunctions")
class FtpServerFragment : Fragment(R.layout.fragment_ftp) {
    private val log: Logger = LoggerFactory.getLogger(FtpServerFragment::class.java)

    private val statusText: AppCompatTextView get() = binding.textViewFtpStatus
    private val url: AppCompatTextView get() = binding.textViewFtpUrl
    private val username: AppCompatTextView get() = binding.textViewFtpUsername
    private val password: AppCompatTextView get() = binding.textViewFtpPassword
    private val port: AppCompatTextView get() = binding.textViewFtpPort
    private val sharedPath: AppCompatTextView get() = binding.textViewFtpPath
    private val ftpBtn: AppCompatButton get() = binding.startStopButton
    private val ftpPasswordVisibleButton: AppCompatImageButton get() = binding.ftpPasswordVisible
    private var accentColor = 0
    private var spannedStatusNoConnection: Spanned? = null
    private var spannedStatusConnected: Spanned? = null
    private var spannedStatusUrl: Spanned? = null
    private var spannedStatusSecure: Spanned? = null
    private var spannedStatusNotRunning: Spanned? = null
    private var snackbar: Snackbar? = null
    private var pendingBatteryOptimizationResult = false

    private var _binding: FragmentFtpBinding? = null
    private val binding get() = _binding!!

    private val mainActivity: MainActivity get() = requireActivity() as MainActivity

    private val activityResultHandlerOnFtpServerPathUpdate =
        createOpenDocumentTreeIntentCallback {
                directoryUri ->
            changeFTPServerPath(directoryUri.toString())
            updatePathText()
        }

    private val activityResultHandlerOnFtpServerPathGrantedSafAccess =
        createOpenDocumentTreeIntentCallback {
                directoryUri ->
            changeFTPServerPath(directoryUri.toString())
            updatePathText()
            doStartServer()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFtpBinding.inflate(inflater)
        accentColor = mainActivity.accent
        mainActivity.findViewById<CoordinatorLayout>(R.id.main_parent)
            .nextFocusDownId = R.id.startStopButton
        updateSpans()
        updateStatus()
        updateViews(mainActivity, binding)
        ftpBtn.setOnClickListener {
            ftpBtnOnClick()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                FtpEventBus.events.collect { event ->
                    onFtpReceiveActions(event)
                }
            }
        }
        return binding.root
    }

    private fun ftpBtnOnClick() {
        if (!FtpServerEngine.isRunning()) {
            if (isConnectedToWifi(requireContext()) ||
                isConnectedToLocalNetwork(requireContext())
            ) {
                startServer()
            } else {
                // no Wi-Fi and no eth, we shouldn't be here in the first place,
                // because of broadcast receiver, but just to be sure
                statusText.text = spannedStatusNoConnection
            }
        } else {
            stopServer()
        }
    }

    // Pending upgrading material-dialogs to simplify the logic here.
    @Suppress("ComplexMethod", "LongMethod")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.choose_ftp_port -> {
                val currentFtpPort = defaultPortFromPreferences
                MaterialDialog.Builder(requireContext())
                    .input(
                        getString(R.string.ftp_port_edit_menu_title),
                        currentFtpPort.toString(),
                        true,
                    ) { _: MaterialDialog?, _: CharSequence? -> }
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .onPositive { dialog: MaterialDialog, _: DialogAction? ->
                        val editText = dialog.inputEditText
                        if (editText != null) {
                            val name = editText.text.toString()
                            val portNumber = name.toIntOrNull()
                            if (portNumber == null || portNumber < 1024) {
                                Toast.makeText(
                                    activity,
                                    R.string.ftp_port_change_error_invalid,
                                    Toast.LENGTH_SHORT,
                                )
                                    .show()
                            } else {
                                changeFTPServerPort(portNumber)
                                Toast.makeText(
                                    activity,
                                    R.string.ftp_port_change_success,
                                    Toast.LENGTH_SHORT,
                                )
                                    .show()
                            }
                        }
                    }
                    .positiveText(getString(R.string.change).uppercase())
                    .negativeText(R.string.cancel)
                    .build()
                    .show()
                return true
            }
            R.id.ftp_path -> {
                if (shouldUseSafFileSystem()) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

                    intent.runIfDocumentsUIExists(mainActivity) {
                        activityResultHandlerOnFtpServerPathUpdate.launch(
                            intent,
                        )
                    }
                } else {
                    val dialogBuilder = FolderChooserDialog.Builder(requireActivity())
                    dialogBuilder
                        .chooseButton(R.string.choose_folder)
                        .initialPath(defaultPathFromPreferences)
                        .goUpLabel(getString(R.string.folder_go_up_one_level))
                        .cancelButton(R.string.cancel)
                        .tag(TAG)
                        .build()
                        .show(activity)
                }

                return true
            }
            R.id.ftp_login -> {
                val loginDialogBuilder = MaterialDialog.Builder(requireContext())
                val loginDialogView =
                    DialogFtpLoginBinding.inflate(LayoutInflater.from(requireContext())).apply {
                        initLoginDialogViews(this)
                        loginDialogBuilder.onPositive { _: MaterialDialog, _: DialogAction ->
                            if (checkboxFtpAnonymous.isChecked) {
                                // remove preferences
                                setFTPUsername("")
                                setFTPPassword("")
                            } else {
                                // password and username field not empty, let's set them to preferences
                                setFTPUsername(editTextDialogFtpUsername.text.toString())
                                setFTPPassword(editTextDialogFtpPassword.text.toString())
                            }
                        }
                    }
                val dialog =
                    loginDialogBuilder.customView(loginDialogView.root, true)
                        .title(getString(R.string.ftp_login))
                        .positiveText(getString(R.string.set).uppercase())
                        .negativeText(getString(R.string.cancel))
                        .build()

                // TextWatcher for port number was deliberately removed. It didn't work anyway, so
                // no reason to keep here. Pending reimplementation when material-dialogs lib is
                // upgraded.

                dialog.show()
                return true
            }
            R.id.checkbox_ftp_readonly -> {
                val shouldReadonly = !item.isChecked
                item.isChecked = shouldReadonly
                readonlyPreference = shouldReadonly
                updatePathText()
                promptUserToRestartServer()
                return true
            }
            R.id.checkbox_ftp_secure -> {
                val shouldSecure = !item.isChecked
                item.isChecked = shouldSecure
                securePreference = shouldSecure
                promptUserToRestartServer()
                return true
            }
            R.id.checkbox_ftp_legacy_filesystem -> {
                val shouldUseSafFileSystem = !item.isChecked
                item.isChecked = shouldUseSafFileSystem
                legacyFileSystemPreference = shouldUseSafFileSystem
                promptUserToRestartServer()
                return true
            }
            R.id.ftp_timeout -> {
                val timeoutBuilder = MaterialDialog.Builder(requireActivity())
                timeoutBuilder.title(
                    getString(R.string.ftp_timeout) +
                        " (" +
                        resources.getString(R.string.ftp_seconds) +
                        ")",
                )
                timeoutBuilder.input(
                    (
                        FtpPreferences.DEFAULT_TIMEOUT.toString() +
                            " " +
                            resources.getString(R.string.ftp_seconds)
                    ),
                    ftpTimeout.toString(),
                    true,
                ) { _: MaterialDialog?, input: CharSequence ->
                    val isInputInteger: Boolean =
                        try {
                            // try parsing for integer check
                            input.toString().toInt()
                            true
                        } catch (e: NumberFormatException) {
                            false
                        }
                    ftpTimeout =
                        if (input.isEmpty() || !isInputInteger) {
                            FtpPreferences.DEFAULT_TIMEOUT
                        } else {
                            Integer.valueOf(input.toString())
                        }
                }
                timeoutBuilder
                    .positiveText(resources.getString(R.string.set).uppercase())
                    .negativeText(resources.getString(R.string.cancel))
                    .build()
                    .show()
                return true
            }
            R.id.exit -> {
                requireActivity().finish()
                return true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        mainActivity.menuInflater.inflate(R.menu.ftp_server_menu, menu)
        menu.findItem(R.id.checkbox_ftp_readonly).isChecked = readonlyPreference
        menu.findItem(R.id.checkbox_ftp_secure).isChecked = securePreference
        menu.findItem(R.id.checkbox_ftp_legacy_filesystem).isChecked = legacyFileSystemPreference
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun shouldUseSafFileSystem(): Boolean {
        return mainActivity.prefs.getBoolean(
            FtpPreferences.KEY_PREFERENCE_SAF_FILESYSTEM,
            false,
        ) &&
            SDK_INT >= M
    }

    private val mWifiReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                // connected to Wi-Fi or eth
                if (isConnectedToLocalNetwork(context)) {
                    ftpBtn.isEnabled = true
                    dismissSnackbar()
                } else {
                    // Wi-Fi or eth connection lost
                    stopServer()
                    statusText.text = spannedStatusNoConnection
                    ftpBtn.isEnabled = false
                    ftpBtn.text = resources.getString(R.string.start_ftp).uppercase()
                    promptUserToEnableWireless()
                }
            }
        }

    /**
     * Handles messages sent from [FtpEventBus].
     *
     * @param signal as [FtpServerEvent]
     */
    @Suppress("StringLiteralDuplication")
    private fun onFtpReceiveActions(signal: FtpServerEvent) {
        updateSpans()
        when (signal) {
            FtpServerEvent.Started, FtpServerEvent.StartedFromTile -> {
                statusText.text =
                    if (securePreference) {
                        spannedStatusSecure
                    } else {
                        spannedStatusConnected
                    }

                url.text = spannedStatusUrl
                ftpBtn.text = resources.getString(R.string.stop_ftp).uppercase()
                ServerRegistry.getProvider(ServerType.FTP)
                    ?.getNotification()
                    ?.updateRunningNotification(
                        requireContext() ?: return,
                        FtpServerEvent.StartedFromTile == signal,
                    )
            }
            FtpServerEvent.FailedToStart -> {
                statusText.text = spannedStatusNotRunning
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show()
                ftpBtn.text = resources.getString(R.string.start_ftp).uppercase()
                url.text = "URL: "
            }
            FtpServerEvent.Stopped -> {
                statusText.text = spannedStatusNotRunning
                url.text = "URL: "
                ftpBtn.text = resources.getString(R.string.start_ftp).uppercase()
            }
        }
        updateStatus()
    }

    @Suppress("LabeledExpression")
    private fun createOpenDocumentTreeIntentCallback(callback: (directoryUri: Uri) -> Unit): ActivityResultLauncher<Intent> {
        return registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            if (it.resultCode == RESULT_OK && SDK_INT >= LOLLIPOP) {
                val directoryUri = it.data?.data ?: return@registerForActivityResult
                requireContext().contentResolver.takePersistableUriPermission(
                    directoryUri,
                    GRANT_URI_RW_PERMISSION,
                )
                callback.invoke(directoryUri)
            }
        }
    }

    /**
     * On API 23+, checks whether the app is exempt from battery optimizations before starting
     * the FTP server. If already exempt, or if the user has previously dismissed the prompt,
     * [callback] is invoked directly. Otherwise a [MaterialDialog] is shown with options to
     * open battery optimization settings, skip, or suppress future prompts.
     */
    private fun checkBatteryOptimizationIfNecessary(callback: () -> Unit) {
        if (SDK_INT < M) {
            callback()
            return
        }
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        val alreadyAsked =
            FtpPreferences.getPreferences(requireContext())
                .getBoolean(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED, false)
        if (pm.isIgnoringBatteryOptimizations(requireContext().packageName) || alreadyAsked) {
            callback()
            return
        }
        MaterialDialog.Builder(requireContext())
            .title(R.string.ftp_battery_optimization_title)
            .content(R.string.ftp_battery_optimization_message)
            .positiveText(R.string.ftp_battery_optimization_action_settings)
            .negativeText(R.string.ftp_battery_optimization_action_skip)
            .neutralText(R.string.ftp_battery_optimization_action_dont_ask)
            .onPositive { dialog, _ ->
                pendingBatteryOptimizationResult = true
                startActivity(
                    Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                )
                dialog.dismiss()
            }
            .onNegative { dialog, _ ->
                dialog.dismiss()
                callback()
            }
            .onNeutral { dialog, _ ->
                FtpPreferences.getPreferences(requireContext()).edit {
                    putBoolean(FtpPreferences.KEY_PREFERENCE_BATTERY_OPTIMIZATION_ASKED, true)
                }
                dialog.dismiss()
                callback()
            }
            .build()
            .show()
    }

    /** Check URI access. Prompt user to DocumentsUI if necessary */
    private fun checkUriAccessIfNecessary(callback: () -> Unit) {
        val directoryUri: String =
            mainActivity.prefs
                .getString(FtpPreferences.KEY_PREFERENCE_PATH, defaultPathFromPreferences)!!
        if (shouldUseSafFileSystem()) {
            directoryUri.toUri().run {
                if (requireContext().checkUriPermission(
                        this,
                        Process.myPid(),
                        Process.myUid(),
                        GRANT_URI_RW_PERMISSION,
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    mainActivity.accent.run {
                        val c = mainActivity.applicationContext

                        MaterialDialog.Builder(mainActivity)
                            .content(R.string.ftp_prompt_accept_first_start_saf_access)
                            .widgetColor(accentColor)
                            .theme(mainActivity.appTheme.getMaterialDialogTheme())
                            .title(R.string.ftp_prompt_accept_first_start_saf_access_title)
                            .positiveText(R.string.ok)
                            .positiveColor(accentColor)
                            .negativeText(R.string.cancel)
                            .negativeColor(accentColor)
                            .onPositive(
                                fun(
                                    dialog: MaterialDialog,
                                    _: DialogAction,
                                ) {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

                                    intent.runIfDocumentsUIExists(mainActivity) {
                                        activityResultHandlerOnFtpServerPathGrantedSafAccess.launch(
                                            intent.also {
                                                if (SDK_INT >= O &&
                                                    directoryUri.startsWith(defaultPathFromPreferences)
                                                ) {
                                                    it.putExtra(
                                                        EXTRA_INITIAL_URI,
                                                        DocumentsContract.buildDocumentUri(
                                                            "com.android.externalstorage.documents",
                                                            "primary:" +
                                                                directoryUri
                                                                    .substringAfter(
                                                                        defaultPathFromPreferences,
                                                                    ),
                                                        ),
                                                    )
                                                }
                                            },
                                        )
                                    }

                                    dialog.dismiss()
                                },
                            ).build().show()
                    }
                } else {
                    callback.invoke()
                }
            }
        } else {
            if (directoryUri.startsWith(ContentResolver.SCHEME_CONTENT)) {
                AppConfig.toast(
                    mainActivity,
                    getString(R.string.ftp_server_fallback_path_reset_prompt),
                )
                resetFTPPath()
            }
            callback.invoke()
        }
    }

    /** Sends a broadcast to start ftp server  */
    private fun startServer() {
        checkBatteryOptimizationIfNecessary {
            checkUriAccessIfNecessary {
                doStartServer()
            }
        }
    }

    /** Sends a broadcast to stop ftp server  */
    private fun stopServer() {
        requireContext().sendBroadcast(
            Intent(FtpPreferences.ACTION_STOP_FTPSERVER)
                .setPackage(requireContext().packageName),
        )
    }

    private fun doStartServer() =
        requireContext().sendBroadcast(
            Intent(FtpPreferences.ACTION_START_FTPSERVER)
                .setPackage(requireContext().packageName),
        )

    override fun onResume() {
        super.onResume()
        val wifiFilter = IntentFilter()
        wifiFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiverCompat(
            mWifiReceiver,
            wifiFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (pendingBatteryOptimizationResult) {
            pendingBatteryOptimizationResult = false
            if (SDK_INT >= M) {
                val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isIgnoringBatteryOptimizations(requireContext().packageName) &&
                    (isConnectedToWifi(requireContext()) || isConnectedToLocalNetwork(requireContext()))
                ) {
                    checkUriAccessIfNecessary { doStartServer() }
                }
            }
        }
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mWifiReceiver)
        dismissSnackbar()
    }

    /** Update UI widgets after change in shared preferences  */
    private fun updateStatus() {
        if (!FtpServerEngine.isRunning()) {
            if (!isConnectedToWifi(requireContext()) &&
                !isConnectedToLocalNetwork(requireContext())
            ) {
                statusText.text = spannedStatusNoConnection
                ftpBtn.isEnabled = false
            } else {
                statusText.text = spannedStatusNotRunning
                ftpBtn.isEnabled = true
            }
            url.text = getString(R.string.ftp_url_label, "")
            ftpBtn.text = resources.getString(R.string.start_ftp).uppercase()
        } else {
            accentColor = mainActivity.accent
            url.text = spannedStatusUrl
            statusText.text = spannedStatusConnected
            ftpBtn.isEnabled = true
            ftpBtn.text = resources.getString(R.string.stop_ftp).uppercase()
        }
        val passwordDecrypted = passwordFromPreferences
        val passwordBulleted: CharSequence =
            OneCharacterCharSequence(
                '\u25CF',
                passwordDecrypted?.length ?: 0,
            )
        username.text = "${resources.getString(R.string.username)}: $usernameFromPreferences"
        password.text = "${resources.getString(R.string.password)}: $passwordBulleted"
        ftpPasswordVisibleButton.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_eye_grey600_24dp,
                mainActivity.theme,
            ),
        )
        ftpPasswordVisibleButton.visibility =
            if (passwordDecrypted?.isEmpty() == true) {
                View.GONE
            } else {
                View.VISIBLE
            }
        ftpPasswordVisibleButton.setOnClickListener {
            if (password.text.toString().contains("\u25CF")) {
                // password was not visible, let's make it visible
                password.text = resources.getString(R.string.password) + ": " + passwordDecrypted
                ftpPasswordVisibleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_eye_off_grey600_24dp,
                        mainActivity.theme,
                    ),
                )
            } else {
                // password was visible, let's hide it
                password.text = resources.getString(R.string.password) + ": " + passwordBulleted
                ftpPasswordVisibleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_eye_grey600_24dp,
                        mainActivity.theme,
                    ),
                )
            }
        }
        port.text = "${resources.getString(R.string.ftp_port)}: $defaultPortFromPreferences"
        updatePathText()

        if (defaultPathFromPreferences == FtpPreferences.defaultPath(requireContext())) {
            sharedPath.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        } else {
            sharedPath.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_all, 0)
        }
    }

    private fun updatePathText() {
        val sb =
            StringBuilder(resources.getString(R.string.ftp_path))
                .append(": ")
                .append(pathToDisplayString(defaultPathFromPreferences))
        if (readonlyPreference) sb.append(" \uD83D\uDD12")
        sharedPath.text = sb.toString()
        setListener()
    }

    private fun setListener() {
        sharedPath.setOnTouchListener { _, event ->

            if (sharedPath.compoundDrawables[2] != null && event.action == KeyEvent.ACTION_DOWN) {
                if (event.x >= sharedPath.right - sharedPath.compoundDrawables[2].bounds.width()) {
                    resetFTPPath()
                    updateStatus()

                    AppConfig.toast(
                        mainActivity,
                        getString(R.string.ftp_server_reset_notify),
                    )
                }
            }

            false
        }
    }

    private fun resetFTPPath() {
        mainActivity.prefs
            .edit {
                putString(
                    FtpPreferences.KEY_PREFERENCE_PATH,
                    FtpPreferences.defaultPath(requireContext()),
                )
            }
    }

    /** Updates the status spans  */
    private fun updateSpans() {
        var ftpAddress = ftpAddressString
        if (ftpAddress == null) {
            ftpAddress = ""
            Toast.makeText(
                context,
                resources.getString(R.string.local_inet_addr_error),
                Toast.LENGTH_SHORT,
            )
                .show()
        }
        val statusHead = "${resources.getString(R.string.ftp_status_title)}: "
        spannedStatusConnected =
            HtmlCompat.fromHtml(
                "$statusHead<b>&nbsp;&nbsp;<font color='$accentColor'>" +
                    "${resources.getString(R.string.ftp_status_running)}</font></b>",
                FROM_HTML_MODE_COMPACT,
            )
        spannedStatusUrl =
            HtmlCompat.fromHtml(
                getString(R.string.ftp_url_label, ftpAddress),
                FROM_HTML_MODE_COMPACT,
            )
        spannedStatusNoConnection =
            HtmlCompat.fromHtml(
                "$statusHead<b>&nbsp;&nbsp;&nbsp;&nbsp;" +
                    "<font color='${Utils.getColor(context, android.R.color.holo_red_light)}'>" +
                    "${resources.getString(R.string.ftp_status_no_connection)}</font></b>",
                FROM_HTML_MODE_COMPACT,
            )
        spannedStatusNotRunning =
            HtmlCompat.fromHtml(
                "$statusHead<b>&nbsp;&nbsp;&nbsp;&nbsp;" +
                    "${resources.getString(R.string.ftp_status_not_running)}</b>",
                FROM_HTML_MODE_COMPACT,
            )
        spannedStatusSecure =
            HtmlCompat.fromHtml(
                "$statusHead<b>&nbsp;&nbsp;&nbsp;&nbsp;<font color='${Utils.getColor(
                    context,
                    android.R.color.holo_green_light,
                )}'>" +
                    "${resources.getString(R.string.ftp_status_secure_connection)}</font></b>",
                FROM_HTML_MODE_COMPACT,
            )
    }

    private fun initLoginDialogViews(loginDialogView: DialogFtpLoginBinding) {
        val usernameEditText = loginDialogView.editTextDialogFtpUsername
        val passwordEditText = loginDialogView.editTextDialogFtpPassword
        val anonymousCheckBox = loginDialogView.checkboxFtpAnonymous
        anonymousCheckBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            usernameEditText.isEnabled = !isChecked
            passwordEditText.isEnabled = !isChecked
        }

        // init dialog views as per preferences
        if (usernameFromPreferences == FtpPreferences.DEFAULT_USERNAME) {
            anonymousCheckBox.isChecked = true
        } else {
            usernameEditText.setText(usernameFromPreferences)
            passwordEditText.setText(passwordFromPreferences)
        }
    }

    private fun updateViews(
        mainActivity: MainActivity,
        binding: FragmentFtpBinding,
    ) {
        mainActivity.appbar.setTitle(R.string.ftp)
        mainActivity.hideFab()
        mainActivity.appbar.bottomBar.setVisibility(View.GONE)
        mainActivity.invalidateOptionsMenu()

        val startDividerView = binding.dividerFtpStart
        val statusDividerView = binding.dividerFtpStatus

        when (mainActivity.appTheme) {
            AppTheme.LIGHT -> {
                startDividerView.setBackgroundColor(Utils.getColor(context, R.color.divider))
                statusDividerView.setBackgroundColor(Utils.getColor(context, R.color.divider))
            }
            AppTheme.DARK, AppTheme.BLACK -> {
                startDividerView.setBackgroundColor(
                    Utils.getColor(context, R.color.divider_dark_card),
                )
                statusDividerView.setBackgroundColor(
                    Utils.getColor(context, R.color.divider_dark_card),
                )
            }
            else -> {
            }
        }
        val skinColor = mainActivity.currentColorPreference.primaryFirstTab
        val skinTwoColor = mainActivity.currentColorPreference.primarySecondTab
        mainActivity.updateViews(
            if (MainActivity.currentTab == 1) skinTwoColor.toDrawable() else skinColor.toDrawable(),
        )

        ftpBtn.setOnKeyListener(
            View.OnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            mainActivity.appbar.appbarLayout.requestFocus()
                            mainActivity.appbar.toolbar.requestFocus()
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            ftpBtnOnClick()
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            mainActivity.onBackPressed()
                        }
                        else -> {
                            return@OnKeyListener false
                        }
                    }
                }
                true
            },
        )
    }

    // return address the FTP server is running
    private val ftpAddressString: String?
        get() {
            val ia = getLocalInetAddress(requireContext()) ?: return null
            return (
                (
                    if (securePreference) {
                        FtpPreferences.INITIALS_HOST_SFTP
                    } else {
                        FtpPreferences.INITIALS_HOST_FTP
                    }
                ) +
                    ia.hostAddress +
                    ":" +
                    defaultPortFromPreferences
            )
        }

    private val defaultPortFromPreferences: Int
        get() =
            mainActivity.prefs
                .getInt(FtpPreferences.PORT_PREFERENCE_KEY, FtpPreferences.DEFAULT_PORT)
    private val usernameFromPreferences: String
        get() =
            mainActivity.prefs
                .getString(FtpPreferences.KEY_PREFERENCE_USERNAME, FtpPreferences.DEFAULT_USERNAME)!!

    // can't decrypt the password saved in preferences, remove the preference altogether
    private val passwordFromPreferences: String?
        get() =
            runCatching {
                val encryptedPassword: String =
                    mainActivity.prefs.getString(
                        FtpPreferences.KEY_PREFERENCE_PASSWORD,
                        "",
                    )!!
                if (encryptedPassword == "") {
                    ""
                } else {
                    PasswordUtil.decryptPassword(requireContext(), encryptedPassword)
                }
            }.onFailure {
                log.warn("failed to decrypt ftp server password", it)
                Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show()
                mainActivity.prefs.edit { putString(FtpPreferences.KEY_PREFERENCE_PASSWORD, "") }
            }.getOrNull()

    private val defaultPathFromPreferences: String
        get() {
            return PreferenceManager.getDefaultSharedPreferences(mainActivity)
                .getString(FtpPreferences.KEY_PREFERENCE_PATH, FtpPreferences.defaultPath(requireContext()))!!
        }

    private fun pathToDisplayString(path: String): String {
        return when {
            path.startsWith("file:///") -> {
                path.substringAfter("file://")
            }
            path.startsWith("content://") -> {
                return Uri.parse(path).let {
                    "/storage${it.path?.substringAfter("/tree")?.replace(':', '/')}"
                }
            }
            else -> {
                path
            }
        }
    }

    private fun changeFTPServerPort(port: Int) {
        mainActivity.prefs.edit { putInt(FtpPreferences.PORT_PREFERENCE_KEY, port) }

        // first update spans which will point to an updated status
        updateSpans()
        updateStatus()
    }

    /**
     * Update FTP server shared path in [android.content.SharedPreferences].
     *
     * @param path new shared path. Can be either absolute path (pre 4.4) or URI, which can be
     * <code>file:///</code> or <code>content://</code> as prefix
     */
    fun changeFTPServerPath(path: String) {
        PreferenceManager.getDefaultSharedPreferences(mainActivity).edit {
            if (FileUtils.isRunningAboveStorage(path)) {
                putBoolean(FtpPreferences.KEY_PREFERENCE_ROOT_FILESYSTEM, true)
            }
            putString(FtpPreferences.KEY_PREFERENCE_PATH, path)
        }
        updateStatus()
    }

    private fun setFTPUsername(username: String) {
        mainActivity
            .prefs
            .edit {
                putString(FtpPreferences.KEY_PREFERENCE_USERNAME, username)
            }
        updateStatus()
    }

    @Suppress("LabeledExpression")
    private fun setFTPPassword(password: String) {
        try {
            context?.run {
                mainActivity
                    .prefs
                    .edit {
                        putString(
                            FtpPreferences.KEY_PREFERENCE_PASSWORD,
                            PasswordUtil.encryptPassword(this@run, password),
                        )
                    }
            }
        } catch (e: GeneralSecurityException) {
            log.warn("failed to set ftp password", e)
            Toast.makeText(context, resources.getString(R.string.error), Toast.LENGTH_LONG)
                .show()
        } catch (e: IOException) {
            log.warn("failed to set ftp password", e)
            Toast.makeText(context, resources.getString(R.string.error), Toast.LENGTH_LONG)
                .show()
        }
        updateStatus()
    }

    // Returns timeout from preferences, in seconds
    private var ftpTimeout: Int
        get() =
            mainActivity
                .prefs
                .getInt(FtpPreferences.KEY_PREFERENCE_TIMEOUT, FtpPreferences.DEFAULT_TIMEOUT)
        private set(seconds) {
            mainActivity.prefs.edit { putInt(FtpPreferences.KEY_PREFERENCE_TIMEOUT, seconds) }
        }

    private var securePreference: Boolean
        get() =
            mainActivity
                .prefs
                .getBoolean(FtpPreferences.KEY_PREFERENCE_SECURE, FtpPreferences.DEFAULT_SECURE)
        private set(isSecureEnabled) {
            mainActivity
                .prefs
                .edit {
                    putBoolean(FtpPreferences.KEY_PREFERENCE_SECURE, isSecureEnabled)
                }
        }

    private var readonlyPreference: Boolean
        get() = mainActivity.prefs.getBoolean(FtpPreferences.KEY_PREFERENCE_READONLY, false)
        private set(isReadonly) {
            mainActivity
                .prefs
                .edit {
                    putBoolean(FtpPreferences.KEY_PREFERENCE_READONLY, isReadonly)
                }
        }

    private var legacyFileSystemPreference: Boolean
        get() = mainActivity.prefs.getBoolean(FtpPreferences.KEY_PREFERENCE_SAF_FILESYSTEM, false)
        private set(useSafFileSystem) {
            mainActivity
                .prefs
                .edit {
                    putBoolean(FtpPreferences.KEY_PREFERENCE_SAF_FILESYSTEM, useSafFileSystem)
                }
        }

    private fun promptUserToRestartServer() {
        if (FtpServerEngine.isRunning()) AppConfig.toast(context, R.string.ftp_prompt_restart_server)
    }

    private fun promptUserToEnableWireless() {
        // No wifi, no data, no connection at all
        snackbar =
            Utils.showThemedSnackbar(
                activity as MainActivity?,
                getString(R.string.ftp_server_prompt_connect_to_network),
                BaseTransientBottomBar.LENGTH_INDEFINITE,
                R.string.ftp_server_open_settings,
            ) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        snackbar!!.show()
    }

    private fun dismissSnackbar() = snackbar?.dismiss()

    companion object {
        const val TAG = "FtpServerFragment"
        const val REQUEST_CODE_SAF_FTP = 225
        const val GRANT_URI_RW_PERMISSION =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
