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

package com.amaze.filemanager.test

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.Parcel
import android.os.UserHandle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.BuildConfig
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.filesystem.compressed.CompressedHelper
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowEnvironment
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.random.Random

/**
 * Generate random junk. If no size specified, default to 73 bytes.
 *
 * No need to use SecureRandom for tests IMO.
 */
fun randomBytes(size: Int = 73) = Random(System.currentTimeMillis()).nextBytes(size)

/**
 * Get supported archive extensions from [CompressedHelper] via reflection.
 */
fun supportedArchiveExtensions(): List<String> {
    return CompressedHelper::class.java.declaredFields.filter { field ->
        field.name.startsWith("fileExtension") &&
            field.type == String::class.java &&
            Modifier.isFinal(field.modifiers) &&
            Modifier.isPublic(field.modifiers) &&
            Modifier.isStatic(field.modifiers)
    }.map {
        it.isAccessible = true
        it.get(null).toString()
    }.filter {
        if (BuildConfig.FLAVOR == "play") {
            it != "tar"
        } else {
            it != "tar" && it != "rar"
        }
    }
}

/**
 * Helper method to get specified string from resources.
 *
 * @param id String resource ID
 */
fun getString(@StringRes id: Int) = AppConfig.getInstance().getString(id)

object TestUtils {
    /**
     * Populate "internal device storage" to StorageManager with directory as provided by Robolectric.
     *
     *
     * Tests need storage access must call this on test case setup for SDK >= N to work.
     */
    @JvmStatic
    fun initializeInternalStorage() {
        val parcel = Parcel.obtain()
        val dir = Environment.getExternalStorageDirectory()
        parcel.writeString("FS-internal")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) parcel.writeInt(0)
        parcel.writeString(dir.absolutePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) parcel.writeString(dir.absolutePath)
        parcel.writeString("robolectric internal storage")
        parcel.writeInt(1)
        parcel.writeInt(0)
        parcel.writeInt(1)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) parcel.writeLong((1024 * 1024).toLong())
        parcel.writeInt(0)
        parcel.writeLong((1024 * 1024).toLong())
        parcel.writeParcelable(UserHandle.getUserHandleForUid(0), 0)
        parcel.writeString("1234-5678")
        parcel.writeString(Environment.MEDIA_MOUNTED)
        addVolumeToStorageManager(parcel)

        /*
         * Monkey-patch ShadowEnvironment for Environment.isExternalStorageManager() to work.
         *
         * See https://github.com/robolectric/robolectric/issues/7300
         */
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            ShadowEnvironment.addExternalDir(Environment.getExternalStorageDirectory().absolutePath)
        }
    }

    /**
     * Populate "external device storage" to StorageManager with directory as provided by Robolectric.
     *
     *
     * Tests need storage access must call this on test case setup for SDK >= N to work.
     */
    @JvmStatic
    fun initializeExternalStorage() {
        val parcel = Parcel.obtain()
        val dir = Environment.getExternalStoragePublicDirectory("external")
        parcel.writeString("FS-external")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) parcel.writeInt(0)
        parcel.writeString(dir.absolutePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) parcel.writeString(dir.absolutePath)
        parcel.writeString("robolectric external storage")
        parcel.writeInt(0)
        parcel.writeInt(1)
        parcel.writeInt(0)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) parcel.writeLong((1024 * 1024).toLong())
        parcel.writeInt(0)
        parcel.writeLong((1024 * 1024).toLong())
        parcel.writeParcelable(UserHandle.getUserHandleForUid(0), 0)
        parcel.writeString("ABCD-EFGH")
        parcel.writeString(Environment.MEDIA_MOUNTED)
        addVolumeToStorageManager(parcel)
    }

    /**
     * Utility method to assist mocking Kotlin objects.
     *
     * Kotlin objects are essentially singletons with a protected INSTANCE generated during
     * compile. So we are injecting our mock copy using reflection.
     */
    @JvmStatic
    fun <T> replaceObjectInstance(clazz: Class<T>, newInstance: T?): T {
        if (!clazz.declaredFields.any {
            it.name == "INSTANCE" && it.type == clazz && Modifier.isStatic(it.modifiers)
        }
        ) {
            throw InstantiationException(
                "clazz ${clazz.canonicalName} does not have a static  " +
                    "INSTANCE field, is it really a Kotlin \"object\"?"
            )
        }

        val instanceField = clazz.getDeclaredField("INSTANCE")
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(instanceField, instanceField.modifiers and Modifier.FINAL.inv())

        instanceField.isAccessible = true
        val originalInstance = instanceField.get(null) as T
        instanceField.set(null, newInstance)
        return originalInstance
    }

    private fun addVolumeToStorageManager(parcel: Parcel) {
        parcel.setDataPosition(0)
        val storageManager = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Context>().getSystemService(
                StorageManager::class.java
            )
        )
        val volume = StorageVolume.CREATOR.createFromParcel(parcel)
        storageManager.addStorageVolume(volume)
    }
}
