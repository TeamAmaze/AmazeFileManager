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

package com.amaze.filemanager.asynchronous.services.ftp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import android.os.Environment
import android.os.IBinder
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.preference.PreferenceManager
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.filesystem.ftpserver.AndroidFileSystemFactory
import com.amaze.filemanager.ui.notifications.FtpNotification
import com.amaze.filemanager.ui.notifications.NotificationConstants
import com.amaze.filemanager.utils.ObtainableServiceBinder
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.ssl.ClientAuth
import org.apache.ftpserver.ssl.impl.DefaultSslConfiguration
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Created by yashwanthreddyg on 09-06-2016.
 *
 *
 * Edited by zent-co on 30-07-2019 Edited by bowiechen on 2019-10-19.
 */
class FtpService : Service(), Runnable {
    private val binder: IBinder = ObtainableServiceBinder(this)

    // Service will broadcast via event bus when server start/stop
    enum class FtpReceiverActions {
        STARTED, STARTED_FROM_TILE, STOPPED, FAILED_TO_START
    }

    private var username: String? = null
    private var password: String? = null
    private var isPasswordProtected = false
    private var isStartedByTile = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        isStartedByTile = intent.getBooleanExtra(TAG_STARTED_BY_TILE, false)
        var attempts = 10
        while (serverThread != null) {
            if (attempts > 0) {
                attempts--
                try {
                    Thread.sleep(1000)
                } catch (ignored: InterruptedException) {
                }
            } else {
                return START_STICKY
            }
        }
        serverThread = Thread(this)
        serverThread!!.start()
        val notification = FtpNotification.startNotification(applicationContext, isStartedByTile)
        startForeground(NotificationConstants.FTP_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @Suppress("LongMethod")
    override fun run() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        FtpServerFactory().run {
            val connectionConfigFactory = ConnectionConfigFactory()
            if (SDK_INT >= KITKAT &&
                preferences.getBoolean(KEY_PREFERENCE_SAF_FILESYSTEM, false)
            ) {
                fileSystem = AndroidFileSystemFactory(applicationContext)
            }

            val usernamePreference = preferences.getString(
                KEY_PREFERENCE_USERNAME,
                DEFAULT_USERNAME
            )
            if (usernamePreference != DEFAULT_USERNAME) {
                username = usernamePreference
                runCatching {
                    password = CryptUtil.decryptPassword(
                        applicationContext, preferences.getString(KEY_PREFERENCE_PASSWORD, "")
                    )
                    isPasswordProtected = true
                }.onFailure {
                    it.printStackTrace()
                    AppConfig.toast(applicationContext, R.string.error)
                    preferences.edit().putString(KEY_PREFERENCE_PASSWORD, "").apply()
                    isPasswordProtected = false
                }
            }
            val user = BaseUser()
            if (!isPasswordProtected) {
                user.name = "anonymous"
                connectionConfigFactory.isAnonymousLoginEnabled = true
            } else {
                user.name = username
                user.password = password
            }
            user.homeDirectory = preferences.getString(
                KEY_PREFERENCE_PATH,
                defaultPath(this@FtpService)
            )
            if (!preferences.getBoolean(KEY_PREFERENCE_READONLY, false)) {
                user.authorities = listOf(WritePermission())
            }

            connectionConfig = connectionConfigFactory.createConnectionConfig()
            userManager.save(user)

            val fac = ListenerFactory()
            if (preferences.getBoolean(KEY_PREFERENCE_SECURE, DEFAULT_SECURE)) {
                try {
                    val keyStore = KeyStore.getInstance("BKS")
                    keyStore.load(resources.openRawResource(R.raw.key), KEYSTORE_PASSWORD)
                    val keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm())
                    keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD)
                    val trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(keyStore)
                    fac.sslConfiguration = DefaultSslConfiguration(
                        keyManagerFactory,
                        trustManagerFactory,
                        ClientAuth.WANT,
                        "TLS",
                        null,
                        "ftpserver"
                    )
                    fac.isImplicitSsl = true
                } catch (e: GeneralSecurityException) {
                    preferences.edit().putBoolean(KEY_PREFERENCE_SECURE, false).apply()
                } catch (e: IOException) {
                    preferences.edit().putBoolean(KEY_PREFERENCE_SECURE, false).apply()
                }
            }
            fac.port = getPort(preferences)
            fac.idleTimeout = preferences.getInt(KEY_PREFERENCE_TIMEOUT, DEFAULT_TIMEOUT)

            addListener("default", fac.createListener())
            runCatching {
                server = createServer().apply {
                    start()
                    EventBus.getDefault()
                        .post(
                            if (isStartedByTile)
                                FtpReceiverActions.STARTED_FROM_TILE
                            else
                                FtpReceiverActions.STARTED
                        )
                }
            }.onFailure {
                EventBus.getDefault().post(FtpReceiverActions.FAILED_TO_START)
            }
        }
    }

    override fun onDestroy() {
        serverThread?.interrupt().also {
            // wait 10 sec for server thread to finish
            serverThread!!.join(10000)

            if (!serverThread!!.isAlive) {
                serverThread = null
            }
            server?.stop().also {
                EventBus.getDefault().post(FtpReceiverActions.STOPPED)
            }
        }
    }

    // Restart the service if the app is closed from the recent list
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        val restartService = Intent(applicationContext, this.javaClass).setPackage(packageName)
        val flag = if (SDK_INT >= M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        val restartServicePI = PendingIntent.getService(
            applicationContext, 1, restartService, flag
        )
        val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000] =
            restartServicePI
    }

    companion object {
        const val DEFAULT_PORT = 2211
        const val DEFAULT_USERNAME = ""
        const val DEFAULT_TIMEOUT = 600 // default timeout, in sec
        const val DEFAULT_SECURE = true
        const val PORT_PREFERENCE_KEY = "ftpPort"
        const val KEY_PREFERENCE_PATH = "ftp_path"
        const val KEY_PREFERENCE_USERNAME = "ftp_username"
        const val KEY_PREFERENCE_PASSWORD = "ftp_password_encrypted"
        const val KEY_PREFERENCE_TIMEOUT = "ftp_timeout"
        const val KEY_PREFERENCE_SECURE = "ftp_secure"
        const val KEY_PREFERENCE_READONLY = "ftp_readonly"
        const val KEY_PREFERENCE_SAF_FILESYSTEM = "ftp_saf_filesystem"
        const val INITIALS_HOST_FTP = "ftp://"
        const val INITIALS_HOST_SFTP = "ftps://"
        private const val WIFI_AP_ADDRESS_PREFIX = "192.168.43."
        private val KEYSTORE_PASSWORD = "vishal007".toCharArray()

        // RequestStartStopReceiver listens for these actions to start/stop this server
        const val ACTION_START_FTPSERVER =
            "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_START_FTPSERVER"
        const val ACTION_STOP_FTPSERVER =
            "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_STOP_FTPSERVER"
        const val TAG_STARTED_BY_TILE = "started_by_tile"
        // attribute of action_started, used by notification

        private var serverThread: Thread? = null
        private var server: FtpServer? = null

        /**
         * Derive the FTP server's default share path, depending the user's Android version.
         *
         * Default it's the internal storage's root as java.io.File; otherwise it's content://
         * based URI if it's running on Android 7.0 or above.
         */
        @JvmStatic
        fun defaultPath(context: Context): String {
            return if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_PREFERENCE_SAF_FILESYSTEM, false) && SDK_INT > M
            ) {
                DocumentsContract.buildTreeDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:"
                ).toString()
            } else {
                Environment.getExternalStorageDirectory().absolutePath
            }
        }

        /**
         * Indicator whether FTP service is running
         */
        @JvmStatic
        fun isRunning(): Boolean = server?.let {
            !it.isStopped
        } ?: false

        /**
         * Is the device connected to local network, either Ethernet or Wifi?
         */
        @JvmStatic
        fun isConnectedToLocalNetwork(context: Context): Boolean {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            var connected: Boolean
            if (SDK_INT >= M) {
                return cm.activeNetwork?.let { activeNetwork ->
                    cm.getNetworkCapabilities(activeNetwork)?.let { ni ->
                        ni.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) or
                            ni.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    } ?: false
                } ?: false
            } else {
                connected = cm.activeNetworkInfo?.let { ni ->
                    ni.isConnected && (
                        ni.type and (
                            ConnectivityManager.TYPE_WIFI
                                or ConnectivityManager.TYPE_ETHERNET
                            ) != 0
                        )
                } ?: false
                if (!connected) {
                    connected = runCatching {
                        NetworkInterface.getNetworkInterfaces().toList().find { netInterface ->
                            netInterface.displayName.startsWith("rndis")
                        }
                    }.getOrElse { null } != null
                }
            }

            return connected
        }

        /**
         * Is the device connected to Wifi?
         */
        @JvmStatic
        fun isConnectedToWifi(context: Context): Boolean {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (SDK_INT >= M) {
                cm.activeNetwork?.let {
                    cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                } ?: false
            } else {
                cm.activeNetworkInfo?.let {
                    it.isConnected && it.type == ConnectivityManager.TYPE_WIFI
                } ?: false
            }
        }

        /**
         * Is the device's wifi hotspot enabled?
         */
        @JvmStatic
        fun isEnabledWifiHotspot(context: Context): Boolean {
            val wm = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            return callIsWifiApEnabled(wm)
        }

        /**
         * Determine device's IP address
         */
        @JvmStatic
        fun getLocalInetAddress(context: Context): InetAddress? {
            if (!isConnectedToLocalNetwork(context) && !isEnabledWifiHotspot(context)) {
                return null
            }
            if (isConnectedToWifi(context)) {
                val wm = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                val ipAddress = wm.connectionInfo.ipAddress
                return if (ipAddress == 0) null else intToInet(ipAddress)
            }
            runCatching {
                NetworkInterface.getNetworkInterfaces().iterator().forEach { netinterface ->
                    netinterface.inetAddresses.iterator().forEach { address ->
                        if (address.hostAddress.startsWith(WIFI_AP_ADDRESS_PREFIX) &&
                            isEnabledWifiHotspot(context)
                        ) {
                            return address
                        }

                        // this is the condition that sometimes gives problems
                        if (!address.isLoopbackAddress &&
                            !address.isLinkLocalAddress &&
                            !isEnabledWifiHotspot(context)
                        ) {
                            return address
                        }
                    }
                }
            }.onFailure { e ->
                e.printStackTrace()
            }
            return null
        }

        private fun intToInet(value: Int): InetAddress? {
            val bytes = ByteArray(4)
            for (i in 0..3) {
                bytes[i] = byteOfInt(value, i)
            }
            return try {
                InetAddress.getByAddress(bytes)
            } catch (e: UnknownHostException) {
                // This only happens if the byte array has a bad length
                null
            }
        }

        private fun byteOfInt(value: Int, which: Int): Byte {
            val shift = which * 8
            return (value shr shift).toByte()
        }

        private fun getPort(preferences: SharedPreferences): Int {
            return preferences.getInt(PORT_PREFERENCE_KEY, DEFAULT_PORT)
        }

        private fun callIsWifiApEnabled(wifiManager: WifiManager): Boolean = runCatching {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.invoke(wifiManager) as Boolean
        }.getOrElse {
            it.printStackTrace()
            false
        }
    }
}
