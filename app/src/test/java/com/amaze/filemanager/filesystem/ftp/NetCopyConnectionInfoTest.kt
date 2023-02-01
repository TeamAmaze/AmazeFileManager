/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.ftp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.shadows.ShadowMultiDex
import com.amaze.filemanager.test.ShadowPasswordUtil
import com.amaze.filemanager.utils.urlDecoded
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URLDecoder.decode
import java.net.URLEncoder.encode

/* ktlint-disable max-line-length */
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [ShadowPasswordUtil::class, ShadowMultiDex::class]
)
@Suppress("StringLiteralDuplication")
class NetCopyConnectionInfoTest {

    /**
     * Test unsupported URL prefixes should throw IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun testUnsupportedHttpPrefix() {
        NetCopyConnectionInfo("http://github.com")
    }

    /**
     * Test to verify SMB prefix is supported.
     */
    @Test
    fun testSmbPrefixSupported() {
        NetCopyConnectionInfo("smb://user:pass@127.0.0.1")
    }

    /**
     * Test parsing garbage.
     */
    @Test(expected = IllegalArgumentException::class)
    fun testParsingInvalid() {
        NetCopyConnectionInfo("abcdefgh")
        NetCopyConnectionInfo("svx[f//shf")
        NetCopyConnectionInfo("ftp//abcde@12345@127.0.0.1")
    }

    /**
     * Tests simple parsing.
     */
    @Test
    fun testSimple() {
        NetCopyConnectionInfo(
            "ftp://testuser:testpassword@127.0.0.1:22222"
        ).run {
            assertEquals("ftp://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertNull(defaultPath)
            assertNull(queryString)
            assertNull(arguments)
            assertNull(lastPathSegment())
        }
    }

    /**
     * Test parse URL with query string.
     */
    @Test
    fun testQueryString() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222?tls=implicit&passive=false"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertNull(defaultPath)
            assertNull(lastPathSegment())
            assertEquals("tls=implicit&passive=false", queryString)
            assertNotNull(arguments)
            arguments?.run {
                assertEquals("implicit", this["tls"])
                assertEquals("false", this["passive"])
            }
        }
    }

    /**
     * Test default path in URL.
     */
    @Test
    fun testDefaultPath() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222/srv/tomopet-service"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertEquals("/srv/tomopet-service", defaultPath)
            assertEquals("tomopet-service", lastPathSegment())
            assertNull(queryString)
            assertNull(arguments)
        }
    }

    /**
     * Test default path in URL.
     */
    @Test
    fun testDefaultPathWithFilename() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222/srv/tomopet-service/history.txt"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertEquals("/srv/tomopet-service", defaultPath)
            assertEquals("history.txt", lastPathSegment())
            assertNull(queryString)
            assertNull(arguments)
        }
    }

    /**
     * Test default path and query string in URL.
     */
    @Test
    fun testDefaultPathWithQueryString() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222/srv/tomopet-service?tls=explicit&passive=true"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertEquals("/srv/tomopet-service", defaultPath)
            assertEquals("tomopet-service", lastPathSegment())
            assertEquals("tls=explicit&passive=true", queryString)
            assertNotNull(arguments)
            arguments?.run {
                assertEquals("explicit", this["tls"])
                assertEquals("true", this["passive"])
            }
        }
    }

    /**
     * Test default path in URL.
     */
    @Test
    fun testDefaultPathURLEncoded() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222/Users/TranceLove/My+Documents/%40TranceLove%231433%261434"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertEquals("/Users/TranceLove/My+Documents/%40TranceLove%231433%261434", defaultPath)
            assertEquals(
                "/Users/TranceLove/My Documents/@TranceLove#1433&1434",
                defaultPath?.urlDecoded()
            )
            assertEquals("%40TranceLove%231433%261434", lastPathSegment())
            assertEquals("@TranceLove#1433&1434", lastPathSegment()?.urlDecoded())
            assertNull(queryString)
            assertNull(arguments)
        }
    }

    /**
     * Test default path in URL.
     */
    @Test
    fun testDefaultPathWithFilenameURLEncoded() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222/home/trancelove/My+Web+Sites/Test/Awesome-stars/%7BMaruell+Horbis%7D+Tris%2BSurplus+40%25+off+%40rugio.txt"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertEquals("/home/trancelove/My+Web+Sites/Test/Awesome-stars", defaultPath)
            assertEquals(
                "/home/trancelove/My Web Sites/Test/Awesome-stars",
                defaultPath?.urlDecoded()
            )
            assertEquals(
                "%7BMaruell+Horbis%7D+Tris%2BSurplus+40%25+off+%40rugio.txt",
                lastPathSegment()
            )
            assertEquals(
                "{Maruell Horbis} Tris+Surplus 40% off @rugio.txt",
                lastPathSegment()?.urlDecoded()
            )
            assertNull(queryString)
            assertNull(arguments)
        }
    }

    /**
     * Test default path and query string in URL.
     */
    @Test
    fun testDefaultPathWithQueryStringURLEncoded() {
        NetCopyConnectionInfo(
            "ftps://testuser:testpassword@127.0.0.1:22222/home/trancelove/My+Web+Sites/Test/Awesome-stars/%7BMaruell+Horbis%7D+Tris%2BSurplus+40%25+off+%40rugio.txt?easter_egg=%7B%7D%28%29%26%5E%25*%3C%3E%21%40%23%24%25%3F%3A%22%3B%27"
        ).run {
            assertEquals("ftps://", prefix)
            assertEquals("testuser", username)
            assertEquals("testpassword", password)
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertEquals("/home/trancelove/My+Web+Sites/Test/Awesome-stars", defaultPath)
            assertEquals(
                "/home/trancelove/My Web Sites/Test/Awesome-stars",
                defaultPath?.urlDecoded()
            )
            assertEquals(
                "%7BMaruell+Horbis%7D+Tris%2BSurplus+40%25+off+%40rugio.txt",
                lastPathSegment()
            )
            assertEquals(
                "{Maruell Horbis} Tris+Surplus 40% off @rugio.txt",
                lastPathSegment()?.urlDecoded()
            )
            assertEquals(
                "easter_egg=%7B%7D%28%29%26%5E%25*%3C%3E%21%40%23%24%25%3F%3A%22%3B%27",
                queryString
            )
            assertNotNull(arguments)
            arguments?.run {
                assertEquals(
                    "%7B%7D%28%29%26%5E%25*%3C%3E%21%40%23%24%25%3F%3A%22%3B%27",
                    this["easter_egg"]
                )
                assertEquals("{}()&^%*<>!@#\$%?:\";'", this["easter_egg"]?.urlDecoded())
            }
        }
    }

    /**
     * Tests difficult credentials.
     */
    @Test
    fun testDifficultCredentials() {
        NetCopyConnectionInfo(
            "ftp://testuser:${encode("testP@##word", Charsets.UTF_8.name())}@127.0.0.1:22222"
        ).run {
            assertEquals("ftp://", prefix)
            assertEquals("testuser", username)
            assertEquals("testP%40%23%23word", password)
            assertEquals("testP@##word", decode("testP@##word", Charsets.UTF_8.name()))
            assertEquals("127.0.0.1", host)
            assertEquals(22222, port)
            assertNull(defaultPath)
            assertNull(queryString)
        }
    }

    /**
     * Test parsing complex credentials.
     */
    @Test
    fun testComplexCredentials() {
        val username = "user2816@user.com"
        val password = "#$%^&*()10-={}"
        val _username = encode(username, Charsets.UTF_8.name())
        val _password = encode(password, Charsets.UTF_8.name())

        NetCopyConnectionInfo("ssh://$_username:$_password@127.0.0.1:32").run {
            assertEquals("ssh://", this.prefix)
            assertEquals("127.0.0.1", this.host)
            assertEquals(32, this.port)
            assertEquals(username, decode(this.username, Charsets.UTF_8.name()))
            assertEquals(password, decode(this.password, Charsets.UTF_8.name()))
        }
    }

    /**
     * Test parsing cleaning up duplicated slashes.
     */
    @Test
    fun testParseDuplicatedSlashes() {
        NetCopyConnectionInfo("smb://user:pass@127.0.0.1/test/1/2/3/4.txt").run {
            assertEquals("smb://user@127.0.0.1/test/1/2/3", this.toString())
            assertEquals("4.txt", this.lastPathSegment())
        }
        NetCopyConnectionInfo("smb://user:pass@127.0.0.1/test//1///2////3/4.txt").run {
            assertEquals("smb://user@127.0.0.1/test/1/2/3", this.toString())
            assertEquals("4.txt", this.lastPathSegment())
        }
        NetCopyConnectionInfo("ssh://user:pass@127.0.0.1/a/b/c/d/e").run {
            assertEquals("ssh://user@127.0.0.1/a/b/c/d/e", this.toString())
            assertNotNull(this.lastPathSegment())
        }
        NetCopyConnectionInfo(
            "ftp://127.0.0.1////a/bunch///of///slash//folders////////////test.log"
        ).run {
//            assertEquals("ftp://127.0.0.1/a/bunch/of/slash/folders", this.toString())
            assertEquals("test.log", this.lastPathSegment())
        }
    }
}
/* ktlint-disable max-line-length */
