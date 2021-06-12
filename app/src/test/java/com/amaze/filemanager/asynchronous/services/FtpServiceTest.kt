/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Environment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.services.ftp.FtpService
import com.amaze.filemanager.filesystem.ftpserver.AndroidFileSystemFactory
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.apache.commons.net.ftp.FTPClient
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.util.ReflectionHelpers
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(sdk = [KITKAT], shadows = [ShadowMultiDex::class])
@LooperMode(LooperMode.Mode.PAUSED)
@Suppress("StringLiteralDuplication")
class FtpServiceTest {

    private val FTP_PORT = 62222

    private var server: FtpServer? = null

    private val randomContent = Random.nextBytes(16)

    private var ftpClient: FTPClient? = null

    companion object {

        val directories = arrayOf(
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_PODCASTS,
            Environment.DIRECTORY_RINGTONES,
            Environment.DIRECTORY_ALARMS,
            Environment.DIRECTORY_NOTIFICATIONS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOCUMENTS,
            "1/2/3/4/5/6/7"
        )
    }

    /**
     * Test setup
     */
    @Before
    fun setUp() {
        Environment.getExternalStorageDirectory().run {
            directories.forEach { dir ->
                File(this, dir).mkdirs()
            }
        }

        setupNetwork()
        PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext())
            .run {
                edit().putString(
                    FtpService.KEY_PREFERENCE_PATH,
                    Environment.getExternalStorageDirectory().absolutePath
                )
                    .apply()
            }

        File(Environment.getExternalStorageDirectory(), "test.bin").let { file ->
            file.writeBytes(randomContent)
            shadowOf(ApplicationProvider.getApplicationContext<Context>().contentResolver)
                .registerInputStream(Uri.fromFile(file), FileInputStream(file))
        }

        FtpServerFactory().run {
            val connectionConfigFactory = ConnectionConfigFactory()
            val user = BaseUser()
            user.name = "anonymous"
            user.homeDirectory = Environment.getExternalStorageDirectory().absolutePath
            connectionConfigFactory.isAnonymousLoginEnabled = true
            connectionConfig = connectionConfigFactory.createConnectionConfig()
            userManager.save(user)

            fileSystem = AndroidFileSystemFactory(
                ApplicationProvider.getApplicationContext()
            )
            addListener(
                "default",
                ListenerFactory().also {
                    it.port = FTP_PORT
                }.createListener()
            )

            server = createServer().apply {
                start()
            }
        }

        ftpClient = FTPClient().also {
            it.connect("127.0.0.1", FTP_PORT)
            it.login("anonymous", "no@e.mail")
            it.enterLocalPassiveMode()
        }
    }

    private fun setupNetwork() {
        val cm = shadowOf(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
        val wifiManager = shadowOf(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
        )
        cm.setActiveNetworkInfo(
            ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI,
                -1,
                true,
                NetworkInfo.State.CONNECTED
            )
        )
        ReflectionHelpers.callInstanceMethod<Any>(
            wifiManager,
            "setWifiEnabled",
            ReflectionHelpers.ClassParameter.from(Boolean::class.java, true)
        )
        ReflectionHelpers.callInstanceMethod<WifiInfo>(
            wifiManager,
            "getConnectionInfo"
        ).run {
            ReflectionHelpers.callInstanceMethod<Any>(
                this,
                "setInetAddress",
                ReflectionHelpers.ClassParameter.from(
                    InetAddress::class.java,
                    InetAddress.getLoopbackAddress()
                )
            )
        }
    }

    /**
     * Kill FTP server if there is one running
     */
    @After
    fun tearDown() {
        ftpClient?.logout()
        server?.stop()
    }

    /**
     * Test on change directory functions
     */
    @Test
    fun testChdir() {
        ftpClient!!.run {
            assertEquals(directories.size + 1, listFiles().size)
            assertTrue(changeWorkingDirectory("Download"))
            assertEquals(0, listFiles().size)
            assertTrue(changeWorkingDirectory(".."))
            assertEquals(directories.size + 1, listFiles().size)
            assertTrue(changeWorkingDirectory("/1/2/3/4/5/6/7"))
            assertEquals(0, listFiles().size)
            assertTrue(changeWorkingDirectory("../"))
            assertTrue(printWorkingDirectory().startsWith("/1/2/3/4/5/6"))
            assertTrue(changeToParentDirectory())
            assertTrue(printWorkingDirectory().startsWith("/1/2/3/4/5"))
            assertTrue(changeWorkingDirectory("../../.."))
            assertTrue(printWorkingDirectory().startsWith("/1/2"))
        }
    }

    /**
     * Test remove directory function
     */
    @Test
    fun testRmDir() {
        ftpClient!!.run {
            assertTrue(changeWorkingDirectory("/"))
            assertTrue(makeDirectory("foobar"))
            assertEquals(directories.size + 2, listFiles().size)
            assertTrue(removeDirectory("foobar"))
            assertEquals(directories.size + 1, listFiles().size)
            assertFalse(changeWorkingDirectory("foobar"))
            assertTrue(listFiles("/foobar").isNullOrEmpty())
        }
    }

    /**
     * Test download file
     */
    @Test
    fun testDownloadFile() {
        ftpClient!!.run {
            assertFalse(deleteFile("/nonexist.file.txt"))
            assertNull(retrieveFileStream("/not/existing/file"))
            assertFalse(
                retrieveFile(
                    "/barrier/nonexist.file.jpg",
                    FileOutputStream(File.createTempFile("notused", "output"))
                )
            )
            assertTrue(changeWorkingDirectory("/"))
            retrieveFileStream("test.bin").let {
                assertNotNull(it)
                it.close()
            }
            completePendingCommand()
            assertTrue(printWorkingDirectory() == "/")
        }
    }

    /**
     * Test upload file
     */
    @Test
    fun testUploadFile() {
        ftpClient!!.run {
            storeFileStream("/test2.bin").let {
                ByteArrayInputStream(Random.nextBytes(24)).copyTo(it)
                it.flush()
                it.close()
            }
            completePendingCommand()
            assertTrue(rename("/test2.bin", "/test2.arc.bin"))
        }
    }

    /**
     * Test working with files and folders at subfolders
     */
    @Test
    fun testUploadFileToSubDir() {
        ftpClient!!.run {
            assertTrue(makeDirectory("/CROSS OVER"))
            storeFileStream("/CROSS OVER/test3.bin").let {
                ByteArrayInputStream(Random.nextBytes(24)).copyTo(it)
                it.flush()
                it.close()
            }
            completePendingCommand()
            assertTrue(changeWorkingDirectory("/CROSS OVER"))
            listFiles().let {
                assertEquals(1, it.size)
                assertEquals("test3.bin", it[0].name)
            }
            assertTrue(makeDirectory("/CROSS OVER/multiple"))
            assertTrue(makeDirectory("/CROSS OVER/multiple/levels down"))
            assertTrue(changeWorkingDirectory("/CROSS OVER/multiple"))
            assertTrue(changeWorkingDirectory("levels down"))
            assertEquals("/CROSS OVER/multiple/levels down", printWorkingDirectory())
            assertTrue(deleteFile("/CROSS OVER/test3.bin"))
            assertTrue(changeWorkingDirectory("/CROSS OVER"))
            listFiles().let {
                assertEquals(1, it.size)
                assertEquals("multiple", it[0].name)
            }
        }
    }
}
