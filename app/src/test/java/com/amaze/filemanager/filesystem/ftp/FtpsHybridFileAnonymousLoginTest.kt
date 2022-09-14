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

import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import org.apache.ftpserver.FtpServerFactory
import org.junit.Ignore

/**
 * Test [HybridFile] FTP protocol over secure connection handling with anonymous logins.
 */
@Ignore
class FtpsHybridFileAnonymousLoginTest : FtpsHybridFileTest() {

    override val ftpPort: Int
        get() = PORT
    override val ftpUrl: String
        get() = NetCopyClientUtils.encryptFtpPathAsNecessary(
            "${ftpPrefix}127.0.0.1:$ftpPort"
        )

    companion object {
        private const val PORT = 2224
    }

    override fun saveConnectionSettings() =
        TestUtils.saveFtpConnectionSettings("", "", certInfo, PORT)

    override fun createFtpServerFactory(): FtpServerFactory =
        FtpHybridFileAnonymousLoginTest.createAnonymousFtpServerFactory().also {
            it.addListener(
                "default",
                createDefaultFtpServerListener()
            )
        }
}
