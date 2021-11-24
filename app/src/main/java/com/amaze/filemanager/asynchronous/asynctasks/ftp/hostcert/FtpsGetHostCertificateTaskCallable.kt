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

package com.amaze.filemanager.asynchronous.asynctasks.ftp.hostcert

import androidx.annotation.WorkerThread
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.CONNECT_TIMEOUT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.utils.X509CertificateUtil
import org.apache.commons.net.ftp.FTPSClient
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import javax.net.ssl.HostnameVerifier

open class FtpsGetHostCertificateTaskCallable(
    private val hostname: String,
    private val port: Int
) : Callable<JSONObject> {

    @WorkerThread
    override fun call(): JSONObject? {
        val latch = CountDownLatch(1)
        var result: JSONObject? = null
        val ftpClient = createFTPClient()
        ftpClient.connectTimeout = CONNECT_TIMEOUT
        ftpClient.controlEncoding = Charsets.UTF_8.name()
        ftpClient.hostnameVerifier = HostnameVerifier { _, session ->
            if (session.peerCertificateChain.isNotEmpty()) {
                val certinfo = X509CertificateUtil.parse(session.peerCertificateChain[0])
                result = JSONObject(certinfo)
            }
            latch.countDown()
            true
        }
        ftpClient.connect(hostname, port)
        latch.await()
        ftpClient.disconnect()
        return result
    }

    protected open fun createFTPClient(): FTPSClient =
        NetCopyClientConnectionPool.ftpClientFactory.create(FTPS_URI_PREFIX) as FTPSClient
}
