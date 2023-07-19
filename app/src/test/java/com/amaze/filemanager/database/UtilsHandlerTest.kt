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

package com.amaze.filemanager.database

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import android.os.Environment
import android.os.Environment.DIRECTORY_DCIM
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_MUSIC
import android.os.Environment.DIRECTORY_PICTURES
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.models.OperationData
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.smb.SmbUtil
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowMultiDex::class, ShadowPasswordUtil::class],
    sdk = [KITKAT, P, Build.VERSION_CODES.R]
)
class UtilsHandlerTest {

    companion object {
        /**
         * Enforce use of in-mem database during test.
         */
        @BeforeClass
        @JvmStatic
        fun bootstrap() {
            UtilitiesDatabase.overrideDatabaseBuilder = { context ->
                Room.inMemoryDatabaseBuilder(context, UtilitiesDatabase::class.java)
            }
        }
    }

    /**
     * Test setup. Set all RxJava thread scheduler to trampoline and clear database tables
     * for sanity.
     */
    @Before
    fun setUp() {
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.reset()
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        AppConfig.getInstance().utilitiesDatabase.run {
            clearAllTables()
        }
    }

    /**
     * Test simply save a [OperationData]. SMB path chosen here.
     */
    @Test
    fun testSimpleSaveSmb() {
        val path = "smb://user:password@127.0.0.1/user"
        val o = OperationData(
            UtilsHandler.Operation.SMB,
            "SMB Connection 1",
            SmbUtil.getSmbEncryptedPath(
                AppConfig.getInstance(),
                path
            )
        )
        AppConfig.getInstance().run {
            utilsHandler.run {
                saveToDatabase(o)
                val verify = smbList
                assertEquals(1, verify.size)
                assertEquals(
                    SmbUtil.getSmbEncryptedPath(
                        AppConfig.getInstance(),
                        path
                    ),
                    verify[0][1]
                )
            }
            utilitiesDatabase.run {
                val verify = smbEntryDao().list().blockingGet()
                assertEquals(1, verify.size)
                assertNotEquals(path, verify.first().path)
            }
        }
    }

    /**
     * Test repeatedly save a SMB path with same connection name.
     */
    @Test
    fun testRepeatedSaveSmb() {
        val path = "smb://user:password@127.0.0.1/user"
        val o = OperationData(
            UtilsHandler.Operation.SMB,
            "SMB Connection 1",
            SmbUtil.getSmbEncryptedPath(
                AppConfig.getInstance(),
                path
            )
        )
        AppConfig.getInstance().run {
            utilsHandler.run {
                saveToDatabase(o)
                saveToDatabase(o)
                val verify1 = smbList
                assertEquals(1, verify1.size)
                val verify2 = smbList
                assertEquals(1, verify2.size)
            }
            utilitiesDatabase.run {
                val verify1 = smbEntryDao().list().blockingGet()
                assertEquals(1, verify1.size)
                val verify2 = smbEntryDao().list().blockingGet()
                assertEquals(1, verify2.size)
                assertEquals(verify1[0].name, verify2[0].name)
                assertEquals(verify1[0].path, verify2[0].path)
                assertEquals(verify1[0], verify2[0])
            }
        }
    }

    /**
     * Test repeatedly save a history path.
     */
    @Test
    fun testRepeatedSaveHistory() {
        val o = OperationData(UtilsHandler.Operation.HISTORY, "/storage/1234-5678/Documents")
        AppConfig.getInstance().run {
            utilsHandler.run {
                saveToDatabase(o)
                saveToDatabase(o)
                saveToDatabase(o)
                saveToDatabase(o)
                val verify = historyLinkedList
                assertEquals(1, verify.size)
                assertEquals(verify[0], o.path)
            }
        }
    }

    /**
     * Test prepopulate bookmark function.
     */
    @Test
    fun testSaveCommonBookmarks() {
        AppConfig.getInstance().utilsHandler.run {
            addCommonBookmarks()
            val verify = bookmarksList.map {
                Pair(it[0], it[1])
            }
            for (
                d in arrayOf(
                    DIRECTORY_DOWNLOADS,
                    DIRECTORY_DCIM,
                    DIRECTORY_MUSIC,
                    DIRECTORY_MOVIES,
                    DIRECTORY_PICTURES
                )
            ) {
                assertTrue(
                    verify.contains(
                        Pair<String, String>(
                            d,
                            File(Environment.getExternalStorageDirectory(), d).absolutePath
                        )
                    )
                )
            }
        }
    }

    /**
     * Test repeatedly save a SSH connection setting with password.
     */
    @Test
    fun testRepeatedSaveSshWithPassword() {
        val o = OperationData(
            UtilsHandler.Operation.SFTP,
            "ssh://root:toor@127.0.0.1/root/.ssh",
            "SSH connection 1",
            "ab:cd:ef:gh",
            null,
            null
        )
        AppConfig.getInstance().run {
            utilsHandler.run {
                saveToDatabase(o)
                saveToDatabase(o)
                saveToDatabase(o)
                val verify = sftpList
                assertEquals(1, verify.size)
                assertEquals(o.name, verify[0][0])
                assertEquals(o.path, verify[0][1])
            }
            utilitiesDatabase.run {
                val verify = sftpEntryDao().list().blockingGet()
                assertEquals(1, verify.size)
                verify.first().let {
                    assertEquals(o.path, it.path)
                    assertEquals(o.name, it.name)
                    assertEquals(o.hostKey, it.hostKey)
                    assertNull(it.sshKey)
                    assertNull(it.sshKeyName)
                }
            }
        }
    }

    /**
     * Test repeatedly save a SSH connection setting with password.
     */
    @Test
    fun testRepeatedSaveSshWithKeyAuth() {
        val o = OperationData(
            UtilsHandler.Operation.SFTP,
            "ssh://root@127.0.0.1/root/.ssh",
            "SSH connection 1",
            "ab:cd:ef:gh",
            "Test SSH key",
            "abcdefghijkl"
        )
        AppConfig.getInstance().run {
            utilsHandler.run {
                saveToDatabase(o)
                saveToDatabase(o)
                saveToDatabase(o)
                val verify = sftpList
                assertEquals(1, verify.size)
                assertEquals(o.name, verify[0][0])
                assertEquals(o.path, verify[0][1])
            }
            utilitiesDatabase.run {
                val verify = sftpEntryDao().list().blockingGet()
                assertEquals(1, verify.size)
                verify.first().let {
                    assertEquals(o.path, it.path)
                    assertEquals(o.name, it.name)
                    assertEquals(o.hostKey, it.hostKey)
                    assertEquals(o.sshKey, it.sshKey)
                    assertEquals(o.sshKeyName, it.sshKeyName)
                }
            }
        }
    }
}
