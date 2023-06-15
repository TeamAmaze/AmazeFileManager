/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.services

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.*
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.IBinder
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.management.ServiceWatcherUtil
import com.amaze.filemanager.filesystem.FileUtil
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.filesystem.files.FileUtils
import com.amaze.filemanager.filesystem.files.GenericCopyUtil
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.notifications.NotificationConstants
import com.amaze.filemanager.utils.DatapointParcelable
import com.amaze.filemanager.utils.ObtainableServiceBinder
import com.amaze.filemanager.utils.ProgressHandler
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream

@Suppress("TooManyFunctions") // Hack.
class ZipService : AbstractProgressiveService() {

    private val log: Logger = LoggerFactory.getLogger(ZipService::class.java)

    private val mBinder: IBinder = ObtainableServiceBinder(this)
    private val disposables = CompositeDisposable()
    private lateinit var mNotifyManager: NotificationManagerCompat
    private lateinit var mBuilder: NotificationCompat.Builder
    private var progressListener: ProgressListener? = null
    private val progressHandler = ProgressHandler()

    // list of data packages, to initiate chart in process viewer fragment
    private val dataPackages = ArrayList<DatapointParcelable>()
    private var accentColor = 0
    private var sharedPreferences: SharedPreferences? = null
    private var customSmallContentViews: RemoteViews? = null
    private var customBigContentViews: RemoteViews? = null

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver1, IntentFilter(KEY_COMPRESS_BROADCAST_CANCEL))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val mZipPath = intent.getStringExtra(KEY_COMPRESS_PATH)
        val baseFiles: ArrayList<HybridFileParcelable> =
            intent.getParcelableArrayListExtra(KEY_COMPRESS_FILES)!!
        val zipFile = File(mZipPath)
        mNotifyManager = NotificationManagerCompat.from(applicationContext)
        if (!zipFile.exists()) {
            try {
                zipFile.createNewFile()
            } catch (e: IOException) {
                log.warn("failed to create zip file", e)
            }
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        accentColor = (application as AppConfig)
            .utilsProvider
            .colorPreference
            .getCurrentUserColorPreferences(this, sharedPreferences).accent

        val notificationIntent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.KEY_INTENT_PROCESS_VIEWER, true)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            getPendingIntentFlag(0)
        )

        customSmallContentViews = RemoteViews(packageName, R.layout.notification_service_small)
        customBigContentViews = RemoteViews(packageName, R.layout.notification_service_big)

        val stopIntent = Intent(KEY_COMPRESS_BROADCAST_CANCEL)
        val stopPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1234,
            stopIntent,
            getPendingIntentFlag(FLAG_UPDATE_CURRENT)
        )
        val action = NotificationCompat.Action(
            R.drawable.ic_zip_box_grey,
            getString(R.string.stop_ftp),
            stopPendingIntent
        )
        mBuilder = NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID)
            .setSmallIcon(R.drawable.ic_zip_box_grey)
            .setContentIntent(pendingIntent)
            .setCustomContentView(customSmallContentViews)
            .setCustomBigContentView(customBigContentViews)
            .setCustomHeadsUpContentView(customSmallContentViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(action)
            .setOngoing(true)
            .setColor(accentColor)

        NotificationConstants.setMetadata(this, mBuilder, NotificationConstants.TYPE_NORMAL)
        startForeground(NotificationConstants.ZIP_ID, mBuilder.build())
        initNotificationViews()
        super.onStartCommand(intent, flags, startId)
        super.progressHalted()
        val zipTask = CompressTask(this, baseFiles, zipFile.absolutePath)
        disposables.add(zipTask.compress())
        // If we get killed, after returning from here, restart
        return START_NOT_STICKY
    }

    override fun getNotificationManager(): NotificationManagerCompat = mNotifyManager

    override fun getNotificationBuilder(): NotificationCompat.Builder = mBuilder

    override fun getNotificationId(): Int = NotificationConstants.ZIP_ID

    @StringRes
    override fun getTitle(move: Boolean): Int = R.string.compressing

    override fun getNotificationCustomViewSmall(): RemoteViews = customSmallContentViews!!

    override fun getNotificationCustomViewBig(): RemoteViews = customBigContentViews!!

    override fun getProgressListener(): ProgressListener? = progressListener

    override fun setProgressListener(progressListener: ProgressListener?) {
        this.progressListener = progressListener
    }

    override fun getDataPackages(): ArrayList<DatapointParcelable> = dataPackages

    override fun getProgressHandler(): ProgressHandler = progressHandler

    override fun clearDataPackages() = dataPackages.clear()

    inner class CompressTask(
        private val zipService: ZipService,
        private val baseFiles: ArrayList<HybridFileParcelable>,
        private val zipPath: String
    ) {

        private lateinit var zos: ZipOutputStream
        private lateinit var watcherUtil: ServiceWatcherUtil

        /**
         * Main use case for executing zipping task by given [zipPath]
         */
        fun compress(): Disposable {
            return Completable.create { emitter ->
                // setting up service watchers and initial data packages
                // finding total size on background thread (this is necessary condition for SMB!)
                val totalBytes = FileUtils.getTotalBytes(baseFiles, zipService.applicationContext)
                progressHandler.sourceSize = baseFiles.size
                progressHandler.totalSize = totalBytes

                progressHandler.setProgressListener { speed: Long ->
                    publishResults(speed, false, false)
                }
                zipService.addFirstDatapoint(
                    baseFiles[0].getName(applicationContext),
                    baseFiles.size,
                    totalBytes,
                    false
                )
                execute(
                    emitter,
                    zipService.applicationContext,
                    FileUtils.hybridListToFileArrayList(baseFiles),
                    zipPath
                )

                emitter.onComplete()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        watcherUtil.stopWatch()
                        val intent = Intent(MainActivity.KEY_INTENT_LOAD_LIST)
                            .putExtra(MainActivity.KEY_INTENT_LOAD_LIST_FILE, zipPath)
                        zipService.sendBroadcast(intent)
                        zipService.stopSelf()
                    },
                    { log.error(it.message ?: "ZipService.CompressAsyncTask.compress failed") }
                )
        }

        /**
         * Deletes the destination file zip file if exists
         */
        fun cancel() {
            progressHandler.cancelled = true
            val zipFile = File(zipPath)
            if (zipFile.exists()) zipFile.delete()
        }

        /**
         * Main logic for zipping specified files.
         */
        fun execute(
            emitter: CompletableEmitter,
            context: Context,
            baseFiles: ArrayList<File>,
            zipPath: String
        ) {
            val out: OutputStream?
            val zipDirectory = File(zipPath)
            watcherUtil = ServiceWatcherUtil(progressHandler)
            watcherUtil.watch(this@ZipService)
            try {
                out = FileUtil.getOutputStream(zipDirectory, context)
                zos = ZipOutputStream(BufferedOutputStream(out))
                for ((fileProgress, file) in baseFiles.withIndex()) {
                    if (emitter.isDisposed) return
                    progressHandler.fileName = file.name
                    progressHandler.sourceFilesProcessed = fileProgress + 1
                    compressFile(file, "")
                }
            } catch (e: IOException) {
                log.warn("failed to zip file", e)
            } finally {
                try {
                    zos.flush()
                    zos.close()
                    context.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            .setData(Uri.fromFile(zipDirectory))
                    )
                } catch (e: IOException) {
                    log.warn("failed to close zip streams", e)
                }
            }
        }

        @Throws(IOException::class, NullPointerException::class, ZipException::class)
        private fun compressFile(file: File, path: String) {
            if (progressHandler.cancelled) return
            if (!file.isDirectory) {
                zos.putNextEntry(createZipEntry(file, path))
                val buf = ByteArray(GenericCopyUtil.DEFAULT_BUFFER_SIZE)
                var len: Int
                BufferedInputStream(FileInputStream(file)).use { bufferedInputStream ->
                    while (bufferedInputStream.read(buf).also { len = it } > 0) {
                        if (!progressHandler.cancelled) {
                            zos.write(buf, 0, len)
                            ServiceWatcherUtil.position += len.toLong()
                        } else break
                    }
                }
                return
            }
            file.listFiles()?.forEach {
                compressFile(it, "${createZipEntryPrefixWith(path)}${file.name}")
            }
        }
    }

    private fun createZipEntryPrefixWith(path: String): String =
        if (path.isEmpty()) {
            path
        } else {
            "$path/"
        }

    private fun createZipEntry(file: File, path: String): ZipEntry =
        ZipEntry("${createZipEntryPrefixWith(path)}${file.name}").apply {
            if (SDK_INT >= O) {
                val attrs = Files.readAttributes(
                    Paths.get(file.absolutePath),
                    BasicFileAttributes::class.java
                )
                setCreationTime(attrs.creationTime())
                    .setLastAccessTime(attrs.lastAccessTime())
                    .lastModifiedTime = attrs.lastModifiedTime()
            } else {
                time = file.lastModified()
            }
        }

    /*
     * Class used for the client Binder. Because we know this service always runs in the same process
     * as its clients, we don't need to deal with IPC.
     */
    private val receiver1: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            progressHandler.cancelled = true
        }
    }

    override fun onBind(arg0: Intent): IBinder = mBinder

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver1)
        disposables.dispose()
    }

    companion object {
        const val KEY_COMPRESS_PATH = "zip_path"
        const val KEY_COMPRESS_FILES = "zip_files"
        const val KEY_COMPRESS_BROADCAST_CANCEL = "zip_cancel"
    }
}
