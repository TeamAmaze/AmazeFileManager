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

package com.amaze.filemanager.filesystem.ssh

import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.filesystem.HybridFile
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.test.randomBytes
import com.amaze.filemanager.utils.OnFileFound
import org.awaitility.Awaitility.await
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Test listing files on SSH server.
 */
@Suppress("StringLiteralDuplication")
class FilesOnSshdTest : AbstractSftpServerTest() {

    /**
     * Test list directories normally
     */
    @Test
    fun testNormalListDirs() {
        for (s in arrayOf("sysroot", "srv", "var", "tmp", "bin", "lib", "usr", "sysroot+v2")) {
            File(Environment.getExternalStorageDirectory(), s).mkdir()
        }
        assertTrue(performVerify())
    }

    /**
     * Test list directories and symbolic links
     */
    @Test
    fun testListDirsAndSymlinks() {
        createNecessaryDirsForSymlinkRelatedTests()
        assertTrue(performVerify())
    }

    /**
     * Test listing directories and files with special characters. This includes colon signs, which
     * is possible - even on Windows. See #2964
     */
    @Test
    fun testListFilesWithSpecialChars() {
        createFilesAndDirectoriesWithSpecialChars()
        performVerify2()
        val file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/sysroot%2Bv2/test%2Bfile.bin"
        )
        val content = file.getInputStream(AppConfig.getInstance())?.readBytes()
        assertNotNull(content)
        assertTrue(true == content?.isNotEmpty())
    }

    private fun createNecessaryDirsForSymlinkRelatedTests() {
        Environment.getExternalStorageDirectory().let { root ->
            val sysroot = File(root, "sysroot")
            sysroot.mkdir()
            for (s in arrayOf("srv", "var", "tmp")) {
                val subdir = File(sysroot, s)
                subdir.mkdir()
                Files.createSymbolicLink(
                    Paths.get(File(root, s).absolutePath),
                    Paths.get(subdir.absolutePath)
                )
            }
            for (s in arrayOf("bin", "lib", "usr")) {
                File(root, s).mkdir()
            }
            File(root, "sysroot+v2").mkdirs()
        }
    }

    private fun createFilesAndDirectoriesWithSpecialChars() {
        File(Environment.getExternalStorageDirectory(), "sysroot+v2").let {
            it.mkdirs()
            File(it, "D:").run {
                mkdirs()
                File(this, "Users").mkdirs()
            }
            ByteArrayInputStream(randomBytes()).copyTo(FileOutputStream(File(it, "test+file.bin")))
        }
    }

    private fun performVerify(): Boolean {
        val result: MutableList<String> = ArrayList()
        val file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort"
        )
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    assertTrue("${fileFound.path} not seen as directory", fileFound.isDirectory)
                    result.add(fileFound.name)
                }
            }
        )
        await().until { result.size == 8 }
        assertThat<List<String>>(
            result,
            Matchers.hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr", "sysroot+v2")
        )
        return true
    }

    private fun performVerify2(): Boolean {
        val result: MutableList<String> = ArrayList()
        var file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/sysroot%2Bv2"
        )
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    result.add(fileFound.name)
                }
            }
        )
        await().atMost(90, TimeUnit.SECONDS).until { result.size == 2 }
        assertThat<List<String>>(
            result,
            Matchers.hasItems("test+file.bin", "D:")
        )
        result.clear()
        file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/sysroot%2Bv2/D:"
        )
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    result.add(fileFound.name)
                }
            }
        )
        await().until { result.size == 1 }
        assertThat<List<String>>(
            result,
            Matchers.hasItems("Users")
        )
        result.clear()
        file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/sysroot%2Bv2/D%3A"
        )
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    result.add(fileFound.name)
                }
            }
        )
        await().until { result.size == 1 }
        assertThat<List<String>>(
            result,
            Matchers.hasItems("Users")
        )
        return true
    }

    /**
     * Test list files, directories and symbolic links altogether.
     */
    @Test
    @Throws(Exception::class)
    fun testListDirsAndFilesAndSymlinks() {
        createNecessaryDirsForSymlinkRelatedTests()
        for (i in 1..4) {
            val f = File(Environment.getExternalStorageDirectory(), "$i.txt")
            val out = FileOutputStream(f)
            out.write(i)
            out.close()
            Files.createSymbolicLink(
                Paths.get(
                    File(Environment.getExternalStorageDirectory(), "symlink$i.txt")
                        .absolutePath
                ),
                Paths.get(f.absolutePath)
            )
        }
        val dirs: MutableList<String> = ArrayList()
        val files: MutableList<String> = ArrayList()
        val file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort"
        )
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    if (!fileFound.name.endsWith(".txt")) {
                        assertTrue(
                            fileFound.path + " not seen as directory",
                            fileFound.isDirectory
                        )
                        dirs.add(fileFound.name)
                    } else {
                        assertFalse(
                            fileFound.path + " not seen as file",
                            fileFound.isDirectory
                        )
                        files.add(fileFound.name)
                    }
                }
            }
        )
        await().until { dirs.size == 8 }
        assertThat<List<String>>(
            dirs,
            Matchers.hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr", "sysroot+v2")
        )
        assertThat<List<String>>(
            files,
            Matchers.hasItems(
                "1.txt",
                "2.txt",
                "3.txt",
                "4.txt",
                "symlink1.txt",
                "symlink2.txt",
                "symlink3.txt",
                "symlink4.txt"
            )
        )
    }

    /**
     * Test list directories with broken symbolic links.
     */
    @Test
    @Throws(Exception::class)
    fun testListDirsAndBrokenSymlinks() {
        createNecessaryDirsForSymlinkRelatedTests()
        Files.createSymbolicLink(
            Paths.get(
                File(Environment.getExternalStorageDirectory(), "b0rken.symlink")
                    .absolutePath
            ),
            Paths.get(File("/tmp/notfound.file").absolutePath)
        )
        assertTrue(performVerify())
    }

    /**
     * Test listing directories with full path to directory to list.
     */
    @Test
    @Throws(Exception::class)
    fun testListDirsWithDirectPathToDir() {
        createNecessaryDirsForSymlinkRelatedTests()
        for (i in 1..4) {
            val f = File(File(Environment.getExternalStorageDirectory(), "tmp"), "$i.txt")
            val out = FileOutputStream(f)
            out.write(i)
            out.close()
        }
        val result: MutableList<String> = ArrayList()
        var file = HybridFile(
            OpenMode.SFTP,
            "ssh://$USERNAME:$encryptedPassword@$HOST:$serverPort/tmp"
        )
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    assertFalse("${fileFound.path} not seen as directory", fileFound.isDirectory)
                    result.add(fileFound.name)
                }
            }
        )
        await().until { result.size == 4 }
        assertThat<List<String>>(
            result,
            Matchers.hasItems("1.txt", "2.txt", "3.txt", "4.txt")
        )
        val result2: MutableList<String> = ArrayList()
        file =
            HybridFile(OpenMode.SFTP, file.getParent(ApplicationProvider.getApplicationContext()))
        file.forEachChildrenFile(
            ApplicationProvider.getApplicationContext(),
            false,
            object : OnFileFound {
                override fun onFileFound(fileFound: HybridFileParcelable) {
                    assertTrue("${fileFound.path} not seen as directory", fileFound.isDirectory)
                    result2.add(fileFound.name)
                }
            }
        )
        await().until { result2.size == 8 }
        assertThat<List<String>>(
            result2,
            Matchers.hasItems("sysroot", "srv", "var", "tmp", "bin", "lib", "usr", "sysroot+v2")
        )
    }
}
