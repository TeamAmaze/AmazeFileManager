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

import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool
import com.amaze.filemanager.filesystem.ssh.CustomSshJConfig
import com.amaze.filemanager.utils.PasswordUtil
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.net.URLDecoder.decode
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.concurrent.Callable
import kotlin.text.Charsets.UTF_8

class SshAuthenticationTaskCallable(
    private val hostname: String,
    private val port: Int,
    private val hostKey: String,
    private val username: String,
    private val password: String? = null,
    private val privateKey: KeyPair? = null
) : Callable<SSHClient> {

    init {
        require(
            true == password?.isNotEmpty() || privateKey != null
        ) {
            "Must provide either password or privateKey"
        }
    }

    override fun call(): SSHClient {
        val sshClient = NetCopyClientConnectionPool.sshClientFactory
            .create(CustomSshJConfig()).also {
                it.addHostKeyVerifier(hostKey)
                it.connectTimeout = NetCopyClientConnectionPool.CONNECT_TIMEOUT
            }
        return run {
            sshClient.connect(hostname, port)
            if (privateKey != null) {
                sshClient.authPublickey(
                    decode(username, UTF_8.name()),
                    object : KeyProvider {
                        override fun getPrivate(): PrivateKey = privateKey.private

                        override fun getPublic(): PublicKey = privateKey.public

                        override fun getType(): KeyType = KeyType.fromKey(public)
                    }
                )
                sshClient
            } else {
                sshClient.authPassword(
                    decode(username, UTF_8.name()),
                    decode(
                        PasswordUtil.decryptPassword(
                            AppConfig.getInstance(),
                            password!!
                        ),
                        UTF_8.name()
                    )
                )
                sshClient
            }
        }
    }
}
