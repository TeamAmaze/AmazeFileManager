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

package com.amaze.filemanager.adapters.data

import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.P
import android.webkit.MimeTypeMap
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.adapters.data.IconDataParcelable.IMAGE_FROMCLOUD
import com.amaze.filemanager.adapters.data.IconDataParcelable.IMAGE_FROMFILE
import com.amaze.filemanager.adapters.data.IconDataParcelable.IMAGE_RES
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.fileoperations.filesystem.OpenMode
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE
import com.amaze.filemanager.utils.AppConstants.MEGABYTE
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [LOLLIPOP, P, Build.VERSION_CODES.R],
    shadows = [ShadowMultiDex::class],
)
class LayoutElementParcelableTest {
    @Before
    fun setUp() {
        // By default Robolectric's MimeTypeMap is empty, we need to populate them
        val mimeTypeMap = Shadows.shadowOf(MimeTypeMap.getSingleton())
        mimeTypeMap.addExtensionMimeTypMapping("jpg", "image/jpg")
        mimeTypeMap.addExtensionMimeTypMapping("apk", "application/vnd.android.package-archive")
        // Reset size-cap preference to default (no cap) before each test
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .remove(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE)
            .apply()
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .remove(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE)
            .apply()
    }

    // ---------------------------------------------------------------- Remote: SFTP

    /** Remote file above the 1 MB size limit → IMAGE_RES (no thumbnail) */
    @Test
    fun testConstructorWithBigRemoteFile() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1) // idx=1 → 1 MB cap
            .apply()
        val a =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "test-verify.jpg",
                "ssh://127.0.0.1:22222/home/user/test-verify.jpg",
                "777",
                "",
                "17.89 MB",
                17_889_945,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SFTP,
            )
        assertEquals(IMAGE_RES, a.iconData.type)
    }

    /** Remote file below the 1 MB size limit → IMAGE_FROMCLOUD */
    @Test
    fun testConstructorWithSmallRemoteFile() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1) // idx=1 → 1 MB cap
            .apply()
        val b =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "test-verify.jpg",
                "ssh://127.0.0.1:22222/home/user/test-verify.jpg",
                "777",
                "",
                "100 KB",
                102_400,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SFTP,
            )
        assertEquals(IMAGE_FROMCLOUD, b.iconData.type)
    }

    /**
     * Remote file exactly at the 1 MB size limit → IMAGE_FROMCLOUD.
     * Verifies the inclusive (<=) comparison.
     */
    @Test
    fun testConstructorWithFileExactlyAtSizeLimit() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1) // idx=1 → 1 MB cap
            .apply()
        val exactlyOneMb = (1 * MEGABYTE).toLong()
        val c =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "test-verify.jpg",
                "ssh://127.0.0.1:22222/home/user/test-verify.jpg",
                "777",
                "",
                "1 MB",
                exactlyOneMb,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SFTP,
            )
        assertEquals(IMAGE_FROMCLOUD, c.iconData.type)
    }

    /**
     * No size cap configured (idx=0, the default) → even a large remote file gets IMAGE_FROMCLOUD.
     */
    @Test
    fun testConstructorWithNoSizeCapRemoteFile() {
        // idx=0 is already the default, but be explicit
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 0)
            .apply()
        val d =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "test-verify.jpg",
                "ssh://127.0.0.1:22222/home/user/test-verify.jpg",
                "777",
                "",
                "500 MB",
                500L * MEGABYTE,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SFTP,
            )
        assertEquals(IMAGE_FROMCLOUD, d.iconData.type)
    }

    // ---------------------------------------------------------------- Remote: other cloud modes

    /** SMB file within size cap → IMAGE_FROMCLOUD */
    @Test
    fun testConstructorWithSmbFileWithinCap() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1)
            .apply()
        val e =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "photo.jpg",
                "smb://192.168.1.1/share/photo.jpg",
                "777",
                "",
                "512 KB",
                512 * 1024L,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.SMB,
            )
        assertEquals(IMAGE_FROMCLOUD, e.iconData.type)
    }

    /** DROPBOX file above size cap → IMAGE_RES */
    @Test
    fun testConstructorWithDropboxFileAboveCap() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1)
            .apply()
        val f =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "large.jpg",
                "/large.jpg",
                "",
                "",
                "5 MB",
                5L * MEGABYTE,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.DROPBOX,
            )
        assertEquals(IMAGE_RES, f.iconData.type)
    }

    // ---------------------------------------------------------------- Remote: FTP (no thumbnails)

    /**
     * FTP file → always IMAGE_RES regardless of size.
     * FTPClient is thread-unsafe so thumbnails are disabled for FTP.
     */
    @Test
    fun testConstructorWithFtpFileIsAlwaysImageRes() {
        // Even with no size cap, FTP must never load thumbnails
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 0) // no cap
            .apply()
        val g =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "photo.jpg",
                "ftp://192.168.1.1/photo.jpg",
                "",
                "",
                "100 KB",
                102_400,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.FTP,
            )
        assertEquals(IMAGE_RES, g.iconData.type)
    }

    // ---------------------------------------------------------------- Local modes

    /** Local image file → IMAGE_FROMFILE (size cap does not apply to local files) */
    @Test
    fun testConstructorWithLocalImageFile() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1) // cap set, should be ignored
            .apply()
        val h =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "local.jpg",
                "/sdcard/local.jpg",
                "",
                "",
                "50 MB",
                50L * MEGABYTE,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.FILE,
            )
        assertEquals(IMAGE_FROMFILE, h.iconData.type)
    }

    /** Local APK file → IMAGE_FROMFILE regardless of size cap */
    @Test
    fun testConstructorWithLocalApkFile() {
        PreferenceManager.getDefaultSharedPreferences(AppConfig.getInstance())
            .edit()
            .putInt(PREFERENCE_SHOW_REMOTE_THUMB_MAX_SIZE, 1)
            .apply()
        val i =
            LayoutElementParcelable(
                AppConfig.getInstance(),
                false,
                "app.apk",
                "/sdcard/app.apk",
                "",
                "",
                "30 MB",
                30L * MEGABYTE,
                false,
                System.currentTimeMillis().toString(),
                false,
                true,
                OpenMode.FILE,
            )
        assertEquals(IMAGE_FROMFILE, i.iconData.type)
    }
}
