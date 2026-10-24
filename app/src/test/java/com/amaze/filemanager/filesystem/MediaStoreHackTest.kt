/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.filesystem.MediaStoreHackTest.Companion.FAKE_ROW_ID
import com.amaze.filemanager.shadows.ShadowMultiDex
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [MediaStoreHack.getUriForMusicMediaFrom].
 *
 * The new method queries [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] by file-system path and
 * returns a content:// [android.net.Uri] when a matching row exists, or `null` when it does not.
 *
 * Robolectric is needed so that Android framework statics (e.g.
 * [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI], [android.net.Uri]) are properly initialised.
 * The [ContentResolver] itself is mocked via MockK so tests are not affected by Robolectric 4.9's
 * limited in-process MediaStore ContentProvider support.
 */
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [M, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class],
)
class MediaStoreHackTest {
    companion object {
        private const val TEST_AUDIO_PATH = "/storage/emulated/0/Music/test_ringtone.mp3"
        private const val ABSENT_AUDIO_PATH = "/storage/emulated/0/Music/nonexistent.mp3"
        private const val FAKE_ROW_ID = 42
    }

    /**
     * Builds a mock [Context] whose [ContentResolver] returns [cursor] for any query
     * against [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI].
     */
    private fun contextWithCursor(cursor: Cursor?): Context {
        val mockResolver = mockk<ContentResolver>()
        every {
            mockResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                any(),
                any(),
                any(),
                null,
            )
        } returns cursor

        return mockk<Context>().also { ctx ->
            every { ctx.contentResolver } returns mockResolver
        }
    }

    /**
     * Builds a mock [Cursor] that simulates a single-row result with [_ID][MediaStore.Audio.Media._ID]
     * equal to [FAKE_ROW_ID].
     */
    private fun singleRowCursor(): Cursor {
        val idColumnIndex = 0
        return mockk<Cursor>(relaxed = true).also { cursor ->
            every { cursor.moveToFirst() } returns true
            every { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns idColumnIndex
            every { cursor.getInt(idColumnIndex) } returns FAKE_ROW_ID
            every { cursor.close() } just runs
        }
    }

    /**
     * Builds a mock [Cursor] that simulates an empty result set (no matching rows).
     */
    private fun emptyCursor(): Cursor =
        mockk<Cursor>(relaxed = true).also { cursor ->
            every { cursor.moveToFirst() } returns false
            every { cursor.close() } just runs
        }

    /**
     * After test clean up.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * When the ContentResolver returns a cursor with a matching row,
     * [MediaStoreHack.getUriForMusicMediaFrom] must return a non-null `content://` URI under
     * [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] whose last path segment is the row id.
     */
    @Test
    fun testGetUriForMusicMediaFromReturnsUriWhenCursorHasMatchingRow() {
        val context = contextWithCursor(singleRowCursor())

        val result = MediaStoreHack.getUriForMusicMediaFrom(TEST_AUDIO_PATH, context)

        assertNotNull("Expected a non-null URI when the cursor has a matching row", result)
        assertEquals("content", result!!.scheme)
        assertTrue(
            "Returned URI should be under MediaStore.Audio.Media.EXTERNAL_CONTENT_URI",
            result.toString().startsWith(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),
            ),
        )
        assertEquals(
            "Last path segment must equal the row id returned by the cursor",
            FAKE_ROW_ID.toString(),
            result.lastPathSegment,
        )
    }

    /**
     * When the ContentResolver returns a cursor with NO matching rows,
     * [MediaStoreHack.getUriForMusicMediaFrom] must return `null`.
     */
    @Test
    fun testGetUriForMusicMediaFromReturnsNullWhenCursorIsEmpty() {
        val context = contextWithCursor(emptyCursor())

        val result = MediaStoreHack.getUriForMusicMediaFrom(ABSENT_AUDIO_PATH, context)

        assertNull(
            "Expected null URI when the cursor is empty (no matching row)",
            result,
        )
    }

    /**
     * When the ContentResolver returns a `null` cursor (provider error or unavailable),
     * [MediaStoreHack.getUriForMusicMediaFrom] must return `null` without throwing.
     */
    @Test
    fun testGetUriForMusicMediaFromReturnsNullWhenCursorIsNull() {
        val context = contextWithCursor(null)

        val result = MediaStoreHack.getUriForMusicMediaFrom(TEST_AUDIO_PATH, context)

        assertNull(
            "Expected null URI when the ContentResolver returns a null cursor",
            result,
        )
    }

    /**
     * The URI returned by [MediaStoreHack.getUriForMusicMediaFrom] must use only
     * the row [_ID][MediaStore.Audio.Media._ID] from the cursor — confirming that
     * different row ids produce distinct URIs (selection correctness guard).
     */
    @Test
    fun testGetUriForMusicMediaFromBuildsUriFromCursorId() {
        val alternativeId = 99
        val altCursor =
            mockk<Cursor>(relaxed = true).also { cursor ->
                every { cursor.moveToFirst() } returns true
                every { cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns 0
                every { cursor.getInt(0) } returns alternativeId
                every { cursor.close() } just runs
            }
        val context = contextWithCursor(altCursor)

        val result = MediaStoreHack.getUriForMusicMediaFrom(TEST_AUDIO_PATH, context)

        assertNotNull(result)
        assertEquals(
            "URI last path segment must equal the alternative row id",
            alternativeId.toString(),
            result!!.lastPathSegment,
        )
        // Verify it is distinct from a URI built with FAKE_ROW_ID
        val firstResult =
            MediaStoreHack.getUriForMusicMediaFrom(
                TEST_AUDIO_PATH,
                contextWithCursor(singleRowCursor()),
            )
        assertTrue(
            "URIs built from different row ids must differ",
            result.toString() != firstResult.toString(),
        )
    }
}
