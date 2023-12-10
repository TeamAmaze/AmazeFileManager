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

import android.os.Build.VERSION_CODES.P
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.services.FtpServiceAndroidFileSystemIntegrationTest
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.Operations
import com.amaze.filemanager.filesystem.OperationsTest
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.filesystem.ssh.test.TestUtils
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.apache.commons.net.ftp.FTPClient
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.Listener
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.awaitility.Awaitility.await
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.net.InetAddress.getLoopbackAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [P],
    shadows = [ShadowPasswordUtil::class, ShadowMultiDex::class]
)
@Suppress("StringLiteralDuplication")
@Ignore
open class FtpHybridFileTest {

    protected lateinit var tmpFile: File
    protected lateinit var hybridFile: HybridFile
    protected lateinit var ftpServer: FtpServer

    protected open val ftpPrefix: String
        get() = FTP_URI_PREFIX

    /**
     * Workaround. Tests are being run in parallel, hence different FTP server integration
     * tests will need a different port to prevent port in use exceptions.
     */
    protected open val ftpPort: Int
        get() = PORT
    protected open val ftpUrl: String
        get() = NetCopyClientUtils.encryptFtpPathAsNecessary(
            "${ftpPrefix}$USERNAME:$PASSWORD@127.0.0.1:$ftpPort"
        )

    companion object {
        const val USERNAME = "ftpuser"
        const val PASSWORD = "passw0rD"

        private const val PORT = 2221
    }

    /**
     * Setup FTP server for testing.
     *
     * Just like [AbstractSftpServerTest], have to use setUp and tearDown instead of static init/
     * destroy methods for tests to work.
     */
    @Before
    open fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler {
            Schedulers.trampoline()
        }
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler {
            Schedulers.trampoline()
        }
        NetCopyClientUtils.getScheduler = {
            Schedulers.trampoline()
        }
        Environment.getExternalStorageDirectory().run {
            FtpServiceAndroidFileSystemIntegrationTest.directories.forEach { dir ->
                File(this, dir).mkdirs()
            }
        }
        tmpFile = File.createTempFile("test", ".bin")
        tmpFile.deleteOnExit()
        ftpServer = createFtpServerFactory().createServer()
        ftpServer.start()
        saveConnectionSettings()
        await().atMost(30, TimeUnit.SECONDS).until { isServerReady() }
        val verify = createConnection()
        verify?.run {
            assertTrue(this is FTPClientImpl)
            assertNotNull(getClientImpl())
            hybridFile = HybridFile(OpenMode.FTP, ftpUrl)
            assertTrue(hybridFile.isFtp)
        } ?: fail(
            "Unable to obtain connection.\n" +
                "\n" +
                " Was trying $ftpUrl, FTP server is running at " +
                getLoopbackAddress().hostAddress +
                " and port $ftpPort"
        )
    }

    /**
     * Shutdown FTP server.
     */
    @After
    fun tearDown() {
        if (!ftpServer.isStopped) {
            ftpServer.stop()
        }
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        NetCopyClientConnectionPool.shutdown()
    }

//    /**
//     * Test list files
//     *
//     * @see HybridFile.forEachChildrenFile
//     * @see HybridFile.listFiles
//     */
//    @Test
//    @FlakyTest()
//    fun testListFile() {
//        val files = hybridFile.listFiles(AppConfig.getInstance(), false)
//        assertEquals(FtpServiceAndroidFileSystemIntegrationTest.directories.size, files.size)
//    }
//
//    /**
//     * Test create file
//     *
//     * @see Operations.mkfile
//     */
//    @Test
//    @FlakyTest()
//    fun testMkFile() {
//        val newFile = HybridFile(OpenMode.FTP, "$ftpUrl/${tmpFile.name}")
//        val latch = CountDownLatch(1)
//        Operations.mkfile(
//            hybridFile,
//            newFile,
//            AppConfig.getInstance(),
//            false,
//            object : OperationsTest.AbstractErrorCallback() {
//                override fun done(file: HybridFile?, b: Boolean) {
//                    assertTrue(true == file?.exists())
//                    assertEquals(newFile.path, file?.path)
//                    assertNotNull(file?.ftpFile)
//                    latch.countDown()
//                }
//            }
//        )
//        latch.await()
//    }
//
//    /**
//     * Test rename file
//     *
//     * @see Operations.rename
//     */
//    @Test
//    @FlakyTest()
//    fun testRenameFile() {
//        val oldFile = HybridFile(OpenMode.FTP, "$ftpUrl/${tmpFile.name}")
//        val newFile = HybridFile(OpenMode.FTP, "$ftpUrl/${tmpFile.name}-new")
//        var latch = CountDownLatch(1)
//        Operations.mkfile(
//            hybridFile,
//            oldFile,
//            AppConfig.getInstance(),
//            false,
//            object : OperationsTest.AbstractErrorCallback() {
//                override fun done(file: HybridFile?, b: Boolean) {
//                    assertTrue(true == file?.exists())
//                    assertEquals(oldFile.path, file?.path)
//                    assertNotNull(file?.ftpFile)
//                    latch.countDown()
//                }
//            }
//        )
//        latch.await()
//        latch = CountDownLatch(1)
//        Operations.rename(
//            oldFile,
//            newFile,
//            false,
//            AppConfig.getInstance(),
//            object : OperationsTest.AbstractErrorCallback() {
//                override fun done(file: HybridFile?, b: Boolean) {
//                    assertTrue(true == file?.exists())
//                    assertFalse(oldFile.exists())
//                    assertTrue(newFile.exists())
//                    assertEquals(newFile.path, file?.path)
//                    assertNotNull(file?.ftpFile)
//                    latch.countDown()
//                }
//            }
//        )
//        latch.await()
//    }
//
//    /**
//     * Test file I/O.
//     *
//     * @see HybridFile.getOutputStream
//     * @see HybridFile.getInputStream
//     */
//    @Test
//    @FlakyTest()
//    fun testFileIO() {
//        val randomBytes = Random(System.currentTimeMillis()).nextBytes(32)
//        val f = HybridFile(
//            OpenMode.FTP,
//            "$ftpUrl/${tmpFile.name}"
//        )
//        f.getOutputStream(AppConfig.getInstance())?.run {
//            ByteArrayInputStream(randomBytes).copyTo(this)
//            this.close()
//        } ?: fail("Unable to get OutputStream")
//        await().atMost(10, TimeUnit.SECONDS).until {
//            randomBytes.size.toLong() == f.length(AppConfig.getInstance())
//        }
//        f.getInputStream(AppConfig.getInstance())?.run {
//            val verify = this.readBytes()
//            assertArrayEquals(randomBytes, verify)
//        } ?: fail("Unable to get InputStream")
//    }

    /**
     * Test create dir.
     *
     * @see Operations.mkdir
     */
    @Test
    @FlakyTest()
    fun testMkdir() {
        for (
            dir: String in arrayOf(
//                "newfolder",
//                "new folder 2",
                "new%20folder%203",
                "あいうえお",
                "multiple/levels/down the pipe"
            )
        ) {
            val newFile = HybridFile(OpenMode.FTP, "$ftpUrl/$dir")
            val latch = CountDownLatch(1)
            Operations.mkdir(
                hybridFile,
                newFile,
                AppConfig.getInstance(),
                false,
                object : OperationsTest.AbstractErrorCallback() {
                    override fun done(file: HybridFile?, b: Boolean) {
                        assertTrue(true == file?.exists())
                        assertEquals(newFile.path, file?.path)
                        latch.countDown()
                    }
                }
            )
            latch.await()
        }
    }

    protected open fun beforeCreateFtpServer() = Unit

    protected open fun saveConnectionSettings() =
        TestUtils.saveFtpConnectionSettings(USERNAME, PASSWORD)

    protected open fun createConnection(): NetCopyClient<FTPClient>? {
        return NetCopyClientConnectionPool.getConnection(ftpUrl)
    }

    protected open fun createFtpServerFactory(): FtpServerFactory {
        return FtpServerFactory().also {
            val connectionConfigFactory = ConnectionConfigFactory()
            val user = BaseUser()
            user.name = USERNAME
            user.password = PASSWORD
            user.homeDirectory = Environment.getExternalStorageDirectory().absolutePath
            user.authorities = listOf(WritePermission())
            it.userManager.save(user)
            it.connectionConfig = connectionConfigFactory.createConnectionConfig()
            it.addListener(
                "default",
                createDefaultFtpServerListener()
            )
        }
    }

    protected open fun createDefaultFtpServerListener(): Listener {
        return ListenerFactory().apply {
            port = ftpPort
            serverAddress = "127.0.0.1"
        }.createListener()
    }

    private fun isServerReady(): Boolean {
        return Socket().let {
            try {
                it.connect(InetSocketAddress("127.0.0.1", ftpPort))
                true
            } catch (e: SocketException) {
                false
            } finally {
                it.close()
            }
        }
    }
}
