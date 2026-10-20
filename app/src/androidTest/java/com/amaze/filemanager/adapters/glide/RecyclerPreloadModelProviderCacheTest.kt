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

package com.amaze.filemanager.adapters.glide

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.system.Os
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.amaze.filemanager.R
import com.amaze.filemanager.adapters.data.IconDataParcelable
import com.amaze.filemanager.ui.activities.MainActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.CRC32

/**
 * Exercise the actual local thumbnail provider, Glide cache and Android bitmap decoder.
 */
@RunWith(AndroidJUnit4::class)
class RecyclerPreloadModelProviderCacheTest {
    /** Allow the ordinary file-manager activity to start on the supported test devices. */
    @get:Rule
    val storagePermission =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activity: MainActivity
    private lateinit var directory: File

    /**
     * Use a unique app-owned directory without clearing Glide's cache or other application data.
     */
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.cacheDir, "thumbnail-4269-${UUID.randomUUID()}")
        assertTrue(directory.mkdirs())
        scenario =
            ActivityScenario.launch(
                Intent(context, MainActivity::class.java).putExtra("path", directory.absolutePath),
            )
        scenario.onActivity { activity = it }
    }

    /**
     * Close only the test activity and remove this test's synthetic input files.
     */
    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
        if (::directory.isInitialized) directory.deleteRecursively()
    }

    /**
     * A grid thumbnail must use the replacement inode even with identical size and timestamp.
     */
    @Test
    fun testGridCacheRefreshesRenamedImageWithSameSizeAndTime() {
        verifyRenamedImage(isCircled = false)
    }

    /**
     * The list's circular thumbnail must also use the replacement image's actual pixels.
     */
    @Test
    fun testCircularCacheRefreshesRenamedImageWithSameSizeAndTime() {
        verifyRenamedImage(isCircled = true)
    }

    /**
     * Each local request must keep its own model and options while unchanged images remain cached.
     */
    @Test
    fun testLocalBuildersAreIndependentAndUnchangedImageStaysCached() {
        val images = createImages()
        val provider = createProvider(isCircled = false)
        val firstRequest = requestFor(provider, images.first)
        val secondRequest = requestFor(provider, images.second)
        assertNotSame(firstRequest, secondRequest)
        assertEquals(Color.RED, decode(firstRequest).color)
        assertEquals(Color.BLUE, decode(secondRequest).color)
        assertCachedRed(provider, images.first)
    }

    /**
     * Modifying a file in place must refresh its thumbnail while keeping its filesystem identity.
     */
    @Test
    fun testInPlaceModificationRefreshesCachedThumbnail() {
        val images = createImages()
        val provider = createProvider(isCircled = false)
        assertEquals(Color.RED, decode(requestFor(provider, images.first)).color)
        val before = Os.stat(images.first.absolutePath)
        images.first.writeBytes(images.second.readBytes())
        assertTrue(images.first.setLastModified(FIXTURE_TIME + 5000))
        val after = Os.stat(images.first.absolutePath)
        assertEquals(before.st_dev, after.st_dev)
        assertEquals(before.st_ino, after.st_ino)
        assertEquals(before.st_size, after.st_size)
        assertTrue(before.st_mtime != after.st_mtime)
        assertEquals(Color.BLUE, decode(requestFor(provider, images.first)).color)
    }

    /**
     * A missing absolute local file must retain Glide's normal load failure rather than stat crashing.
     */
    @Test
    fun testMissingLocalFileRetainsGlideFailure() {
        val provider = createProvider(isCircled = false)
        val missing = File(directory, "missing.png")
        assertFalse(missing.exists())
        val future = requestFor(provider, missing).submit(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
        try {
            future.get(15, TimeUnit.SECONDS)
            fail("A missing image must not decode successfully")
        } catch (failure: ExecutionException) {
            assertTrue(failure.cause is GlideException)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                Glide.with(activity).clear(future)
            }
        }
        assertFalse(missing.exists())
    }

    /**
     * Reproduce delete-and-rename with two real files sharing size and mtime but not inode identity.
     */
    private fun verifyRenamedImage(isCircled: Boolean) {
        val images = createImages()
        val provider = createProvider(isCircled)
        val before = Os.stat(images.first.absolutePath)
        val incoming = Os.stat(images.second.absolutePath)
        val originalModifiedMillis = images.first.lastModified()
        assertEquals(before.st_dev, incoming.st_dev)
        assertTrue(before.st_ino != incoming.st_ino)
        assertEquals(before.st_size, incoming.st_size)
        assertEquals(before.st_mtime, incoming.st_mtime)
        assertEquals(images.first.lastModified(), images.second.lastModified())
        assertEquals(Color.RED, decode(requestFor(provider, images.first)).color)
        assertEquals(Color.BLUE, decode(requestFor(provider, images.second)).color)
        assertCachedRed(provider, images.first)

        assertTrue(images.first.delete())
        assertTrue(images.second.renameTo(images.first))
        val replacement = Os.stat(images.first.absolutePath)
        assertEquals(incoming.st_dev, replacement.st_dev)
        assertEquals(incoming.st_ino, replacement.st_ino)
        assertEquals(before.st_size, replacement.st_size)
        assertEquals(before.st_mtime, replacement.st_mtime)
        assertEquals(originalModifiedMillis, images.first.lastModified())
        val actual = decode(requestFor(provider, images.first))
        Log.i(
            LOG_TAG,
            "renamed crop=$isCircled oldInode=${before.st_ino} newInode=${replacement.st_ino}" +
                " size=${replacement.st_size} mtime=${replacement.st_mtime}" +
                " decoded=${actual.color} source=${actual.source}",
        )
        assertEquals("Renamed image must show its blue bytes, not the deleted red image", Color.BLUE, actual.color)
    }

    /**
     * Confirm cache reuse for an unchanged image instead of accepting a blanket cache-disable fix.
     */
    private fun assertCachedRed(
        provider: RecyclerPreloadModelProvider,
        file: File,
    ) {
        val repeated = decode(requestFor(provider, file))
        assertEquals(Color.RED, repeated.color)
        assertTrue(
            "The unchanged thumbnail must come from a real Glide cache: ${repeated.source}",
            repeated.source in
                setOf(DataSource.MEMORY_CACHE, DataSource.RESOURCE_DISK_CACHE, DataSource.DATA_DISK_CACHE),
        )
        Log.i(LOG_TAG, "unchanged image source=${repeated.source} color=${repeated.color}")
    }

    /**
     * Construct the production provider with an actual activity-attached fragment and crop choice.
     */
    private fun createProvider(isCircled: Boolean): RecyclerPreloadModelProvider {
        lateinit var provider: RecyclerPreloadModelProvider
        scenario.onActivity {
            provider =
                RecyclerPreloadModelProvider(
                    requireNotNull(it.currentMainFragment),
                    emptyList(),
                    isCircled,
                )
        }
        return provider
    }

    /**
     * Request the actual local-file model on the UI thread as the adapter does.
     */
    private fun requestFor(
        provider: RecyclerPreloadModelProvider,
        file: File,
    ): RequestBuilder<Drawable> {
        lateinit var request: RequestBuilder<Drawable>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            request =
                requireNotNull(
                    provider.getPreloadRequestBuilder(
                        IconDataParcelable(
                            IconDataParcelable.IMAGE_FROMFILE,
                            file.absolutePath,
                            R.drawable.ic_doc_image,
                        ),
                    ),
                )
        }
        return request
    }

    /**
     * Decode through real Glide, record its data source and read native bitmap pixels before release.
     */
    private fun decode(request: RequestBuilder<Drawable>): DecodedThumbnail {
        val source = AtomicReference<DataSource>()
        val future =
            request.listener(
                object : RequestListener<Drawable> {
                    /** Let Glide report an actual decode failure through its FutureTarget. */
                    override fun onLoadFailed(
                        error: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean = false

                    /** Record the actual cache or local source without replacing the decoded image. */
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        source.set(dataSource)
                        return false
                    }
                },
            ).submit(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
        try {
            val drawable = future.get(15, TimeUnit.SECONDS)
            assertTrue("Glide must return a real decoded bitmap", drawable is BitmapDrawable)
            val bitmap = (drawable as BitmapDrawable).bitmap
            return DecodedThumbnail(
                bitmap.getPixel(bitmap.width / 2, bitmap.height / 2),
                requireNotNull(source.get()),
            )
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                Glide.with(activity).clear(future)
            }
        }
    }

    /**
     * Generate valid red and blue PNGs, equalizing their lengths with ordinary PNG text metadata.
     */
    private fun createImages(): ImagePair {
        val first = File(directory, "image-one.png")
        val second = File(directory, "image-two.png")
        writePng(first, Color.RED)
        writePng(second, Color.BLUE)
        val length = maxOf(first.length(), second.length()).toInt() + 20
        addPngText(first, length)
        addPngText(second, length)
        assertTrue(first.setLastModified(FIXTURE_TIME))
        assertTrue(second.setLastModified(FIXTURE_TIME))
        assertEquals(first.length(), second.length())
        assertEquals(first.lastModified(), second.lastModified())
        return ImagePair(first, second)
    }

    /**
     * Write an actual Android-encoded opaque PNG with a known center color.
     */
    private fun writePng(
        file: File,
        color: Int,
    ) {
        val bitmap = Bitmap.createBitmap(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(color)
            file.outputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Insert a valid CRC-protected tEXt chunk before IEND so both ordinary PNG files have equal size.
     */
    private fun addPngText(
        file: File,
        totalLength: Int,
    ) {
        val original = file.readBytes()
        assertEquals("IEND", String(original, original.size - 8, 4, Charsets.US_ASCII))
        val payload = ByteArray(totalLength - original.size - 12) { 'x'.code.toByte() }
        "fixture".toByteArray(Charsets.US_ASCII).copyInto(payload)
        payload[7] = 0
        val type = "tEXt".toByteArray(Charsets.US_ASCII)
        val crc =
            CRC32().apply {
                update(type)
                update(payload)
            }
        DataOutputStream(file.outputStream()).use { output ->
            output.write(original, 0, original.size - 12)
            output.writeInt(payload.size)
            output.write(type)
            output.write(payload)
            output.writeInt(crc.value.toInt())
            output.write(original, original.size - 12, 12)
        }
        assertEquals(totalLength.toLong(), file.length())
    }

    /** Input files remain distinct until the test performs the owner's delete-and-rename steps. */
    private data class ImagePair(val first: File, val second: File)

    /** A pixel and observed source are copied before releasing Glide's pooled bitmap resource. */
    private data class DecodedThumbnail(val color: Int, val source: DataSource)

    companion object {
        private const val FIXTURE_TIME = 1_700_000_000_000L
        private const val THUMBNAIL_SIZE = 64
        private const val LOG_TAG = "Amaze4269CacheTest"
    }
}
