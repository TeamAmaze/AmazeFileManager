package com.amaze.filemanager.database.models.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Test [Grid] model.
 */
@Suppress("StringLiteralDuplication")
class GridTest {

    /**
     * Test [Grid.equals] for equality.
     */
    @Test
    fun testEquals() {
        val a = Grid("/storage/emulated/0")
        val b = Grid("/storage/emulated/0")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [Grid.equals] for inequality.
     */
    @Test
    fun testNotEquals() {
        val a = Grid("/storage/emulated/0")
        val b = Grid("/storage/emulated/1")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    /**
     * Test [Grid.equals] for inequality with other class.
     */
    @Test
    fun testForeignClassNotEquals() {
        val a = Grid("/storage/emulated/0")
        val b = List("/storage/emulated/0")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }
}
