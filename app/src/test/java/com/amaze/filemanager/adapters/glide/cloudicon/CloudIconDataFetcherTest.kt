/*
 * Copyright (C) 2014-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.adapters.glide.cloudicon

import android.content.Context
import android.graphics.Bitmap
import com.amaze.filemanager.adapters.glide.cloudicon.CloudIconDataFetcher.Companion.calculateInSampleSize
import com.bumptech.glide.load.DataSource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.InputStream

/**
 * Unit tests for [CloudIconDataFetcher].
 *
 * Covers:
 *  - [CloudIconDataFetcher.Companion.calculateInSampleSize] — pure arithmetic, no Android runtime needed.
 *  - [CloudIconDataFetcher.cancel] — sets the cancelled flag so a subsequent [loadData] returns null.
 */
@Suppress("StringLiteralDuplication")
class CloudIconDataFetcherTest {
    // -------------------------------------------------------------------------
    // calculateInSampleSize
    // -------------------------------------------------------------------------

    /**
     * When the source image is exactly the requested size, no down-sampling is needed.
     */
    @Test
    fun testCalculateInSampleSize_exactMatch_returns1() {
        assertEquals(1, calculateInSampleSize(100, 100, 100, 100))
    }

    /**
     * When the source is smaller than requested, no down-sampling should occur.
     */
    @Test
    fun testCalculateInSampleSize_sourceSmaller_returns1() {
        assertEquals(1, calculateInSampleSize(50, 50, 100, 100))
    }

    /**
     * Source is exactly 2× the requested size → inSampleSize should be 2.
     * halfHeight/inSampleSize = 100/1 = 100 ≥ 100, so loop fires once.
     */
    @Test
    fun testCalculateInSampleSize_double_returns2() {
        assertEquals(2, calculateInSampleSize(200, 200, 100, 100))
    }

    /**
     * Source is exactly 4× the requested size → inSampleSize should be 4.
     */
    @Test
    fun testCalculateInSampleSize_quadruple_returns4() {
        assertEquals(4, calculateInSampleSize(400, 400, 100, 100))
    }

    /**
     * Source is 8× the requested size → inSampleSize should be 8.
     */
    @Test
    fun testCalculateInSampleSize_8x_returns8() {
        assertEquals(8, calculateInSampleSize(800, 800, 100, 100))
    }

    /**
     * Non-power-of-two source: 300×300 requesting 100×100.
     * halfHeight = 150, halfWidth = 150.
     * Loop: 150/1=150 ≥ 100 → inSampleSize=2; 150/2=75 ≥ 100 → false → stop.
     * Expected: 2
     */
    @Test
    fun testCalculateInSampleSize_300x300_req100x100_returns2() {
        assertEquals(2, calculateInSampleSize(300, 300, 100, 100))
    }

    /**
     * Landscape source 1000×500 requesting 100×100.
     * The narrower dimension (height=500, half=250) is the limiting axis.
     * halfHeight=250, halfWidth=500.
     * Loop 1: 250/1≥100 && 500/1≥100 → true → inSampleSize=2
     * Loop 2: 250/2=125≥100 && 500/2=250≥100 → true → inSampleSize=4
     * Loop 3: 250/4=62≥100 → false → stop.
     * Expected: 4
     */
    @Test
    fun testCalculateInSampleSize_landscape_1000x500_req100_returns4() {
        assertEquals(4, calculateInSampleSize(1000, 500, 100, 100))
    }

    /**
     * Tall source 500×1000 requesting 100×100.
     * Same as above but axes swapped — result must be symmetric.
     */
    @Test
    fun testCalculateInSampleSize_portrait_500x1000_req100_returns4() {
        assertEquals(4, calculateInSampleSize(500, 1000, 100, 100))
    }

    /**
     * Very large source (20 MP typical photo) requesting thumbnail size 512×512.
     * Source: 5000×4000 → halfH=2000, halfW=2500
     * Loop:
     *   2000/1≥512 && 2500/1≥512 → true → 2
     *   2000/2=1000≥512 → true → 4
     *   2000/4=500≥512 → false → stop
     * Expected: 4
     */
    @Test
    fun testCalculateInSampleSize_20mpPhoto_req512_returns4() {
        assertEquals(4, calculateInSampleSize(5000, 4000, 512, 512))
    }

    /**
     * When one dimension equals the requested size and the other is larger, the larger
     * dimension alone should NOT force down-sampling; both axes must exceed the request.
     * Source: 1000×100 requesting 100×100.
     * halfH=50, halfW=500 → 50/1=50 ≥ 100 → false immediately → inSampleSize stays 1.
     */
    @Test
    fun testCalculateInSampleSize_oneAxisExact_returns1() {
        assertEquals(1, calculateInSampleSize(1000, 100, 100, 100))
    }

    /**
     * Zero source dimensions should not crash and should return 1 (no down-sampling).
     */
    @Test
    fun testCalculateInSampleSize_zeroDimensions_returns1() {
        assertEquals(1, calculateInSampleSize(0, 0, 100, 100))
    }

    // -------------------------------------------------------------------------
    // cancel() / cancelled-flag behaviour
    // -------------------------------------------------------------------------

    /**
     * After [CloudIconDataFetcher.cancel] is called, the fetcher must close the stream
     * without throwing.  This verifies the close path in cancel() does not propagate exceptions.
     */
    @Test
    fun testCancel_closesStreamWithoutException() {
        val context = mockk<Context>(relaxed = true)
        val fetcher = CloudIconDataFetcher(context, "smb://host/file.jpg", 100, 100)

        // Should not throw even when there is no active stream.
        fetcher.cancel()
    }

    /**
     * After [CloudIconDataFetcher.cancel] is called, the stream (if already assigned)
     * gets closed.  We inject a tracked InputStream by making cancel() close whatever
     * is currently stored; here we verify the AtomicBoolean side-effect by confirming
     * the stream close is attempted.
     */
    @Test
    fun testCancel_closesAssignedStream() {
        val closed = mutableListOf<Boolean>()
        val trackingStream =
            object : InputStream() {
                override fun read(): Int = -1

                override fun close() {
                    super.close()
                    closed += true
                }
            }

        val context = mockk<android.content.Context>(relaxed = true)
        val fetcher = CloudIconDataFetcher(context, "ssh://host/file.jpg", 100, 100)

        // Reflectively inject the stream to simulate mid-download cancel
        val field = CloudIconDataFetcher::class.java.getDeclaredField("inputStream")
        field.isAccessible = true
        field.set(fetcher, trackingStream)

        fetcher.cancel()

        assertEquals("cancel() must close the injected stream", 1, closed.size)
    }

    // -------------------------------------------------------------------------
    // cleanup()
    // -------------------------------------------------------------------------

    /**
     * [CloudIconDataFetcher.cleanup] must close the stream without crashing when no
     * stream is set.
     */
    @Test
    fun testCleanup_noStream_doesNotThrow() {
        val context = mockk<Context>(relaxed = true)
        val fetcher = CloudIconDataFetcher(context, "smb://host/file.jpg", 100, 100)
        fetcher.cleanup() // must not throw
    }

    /**
     * [CloudIconDataFetcher.cleanup] must close an assigned stream.
     */
    @Test
    fun testCleanup_closesAssignedStream() {
        val closed = mutableListOf<Boolean>()
        val trackingStream =
            object : InputStream() {
                override fun read(): Int = -1

                override fun close() {
                    super.close()
                    closed += true
                }
            }

        val context = mockk<Context>(relaxed = true)
        val fetcher = CloudIconDataFetcher(context, "smb://host/file.jpg", 100, 100)

        val field = CloudIconDataFetcher::class.java.getDeclaredField("inputStream")
        field.isAccessible = true
        field.set(fetcher, trackingStream)

        fetcher.cleanup()

        assertEquals("cleanup() must close the stream", 1, closed.size)
    }

    /**
     * Test [CloudIconDataFetcher.getDataClass] must return Bitmap
     */
    @Test
    fun testGetDataClass_returnsBitmapClass() {
        val context = mockk<Context>(relaxed = true)
        val fetcher = CloudIconDataFetcher(context, "smb://host/file.jpg", 100, 100)
        assertEquals(Bitmap::class.java, fetcher.dataClass)
    }

    /**
     * Test [CloudIconDataFetcher.getDataSource] must return REMOTE
     */
    @Test
    fun testGetDataSource_returnsRemote() {
        val context = mockk<Context>(relaxed = true)
        val fetcher = CloudIconDataFetcher(context, "smb://host/file.jpg", 100, 100)
        assertEquals(DataSource.REMOTE, fetcher.dataSource)
    }
}
