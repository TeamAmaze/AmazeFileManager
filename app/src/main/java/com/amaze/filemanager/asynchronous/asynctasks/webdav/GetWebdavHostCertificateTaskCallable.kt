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

package com.amaze.filemanager.asynchronous.asynctasks.webdav

import android.annotation.SuppressLint
import com.amaze.filemanager.utils.toHex
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class GetWebdavHostCertificateTaskCallable(
    private val url: String,
    private val firstContact: Boolean = false,
) : Callable<Pair<String, String>> {
    private lateinit var serverCertificate: X509Certificate

    override fun call(): Pair<String, String> {
        val trustManager = createTrustManagerForGettingCertificate()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)

        val client =
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }.build()

        client.newCall(Request.Builder().head().url(url).build()).execute()

        return Pair(sha256(serverCertificate), toPemString(serverCertificate))
    }

    internal fun sha256(of: X509Certificate): String {
        val md = MessageDigest.getInstance("SHA-256")
        val der = of.encoded
        md.update(der)
        val digest = md.digest()

        return digest.toHex(":")
    }

    internal fun toPemString(from: X509Certificate): String {
        val stringWriter = StringWriter()
        PemWriter(stringWriter).runCatching {
            val pemGenerator = JcaMiscPEMGenerator(from)
            writeObject(pemGenerator)
        }
        stringWriter.close()
        return stringWriter.toString()
    }

    @SuppressLint("CustomX509TrustManager")
    private fun createTrustManagerForGettingCertificate(): X509TrustManager {
        return object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

            override fun checkClientTrusted(
                chain: Array<X509Certificate>?,
                authType: String?,
            ) = Unit

            override fun checkServerTrusted(
                chain: Array<X509Certificate>?,
                authType: String?,
            ) {
                serverCertificate = chain!![0]
            }
        }
    }
}
