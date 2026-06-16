/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.fileoperations.filesystem.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.reflect.Field

/**
 * Storage device strategy for API 24+ using StorageManager/VolumeManager.
 * Detects removable storage volumes without requiring USB permissions.
 */
@RequiresApi(Build.VERSION_CODES.N)
internal class VolumeManagerStrategy : StorageDeviceStrategy {
    companion object {
        private const val TAG = "VolumeManagerStrategy"
    }

    private var changeListener: OnDeviceChangeListener? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var storageVolumeCallback: Any? = null // StorageManager.StorageVolumeCallback for API 30+

    override val requiresUsbPermission: Boolean = false

    override fun getRemovableDevices(context: Context): List<StorageDeviceRepresentation> {
        val storageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                ?: return emptyList()

        return storageManager.storageVolumes
            .filter { volume ->
                volume.isRemovable && (
                    volume.state == Environment.MEDIA_MOUNTED ||
                        volume.state == Environment.MEDIA_MOUNTED_READ_ONLY
                )
            }
            .mapNotNull { volume -> createVolumeStorageDevice(context, volume) }
    }

    override fun createSafIntent(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Intent {
        // For API 29+, use StorageVolume.createOpenDocumentTreeIntent() if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && device is VolumeStorageDevice) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            val volume = findStorageVolume(storageManager, device)
            if (volume != null) {
                return volume.createOpenDocumentTreeIntent().apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                    )
                }
            }
        }

        // Fallback to generic ACTION_OPEN_DOCUMENT_TREE
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }
    }

    override fun registerChangeCallback(
        context: Context,
        listener: OnDeviceChangeListener,
    ) {
        changeListener = listener

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Use StorageManager.registerStorageVolumeCallback
            registerStorageVolumeCallback(context)
        } else {
            // API 24-29: Use broadcast receiver for media events
            registerMediaBroadcastReceiver(context)
        }
    }

    override fun unregisterChangeCallback(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            unregisterStorageVolumeCallback(context)
        } else {
            unregisterMediaBroadcastReceiver(context)
        }
        changeListener = null
    }

    /**
     * Register StorageVolumeCallback for API 30+.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun registerStorageVolumeCallback(context: Context) {
        val storageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                ?: return

        val callback =
            object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) {
                    val devices = getRemovableDevices(context)
                    changeListener?.onDevicesChanged(devices)
                }
            }

        storageManager.registerStorageVolumeCallback(
            context.mainExecutor,
            callback,
        )
        storageVolumeCallback = callback
    }

    /**
     * Unregister StorageVolumeCallback for API 30+.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun unregisterStorageVolumeCallback(context: Context) {
        val callback = storageVolumeCallback as? StorageManager.StorageVolumeCallback ?: return
        val storageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
                ?: return

        runCatching {
            storageManager.unregisterStorageVolumeCallback(callback)
        }.onFailure { e ->
            Log.w(TAG, "Failed to unregister storage volume callback", e)
        }
        storageVolumeCallback = null
    }

    /**
     * Register broadcast receiver for media mount/unmount events (API 24-29).
     */
    private fun registerMediaBroadcastReceiver(context: Context) {
        broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        Intent.ACTION_MEDIA_MOUNTED,
                        Intent.ACTION_MEDIA_UNMOUNTED,
                        Intent.ACTION_MEDIA_EJECT,
                        Intent.ACTION_MEDIA_REMOVED,
                        Intent.ACTION_MEDIA_BAD_REMOVAL,
                        -> {
                            // Delay slightly to allow system to update volume states
                            Handler(Looper.getMainLooper()).postDelayed({
                                val devices = getRemovableDevices(ctx)
                                changeListener?.onDevicesChanged(devices)
                            }, 500)
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                addDataScheme("file")
            }

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    /**
     * Unregister media broadcast receiver.
     */
    private fun unregisterMediaBroadcastReceiver(context: Context) {
        broadcastReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver already unregistered", e)
            }
        }
        broadcastReceiver = null
    }

    /**
     * Find a StorageVolume matching a VolumeStorageDevice.
     */
    private fun findStorageVolume(
        storageManager: StorageManager?,
        device: VolumeStorageDevice,
    ): StorageVolume? {
        return storageManager?.storageVolumes?.find { volume ->
            volume.uuid == device.uuid
        }
    }

    /**
     * Create a VolumeStorageDevice from a StorageVolume.
     */
    private fun createVolumeStorageDevice(
        context: Context,
        volume: StorageVolume,
    ): VolumeStorageDevice? {
        val uuid = volume.uuid ?: return null // Skip volumes without UUID (typically internal storage)

        val description = volume.getDescription(context) ?: "Removable Storage"
        val path = getVolumePath(volume)

        return VolumeStorageDevice(
            uuid = uuid,
            description = description,
            path = path,
        )
    }

    /**
     * Get the mount path of a StorageVolume using reflection (not always available via public API).
     */
    private fun getVolumePath(volume: StorageVolume): String? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                volume.directory?.absolutePath
            } else {
                // Use reflection for older APIs
                val field: Field = StorageVolume::class.java.getDeclaredField("mPath")
                field.isAccessible = true
                (field.get(volume) as? File)?.absolutePath
            }
        }.getOrElse { e ->
            Log.d(TAG, "Could not get volume path", e)
            null
        }
    }
}
