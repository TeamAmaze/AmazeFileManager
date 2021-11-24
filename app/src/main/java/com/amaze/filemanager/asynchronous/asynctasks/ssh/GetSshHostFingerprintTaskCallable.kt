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

package com.amaze.filemanager.asynchronous.asynctasks.ssh

import android.util.Log
import com.amaze.filemanager.filesystem.ftp.FtpConnectionPool
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig
import com.amaze.filemanager.filesystem.ssh.SshClientUtils
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.PublicKey
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch

class GetSshHostFingerprintTaskCallable(
    private val hostname: String,
    private val port: Int
) : Callable<PublicKey> {

    companion object {
        const val TAG = "GetSshHostFingerprint"
    }

    override fun call(): PublicKey {
        var holder: PublicKey? = null
        val latch = CountDownLatch(1)
        val sshClient = FtpConnectionPool.sshClientFactory.create(CustomSshJConfig()).also {
            it.connectTimeout = FtpConnectionPool.CONNECT_TIMEOUT
            it.addHostKeyVerifier(object : HostKeyVerifier {
                override fun verify(hostname: String?, port: Int, key: PublicKey?): Boolean {
                    holder = key
                    latch.countDown()
                    return true
                }
                override fun findExistingAlgorithms(
                    hostname: String?,
                    port: Int
                ): MutableList<String> = Collections.emptyList()
            })
        }
        return runCatching {
            sshClient.connect(hostname, port)
            latch.await()
            holder!!
        }.onFailure {
            Log.e(TAG, "Unable to connect to [$hostname:$port]", it)
            latch.countDown()
        }.getOrThrow().also {
            SshClientUtils.tryDisconnect(sshClient)
        }
    }
}
