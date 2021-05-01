/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.compressed.extractcontents.helpers

import android.content.Context
import com.amaze.filemanager.file_operations.utils.UpdatePosition
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorInputStream
import java.io.InputStream
import java.lang.reflect.Constructor

abstract class AbstractCompressedTarArchiveExtractor(
    context: Context,
    filePath: String,
    outputPath: String,
    listener: OnUpdate,
    updatePosition: UpdatePosition
) :
    AbstractCommonsArchiveExtractor(context, filePath, outputPath, listener, updatePosition) {

    private val compressorInputStreamConstructor: Constructor<out CompressorInputStream>

    init {
        compressorInputStreamConstructor = getCompressorInputStreamClass()
            .getDeclaredConstructor(InputStream::class.java)
        compressorInputStreamConstructor.isAccessible = true
    }

    /**
     * Subclasses implement this method to specify the [CompressorInputStream] class to be used. It
     * will be used to create the backing inputstream beneath [TarArchiveInputStream] in
     * [createFrom].
     *
     * @return Class representing the implementation will be handling
     */
    abstract fun getCompressorInputStreamClass(): Class<out CompressorInputStream>

    override fun createFrom(inputStream: InputStream): TarArchiveInputStream {
        return TarArchiveInputStream(compressorInputStreamConstructor.newInstance(inputStream))
    }
}
