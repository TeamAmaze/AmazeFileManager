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

package com.amaze.filemanager.filesystem.ftp

import android.os.Environment
import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.junit.Ignore

/**
 * Test [HybridFile] FTP protocol handling with anonymous logins.
 */
@Ignore
class FtpHybridFileAnonymousLoginTest : FtpHybridFileTest() {

    override val ftpPort: Int
        get() = PORT
    override val ftpUrl: String
        get() = NetCopyClientUtils.encryptFtpPathAsNecessary(
            "${ftpPrefix}127.0.0.1:$ftpPort"
        )

    override fun saveConnectionSettings() {
        TestUtils.saveFtpConnectionSettings("", "")
    }

    companion object {

        private const val PORT = 2223

        /**
         * Extracted [FtpServerFactory] with anonymous login support into separate factory method.
         */
        @JvmStatic
        fun createAnonymousFtpServerFactory(): FtpServerFactory = FtpServerFactory().also {
            val connectionConfigFactory = ConnectionConfigFactory()
            connectionConfigFactory.isAnonymousLoginEnabled = true
            val user = BaseUser()
            user.name = FTPClientImpl.ANONYMOUS
            user.homeDirectory = Environment.getExternalStorageDirectory().absolutePath
            user.authorities = listOf(WritePermission())
            it.userManager.save(user)
            it.connectionConfig = connectionConfigFactory.createConnectionConfig()
        }
    }

    override fun createFtpServerFactory(): FtpServerFactory {
        return createAnonymousFtpServerFactory().also {
            it.addListener(
                "default",
                createDefaultFtpServerListener()
            )
        }
    }
}
