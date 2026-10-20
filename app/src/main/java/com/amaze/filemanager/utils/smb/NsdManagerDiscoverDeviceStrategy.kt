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

package com.amaze.filemanager.utils.smb

import android.content.Context.NSD_SERVICE
import android.content.Context.WIFI_SERVICE
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.utils.ComputerParcelable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * [SmbDeviceScannerObservable.DiscoverDeviceStrategy] implementation using Android's
 * [NsdManager] to discover SMB devices using mDNS/Bonjour/ZeroConf.
 *
 * @see SmbDeviceScannerObservable
 * @see NsdManager
 *
 */
class NsdManagerDiscoverDeviceStrategy : SmbDeviceScannerObservable.DiscoverDeviceStrategy {
    companion object {
        internal const val SERVICE_TYPE_SMB = "_smb._tcp."
        private val logger: Logger =
            LoggerFactory.getLogger(NsdManagerDiscoverDeviceStrategy::class.java)
    }

    private val wifiManager: WifiManager? =
        AppConfig.getInstance().applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager
    private val nsdManager: NsdManager =
        AppConfig.getInstance().applicationContext.getSystemService(NSD_SERVICE) as NsdManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    override fun discoverDevices(callback: (ComputerParcelable) -> Unit) {
        multicastLock =
            wifiManager?.createMulticastLock("smb_mdns_discovery")?.apply {
                setReferenceCounted(true)
            }
        multicastLock?.acquire()
        discoveryListener = createDiscoveryListener(callback)
        nsdManager.discoverServices(
            SERVICE_TYPE_SMB,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener,
        )
    }

    override fun onCancel() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
        multicastLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    /**
     * Creates a new [NsdManager.DiscoveryListener] to handle service discovery events.
     *
     * For backward compatibility, uses [NsdManager.ResolveListener] to resolve services
     * and perform the callback.
     */
    private fun createDiscoveryListener(callback: (ComputerParcelable) -> Unit): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Just to be sure.
                if (serviceInfo.serviceType != SERVICE_TYPE_SMB) {
                    logger.warn("Unknown Service Type: ${serviceInfo.serviceType} for service: ${serviceInfo.serviceName}")
                } else {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                                val host =
                                    if (SDK_INT >= UPSIDE_DOWN_CAKE) {
                                        resolvedServiceInfo.hostAddresses.firstOrNull()
                                    } else {
                                        resolvedServiceInfo.host
                                    }
                                if (host != null && host.hostAddress?.isNotEmpty() == true) {
                                    val computer =
                                        ComputerParcelable(
                                            name = resolvedServiceInfo.serviceName,
                                            addr = host.hostAddress!!,
                                        )
                                    callback(computer)
                                }
                            }

                            override fun onResolveFailed(
                                serviceInfo: NsdServiceInfo?,
                                errorCode: Int,
                            ) {
                                logger.error(
                                    "Service resolve failed: ${serviceInfo?.serviceName} with error code: $errorCode",
                                )
                            }
                        },
                    )
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                logger.debug("Service lost: ${serviceInfo?.serviceName}")
            }

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                logger.error("Service discovery start failed: $serviceType with error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                logger.debug("Service discovery stop failed: $serviceType with error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String?) = logger.debug("Service discovery started: $serviceType")

            override fun onDiscoveryStopped(serviceType: String?) = logger.debug("Service discovery stopped: $serviceType")
        }
    }
}
