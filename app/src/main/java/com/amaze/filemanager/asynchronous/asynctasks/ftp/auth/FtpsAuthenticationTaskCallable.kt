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

package com.amaze.filemanager.asynchronous.asynctasks.ftp.auth

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.utils.PasswordUtil
import com.amaze.filemanager.utils.X509CertificateUtil
import com.amaze.filemanager.utils.X509CertificateUtil.FINGERPRINT
import net.schmizz.sshj.userauth.UserAuthException
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.json.JSONObject
import javax.net.ssl.HostnameVerifier

class FtpsAuthenticationTaskCallable(
    hostname: String,
    port: Int,
    private val certInfo: JSONObject,
    username: String,
    password: String
) : FtpAuthenticationTaskCallable(hostname, port, username, password) {

    override fun call(): FTPClient {
        val ftpClient = createFTPClient() as FTPSClient
        ftpClient.connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
        ftpClient.controlEncoding = Charsets.UTF_8.name()
        ftpClient.connect(hostname, port)
        val loginSuccess = if (username.isBlank() && password.isBlank()) {
            ftpClient.login(
                FTPClientImpl.ANONYMOUS,
                FTPClientImpl.generateRandomEmailAddressForLogin()
            )
        } else {
            ftpClient.login(
                username,
                PasswordUtil.decryptPassword(AppConfig.getInstance(), password)
            )
        }
        return if (loginSuccess) {
            // RFC 2228 set protection buffer size to 0
            ftpClient.execPBSZ(0)
            // RFC 2228 set data protection level to PRIVATE
            ftpClient.execPROT("P")
            ftpClient.enterLocalPassiveMode()
            ftpClient
        } else {
            throw UserAuthException("Login failed")
        }
    }

    @Suppress("LabeledExpression")
    override fun createFTPClient(): FTPClient {
        return (
            NetCopyClientConnectionPool.ftpClientFactory.create(FTPS_URI_PREFIX)
                as FTPSClient
            ).apply {
            this.hostnameVerifier = HostnameVerifier { _, session ->
                return@HostnameVerifier if (session.peerCertificateChain.isNotEmpty()) {
                    X509CertificateUtil.parse(
                        session.peerCertificateChain.first()
                    )[FINGERPRINT] == certInfo.get(FINGERPRINT)
                } else {
                    false
                }
            }
        }
    }
}
