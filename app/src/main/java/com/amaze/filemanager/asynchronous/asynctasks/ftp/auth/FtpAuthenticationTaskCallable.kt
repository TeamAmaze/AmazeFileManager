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

import androidx.annotation.WorkerThread
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.CONNECT_TIMEOUT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.utils.PasswordUtil
import net.schmizz.sshj.userauth.UserAuthException
import org.apache.commons.net.ftp.FTPClient
import java.net.URLDecoder.decode
import java.util.concurrent.Callable
import kotlin.text.Charsets.UTF_8

open class FtpAuthenticationTaskCallable(
    protected val hostname: String,
    protected val port: Int,
    protected val username: String,
    protected val password: String
) : Callable<FTPClient> {

    @WorkerThread
    override fun call(): FTPClient {
        val ftpClient = createFTPClient()
        ftpClient.connectTimeout = CONNECT_TIMEOUT
        ftpClient.controlEncoding = Charsets.UTF_8.name()
        ftpClient.connect(hostname, port)
        val loginSuccess = if (username.isBlank() && password.isBlank()) {
            ftpClient.login(
                FTPClientImpl.ANONYMOUS,
                FTPClientImpl.generateRandomEmailAddressForLogin()
            )
        } else {
            ftpClient.login(
                decode(username, UTF_8.name()),
                decode(
                    PasswordUtil.decryptPassword(AppConfig.getInstance(), password),
                    UTF_8.name()
                )
            )
        }
        return if (loginSuccess) {
            ftpClient.enterLocalPassiveMode()
            ftpClient
        } else {
            throw UserAuthException("Login failed")
        }
    }

    protected open fun createFTPClient(): FTPClient =
        NetCopyClientConnectionPool.ftpClientFactory.create(FTP_URI_PREFIX)
}
