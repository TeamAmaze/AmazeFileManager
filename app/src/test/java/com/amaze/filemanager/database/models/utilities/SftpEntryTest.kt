package com.amaze.filemanager.database.models.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test for [SftpEntry] model.
 */
@Suppress("StringLiteralDuplication")
class SftpEntryTest {

    /**
     * Equality test. equals() check for name, path, host SSH key and private key combinations.
     */
    @Test
    fun testEquals() {
        val a = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH key",
            "abcdefghijkl"
        )
        val b = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH key",
            "abcdefghijkl"
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val c = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH key",
            "abcdefghijkl"
        )
        val d = SftpEntry(
            "ssh://root@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            "SSH test auth key",
            "abcdefghijkl"
        )
        assertEquals(c, d)
        assertEquals(c.hashCode(), d.hashCode())

        val e = SftpEntry(
            "ssh://root:toor@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            null,
            null
        )
        val f = SftpEntry(
            "ssh://root:toor@127.0.0.1:22222",
            "SSH connection 1",
            "ab:cd:ef:gh:ij:kl:00",
            null,
            null
        )
        assertEquals(e, f)
        assertEquals(e.hashCode(), f.hashCode())
    }
}
