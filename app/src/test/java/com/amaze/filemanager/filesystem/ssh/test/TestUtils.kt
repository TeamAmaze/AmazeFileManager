/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.ssh.test

import android.os.Looper
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTPS_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.SSH_URI_PREFIX
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPoolFtpTest
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils.encryptFtpPathAsNecessary
import com.amaze.filemanager.filesystem.ssh.NetCopyClientConnectionPoolSshTest
import net.schmizz.sshj.common.SecurityUtils
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.json.JSONObject
import org.robolectric.Shadows
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom

/**
 * Test support util methods.
 */
object TestUtils {

    /**
     * Generate a [KeyPair] for testing.
     */
    fun createKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(1024, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Save FTP connection settings to database.
     */
    fun saveFtpConnectionSettings(
        validUsername: String,
        validPassword: String,
        certInfo: JSONObject? = null,
        port: Int = NetCopyClientConnectionPoolFtpTest.PORT
    ) {
        val utilsHandler = AppConfig.getInstance().utilsHandler
        val fullUri: StringBuilder = StringBuilder().append(
            if (certInfo != null) {
                FTPS_URI_PREFIX
            } else {
                FTP_URI_PREFIX
            }
        )
        if (validUsername != "" && validPassword != "") {
            fullUri.append(validUsername)
            fullUri.append(':').append(validPassword).append("@")
        }
        fullUri.append("${NetCopyClientConnectionPoolFtpTest.HOST}:$port")

        utilsHandler.saveToDatabase(
            OperationData(
                UtilsHandler.Operation.SFTP,
                encryptFtpPathAsNecessary(fullUri.toString()),
                "Test",
                certInfo?.toString(),
                null,
                null
            )
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Save SSH connection settings to database.
     */
    fun saveSshConnectionSettings(
        hostKeyPair: KeyPair,
        validUsername: String,
        validPassword: String?,
        privateKey: PrivateKey?,
        subpath: String? = null,
        port: Int = NetCopyClientConnectionPoolSshTest.PORT
    ) {
        val utilsHandler = AppConfig.getInstance().utilsHandler
        var privateKeyContents: String? = null
        if (privateKey != null) {
            val writer = StringWriter()
            val jw = JcaPEMWriter(writer)
            jw.writeObject(privateKey)
            jw.flush()
            jw.close()
            privateKeyContents = writer.toString()
        }
        val fullUri: StringBuilder = StringBuilder()
            .append(SSH_URI_PREFIX).append(validUsername)
        if (validPassword != null) fullUri.append(':').append(validPassword)
        fullUri.append(
            "@${NetCopyClientConnectionPoolSshTest.HOST}:$port"
        )

        if (true == subpath?.isNotEmpty()) {
            fullUri.append(subpath)
        }

        if (validPassword != null) utilsHandler.saveToDatabase(
            OperationData(
                UtilsHandler.Operation.SFTP,
                fullUri.toString(),
                "Test",
                SecurityUtils.getFingerprint(hostKeyPair.public),
                null,
                null
            )
        ) else utilsHandler.saveToDatabase(
            OperationData(
                UtilsHandler.Operation.SFTP,
                encryptFtpPathAsNecessary(fullUri.toString()),
                "Test",
                SecurityUtils.getFingerprint(hostKeyPair.public),
                "id_rsa",
                privateKeyContents
            )
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}
