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

import androidx.annotation.VisibleForTesting
import com.amaze.filemanager.utils.ComputerParcelable
import com.amaze.filemanager.utils.smb.SmbDeviceScannerObservable.DiscoverDeviceStrategy
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.InetAddress

/**
 * Observable to discover reachable SMB nodes on the network.
 *
 * Uses a series of [DiscoverDeviceStrategy] instances to discover nodes.
 */
class SmbDeviceScannerObservable : Observable<ComputerParcelable>() {

    /**
     * Device discovery strategy interface.
     */
    interface DiscoverDeviceStrategy {
        /**
         * Implement this method to return list of [InetAddress] which has SMB service running.
         */
        fun discoverDevices(callback: (ComputerParcelable) -> Unit)

        /**
         * Implement this method to cleanup resources
         */
        fun onCancel()
    }

    var discoverDeviceStrategies: Array<DiscoverDeviceStrategy> =
        arrayOf(
            WsddDiscoverDeviceStrategy(),
            SameSubnetDiscoverDeviceStrategy()
        )
        @VisibleForTesting set

        @VisibleForTesting get

    private lateinit var observer: Observer<in ComputerParcelable>

    private lateinit var disposable: Disposable

    /**
     * Stop discovering hosts. Notify containing strategies to stop, then stop the created
     * [Observer] obtained at [subscribeActual].
     */
    fun stop() {
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        observer.onComplete()
    }

    /**
     * Call all strategies one by one to discover nodes.
     *
     * Given observer must be able to drop duplicated entries (which ComputerParcelable already
     * has implemented equals() and hashCode()).
     */
    override fun subscribeActual(observer: Observer<in ComputerParcelable>) {
        this.observer = observer
        this.disposable = merge(
            discoverDeviceStrategies.map { strategy ->
                fromCallable {
                    strategy.discoverDevices { addr ->
                        observer.onNext(ComputerParcelable(addr.addr, addr.name))
                    }
                }.subscribeOn(Schedulers.io())
            }
        ).observeOn(Schedulers.computation()).doOnComplete {
            discoverDeviceStrategies.forEach { strategy ->
                strategy.onCancel()
            }
        }.subscribe()
    }
}
