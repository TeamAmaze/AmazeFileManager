package com.amaze.filemanager.ui.dialogs

import android.os.Bundle
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl.Companion.ARG_TLS
import com.amaze.filemanager.filesystem.ftp.NetCopyClientConnectionPool.FTP_URI_PREFIX
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_ADDRESS
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_DEFAULT_PATH
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_EDIT
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_HAS_PASSWORD
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_NAME
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_PASSWORD
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_PORT
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_PROTOCOL
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_USERNAME
import com.amaze.filemanager.utils.PasswordUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [SftpConnectDialog] populating arguments to UI at edit mode.
 */
@Suppress("StringLiteralDuplication")
class SftpConnectDialogArgumentPopulationTest : AbstractSftpConnectDialogUiTests() {
    /**
     * Test scenario with FTP, username and password
     */
    @Test
    fun testFtpWithUsernamePassword() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putString(ARG_USERNAME, "root")
                    it.putString(
                        ARG_PASSWORD,
                        PasswordUtil.encryptPassword(
                            AppConfig.getInstance(),
                            "abcdefgh",
                        ),
                    )
                    it.putBoolean(ARG_HAS_PASSWORD, true)
                    it.putBoolean(ARG_EDIT, true)
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertFalse(binding.chkFtpExplicitTls.isChecked)
                    assertFalse(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("root", binding.usernameET.text.toString())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                }
            },
        )
    }

    /**
     * Test scenario with FTP, username and password and default path
     */
    @Test
    fun testFtpWithUsernamePasswordAndDefaultPath() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putString(ARG_USERNAME, "root")
                    it.putString(ARG_DEFAULT_PATH, "/root/Private")
                    it.putString(
                        ARG_PASSWORD,
                        PasswordUtil.encryptPassword(
                            AppConfig.getInstance(),
                            "abcdefgh",
                        ),
                    )
                    it.putBoolean(ARG_HAS_PASSWORD, true)
                    it.putBoolean(ARG_EDIT, true)
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertFalse(binding.chkFtpExplicitTls.isChecked)
                    assertFalse(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("root", binding.usernameET.text.toString())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                    assertEquals("/root/Private", binding.defaultPathET.text.toString())
                }
            },
        )
    }

    /**
     * Test scenario with FTP, anonymous
     */
    @Test
    fun testFtpWithAnonymous() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putBoolean(ARG_HAS_PASSWORD, false)
                    it.putBoolean(ARG_EDIT, true)
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertFalse(binding.chkFtpExplicitTls.isChecked)
                    assertTrue(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                }
            },
        )
    }

    /**
     * Test scenario with FTP, username and password + explicit TLS option
     */
    @Test
    fun testFtpWithUsernamePasswordAndExplicitTls() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putString(ARG_USERNAME, "root")
                    it.putString(
                        ARG_PASSWORD,
                        PasswordUtil.encryptPassword(
                            AppConfig.getInstance(),
                            "abcdefgh",
                        ),
                    )
                    it.putBoolean(ARG_HAS_PASSWORD, true)
                    it.putString(ARG_TLS, "explicit")
                    it.putBoolean(ARG_EDIT, true)
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertTrue(binding.chkFtpExplicitTls.isChecked)
                    assertFalse(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("root", binding.usernameET.text.toString())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                }
            },
        )
    }

    /**
     * Test scenario with FTP, username and password + explicit TLS option
     */
    @Test
    fun testFtpWithUsernamePasswordAndExplicitTlsPlusDefaultPath() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putString(ARG_USERNAME, "root")
                    it.putString(
                        ARG_PASSWORD,
                        PasswordUtil.encryptPassword(
                            AppConfig.getInstance(),
                            "abcdefgh",
                        ),
                    )
                    it.putString(ARG_DEFAULT_PATH, "/root/Documents")
                    it.putBoolean(ARG_HAS_PASSWORD, true)
                    it.putString(ARG_TLS, "explicit")
                    it.putBoolean(ARG_EDIT, true)
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertTrue(binding.chkFtpExplicitTls.isChecked)
                    assertFalse(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("root", binding.usernameET.text.toString())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                    assertEquals("/root/Documents", binding.defaultPathET.text.toString())
                }
            },
        )
    }

    /**
     * Test scenario with FTP, anonymous and explicit TLS option
     */
    @Test
    fun testFtpWithAnonymousWithExplicitTls() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putBoolean(ARG_HAS_PASSWORD, false)
                    it.putBoolean(ARG_EDIT, true)
                    it.putString(ARG_TLS, "explicit")
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertTrue(binding.chkFtpExplicitTls.isChecked)
                    assertTrue(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                }
            },
        )
    }

    /**
     * Test scenario with FTP, anonymous and explicit TLS option
     */
    @Test
    fun testFtpWithAnonymousWithExplicitTlsAndDefaultPath() {
        doTestWithDialog(
            arguments =
                Bundle().also {
                    it.putString(ARG_PROTOCOL, FTP_URI_PREFIX)
                    it.putString(ARG_NAME, "FTP Connection")
                    it.putString(ARG_ADDRESS, "127.0.0.1")
                    it.putInt(ARG_PORT, 2121)
                    it.putBoolean(ARG_HAS_PASSWORD, false)
                    it.putBoolean(ARG_EDIT, true)
                    it.putString(ARG_TLS, "explicit")
                    it.putString(ARG_DEFAULT_PATH, "/Incoming")
                },
            withDialog = { sftpConnectDialog, materialDialog ->
                assertNotNull(sftpConnectDialog.binding)
                requireNotNull(sftpConnectDialog.binding).let { binding ->
                    assertTrue(binding.chkFtpExplicitTls.isChecked)
                    assertTrue(binding.chkFtpAnonymous.isChecked)
                    assertEquals(2121, binding.portET.text.toString().toInt())
                    assertEquals("FTP Connection", binding.connectionET.text.toString())
                    assertEquals(1, binding.protocolDropDown.selectedItemPosition)
                    assertEquals("", binding.passwordET.text.toString())
                    assertEquals("/Incoming", binding.defaultPathET.text.toString())
                }
            },
        )
    }
}
