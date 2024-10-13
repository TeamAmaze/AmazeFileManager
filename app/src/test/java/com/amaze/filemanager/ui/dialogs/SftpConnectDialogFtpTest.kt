package com.amaze.filemanager.ui.dialogs

import android.os.Bundle
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl.Companion.ARG_TLS
import com.amaze.filemanager.filesystem.ftp.FTPClientImpl.Companion.TLS_EXPLICIT
import com.amaze.filemanager.filesystem.ftp.NetCopyClientUtils
import com.amaze.filemanager.ui.activities.MainActivity
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_PASSWORD
import com.amaze.filemanager.ui.dialogs.SftpConnectDialog.Companion.ARG_USERNAME
import com.amaze.filemanager.utils.PasswordUtil
import org.awaitility.Awaitility.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doCallRealMethod
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.TimeUnit

/**
 * Test [SftpConnectDialog] with FTP(S) connections.
 */
@Suppress("StringLiteralDuplication")
class SftpConnectDialogFtpTest : AbstractSftpConnectDialogTests() {
    /**
     * Test invoke [SftpConnectDialog] with arguments username and password.
     */
    @Test
    fun testInvokeSftpConnectionDialog() {
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 2121)
        verify.putString("name", "FTP Connection")
        verify.putString("username", "root")
        verify.putString("password", "abcdefgh")
        verify.putBoolean("hasPassword", true)
        verify.putBoolean("edit", true)

        testOpenSftpConnectDialog("ftp://root:abcdefgh@127.0.0.1:2121", verify)
    }

    /**
     * Test invoke [SftpConnectDialog] with arguments username and password and explicit TLS option
     */
    @Test
    fun testInvokeSftpConnectionDialogWithExplicitTlsFlagEnabled() {
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 2121)
        verify.putString("name", "FTP Connection")
        verify.putString("username", "root")
        verify.putString("password", "abcdefgh")
        verify.putBoolean("hasPassword", true)
        verify.putString("tls", "explicit")
        verify.putBoolean("edit", true)

        testOpenSftpConnectDialog(
            "ftp://root:abcdefgh@127.0.0.1:2121?tls=explicit",
            verify,
            true,
        )
    }

    /**
     * Test invoke [SftpConnectDialog] without any arguments
     */
    @Test
    fun testInvokeSftpConnectionDialogWithoutUsernamePassword() {
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 2121)
        verify.putString("name", "FTP Connection")
        verify.putBoolean("hasPassword", true)
        verify.putBoolean("edit", true)

        testOpenSftpConnectDialog(
            "ftp://127.0.0.1:2121",
            verify,
            false,
            true,
        )
    }

    /**
     * Test invoke [SftpConnectDialog] without username/password but with explicit TLS option
     */
    @Test
    fun testInvokeSftpConnectionDialogWithoutUsernamePasswordAndExplicitTls() {
        val verify = Bundle()
        verify.putString("address", "127.0.0.1")
        verify.putInt("port", 2121)
        verify.putString("name", "FTP Connection")
        verify.putBoolean("hasPassword", true)
        verify.putString("tls", "explicit")
        verify.putBoolean("edit", true)

        testOpenSftpConnectDialog(
            "ftp://127.0.0.1:2121?tls=explicit",
            verify,
            true,
            true,
        )
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun testOpenSftpConnectDialog(
        uri: String,
        verify: Bundle,
        explicitTls: Boolean = false,
        anonymous: Boolean = false,
    ): SftpConnectDialog {
        val activity = mock(MainActivity::class.java)
        doCallRealMethod().`when`(activity).showSftpDialog(
            any(),
            any(),
            anyBoolean(),
        )
        activity.showSftpDialog(
            "FTP Connection",
            NetCopyClientUtils.encryptFtpPathAsNecessary(uri),
            true,
        )
        assertEquals(1, mc.constructed().size)
        val mocked = mc.constructed()[0]
        await().atMost(10, TimeUnit.SECONDS).until { mocked.arguments != null }
        mocked.arguments?.let { args ->
            if (explicitTls) {
                assertTrue(args.containsKey(ARG_TLS))
                assertEquals(TLS_EXPLICIT, args.getString(ARG_TLS))
            }
            val keys = BUNDLE_KEYS.clone().toMutableList()
            if (anonymous) {
                keys.remove(ARG_USERNAME)
                keys.remove(ARG_PASSWORD)
            }
            for (key in keys) {
                if (args.getString(key) != null) {
                    if (key == ARG_PASSWORD) {
                        assertEquals(
                            verify.getString(key),
                            PasswordUtil.decryptPassword(
                                ApplicationProvider.getApplicationContext(),
                                args.getString(key)!!,
                                Base64.URL_SAFE,
                            ),
                        )
                    } else {
                        assertEquals(verify.getString(key), args.getString(key))
                    }
                }
            }
        }
        return mocked
    }
}
