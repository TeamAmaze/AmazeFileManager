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
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.Q
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.preference.PreferenceManager
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.services.AbstractProgressiveService.getPendingIntentFlag
import com.amaze.filemanager.filesystem.ftpserver.AndroidFileSystemFactory
import com.amaze.filemanager.filesystem.ftpserver.RootFileSystemFactory
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_ROOTMODE
import com.amaze.filemanager.ui.notifications.FtpNotification
import com.amaze.filemanager.ui.notifications.NotificationConstants
import com.amaze.filemanager.utils.ObtainableServiceBinder
import com.amaze.filemanager.utils.PasswordUtil
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.ssl.ClientAuth
import org.apache.ftpserver.ssl.impl.DefaultSslConfiguration
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.greenrobot.eventbus.EventBus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import kotlin.concurrent.thread

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
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStartedByTile = true == intent?.getBooleanExtra(TAG_STARTED_BY_TILE, false)
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

        serverThread = thread(block = this::run)
        val notification = FtpNotification.startNotification(applicationContext, isStartedByTile)
        startForeground(NotificationConstants.FTP_ID, notification)
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock.setReferenceCounted(false)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @Suppress("LongMethod")
    override fun run() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        FtpServerFactory().run {
            val connectionConfigFactory = ConnectionConfigFactory()
            val shouldUseAndroidFileSystem =
                preferences.getBoolean(KEY_PREFERENCE_SAF_FILESYSTEM, false)
            if (SDK_INT >= KITKAT && shouldUseAndroidFileSystem) {
                fileSystem = AndroidFileSystemFactory(applicationContext)
            } else if (preferences.getBoolean(PREFERENCE_ROOTMODE, false)) {
                fileSystem = RootFileSystemFactory()
            } else {
                fileSystem = NativeFileSystemFactory()
            }

            commandFactory = CommandFactoryFactory.create(shouldUseAndroidFileSystem)

            val usernamePreference = preferences.getString(
                KEY_PREFERENCE_USERNAME,
                DEFAULT_USERNAME
            )
            if (usernamePreference != DEFAULT_USERNAME) {
                username = usernamePreference
                runCatching {
                    password = PasswordUtil.decryptPassword(
                        applicationContext,
                        preferences.getString(KEY_PREFERENCE_PASSWORD, "")!!
                    )
                    isPasswordProtected = true
                }.onFailure {
                    log.warn("failed to decrypt password in ftp service", it)
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
                    val keyStorePassword = BuildConfig.FTP_SERVER_KEYSTORE_PASSWORD.toCharArray()
                    keyStore.load(resources.openRawResource(R.raw.key), keyStorePassword)
                    val keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm())
                    keyManagerFactory.init(keyStore, keyStorePassword)
                    val trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(keyStore)
                    fac.sslConfiguration = DefaultSslConfiguration(
                        keyManagerFactory,
                        trustManagerFactory,
                        ClientAuth.WANT,
                        "TLS",
                        enabledCipherSuites,
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
                            if (isStartedByTile) {
                                FtpReceiverActions.STARTED_FROM_TILE
                            } else {
                                FtpReceiverActions.STARTED
                            }
                        )
                }
            }.onFailure {
                EventBus.getDefault().post(FtpReceiverActions.FAILED_TO_START)
            }
        }
    }

    override fun onDestroy() {
        wakeLock.release()
        serverThread?.let { serverThread ->
            serverThread.interrupt()
            // wait 10 sec for server thread to finish
            serverThread.join(10000)

            if (!serverThread.isAlive) {
                Companion.serverThread = null
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
        val flag = getPendingIntentFlag(FLAG_ONE_SHOT)
        val restartServicePI = PendingIntent.getService(
            applicationContext,
            1,
            restartService,
            flag
        )
        val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000] =
            restartServicePI
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FtpService::class.java)

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
        const val KEY_PREFERENCE_ROOT_FILESYSTEM = "ftp_root_filesystem"
        const val INITIALS_HOST_FTP = "ftp://"
        const val INITIALS_HOST_SFTP = "ftps://"

        // RequestStartStopReceiver listens for these actions to start/stop this server
        const val ACTION_START_FTPSERVER =
            "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_START_FTPSERVER"
        const val ACTION_STOP_FTPSERVER =
            "com.amaze.filemanager.services.ftpservice.FTPReceiver.ACTION_STOP_FTPSERVER"
        const val TAG_STARTED_BY_TILE = "started_by_tile"
        // attribute of action_started, used by notification

        private lateinit var _enabledCipherSuites: Array<String>

        init {
            _enabledCipherSuites = LinkedList<String>().apply {
                if (SDK_INT >= Q) {
                    add("TLS_AES_128_GCM_SHA256")
                    add("TLS_AES_256_GCM_SHA384")
                    add("TLS_CHACHA20_POLY1305_SHA256")
                }
                if (SDK_INT >= N) {
                    add("TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256")
                    add("TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256")
                }
                if (SDK_INT >= LOLLIPOP) {
                    add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA")
                    add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256")
                    add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA")
                    add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")
                    add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")
                    add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")
                    add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA")
                    add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384")
                    add("TLS_RSA_WITH_AES_128_GCM_SHA256")
                    add("TLS_RSA_WITH_AES_256_GCM_SHA384")
                }
                if (SDK_INT < LOLLIPOP) {
                    add("TLS_RSA_WITH_AES_128_CBC_SHA")
                    add("TLS_RSA_WITH_AES_256_CBC_SHA")
                }
            }.toTypedArray()
        }

        /**
         * Return a list of available ciphers for ftpserver.
         *
         * Added SDK detection since some ciphers are available only on higher versions, and they
         * have to be on top of the list to make a more secure SSL
         *
         * @see [org.apache.ftpserver.ssl.SslConfiguration]
         * @see [javax.net.ssl.SSLEngine]
         */
        @JvmStatic
        val enabledCipherSuites = _enabledCipherSuites

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
        fun isRunning(): Boolean {
            val server = server ?: return false
            return !server.isStopped
        }

        private fun getPort(preferences: SharedPreferences): Int {
            return preferences.getInt(FtpService.PORT_PREFERENCE_KEY, FtpService.DEFAULT_PORT)
        }
    }
}
