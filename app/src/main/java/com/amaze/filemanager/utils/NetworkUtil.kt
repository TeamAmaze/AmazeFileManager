/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.utils

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException

object NetworkUtil {

    private val log: Logger = LoggerFactory.getLogger(NetworkUtil::class.java)

    private fun getConnectivityManager(context: Context) =
        context.applicationContext.getSystemService(Service.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    /**
     * Is the device connected to local network, either Ethernet or Wifi?
     */
    @JvmStatic
    fun isConnectedToLocalNetwork(context: Context): Boolean {
        val cm = getConnectivityManager(context)
        var connected: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connected = cm.activeNetwork?.let { activeNetwork ->
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
        }

        if (!connected) {
            connected = runCatching {
                NetworkInterface.getNetworkInterfaces().toList().find { netInterface ->
                    netInterface.displayName.startsWith("rndis") or
                        netInterface.displayName.startsWith("wlan")
                }
            }.getOrElse { null } != null
        }

        return connected
    }

    /**
     * Is the device connected to Wifi?
     */
    @JvmStatic
    fun isConnectedToWifi(context: Context): Boolean {
        val cm = getConnectivityManager(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
     * Determine device's IP address.
     *
     * Caveat: doesn't handle IPv6 addresses well. Forcing return IPv4 if possible.
     */
    @JvmStatic
    fun getLocalInetAddress(context: Context): InetAddress? {
        if (!isConnectedToLocalNetwork(context)) {
            return null
        }
        if (isConnectedToWifi(context)) {
            val wm = context.applicationContext.getSystemService(Service.WIFI_SERVICE)
                as WifiManager
            val ipAddress = wm.connectionInfo.ipAddress
            return if (ipAddress == 0) null else intToInet(ipAddress)
        }
        runCatching {
            NetworkInterface.getNetworkInterfaces().iterator().forEach { netinterface ->
                netinterface.inetAddresses.iterator().forEach { address ->
                    // this is the condition that sometimes gives problems
                    if (!address.isLoopbackAddress &&
                        !address.isLinkLocalAddress &&
                        address is Inet4Address
                    ) {
                        return address
                    }
                }
            }
        }.onFailure { e ->
            log.warn("failed to get local inet address", e)
        }
        return null
    }

    /**
     * Utility method to convert an IPv4 address in integer representation to [InetAddress].
     */
    @JvmStatic
    fun intToInet(value: Int): InetAddress? {
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
}
