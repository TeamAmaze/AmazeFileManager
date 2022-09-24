/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.stealthcopter.networktools

import com.stealthcopter.networktools.IPTools.isIpAddressLocalNetwork
import com.stealthcopter.networktools.IPTools.isIpAddressLocalhost
import com.stealthcopter.networktools.portscanning.PortScanTCP
import com.stealthcopter.networktools.portscanning.PortScanUDP
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/** Created by mat on 14/12/15.  */
class PortScan // This class is not to be instantiated
private constructor() {

    private var method = METHOD_TCP
    private var noThreads = 50
    private var address: InetAddress? = null
    private var timeOutMillis = 1000
    private var cancelled = false
    private var ports: MutableList<Int> = ArrayList()
    private val openPortsFound: MutableList<Int> = ArrayList()
    private var portListener: PortListener? = null
    private lateinit var runningFlowable: Flowable<Unit>

    interface PortListener {
        /**
         * Callback function for port scan result
         */
        fun onResult(portNo: Int, open: Boolean)

        /**
         * Callback function for receiving the list of opened ports
         */
        fun onFinished(openPorts: List<Int>?)
    }

    /**
     * Sets the timeout for each port scanned
     *
     * If you raise the timeout you may want to consider increasing the thread count [ ][.setNoThreads] to compensate. We can afford to have quite a high thread count as most of
     * the time the thread is just sitting idle and waiting for the socket to timeout.
     *
     * @param timeOutMillis - the timeout for each ping in milliseconds Recommendations: Local host:
     * 20 - 500 ms - can be very fast as request doesn't need to go over network Local network 500
     * - 2500 ms Remote Scan 2500+ ms
     * @return this object to allow chaining
     */
    fun setTimeOutMillis(timeOutMillis: Int): PortScan {
        require(timeOutMillis >= 0) { "Timeout cannot be less than 0" }
        this.timeOutMillis = timeOutMillis
        return this
    }

    /**
     * Scan the ports to scan
     *
     * @param port - the port to scan
     * @return this object to allow chaining
     */
    fun setPort(port: Int): PortScan {
        ports.clear()
        validatePort(port)
        ports.add(port)
        return this
    }

    /**
     * Scan the ports to scan
     *
     * @param ports - the ports to scan
     * @return this object to allow chaining
     */
    fun setPorts(ports: MutableList<Int>): PortScan {
        // Check all ports are valid
        for (port in ports) {
            validatePort(port)
        }
        this.ports = ports
        return this
    }

    /**
     * Scan the ports to scan
     *
     * @param portString - the ports to scan (comma separated, hyphen denotes a range). For example:
     * "21-23,25,45,53,80"
     * @return this object to allow chaining
     */
    fun setPorts(portString: String): PortScan {
        var portString = portString
        ports.clear()
        val ports: MutableList<Int> = ArrayList()
        portString = portString.substring(portString.indexOf(":") + 1, portString.length)
        for (x in portString.split(",").toTypedArray()) {
            if (x.contains("-")) {
                val start = x.split("-").toTypedArray()[0].toInt()
                val end = x.split("-").toTypedArray()[1].toInt()
                validatePort(start)
                validatePort(end)
                require(end > start) { "Start port cannot be greater than or equal to the end port" }
                for (j in start..end) {
                    ports.add(j)
                }
            } else {
                val start = x.toInt()
                validatePort(start)
                ports.add(start)
            }
        }
        this.ports = ports
        return this
    }

    /**
     * Checks and throws exception if port is not valid
     *
     * @param port - the port to validate
     */
    private fun validatePort(port: Int) {
        require(port >= 1) { "Start port cannot be less than 1" }
        require(port <= 65535) { "Start cannot be greater than 65535" }
    }

    private fun setAddress(address: InetAddress) {
        this.address = address
    }

    private fun setDefaultThreadsAndTimeouts() {
        // Try and work out automatically what kind of host we are scanning
        // local host (this device) / local network / remote
        if (isIpAddressLocalhost(address)) {
            // If we are scanning a the localhost set the timeout to be very short so we get faster
            // results
            // This will be overridden if user calls setTimeoutMillis manually.
            timeOutMillis = TIMEOUT_LOCALHOST
            noThreads = DEFAULT_THREADS_LOCALHOST
        } else if (isIpAddressLocalNetwork(address)) {
            // Assume local network (not infallible)
            timeOutMillis = TIMEOUT_LOCALNETWORK
            noThreads = DEFAULT_THREADS_LOCALNETWORK
        } else {
            // Assume remote network timeouts
            timeOutMillis = TIMEOUT_REMOTE
            noThreads = DEFAULT_THREADS_REMOTE
        }
    }

    /**
     * @param noThreads set the number of threads to work with, note we default to a large number as
     * these requests are network heavy not cpu heavy.
     * @return self
     * @throws IllegalArgumentException - if no threads is less than 1
     */
    @Throws(IllegalArgumentException::class)
    fun setNoThreads(noThreads: Int): PortScan {
        require(noThreads >= 1) { "Cannot have less than 1 thread" }
        this.noThreads = noThreads
        return this
    }

    /**
     * Set scan method, either TCP or UDP
     *
     * @param method - the transport method to use to scan, either PortScan.METHOD_UDP or
     * PortScan.METHOD_TCP
     * @return this object to allow chaining
     * @throws IllegalArgumentException - if invalid method
     */
    private fun setMethod(method: Int): PortScan {
        when (method) {
            METHOD_UDP, METHOD_TCP -> this.method = method
            else -> throw IllegalArgumentException("Invalid method type $method")
        }
        return this
    }

    /**
     * Set scan method to UDP
     *
     * @return this object to allow chaining
     */
    fun setMethodUDP(): PortScan {
        setMethod(METHOD_UDP)
        return this
    }

    /**
     * Set scan method to TCP
     *
     * @return this object to allow chaining
     */
    fun setMethodTCP(): PortScan {
        setMethod(METHOD_TCP)
        return this
    }

    /** Cancel a running ping  */
    fun cancel() {
        cancelled = true
        runningFlowable.unsubscribeOn(Schedulers.computation())
    }

    /**
     * Perform a synchronous (blocking) port scan and return a list of open ports
     *
     * @return - ping result
     */
    fun doScan(): List<Int> {
        cancelled = false
        openPortsFound.clear()
        runningFlowable = createPortScanFlowable().doOnComplete {
            openPortsFound.sort()
        }
        runningFlowable.blockingSubscribe()
        return openPortsFound
    }

    private fun createPortScanFlowable(): Flowable<Unit> {
        return Flowable.fromIterable(ports)
            .parallel(noThreads)
            .runOn(Schedulers.io())
            .map { portNo ->
                PortScanRunnable(address, portNo, timeOutMillis, method).run()
            }.sequential()
            .subscribeOn(Schedulers.computation())
    }

    @Synchronized
    private fun portScanned(port: Int, open: Boolean) {
        if (open) {
            openPortsFound.add(port)
        }
        portListener?.onResult(port, open)
    }

    private inner class PortScanRunnable constructor(
        private val address: InetAddress?,
        private val portNo: Int,
        private val timeOutMillis: Int,
        private val method: Int
    ) : Runnable {
        override fun run() {
            if (cancelled) return
            when (method) {
                METHOD_UDP -> portScanned(
                    portNo,
                    PortScanUDP.scanAddress(address, portNo, timeOutMillis)
                )
                METHOD_TCP -> portScanned(
                    portNo,
                    PortScanTCP.scanAddress(address, portNo, timeOutMillis)
                )
                else -> throw IllegalArgumentException("Invalid method")
            }
        }
    }

    companion object {
        private const val TIMEOUT_LOCALHOST = 25
        private const val TIMEOUT_LOCALNETWORK = 1000
        private const val TIMEOUT_REMOTE = 2500
        private const val DEFAULT_THREADS_LOCALHOST = 7
        private const val DEFAULT_THREADS_LOCALNETWORK = 50
        private const val DEFAULT_THREADS_REMOTE = 50
        private const val METHOD_TCP = 0
        private const val METHOD_UDP = 1

        /**
         * Set the address to ping
         *
         * @param address - Address to be pinged
         * @return this object to allow chaining
         * @throws UnknownHostException - if no IP address for the `host` could be found, or if a
         * scope_id was specified for a global IPv6 address.
         */
        @JvmStatic
        @Throws(UnknownHostException::class)
        fun onAddress(address: String?): PortScan {
            return onAddress(InetAddress.getByName(address))
        }

        /**
         * Set the address to ping
         *
         * @param ia - Address to be pinged
         * @return this object to allow chaining
         */
        @JvmStatic
        fun onAddress(ia: InetAddress): PortScan {
            val portScan = PortScan()
            portScan.setAddress(ia)
            portScan.setDefaultThreadsAndTimeouts()
            return portScan
        }
    }
}
