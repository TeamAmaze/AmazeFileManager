package com.amaze.filemanager.database.models.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Test [SmbEntry] model.
 */
@Suppress("StringLiteralDuplication")
class SmbEntryTest {

    /**
     * Test [SmbEntry.equals] for equality.
     */
    @Test
    fun testEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [SmbEntry.equals] for inequality by name.
     */
    @Test
    fun testNameNotEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = SmbEntry("SMB connection 2", "smb://root:user@127.0.0.1/root")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [SmbEntry.equals] for inequality by path.
     */
    @Test
    fun testPathNotEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/toor")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [SmbEntry.equals] for inequality with other class.
     */
    @Test
    fun testForeignClassNotEquals() {
        val a = SmbEntry("SMB connection 1", "smb://root:user@127.0.0.1/root")
        val b = Bookmark("SMB connection 1", "smb://root:user@127.0.0.1/root")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }
}