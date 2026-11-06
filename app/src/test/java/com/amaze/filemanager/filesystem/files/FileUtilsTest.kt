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

package com.amaze.filemanager.filesystem.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Build.VERSION_CODES.P
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.filesystem.files.FileUtils.getPathsInPath
import com.amaze.filemanager.ui.fragments.preferencefragments.PreferencesConstants.PREFERENCE_ROOTMODE
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.plugins.RxAndroidPlugins.setInitMainThreadSchedulerHandler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.plugins.RxJavaPlugins.setIoSchedulerHandler
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.nio.file.Files
import java.util.TimeZone
import kotlin.jvm.java

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [M, P, Build.VERSION_CODES.R])
@Suppress("TooManyFunctions", "StringLiteralDuplication")
class FileUtilsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Test FileUtils.getPathsInPath() for directory
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathForFolder() {
        getPathsInPath("/etc/default/grub/2/conf.d").run {
            assertEquals(6, size)
            assertArrayEquals(
                arrayOf(
                    "/",
                    "/etc",
                    "/etc/default",
                    "/etc/default/grub",
                    "/etc/default/grub/2",
                    "/etc/default/grub/2/conf.d",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() for file
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathForFile() {
        getPathsInPath("/var/log/nginx/access/2021-01-01/error.log").run {
            assertEquals(7, size)
            assertArrayEquals(
                arrayOf(
                    "/",
                    "/var",
                    "/var/log",
                    "/var/log/nginx",
                    "/var/log/nginx/access",
                    "/var/log/nginx/access/2021-01-01",
                    "/var/log/nginx/access/2021-01-01/error.log",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() for directory
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathForFolderWithSlashAtEnd() {
        getPathsInPath("/system/lib/modules/drivers/net/broadcom/").run {
            assertEquals(7, size)
            assertArrayEquals(
                arrayOf(
                    "/",
                    "/system",
                    "/system/lib",
                    "/system/lib/modules",
                    "/system/lib/modules/drivers",
                    "/system/lib/modules/drivers/net",
                    "/system/lib/modules/drivers/net/broadcom",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with prefixing space in path
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSpacePrefixed() {
        getPathsInPath("  /some/nasty/path/with/space/prefixed").run {
            assertEquals(7, size)
            assertArrayEquals(
                arrayOf(
                    "/",
                    "/some",
                    "/some/nasty",
                    "/some/nasty/path",
                    "/some/nasty/path/with",
                    "/some/nasty/path/with/space",
                    "/some/nasty/path/with/space/prefixed",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with spaces in path
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSpaceInPath() {
        getPathsInPath("/some/nasty/path/with/space  /  in/between").run {
            assertEquals(8, size)
            assertArrayEquals(
                arrayOf(
                    "/",
                    "/some",
                    "/some/nasty",
                    "/some/nasty/path",
                    "/some/nasty/path/with",
                    "/some/nasty/path/with/space  ",
                    "/some/nasty/path/with/space  /  in",
                    "/some/nasty/path/with/space  /  in/between",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with spaces in path
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithNoSlashPrefix() {
        getPathsInPath("some/nasty/path/without/slash  /as/  prefix").run {
            assertEquals(8, size)
            assertArrayEquals(
                arrayOf(
                    "/",
                    "/some",
                    "/some/nasty",
                    "/some/nasty/path",
                    "/some/nasty/path/without",
                    "/some/nasty/path/without/slash  ",
                    "/some/nasty/path/without/slash  /as",
                    "/some/nasty/path/without/slash  /as/  prefix",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with SMB URI
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSmbUri() {
        getPathsInPath("smb://1.2.3.4/some/folder/on/smb").run {
            assertEquals(5, size)
            assertArrayEquals(
                arrayOf(
                    "smb://1.2.3.4",
                    "smb://1.2.3.4/some",
                    "smb://1.2.3.4/some/folder",
                    "smb://1.2.3.4/some/folder/on",
                    "smb://1.2.3.4/some/folder/on/smb",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with SMB URI
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSmbUri2() {
        getPathsInPath("smb://user@1.2.3.4/some/folder/on/smb").run {
            assertEquals(5, size)
            assertArrayEquals(
                arrayOf(
                    "smb://user@1.2.3.4",
                    "smb://user@1.2.3.4/some",
                    "smb://user@1.2.3.4/some/folder",
                    "smb://user@1.2.3.4/some/folder/on",
                    "smb://user@1.2.3.4/some/folder/on/smb",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with SMB URI
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSmbUri3() {
        getPathsInPath("smb://user:password@1.2.3.4/some/folder/on/smb").run {
            assertEquals(5, size)
            assertArrayEquals(
                arrayOf(
                    "smb://user:password@1.2.3.4",
                    "smb://user:password@1.2.3.4/some",
                    "smb://user:password@1.2.3.4/some/folder",
                    "smb://user:password@1.2.3.4/some/folder/on",
                    "smb://user:password@1.2.3.4/some/folder/on/smb",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with SMB URI
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSmbUri4() {
        getPathsInPath("smb://user;workgroup:password@1.2.3.4/some/folder/on/smb").run {
            assertEquals(5, size)
            assertArrayEquals(
                arrayOf(
                    "smb://user;workgroup:password@1.2.3.4",
                    "smb://user;workgroup:password@1.2.3.4/some",
                    "smb://user;workgroup:password@1.2.3.4/some/folder",
                    "smb://user;workgroup:password@1.2.3.4/some/folder/on",
                    "smb://user;workgroup:password@1.2.3.4/some/folder/on/smb",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with SMB URI containing space
     *
     * Legit URI should encode the space to %20, but not for our case. This case is
     * to ensure the space won't get encoded.
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithSmbUriContainingSpace() {
        getPathsInPath("smb://user;workgroup:password@1.2.3.4/user/My Documents").run {
            assertEquals(3, size)
            assertArrayEquals(
                arrayOf(
                    "smb://user;workgroup:password@1.2.3.4",
                    "smb://user;workgroup:password@1.2.3.4/user",
                    "smb://user;workgroup:password@1.2.3.4/user/My Documents",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with FTP URI having port number too
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithFtpUri() {
        getPathsInPath("ftp://user:password@1.2.3.4:3721/some/folder/on/ftp").run {
            assertEquals(5, size)
            assertArrayEquals(
                arrayOf(
                    "ftp://user:password@1.2.3.4:3721",
                    "ftp://user:password@1.2.3.4:3721/some",
                    "ftp://user:password@1.2.3.4:3721/some/folder",
                    "ftp://user:password@1.2.3.4:3721/some/folder/on",
                    "ftp://user:password@1.2.3.4:3721/some/folder/on/ftp",
                ),
                this,
            )
        }
    }

    /**
     * Test FileUtils.getPathsInPath() with FTP URI with multi-byte characters.
     *
     * Legit URI should encode the multi-byte chars, but not for our case. This case is to
     * ensure the multi-byte chars won't get encoded.
     *
     * @see FileUtils.getPathsInPath
     */
    @Test
    fun testGetPathsInPathWithFtpUriWithMultiByteChars() {
        getPathsInPath("ftp://user:password@1.2.3.4:3721/あ/い/う/え/お").run {
            assertEquals(6, size)
            assertArrayEquals(
                arrayOf(
                    "ftp://user:password@1.2.3.4:3721",
                    "ftp://user:password@1.2.3.4:3721/あ",
                    "ftp://user:password@1.2.3.4:3721/あ/い",
                    "ftp://user:password@1.2.3.4:3721/あ/い/う",
                    "ftp://user:password@1.2.3.4:3721/あ/い/う/え",
                    "ftp://user:password@1.2.3.4:3721/あ/い/う/え/お",
                ),
                this,
            )
        }
    }

    /**
     * Test [FileUtils.splitUri]
     */
    @Test
    fun testSplitUri() {
        assertNull(FileUtils.splitUri("/"))
        assertNull(FileUtils.splitUri("/system/lib/"))
        assertNull(FileUtils.splitUri("/storage/emulated/10"))

        FileUtils.splitUri("ftp://user:password@1.2.3.4:3721/あ/い/う/え/お")!!.run {
            assertEquals("ftp://user:password@1.2.3.4:3721", first)
            assertEquals("/あ/い/う/え/お", second)
        }

        FileUtils.splitUri("smb://user;workgroup:password@1.2.3.4/user/My Documents")!!.run {
            assertEquals("smb://user;workgroup:password@1.2.3.4", first)
            assertEquals("/user/My Documents", second)
        }
    }

    /**
     * Test [FileUtils.parseName] for special cases
     */
    @Test
    fun testParseStringForSpecialCases() {
        // Found on TranceLove's GPD XD Gen 1 running LegacyROM (4.4.4) that dirs doesn't even
        // report default folder node size = 4096 or anything
        val lsLine = "drwxr-xr-x root     root              2023-10-21 13:57 acct"

        val systemTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val result = FileUtils.parseName(lsLine, false)
        assertNotNull(result)
        assertEquals("acct", result.name)
        assertEquals("drwxr-xr-x", result.permission)
        assertTrue(result.isDirectory)
        TimeZone.setDefault(systemTz)
    }

    /**
     * Test [FileUtils.parseName]
     */
    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun testParseStringForHybridFileParcelable() {
        // ls
        val lsLines =
            arrayOf(
                "-rwxr-x---   1 root   shell    29431 2009-01-01 08:00 init.rc",
                "lrw-r--r--   1 root   root        15 2009-01-01 08:00 product -> /system/product",
                "drwxr-xr-x  17 root   root      4096 1970-05-19 08:40 system",
                "-r--r--r-- 1 root root 10 1970-01-13 07:32 cpu_variant:arm",
                "lrwxrwxrwx  1 root root 0 2022-10-05 15:39 ac -> ../../devices/platform/GFSH0001:00/power_supply/ac",
                "lrwxrwxrwx   1 root root 0 2022-10-05 00:16 usb -> ../../devices/platform/soc/c440000.qcom,spmi/spmi-0/spmi0-02/c440000.qcom,spmi:qcom,pm8150b@2:qcom,qpnp-smb5/power_supply/usb",
            )

        // stat with old toybox or busybox
        // val a1 = "-rwxr-x--- 1 shell root 512 2009-01-01 08:00:00.000000000 `init.rc'"
        // val b1 = "lrw-r--r-- 1 root root 512 2009-01-01 08:00:00.000000000 `product' -> `/system/product'"
        // val c1 = "drwxr-xr-x 17 root root 512 1970-05-19 08:40:27.269999949 `system'"

        // stat with new toybox
        val statLines =
            arrayOf(
                "-rwxr-x--- 1 shell root 512 1230796800 `init.rc'",
                "lrw-r--r-- 1 root root 512 1230796800 `product' -> `/system/product'",
                "drwxr-xr-x 17 root root 512 11922027 `system'",
                "-r--r--r-- 1 root root 512 1035141 `cpu_variant:arm'",
                "lrwxrwxrwx 1 root root 512 1664955558 /sys/class/power_supply/ac -> '../../devices/platform/GFSH0001:00/power_supply/ac'",
                "lrwxrwxrwx 1 root root 512 1664956626 /sys/class/power_supply/usb -> '../../devices/platform/soc/c440000.qcom,spmi/spmi-0/spmi0-02/c440000.qcom,spmi:qcom,pm8150b@2:qcom,qpnp-smb5/power_supply/usb'",
            )

        val systemTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        lsLines.forEachIndexed { index: Int, s: String ->
            val result1 = FileUtils.parseName(s, false)
            val result2 = FileUtils.parseName(statLines[index].replace("('|`)".toRegex(), ""), true)
            assertEquals(
                "Parse error at index $index.\n lsLines=[$s]\n statLines=[${statLines[index]}]\n",
                result1.name,
                result2.name,
            )
            assertEquals(
                "Parse error at index $index.\n lsLines=[$s]\n statLines=[${statLines[index]}]\n",
                result1.path,
                result2.path,
            )
        }

        TimeZone.setDefault(systemTz)
    }

    /** FileProvider caches path strategies statically, but Robolectric cache dir differs per test */
    @Before
    fun resetFileProviderCache() {
        val field = FileProvider::class.java.getDeclaredField("sCache")
        field.isAccessible = true
        (field.get(null) as MutableMap<*, *>).clear()
    }

    /**
     * Reset RxJava and RxAndroid after test.
     */
    @After
    fun resetRx() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    /**
     * Test [FileUtils.fromContentUri] for storage root.
     */
    @Test
    fun testFromContentUriStorageRoot() {
        val uri = Uri.parse("content://com.amaze.filemanager/storage_root/emulated/0/Download/a.txt")
        assertEquals("/storage/emulated/0/Download/a.txt", FileUtils.fromContentUri(uri).path)
    }

    /**
     * Test [FileUtils.fromContentUri] for media_rw.
     */
    @Test
    fun testFromContentUriMediaRw() {
        val uri = Uri.parse("content://com.amaze.filemanager/media_rw_root/ABCD-1234/a.txt")
        assertEquals("/mnt/media_rw/ABCD-1234/a.txt", FileUtils.fromContentUri(uri).path)
    }

    /**
     * Test [FileUtils.fromContentUri] for the shareable-copy cache.
     */
    @Test
    fun testFromContentUriOpenWithCache() {
        val uri = Uri.parse("content://com.amaze.filemanager/open_with/copy-id/a.db")
        assertEquals(
            File(context.cacheDir, "open_with/copy-id/a.db").path,
            FileUtils.fromContentUri(uri).path,
        )
    }

    /**
     * Test [FileUtils.fromContentUri] for legacy storage root.
     */
    @Test
    fun testFromContentUriLegacyStorageRoot() {
        val uri = Uri.parse("content://com.amaze.filemanager/storage_root/etc/hosts")
        // old URI shape: /storage_root + absolute path
        assertEquals("/etc/hosts", FileUtils.fromContentUri(uri).path)
    }

    /**
     * Test [FileUtils.getShareableUri] for covered path.
     */
    @Test
    fun testGetShareableUriForCoveredPath() {
        val uri = FileUtils.getShareableUri(context, File("/storage/emulated/0/test.txt"))
        assertNotNull(uri)
        assertEquals("/storage_root/emulated/0/test.txt", uri!!.path)
    }

    /**
     * Test [FileUtils.getShareableUri] copies uncovered file.
     */
    @Test
    fun testGetShareableUriCopiesUncoveredFile() {
        val src = File.createTempFile("amaze", ".txt")
        src.writeText("hello")
        try {
            val uri = FileUtils.getShareableUri(context, src)
            assertNotNull(uri)
            assertTrue(uri!!.path!!.startsWith("/open_with/"))
            context.contentResolver.openInputStream(uri)!!.use {
                assertEquals("hello", it.readBytes().toString(Charsets.UTF_8))
            }
        } finally {
            src.delete()
        }
    }

    /**
     * Test [FileUtils.getShareableUri] refuses unreadable files and directories.
     */
    @Test
    fun testGetShareableUriRefusesUnreadableAndDirectories() {
        assertNull(FileUtils.getShareableUri(context, File("/nonexistent/secret.txt")))
        val dir = Files.createTempDirectory("amazedir").toFile()
        try {
            assertNull(FileUtils.getShareableUri(context, dir))
        } finally {
            dir.delete()
        }
    }

    /**
     * Test [FileUtils.buildIntent] does not grant write permission to other apps.
     */
    @Test
    fun testBuildIntentDoesNotGrantToOtherApps() {
        val uri = Uri.parse("content://${context.packageName}/storage_root/emulated/0/a.txt")
        val intent =
            com.amaze.filemanager.ui.dialogs.OpenFileDialogFragment.buildIntent(
                context,
                uri,
                "text/plain",
                false,
                null,
                null,
            )
        assertEquals(
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
            intent.flags and
                (
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ),
        )
        assertFalse(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
    }

    /**
     * Test [FileUtils.getShareableUriAsync] emits without scheduling for covered paths.
     */
    @Test
    fun testGetShareableUriAsyncCoveredPathEmitsWithoutScheduling() {
        val uri =
            FileUtils.getShareableUriAsync(context, File("/storage/emulated/0/test.txt"))
                .blockingGet()
        assertNull(ShadowToast.getLatestToast())
        assertEquals("/storage_root/emulated/0/test.txt", uri!!.path)
    }

    /**
     * Test [FileUtils.getShareableUriAsync] copies uncovered file on IO scheduler.
     */
    @Test
    fun testGetShareableUriAsyncCopiesOnIoScheduler() {
        val ioThreads = mutableListOf<String>()
        setIoSchedulerHandler {
            Schedulers.trampoline().also { ioThreads.add("io") }
        }
        setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        val src = File.createTempFile("amaze", ".txt")
        src.writeText("async")
        try {
            val uri = FileUtils.getShareableUriAsync(context, src).blockingGet()
            assertEquals(listOf("io"), ioThreads)
            assertEquals(
                context.getString(com.amaze.filemanager.R.string.copying),
                ShadowToast.getTextOfLatestToast(),
            )
            context.contentResolver.openInputStream(uri!!)!!.use {
                assertEquals("async", it.readBytes().toString(Charsets.UTF_8))
            }
        } finally {
            src.delete()
        }
    }

    /**
     * Test [FileUtils.getShareableUriAsync] completes empty when not shareable.
     */
    @Test
    fun testGetShareableUriAsyncCompletesEmptyWhenNotShareable() {
        setIoSchedulerHandler { Schedulers.trampoline() }
        setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        assertNull(
            FileUtils.getShareableUriAsync(context, File("/nonexistent/secret.txt")).blockingGet(),
        )
    }

    /**
     * Test [FileUtils.getShareableUri] does not collide when two files with the same name
     * are copied to the cache.
     */
    @Test
    fun testGetShareableUriSameNameDoesNotCollide() {
        val d1 = Files.createTempDirectory("amaze1").toFile()
        val d2 = Files.createTempDirectory("amaze2").toFile()
        val f1 = File(d1, "same.txt").apply { writeText("one") }
        val f2 = File(d2, "same.txt").apply { writeText("two") }
        try {
            val u1 = FileUtils.getShareableUri(context, f1)!!
            val u2 = FileUtils.getShareableUri(context, f2)!!
            assertTrue(u1 != u2)
            assertEquals("one", context.contentResolver.openInputStream(u1)!!.use { it.readBytes().toString(Charsets.UTF_8) })
            assertEquals("two", context.contentResolver.openInputStream(u2)!!.use { it.readBytes().toString(Charsets.UTF_8) })
        } finally {
            listOf(f1, f2, d1, d2).forEach { it.delete() }
        }
    }

    /**
     * Test [FileUtils.getShareableUriAsync] reports error when copy fails and cleans up any half-written copy.
     */
    @Test
    fun testGetShareableUriAsyncReportsRootCopyFailure() {
        setIoSchedulerHandler { Schedulers.trampoline() }
        setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(
                PREFERENCE_ROOTMODE,
                true,
            )
        }
        val observer =
            FileUtils.getShareableUriAsync(context, File("/nonexistent/secret.txt")).test()
        observer.assertError(FileUtils.ShareCopyException::class.java)
        // no half-written copy is left behind
        val leftovers = File(context.cacheDir, FileUtils.OPEN_WITH_CACHE_DIR).listFiles()
        assertTrue(leftovers == null || leftovers.all { it.listFiles().isNullOrEmpty() })
    }
}
