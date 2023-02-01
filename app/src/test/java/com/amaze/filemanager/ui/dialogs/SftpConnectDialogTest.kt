/*
 * Copyright (C) 2014-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.ui.dialogs

import android.os.Bundle
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.database.UtilsHandler
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils
import com.amaze.filemanager.ui.activities.AbstractMainActivityTestBase
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.utils.PasswordUtil
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.MockedConstruction
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doCallRealMethod
import org.robolectric.util.ReflectionHelpers
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.TimeUnit

@Suppress("StringLiteralDuplication")
class SftpConnectDialogTest : AbstractMainActivityTestBase() {

    private lateinit var mc: MockedConstruction<SftpConnectDialog>

    /**
     * Setups before test.
     */
    @Before
    override fun setUp() {
        super.setUp()
        mc = mockConstruction(
            SftpConnectDialog::class.java
        ) { mock: SftpConnectDialog, _: MockedConstruction.Context? ->
            doCallRealMethod().`when`(mock).arguments = any()
            `when`(mock.arguments).thenCallRealMethod()
        }
    }

    /**
     * Post test cleanups.
     */
    @After
    override fun tearDown() {
        super.tearDown()
        mc.close()
    }

    /**
     * Test invoke [SftpConnectDialog] with arguments including keypair name.
     */
    @Test
    fun testInvokeSftpConnectionDialog() {
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 22)
        verify.putString("name", "SCP/SFTP Connection")
        verify.putString("username", "root")
        verify.putBoolean("hasPassword", false)
        verify.putBoolean("edit", true)
        verify.putString("keypairName", "abcdefgh")
        testOpenSftpConnectDialog("ssh://root@127.0.0.1:22", verify)
    }

    /**
     * Test invoke [SftpConnectDialog] with arguments including password.
     */
    @Test
    fun testInvokeSftpConnectionDialogWithPassword() {
        val uri = NetCopyClientUtils.encryptFtpPathAsNecessary("ssh://root:12345678@127.0.0.1:22")
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 22)
        verify.putString("name", "SCP/SFTP Connection")
        verify.putString("username", "root")
        verify.putBoolean("hasPassword", true)
        verify.putBoolean("edit", true)
        verify.putString(
            "password",
            PasswordUtil.encryptPassword(AppConfig.getInstance(), "12345678", Base64.URL_SAFE)
                ?.replace("\n", "")
        )
        testOpenSftpConnectDialog(uri, verify)
    }

    /**
     * Test invoke [SftpConnectDialog] with arguments including password and default path.
     */
    @Test
    fun testInvokeSftpConnectionDialogWithPasswordAndDefaultPath() {
        val uri = NetCopyClientUtils.encryptFtpPathAsNecessary(
            "ssh://root:12345678@127.0.0.1:22/data/incoming"
        )
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 22)
        verify.putString("name", "SCP/SFTP Connection")
        verify.putString("username", "root")
        verify.putBoolean("hasPassword", true)
        verify.putBoolean("edit", true)
        verify.putString("defaultPath", "/data/incoming")
        verify.putString(
            "password",
            PasswordUtil.encryptPassword(AppConfig.getInstance(), "12345678", Base64.URL_SAFE)
                ?.replace("\n", "")
        )
        testOpenSftpConnectDialog(uri, verify)
    }

    /**
     * Test invoke [SftpConnectDialog] with arguments including password and URL encoded path.
     */
    @Test
    @Throws(GeneralSecurityException::class, IOException::class)
    fun testInvokeSftpConnectionDialogWithPasswordAndEncodedDefaultPath() {
        /* ktlint-disable max-line-length */
        val uri = NetCopyClientUtils.encryptFtpPathAsNecessary(
            "ssh://root:12345678@127.0.0.1:22/Users/TranceLove/My+Documents/%7BReference%7D%20Zobius%20Facro%20%24%24%20%23RFII1"
        )
        /* ktlint-enable max-line-length */
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 22)
        verify.putString("name", "SCP/SFTP Connection")
        verify.putString("username", "root")
        verify.putBoolean("hasPassword", true)
        verify.putBoolean("edit", true)
        verify.putString(
            "defaultPath",
            "/Users/TranceLove/My Documents/{Reference} Zobius Facro $$ #RFII1"
        )
        verify.putString(
            "password",
            PasswordUtil.encryptPassword(AppConfig.getInstance(), "12345678", Base64.URL_SAFE)
                ?.replace("\n", "")
        )
        testOpenSftpConnectDialog(uri, verify)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun testOpenSftpConnectDialog(uri: String, verify: Bundle) {
        val activity = mock(MainActivity::class.java)
        val utilsHandler = mock(UtilsHandler::class.java)
        `when`(utilsHandler.getSshAuthPrivateKeyName("ssh://root@127.0.0.1:22"))
            .thenReturn("abcdefgh")
        ReflectionHelpers.setField(activity, "utilsHandler", utilsHandler)
        doCallRealMethod().`when`(activity).showSftpDialog(
            any(),
            any(),
            anyBoolean()
        )
        activity.showSftpDialog(
            "SCP/SFTP Connection",
            NetCopyClientUtils.encryptFtpPathAsNecessary(uri),
            true
        )
        assertEquals(1, mc.constructed().size)
        val mocked = mc.constructed()[0]
        await().atMost(10, TimeUnit.SECONDS).until { mocked.arguments != null }
        for (key in BUNDLE_KEYS) {
            if (mocked.arguments!![key] != null) {
                if (key != "password") {
                    assertEquals(verify[key], mocked.arguments!![key])
                } else {
                    assertEquals(
                        verify[key],
                        PasswordUtil.decryptPassword(
                            ApplicationProvider.getApplicationContext(),
                            (mocked.arguments!![key] as String?)!!,
                            Base64.URL_SAFE
                        )
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        private val BUNDLE_KEYS = arrayOf(
            "address",
            "port",
            "keypairName",
            "name",
            "username",
            "password",
            "edit",
            "defaultPath"
        )
    }
}
