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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Storage device strategy for API 21-23 using UsbManager.
 * Detects USB mass storage devices and handles USB permission requests.
 */
internal class UsbManagerStrategy : StorageDeviceStrategy {
    companion object {
        private const val TAG = "UsbManagerStrategy"
        const val ACTION_USB_PERMISSION = "com.amaze.filemanager.USB_PERMISSION"
    }

    private var changeListener: OnDeviceChangeListener? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    override val requiresUsbPermission: Boolean = true

    override fun getRemovableDevices(context: Context): List<StorageDeviceRepresentation> {
        val usbManager =
            context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return emptyList()

        return usbManager.deviceList.values
            .filter { device -> isMassStorageDevice(device) }
            .map { device -> createUsbStorageDevice(context, usbManager, device) }
    }

    override fun createSafIntent(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Intent {
        // For API 21-23, we use generic ACTION_OPEN_DOCUMENT_TREE
        // User must manually navigate to the USB storage
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

        broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        UsbManager.ACTION_USB_DEVICE_ATTACHED,
                        UsbManager.ACTION_USB_DEVICE_DETACHED,
                        -> {
                            val devices = getRemovableDevices(ctx)
                            changeListener?.onDevicesChanged(devices)
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun unregisterChangeCallback(context: Context) {
        broadcastReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver already unregistered", e)
            }
        }
        broadcastReceiver = null
        changeListener = null
    }

    override fun requestUsbPermission(
        context: Context,
        device: StorageDeviceRepresentation,
        callback: ((StorageDeviceRepresentation, Boolean) -> Unit)?,
    ) {
        if (device !is UsbStorageDevice) {
            callback?.invoke(device, false)
            return
        }

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        val usbDevice = findUsbDevice(usbManager, device)

        if (usbDevice == null) {
            callback?.invoke(device, false)
            return
        }

        if (usbManager?.hasPermission(usbDevice) == true) {
            callback?.invoke(device, true)
            return
        }

        // Create a broadcast receiver for the permission result
        val permissionReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    if (ACTION_USB_PERMISSION == intent.action) {
                        val receivedDevice: UsbDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                            }

                        if (receivedDevice != null &&
                            receivedDevice.vendorId == device.vendorId &&
                            receivedDevice.productId == device.productId
                        ) {
                            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            callback?.invoke(device, granted)
                            try {
                                context.unregisterReceiver(this)
                            } catch (_: IllegalArgumentException) {
                                // Receiver already unregistered
                            }
                        }
                    }
                }
            }

        // Register the receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        ContextCompat.registerReceiver(
            context,
            permissionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Create the pending intent
        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        val permissionIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                pendingIntentFlags,
            )

        // Request permission
        usbManager?.requestPermission(usbDevice, permissionIntent)
    }

    override fun hasUsbPermission(
        context: Context,
        device: StorageDeviceRepresentation,
    ): Boolean {
        if (device !is UsbStorageDevice) return false

        val usbManager =
            context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return false
        val usbDevice = findUsbDevice(usbManager, device) ?: return false

        return usbManager.hasPermission(usbDevice)
    }

    /**
     * Find the raw UsbDevice matching a UsbStorageDevice representation.
     */
    private fun findUsbDevice(
        usbManager: UsbManager?,
        device: UsbStorageDevice,
    ): UsbDevice? {
        return usbManager?.deviceList?.values?.find {
            it.vendorId == device.vendorId && it.productId == device.productId
        }
    }

    /**
     * Check if a UsbDevice is a mass storage device.
     */
    private fun isMassStorageDevice(device: UsbDevice): Boolean {
        return (0 until device.interfaceCount).any { i ->
            device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
        }
    }

    /**
     * Create a UsbStorageDevice from a raw UsbDevice.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun createUsbStorageDevice(
        context: Context,
        usbManager: UsbManager,
        device: UsbDevice,
    ): UsbStorageDevice {
        var serial: String? = null
        var manufacturerName: String? = null
        var productName: String? = null

        val hasPermission = usbManager.hasPermission(device)

        if (hasPermission) {
            try {
                serial = device.serialNumber
                manufacturerName = device.manufacturerName
                productName = device.productName
            } catch (_: SecurityException) {
                Log.w(TAG, "Permission denied reading device info of ${device.vendorId}:${device.productId}")
            }
        } else {
            // Try to get basic info that doesn't require permission
            try {
                manufacturerName = device.manufacturerName
                productName = device.productName
            } catch (_: SecurityException) {
                Log.d(TAG, "Cannot read device name without permission: ${device.vendorId}:${device.productId}")
            }
            // Serial number requires permission on Android 10+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    serial = device.serialNumber
                } catch (_: SecurityException) {
                    Log.d(TAG, "Cannot read serial without permission")
                }
            }
        }

        return UsbStorageDevice(
            vendorId = device.vendorId,
            productId = device.productId,
            serialNumber = serial,
            manufacturerName = manufacturerName,
            productName = productName,
        )
    }
}
