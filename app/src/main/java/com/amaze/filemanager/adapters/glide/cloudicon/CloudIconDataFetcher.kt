/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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
import android.graphics.BitmapFactory
import android.util.Log
import com.amaze.filemanager.filesystem.cloud.CloudUtil
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

class CloudIconDataFetcher(
    private val context: Context,
    private val path: String,
    private val width: Int,
    private val height: Int,
) : DataFetcher<Bitmap> {
    companion object {
        private val TAG = CloudIconDataFetcher::class.java.simpleName

        /**
         * Calculate an [BitmapFactory.Options.inSampleSize] value that keeps the
         * decoded bitmap larger than the requested [reqWidth]×[reqHeight] while
         * still reducing memory usage significantly for oversized sources.
         */
        @JvmStatic
        internal fun calculateInSampleSize(
            outWidth: Int,
            outHeight: Int,
            reqWidth: Int,
            reqHeight: Int,
        ): Int {
            var inSampleSize = 1
            if (outHeight > reqHeight || outWidth > reqWidth) {
                val halfHeight = outHeight / 2
                val halfWidth = outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight &&
                    halfWidth / inSampleSize >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }

    private var inputStream: InputStream? = null
    private val cancelled = AtomicBoolean(false)

    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in Bitmap?>,
    ) {
        try {
            inputStream = CloudUtil.getThumbnailInputStreamForCloud(context, path)
            if (inputStream == null || cancelled.get()) {
                callback.onDataReady(null)
                return
            }

            // Buffer the full stream so we can do a two-pass decode.
            // Pass 1 reads only the dimensions; pass 2 decodes with inSampleSize.
            val bytes = inputStream!!.readBytes()
            if (cancelled.get()) {
                callback.onDataReady(null)
                return
            }

            // --- Pass 1: decode bounds only ---
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            // --- Pass 2: decode with appropriate down-sampling ---
            val decodeOptions =
                BitmapFactory.Options().apply {
                    inSampleSize =
                        calculateInSampleSize(
                            boundsOptions.outWidth,
                            boundsOptions.outHeight,
                            width,
                            height,
                        )
                }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            callback.onDataReady(bitmap)
        } catch (e: Exception) {
            if (cancelled.get()) {
                callback.onDataReady(null)
            } else {
                Log.e(TAG, "Error loading cloud icon for $path", e)
                callback.onLoadFailed(e)
            }
        }
    }

    override fun cleanup() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error cleaning up cloud icon fetch", e)
        }
    }

    override fun cancel() {
        cancelled.set(true)
        // Close the stream to interrupt any in-progress network read so the
        // background thread doesn't keep downloading a file whose result will
        // never be used.
        try {
            inputStream?.close()
        } catch (_: IOException) {
            // Best-effort; the stream may already be closed.
        }
    }

    override fun getDataClass(): Class<Bitmap> = Bitmap::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}
