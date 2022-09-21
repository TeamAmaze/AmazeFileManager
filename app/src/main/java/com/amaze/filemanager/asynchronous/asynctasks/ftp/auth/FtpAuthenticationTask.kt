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

import androidx.annotation.MainThread
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.Task
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import org.apache.commons.net.ftp.FTPClient
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException

class FtpAuthenticationTask(
    private val protocol: String,
    private val host: String,
    private val port: Int,
    private val certInfo: JSONObject?,
    private val username: String,
    private val password: String?
) : Task<FTPClient, FtpAuthenticationTaskCallable> {

    override fun getTask(): FtpAuthenticationTaskCallable {
        return if (protocol == FTP_URI_PREFIX) {
            FtpAuthenticationTaskCallable(
                host,
                port,
                username,
                password ?: ""
            )
        } else {
            FtpsAuthenticationTaskCallable(
                host,
                port,
                certInfo!!,
                username,
                password ?: ""
            )
        }
    }

    @MainThread
    override fun onError(error: Throwable) {
        if (error is SocketException || error is SocketTimeoutException || error is ConnectException
        ) {
            AppConfig.toast(
                AppConfig.getInstance(),
                AppConfig.getInstance()
                    .resources
                    .getString(
                        R.string.ssh_connect_failed,
                        host,
                        port,
                        error.localizedMessage ?: error.message
                    )
            )
        }
    }

    @MainThread
    override fun onFinish(value: FTPClient) {
        android.util.Log.d("TEST", value.toString())
    }
}
