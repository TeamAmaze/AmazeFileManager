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

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.regex.Pattern

/**
 * Created by mat on 14/12/15.
 */
object IPTools {
    /*
     * Ip matching patterns from
     * https://examples.javacodegeeks.com/core-java/util/regex/regular-expressions-for-ip-v4-and-ip-v6-addresses/
     * note that these patterns will match most but not all valid ips
     */
    private val IPV4_PATTERN = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"
    )
    private val IPV6_STD_PATTERN = Pattern.compile(
        "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    )
    private val IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile(
        "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$"
    )

    /**
     * Answers if given string is valid IPv4 address.
     */
    @JvmStatic
    fun isIPv4Address(address: String?): Boolean {
        return address != null && IPV4_PATTERN.matcher(address).matches()
    }

    /**
     * Answers if given string is valid IPv6 address in long form.
     */
    @JvmStatic
    fun isIPv6StdAddress(address: String?): Boolean {
        return address != null && IPV6_STD_PATTERN.matcher(address).matches()
    }

    /**
     * Answers if given string is valid IPv6 address in hex compressed form.
     */
    @JvmStatic
    fun isIPv6HexCompressedAddress(address: String?): Boolean {
        return address != null && IPV6_HEX_COMPRESSED_PATTERN.matcher(address).matches()
    }

    /**
     * Answers if given string is a valid IPv6 address.
     */
    @JvmStatic
    fun isIPv6Address(address: String?): Boolean {
        return address != null && (isIPv6StdAddress(address) || isIPv6HexCompressedAddress(address))
    }

    /*
     * @return The first local IPv4 address, or null
     */
    @JvmStatic
    val localIPv4Address: InetAddress?
        get() {
            val localAddresses = localIPv4Addresses
            return if (localAddresses.isNotEmpty()) localAddresses[0] else null
        }

    /*
     * Return The list of all IPv4 addresses found
     */
    private val localIPv4Addresses: List<InetAddress>
        get() = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { iface ->
                iface.inetAddresses.asSequence().filter { addr ->
                    addr is Inet4Address && !addr.isLoopbackAddress()
                }
            }
        }.getOrDefault(emptyList())

    /**
     * Check if the provided ip address refers to the localhost
     *
     * https://stackoverflow.com/a/2406819/315998
     *
     * @param addr - address to check
     * @return - true if ip address is self
     */
    @JvmStatic
    fun isIpAddressLocalhost(addr: InetAddress?): Boolean {
        return addr?.run {
            // Check if the address is a valid special local or loop back
            if (addr.isAnyLocalAddress || addr.isLoopbackAddress) true else try {
                NetworkInterface.getByInetAddress(addr) != null
            } catch (e: SocketException) {
                false
            }
        } ?: false
    }

    /**
     * Check if the provided ip address refers to the localhost
     *
     * https://stackoverflow.com/a/2406819/315998
     *
     * @param addr - address to check
     * @return - true if ip address is self
     */
    @JvmStatic
    fun isIpAddressLocalNetwork(addr: InetAddress?): Boolean =
        addr != null && addr.isSiteLocalAddress
}
