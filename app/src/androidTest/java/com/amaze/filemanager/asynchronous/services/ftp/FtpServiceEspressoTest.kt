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

package com.amaze.filemanager.asynchronous.services.ftp

import android.content.Intent
import android.os.Environment
import android.util.Base64
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import com.amaze.filemanager.filesystem.files.CryptUtil
import com.amaze.filemanager.utils.ObtainableServiceBinder
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
@Suppress("StringLiteralDuplication")
class FtpServiceEspressoTest {

    @get:Rule
    var serviceTestRule = ServiceTestRule()

    private var service: FtpService? = null

    /**
     * Kill running FtpService if there is one.
     */
    @After
    fun shutDown() {
        service?.onDestroy()
    }

    /**
     * Test FTP service
     */
    @Test
    fun testFtpService() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
            .edit()
            .putBoolean(FtpService.KEY_PREFERENCE_SECURE, false)
            .remove(FtpService.KEY_PREFERENCE_USERNAME)
            .remove(FtpService.KEY_PREFERENCE_PASSWORD)
            .commit()
        service = create(
            Intent(FtpService.ACTION_START_FTPSERVER)
                .putExtra(FtpService.TAG_STARTED_BY_TILE, false)
        )

        assertTrue(FtpService.isRunning())
        waitForServer()
        FTPClient().run {
            loginAndVerifyWith(this)
            testUploadWith(this)
            testDownloadWith(this)
        }
    }

    /**
     * Test FTP service over SSL
     */
    @Test
    fun testSecureFtpService() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
            .edit()
            .putBoolean(FtpService.KEY_PREFERENCE_SECURE, true)
            .remove(FtpService.KEY_PREFERENCE_USERNAME)
            .remove(FtpService.KEY_PREFERENCE_PASSWORD)
            .commit()
        service = create(
            Intent(FtpService.ACTION_START_FTPSERVER)
                .putExtra(FtpService.TAG_STARTED_BY_TILE, false)
        )

        assertTrue(FtpService.isRunning())
        waitForServer()

        FTPSClient(true).run {
            loginAndVerifyWith(this)
            testUploadWith(this)
            testDownloadWith(this)
        }
    }

    /**
     * Test to ensure FTP service cannot login anonymously after username/password is set
     */
    @Test
    fun testUsernameEnabledAnonymousCannotLogin() {
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
            .edit()
            .putBoolean(FtpService.KEY_PREFERENCE_SECURE, false)
            .putString(FtpService.KEY_PREFERENCE_USERNAME, "amazeftp")
            .putString(
                FtpService.KEY_PREFERENCE_PASSWORD,
                CryptUtil.encryptPassword(
                    ApplicationProvider.getApplicationContext(),
                    "passw0rD"
                )
            )
            .commit()
        service = create(
            Intent(FtpService.ACTION_START_FTPSERVER)
                .putExtra(FtpService.TAG_STARTED_BY_TILE, false)
        )

        assertTrue(FtpService.isRunning())
        waitForServer()

        FTPClient().run {
            connect("localhost", FtpService.DEFAULT_PORT)
            assertFalse(login("anonymous", "test@example.com"))
            assertTrue(login("amazeftp", "passw0rD"))
            logout()
        }
    }

    private fun loginAndVerifyWith(ftpClient: FTPClient) {
        ftpClient.connect("localhost", FtpService.DEFAULT_PORT)
        ftpClient.login("anonymous", "test@example.com")
        ftpClient.changeWorkingDirectory("/")
        val files = ftpClient.listFiles()
        assertNotNull(files)
        assertTrue(
            "No files found on device? It is also possible that app doesn't have " +
                "permission to access storage, which may occur on broken Android emulators",
            files.size > 0
        )
        var downloadFolderExists = false
        for (f in files) {
            if (f.name.equals("download", ignoreCase = true)) downloadFolderExists = true
        }
        ftpClient.logout()
        ftpClient.disconnect()
        assertTrue(
            "Download folder not found on device. Either storage is not available, " +
                "or something is really wrong with FtpService. Check logcat.",
            downloadFolderExists
        )
    }

    private fun testUploadWith(ftpClient: FTPClient) {
        val bytes1 = ByteArray(32)
        val bytes2 = ByteArray(32)
        SecureRandom().run {
            setSeed(System.currentTimeMillis())
            nextBytes(bytes1)
            nextBytes(bytes2)
        }

        val randomString = Base64.encodeToString(bytes1, Base64.DEFAULT)
        ftpClient.run {
            connect("localhost", FtpService.DEFAULT_PORT)
            login("anonymous", "test@example.com")
            changeWorkingDirectory("/")
            enterLocalPassiveMode()
            setFileType(FTP.ASCII_FILE_TYPE)
            ByteArrayInputStream(randomString.toByteArray(charset("utf-8"))).run {
                storeFile("test.txt", this)
                close()
            }
            ByteArrayInputStream(bytes2).run {
                setFileType(FTP.BINARY_FILE_TYPE)
                storeFile("test.bin", this)
                close()
            }
            logout()
            disconnect()

            File(Environment.getExternalStorageDirectory(), "test.txt").run {
                assertTrue(exists())
                val verifyContent = ByteArrayOutputStream()
                FileInputStream(this).copyTo(verifyContent)
                assertEquals(randomString, verifyContent.toString("utf-8"))
                delete()
            }

            File(Environment.getExternalStorageDirectory(), "test.bin").run {
                assertTrue(exists())
                val verifyContent = ByteArrayOutputStream()
                FileInputStream(this).copyTo(verifyContent)
                assertArrayEquals(bytes2, verifyContent.toByteArray())
                delete()
            }
        }
    }

    private fun testDownloadWith(ftpClient: FTPClient) {
        val testFile1 = File(Environment.getExternalStorageDirectory(), "test.txt")
        val testFile2 = File(Environment.getExternalStorageDirectory(), "test.bin")
        val bytes1 = ByteArray(32)
        val bytes2 = ByteArray(32)
        SecureRandom().run {
            setSeed(System.currentTimeMillis())
            nextBytes(bytes1)
            nextBytes(bytes2)
        }

        val randomString = Base64.encodeToString(bytes1, Base64.DEFAULT)
        FileWriter(testFile1).run {
            write(randomString)
            close()
        }

        FileOutputStream(testFile2).run {
            write(bytes2, 0, bytes2.size)
            close()
        }

        ftpClient.run {
            connect("localhost", FtpService.DEFAULT_PORT)
            login("anonymous", "test@example.com")
            changeWorkingDirectory("/")
            enterLocalPassiveMode()
            setFileType(FTP.ASCII_FILE_TYPE)

            ByteArrayOutputStream().run {
                retrieveFile("test.txt", this)
                close()
                assertEquals(randomString, toString("utf-8"))
            }

            setFileType(FTP.BINARY_FILE_TYPE)

            ByteArrayOutputStream().run {
                retrieveFile("test.bin", this)
                close()
                assertArrayEquals(bytes2, toByteArray())
            }

            logout()
            disconnect()
        }

        testFile1.delete()
        testFile2.delete()
    }

    private fun create(intent: Intent): FtpService {
        val binder = serviceTestRule
            .bindService(
                intent.setClass(
                    ApplicationProvider.getApplicationContext(),
                    FtpService::class.java
                )
            )
        return ((binder as ObtainableServiceBinder<FtpService>).service as FtpService).also {
            it.onStartCommand(intent, 0, 0)
        }
    }

    private fun waitForServer() {
        var available = false
        while (!available) {
            Socket().run {
                available = try {
                    connect(InetSocketAddress(InetAddress.getLocalHost(), FtpService.DEFAULT_PORT))
                    close()
                    true
                } catch (e: SocketException) {
                    false
                }
            }
        }
    }
}
