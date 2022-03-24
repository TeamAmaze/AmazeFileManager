package com.amaze.filemanager.utils.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test [SecretKeygen] runs on real device. Necessary since Robolectric doesn't have shadows for
 * AndroidKeyStore.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class SecretKeygenEspressoTest {

    /**
     * Test [SecretKeygen.getSecretKey].
     */
    @Test
    fun testGetSecretKey() {
        SecretKeygen.getSecretKey()?.run {
            assertNotNull(this)
            assertEquals("aes", this.algorithm.lowercase())
        } ?: fail("Unable to obtain secret key")
    }
}