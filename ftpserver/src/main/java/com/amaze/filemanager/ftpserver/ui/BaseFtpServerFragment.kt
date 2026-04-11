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

package com.amaze.filemanager.ftpserver.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.amaze.filemanager.ftpserver.R
import com.amaze.filemanager.ftpserver.databinding.FragmentFtpBinding
import com.amaze.filemanager.ftpserver.service.FtpEventBus
import com.amaze.filemanager.ftpserver.service.FtpPreferences
import com.amaze.filemanager.ftpserver.service.FtpServerEngine
import com.amaze.filemanager.ftpserver.service.FtpServerEvent
import kotlinx.coroutines.launch

/**
 * Base fragment for FTP server UI.
 *
 * This provides the core FTP server UI functionality that can be extended
 * by the app module to add app-specific features.
 */
@Suppress("StringLiteralDuplication")
abstract class BaseFtpServerFragment : Fragment() {
    private var _binding: FragmentFtpBinding? = null
    protected val binding get() = _binding!!

    private var spannedStatusNoConnection: Spanned? = null
    private var spannedStatusConnected: Spanned? = null
    private var spannedStatusUrl: Spanned? = null
    private var spannedStatusSecure: Spanned? = null
    private var spannedStatusNotRunning: Spanned? = null

    /**
     * Get the accent color for the UI
     */
    abstract fun getAccentColor(): Int

    /**
     * Check if device is connected to a local network
     */
    abstract fun isConnectedToLocalNetwork(): Boolean

    /**
     * Check if device is connected to WiFi
     */
    abstract fun isConnectedToWifi(): Boolean

    /**
     * Get the local IP address
     */
    abstract fun getLocalAddress(): String?

    /**
     * Start the FTP service
     */
    abstract fun startFtpService(startedByTile: Boolean)

    /**
     * Stop the FTP service
     */
    abstract fun stopFtpService()

    /**
     * Show a snackbar prompting user to enable wireless
     */
    abstract fun promptUserToEnableWireless()

    /**
     * Dismiss any shown snackbar
     */
    abstract fun dismissSnackbar()

    /**
     * Get encrypted password from preferences
     */
    abstract fun getEncryptedPassword(): String?

    /**
     * Decrypt password
     */
    abstract fun decryptPassword(encryptedPassword: String): String?

    /**
     * Handle path change request
     */
    abstract fun onPathChangeRequested()

    /**
     * Handle login change request
     */
    abstract fun onLoginChangeRequested()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFtpBinding.inflate(inflater, container, false)

        updateSpans()
        updateStatus()
        updateViews()

        binding.startStopButton.setOnClickListener {
            onStartStopButtonClick()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                FtpEventBus.events.collect { event ->
                    onFtpServerEvent(event)
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        registerWifiReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterWifiReceiver()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.ftp_server_menu, menu)
        menu.findItem(R.id.checkbox_ftp_readonly)?.isChecked =
            FtpPreferences.isReadOnly(requireContext())
        menu.findItem(R.id.checkbox_ftp_secure)?.isChecked =
            FtpPreferences.isSecure(requireContext())
        menu.findItem(R.id.checkbox_ftp_legacy_filesystem)?.isChecked =
            FtpPreferences.useSafFilesystem(requireContext())
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.choose_ftp_port -> {
                showPortDialog()
                return true
            }
            R.id.ftp_path -> {
                onPathChangeRequested()
                return true
            }
            R.id.ftp_login -> {
                onLoginChangeRequested()
                return true
            }
            R.id.checkbox_ftp_readonly -> {
                val newValue = !item.isChecked
                item.isChecked = newValue
                setReadonlyPreference(newValue)
                updatePathText()
                promptUserToRestartServer()
                return true
            }
            R.id.checkbox_ftp_secure -> {
                val newValue = !item.isChecked
                item.isChecked = newValue
                setSecurePreference(newValue)
                promptUserToRestartServer()
                return true
            }
            R.id.checkbox_ftp_legacy_filesystem -> {
                val newValue = !item.isChecked
                item.isChecked = newValue
                setSafFilesystemPreference(newValue)
                promptUserToRestartServer()
                return true
            }
            R.id.ftp_timeout -> {
                showTimeoutDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onStartStopButtonClick() {
        if (!FtpServerEngine.isRunning()) {
            if (isConnectedToWifi() || isConnectedToLocalNetwork()) {
                startServer()
            } else {
                binding.textViewFtpStatus.text = spannedStatusNoConnection
            }
        } else {
            stopServer()
        }
    }

    private fun startServer() {
        startFtpService(false)
    }

    private fun stopServer() {
        stopFtpService()
    }

    private fun onFtpServerEvent(event: FtpServerEvent) {
        updateSpans()
        when (event) {
            is FtpServerEvent.Started, is FtpServerEvent.StartedFromTile -> {
                val isSecure = FtpPreferences.isSecure(requireContext())
                binding.textViewFtpStatus.text =
                    if (isSecure) {
                        spannedStatusSecure
                    } else {
                        spannedStatusConnected
                    }
                binding.textViewFtpUrl.text = spannedStatusUrl
                binding.startStopButton.text = getString(R.string.ftpmod_stop).uppercase()
            }
            is FtpServerEvent.FailedToStart -> {
                binding.textViewFtpStatus.text = spannedStatusNotRunning
                Toast.makeText(context, R.string.ftpmod_unknown_error, Toast.LENGTH_LONG).show()
                binding.startStopButton.text = getString(R.string.ftpmod_start).uppercase()
                binding.textViewFtpUrl.text = "URL: "
            }
            is FtpServerEvent.Stopped -> {
                binding.textViewFtpStatus.text = spannedStatusNotRunning
                binding.textViewFtpUrl.text = "URL: "
                binding.startStopButton.text = getString(R.string.ftpmod_start).uppercase()
            }
        }
        updateStatus()
    }

    private fun updateSpans() {
        val accentColor = String.format("%06X", 0xFFFFFF and getAccentColor())

        spannedStatusNoConnection =
            HtmlCompat.fromHtml(
                "${getString(R.string.ftpmod_status_label)} " +
                    "<font color='#$accentColor'><b>${getString(R.string.ftpmod_status_no_connection)}</b></font>",
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )

        spannedStatusConnected =
            HtmlCompat.fromHtml(
                "${getString(R.string.ftpmod_status_label)} " +
                    "<font color='#$accentColor'><b>${getString(R.string.ftpmod_status_running)}</b></font>",
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )

        spannedStatusSecure =
            HtmlCompat.fromHtml(
                "${getString(R.string.ftpmod_status_label)} " +
                    "<font color='#$accentColor'><b>${getString(R.string.ftpmod_status_secure_connection)}</b></font>",
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )

        spannedStatusNotRunning =
            HtmlCompat.fromHtml(
                "${getString(R.string.ftpmod_status_label)} " +
                    "<font color='#$accentColor'><b>${getString(R.string.ftpmod_status_not_running)}</b></font>",
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )

        val address = getLocalAddress()
        val port = FtpPreferences.getPort(requireContext())
        val isSecure = FtpPreferences.isSecure(requireContext())
        val prefix = if (isSecure) FtpPreferences.INITIALS_HOST_SFTP else FtpPreferences.INITIALS_HOST_FTP
        val urlText = if (address != null) "$prefix$address:$port/" else ""

        spannedStatusUrl =
            HtmlCompat.fromHtml(
                "${getString(R.string.ftpmod_url_label)} " +
                    "<font color='#$accentColor'><b>$urlText</b></font>",
                HtmlCompat.FROM_HTML_MODE_COMPACT,
            )
    }

    private fun updateStatus() {
        if (_binding == null) return

        if (!isConnectedToLocalNetwork() && !isConnectedToWifi()) {
            binding.textViewFtpStatus.text = spannedStatusNoConnection
            binding.startStopButton.isEnabled = false
        } else {
            binding.startStopButton.isEnabled = true
            if (FtpServerEngine.isRunning()) {
                binding.textViewFtpStatus.text =
                    if (FtpPreferences.isSecure(requireContext())) {
                        spannedStatusSecure
                    } else {
                        spannedStatusConnected
                    }
                binding.textViewFtpUrl.text = spannedStatusUrl
                binding.startStopButton.text = getString(R.string.ftpmod_stop).uppercase()
            } else {
                binding.textViewFtpStatus.text = spannedStatusNotRunning
                binding.textViewFtpUrl.text = "URL: "
                binding.startStopButton.text = getString(R.string.ftpmod_start).uppercase()
            }
        }
    }

    private fun updateViews() {
        updateUsernameText()
        updatePasswordText()
        updatePortText()
        updatePathText()
    }

    private fun updateUsernameText() {
        val username = FtpPreferences.getUsername(requireContext())
        val displayName = username.ifEmpty { getString(R.string.ftpmod_anonymous) }
        binding.textViewFtpUsername.text = "${getString(R.string.ftpmod_username_label)}$displayName"
    }

    private fun updatePasswordText() {
        val username = FtpPreferences.getUsername(requireContext())
        if (username.isEmpty()) {
            binding.textViewFtpPassword.text = "${getString(R.string.ftpmod_password_label)}••••••••"
            binding.ftpPasswordVisible.visibility = View.GONE
        } else {
            binding.textViewFtpPassword.text = "${getString(R.string.ftpmod_password_label)}••••••••"
            binding.ftpPasswordVisible.visibility = View.VISIBLE
        }
    }

    private fun updatePortText() {
        val port = FtpPreferences.getPort(requireContext())
        binding.textViewFtpPort.text = "${getString(R.string.ftpmod_port_label)}$port"
    }

    protected fun updatePathText() {
        val path = FtpPreferences.getPath(requireContext())
        val readOnly = if (FtpPreferences.isReadOnly(requireContext())) " (R/O)" else ""
        binding.textViewFtpPath.text = "${getString(R.string.ftpmod_path_label)}$path$readOnly"
    }

    private fun setReadonlyPreference(value: Boolean) {
        FtpPreferences.getPreferences(requireContext()).edit {
            putBoolean(FtpPreferences.KEY_PREFERENCE_READONLY, value)
        }
    }

    private fun setSecurePreference(value: Boolean) {
        FtpPreferences.getPreferences(requireContext()).edit {
            putBoolean(FtpPreferences.KEY_PREFERENCE_SECURE, value)
        }
    }

    private fun setSafFilesystemPreference(value: Boolean) {
        FtpPreferences.getPreferences(requireContext()).edit {
            putBoolean(FtpPreferences.KEY_PREFERENCE_SAF_FILESYSTEM, value)
        }
    }

    private fun promptUserToRestartServer() {
        if (FtpServerEngine.isRunning()) {
            Toast.makeText(context, R.string.ftpmod_prompt_restart_server, Toast.LENGTH_SHORT).show()
        }
    }

    protected open fun showPortDialog() {
        // Override in subclass to show port dialog with material-dialogs
    }

    protected open fun showTimeoutDialog() {
        // Override in subclass to show timeout dialog with material-dialogs
    }

    private val wifiReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (isConnectedToLocalNetwork()) {
                    binding.startStopButton.isEnabled = true
                    dismissSnackbar()
                } else {
                    stopServer()
                    binding.textViewFtpStatus.text = spannedStatusNoConnection
                    binding.startStopButton.isEnabled = false
                    binding.startStopButton.text = getString(R.string.ftpmod_start).uppercase()
                    promptUserToEnableWireless()
                }
            }
        }

    private fun registerWifiReceiver() {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        ContextCompat.registerReceiver(
            requireContext(),
            wifiReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun unregisterWifiReceiver() {
        try {
            requireContext().unregisterReceiver(wifiReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    companion object {
        const val TAG = "FtpServerFragment"
    }
}
