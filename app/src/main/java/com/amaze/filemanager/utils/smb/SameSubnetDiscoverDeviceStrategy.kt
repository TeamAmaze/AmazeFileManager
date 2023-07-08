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

package com.amaze.filemanager.utils.smb

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.utils.ComputerParcelable
import com.amaze.filemanager.utils.NetworkUtil
import com.stealthcopter.networktools.PortScan
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.Inet6Address
import java.net.InetAddress

/**
 * [SmbDeviceScannerObservable.DiscoverDeviceStrategy] to just loop through other addresses within
 * same subnet (/24 netmask) and knock their SMB service ports for reachability.
 *
 * Will bypass [Inet6Address] device addresses. They may have much bigger neighourhood host count;
 * also for devices using IPv6, they shall be covered by [WsddDiscoverDeviceStrategy] anyway.
 *
 * TODO: if we can get the gateway using __legit__ API, may swarm the network in broader netmasks
 */
class SameSubnetDiscoverDeviceStrategy : SmbDeviceScannerObservable.DiscoverDeviceStrategy {

    private lateinit var worker: Disposable

    companion object {
        private const val HOST_UP_TIMEOUT = 1000
        private const val PARALLELISM = 10
        private val TCP_PORTS = arrayListOf(139, 445)
    }

    /**
     * No need to cleanup resources
     */
    override fun onCancel() {
        if (!worker.isDisposed) {
            worker.dispose()
        }
    }

    override fun discoverDevices(callback: (ComputerParcelable) -> Unit) {
        val neighbourhoods = getNeighbourhoodHosts()
        worker = Flowable.fromIterable(neighbourhoods)
            .parallel(PARALLELISM)
            .runOn(Schedulers.io())
            .map { addr ->
                if (addr.isReachable(HOST_UP_TIMEOUT)) {
                    val portsReachable = listOf(
                        PortScan.onAddress(addr).setPorts(TCP_PORTS).setMethodTCP().doScan()
                    ).flatten()
                    if (portsReachable.isNotEmpty()) {
                        addr
                    } else {
                        false
                    }
                } else {
                    false
                }
            }.filter {
                it is InetAddress
            }.doOnNext { addr ->
                addr as InetAddress
                callback.invoke(
                    ComputerParcelable(
                        addr.hostAddress,
                        if (addr.hostName == addr.hostAddress) {
                            addr.canonicalHostName
                        } else {
                            addr.hostName
                        }
                    )
                )
            }.sequential().subscribe()
    }

    private fun getNeighbourhoodHosts(): List<InetAddress> {
        val deviceAddress = NetworkUtil.getLocalInetAddress(AppConfig.getInstance())
        return deviceAddress?.let { addr ->
            if (addr is Inet6Address) {
                // IPv6 neigbourhood hosts can be very big - that should use wsdd instead; hence
                // empty list here
                emptyList()
            } else {
                val networkPrefix: String = addr.hostAddress.substringBeforeLast('.')
                (1..254).map {
                    InetAddress.getByName("$networkPrefix.$it")
                }
            }
        } ?: emptyList()
    }
}
