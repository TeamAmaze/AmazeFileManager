package com.amaze.filemanager.asynchronous.services

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.asynchronous.services.ftp.FtpService
import com.amaze.filemanager.shadows.ShadowMultiDex
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Test [FtpService.enabledCipherSuites].
 *
 * This test is deliberately set to run on all available SDKs to ensure no one is missing out.
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowMultiDex::class])
class FtpServiceSupportedCiphersTest {

    /**
     * Check for enabled ciphers, to ensure no unsupported ciphers on the list.
     *
     * @see FtpService.enabledCipherSuites
     * @see javax.net.ssl.SSLEngine
     */
    @Test
    fun testSupportedCiphers() {
        val verify = FtpService.enabledCipherSuites
        assertNotNull(verify)
        assertTrue(verify.isNotEmpty())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertTrue(verify.contains("TLS_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_AES_256_GCM_SHA384"))
            assertTrue(verify.contains("TLS_CHACHA20_POLY1305_SHA256"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256"))
            assertTrue(verify.contains("TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256"))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"))
            assertTrue(verify.contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"))
            assertTrue(verify.contains("TLS_RSA_WITH_AES_128_GCM_SHA256"))
            assertTrue(verify.contains("TLS_RSA_WITH_AES_256_GCM_SHA384"))
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            assertTrue(verify.contains("TLS_RSA_WITH_AES_128_CBC_SHA"))
            assertTrue(verify.contains("TLS_RSA_WITH_AES_256_CBC_SHA"))
        }
    }
}
