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

package com.amaze.filemanager.ftpserver.service

import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.Q
import java.util.LinkedList

/**
 * Provides SSL/TLS cipher suites configuration for FTP server.
 */
object FtpCipherSuites {
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
    @SuppressLint("ObsoleteSdkInt")
    val enabledCipherSuites: Array<String> =
        LinkedList<String>().apply {
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
