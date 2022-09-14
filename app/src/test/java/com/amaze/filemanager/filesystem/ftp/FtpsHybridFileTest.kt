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

import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import com.amaze.filemanager.utils.X509CertificateUtil
import org.apache.ftpserver.listener.Listener
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.ssl.ClientAuth
import org.apache.ftpserver.ssl.impl.DefaultSslConfiguration
import org.json.JSONObject
import org.junit.Ignore
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import javax.security.cert.X509Certificate

@Ignore
open class FtpsHybridFileTest : FtpHybridFileTest() {

    private lateinit var keyStore: KeyStore
    private lateinit var keyStorePassword: CharArray
    protected lateinit var certInfo: JSONObject

    override val ftpPrefix: String
        get() = FTPS_URI_PREFIX
    override val ftpPort: Int
        get() = PORT

    companion object {
        private const val PORT = 2222
    }

    override fun setUp() {
        keyStore = KeyStore.getInstance("BKS")
        keyStorePassword = BuildConfig.FTP_SERVER_KEYSTORE_PASSWORD.toCharArray()
        keyStore.load(
            AppConfig.getInstance().resources.openRawResource(R.raw.key),
            keyStorePassword
        )
        certInfo = JSONObject(
            X509CertificateUtil.parse(
                X509Certificate.getInstance(keyStore.getCertificate("ftpserver").encoded)
            )
        )
        super.setUp()
    }

    override fun saveConnectionSettings() =
        TestUtils.saveFtpConnectionSettings(USERNAME, PASSWORD, certInfo, PORT)

    override fun createDefaultFtpServerListener(): Listener {
        val keyManagerFactory = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, keyStorePassword)
        val trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        return ListenerFactory().apply {
            sslConfiguration = DefaultSslConfiguration(
                keyManagerFactory,
                trustManagerFactory,
                ClientAuth.WANT,
                "TLSv1.2",
                null,
                "ftpserver"
            )
            isImplicitSsl = true
            port = ftpPort
        }.createListener()
    }
}
