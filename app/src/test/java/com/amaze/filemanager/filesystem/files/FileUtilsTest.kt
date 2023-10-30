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

import android.os.Build
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.P
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.filesystem.files.FileUtils.getPathsInPath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.*

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [KITKAT, P, Build.VERSION_CODES.R])
@Suppress("TooManyFunctions", "StringLiteralDuplication")
class FileUtilsTest {

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
                    "/etc/default/grub/2/conf.d"
                ),
                this
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
                    "/var/log/nginx/access/2021-01-01/error.log"
                ),
                this
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
                    "/system/lib/modules/drivers/net/broadcom"
                ),
                this
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
                    "/some/nasty/path/with/space/prefixed"
                ),
                this
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
                    "/some/nasty/path/with/space  /  in/between"
                ),
                this
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
                    "/some/nasty/path/without/slash  /as/  prefix"
                ),
                this
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
                    "smb://1.2.3.4/some/folder/on/smb"
                ),
                this
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
                    "smb://user@1.2.3.4/some/folder/on/smb"
                ),
                this
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
                    "smb://user:password@1.2.3.4/some/folder/on/smb"
                ),
                this
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
                    "smb://user;workgroup:password@1.2.3.4/some/folder/on/smb"
                ),
                this
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
                    "smb://user;workgroup:password@1.2.3.4/user/My Documents"
                ),
                this
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
                    "ftp://user:password@1.2.3.4:3721/some/folder/on/ftp"
                ),
                this
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
                    "ftp://user:password@1.2.3.4:3721/あ/い/う/え/お"
                ),
                this
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
    @Test
    fun testParseStringForHybridFileParcelable() {
        /* ktlint-disable max-line-length */
        // ls
        val lsLines = arrayOf(
            "-rwxr-x---   1 root   shell    29431 2009-01-01 08:00 init.rc",
            "lrw-r--r--   1 root   root        15 2009-01-01 08:00 product -> /system/product",
            "drwxr-xr-x  17 root   root      4096 1970-05-19 08:40 system",
            "-r--r--r-- 1 root root 10 1970-01-13 07:32 cpu_variant:arm",
            "lrwxrwxrwx  1 root root 0 2022-10-05 15:39 ac -> ../../devices/platform/GFSH0001:00/power_supply/ac",
            "lrwxrwxrwx   1 root root 0 2022-10-05 00:16 usb -> ../../devices/platform/soc/c440000.qcom,spmi/spmi-0/spmi0-02/c440000.qcom,spmi:qcom,pm8150b@2:qcom,qpnp-smb5/power_supply/usb"
        )

        // stat with old toybox or busybox
        // val a1 = "-rwxr-x--- 1 shell root 512 2009-01-01 08:00:00.000000000 `init.rc'"
        // val b1 = "lrw-r--r-- 1 root root 512 2009-01-01 08:00:00.000000000 `product' -> `/system/product'"
        // val c1 = "drwxr-xr-x 17 root root 512 1970-05-19 08:40:27.269999949 `system'"

        // stat with new toybox
        val statLines = arrayOf(
            "-rwxr-x--- 1 shell root 512 1230796800 `init.rc'",
            "lrw-r--r-- 1 root root 512 1230796800 `product' -> `/system/product'",
            "drwxr-xr-x 17 root root 512 11922027 `system'",
            "-r--r--r-- 1 root root 512 1035141 `cpu_variant:arm'",
            "lrwxrwxrwx 1 root root 512 1664955558 /sys/class/power_supply/ac -> '../../devices/platform/GFSH0001:00/power_supply/ac'",
            "lrwxrwxrwx 1 root root 512 1664956626 /sys/class/power_supply/usb -> '../../devices/platform/soc/c440000.qcom,spmi/spmi-0/spmi0-02/c440000.qcom,spmi:qcom,pm8150b@2:qcom,qpnp-smb5/power_supply/usb'"
        )
        /* ktlint-enable max-line-length */

        val systemTz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        lsLines.forEachIndexed { index: Int, s: String ->
            val result1 = FileUtils.parseName(s, false)
            val result2 = FileUtils.parseName(statLines[index].replace("('|`)".toRegex(), ""), true)
            assertEquals(
                "Parse error at index $index.\n lsLines=[$s]\n statLines=[${statLines[index]}]\n",
                result1.name,
                result2.name
            )
            assertEquals(
                "Parse error at index $index.\n lsLines=[$s]\n statLines=[${statLines[index]}]\n",
                result1.path,
                result2.path
            )
        }

        TimeZone.setDefault(systemTz)
    }
}
