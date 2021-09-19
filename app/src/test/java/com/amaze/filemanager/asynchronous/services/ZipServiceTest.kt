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

package com.amaze.filemanager.asynchronous.services

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION_CODES.*
import android.os.Looper.getMainLooper
import androidx.test.core.app.ApplicationProvider
import com.amaze.filemanager.asynchronous.services.ZipService.Companion.KEY_COMPRESS_FILES
import com.amaze.filemanager.asynchronous.services.ZipService.Companion.KEY_COMPRESS_PATH
import com.amaze.filemanager.filesystem.HybridFileParcelable
import com.amaze.filemanager.shadows.ShadowMultiDex
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import org.awaitility.Awaitility.await
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime.parse
import java.time.ZoneId.of
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(shadows = [ShadowMultiDex::class], sdk = [JELLY_BEAN, KITKAT, P])
class ZipServiceTest {

    val dt = DateTimeFormatter.ofPattern("yyyyMMddkkmm")
    val dates = listOf(
        parse("201906121300", dt).atZone(of("Antarctica/South_Pole")),
        parse("201906152100", dt).atZone(of("Asia/Taipei")),
        parse("201906161530", dt).atZone(of("Australia/Adelaide")),
        parse("201907020000", dt).atZone(of("Pacific/Norfolk")),
        parse("201907211330", dt).atZone(of("Europe/Dublin")),
        parse("201908180929", dt).atZone(of("Europe/Warsaw")),
        parse("201908230700", dt).atZone(of("America/Indianapolis")),
        parse("201908312330", dt).atZone(of("Asia/Tokyo")),
        parse("201909191315", dt).atZone(of("Asia/Ho_Chi_Minh")),
        parse("201910010700", dt).atZone(of("America/Havana")),
        parse("201911080109", dt).atZone(of("Europe/Oslo")),
        parse("201911111030", dt).atZone(of("Australia/North")),
        parse("201911120300", dt).atZone(of("Asia/Gaza")),
        parse("201911170400", dt).atZone(of("Europe/Kiev")),
        parse("201912312300", dt).atZone(of("America/Ojinaga")),
        parse("202006301900", dt).atZone(of("Africa/Nairobi")),
        parse("202008230830", dt).atZone(of("Pacific/Guam"))
    )

    var context: Context? = null

    /**
     * Pre-test setup.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /**
     * Test service normal run
     */
    @Test
    fun testServiceNormalRun() {
        val zipPath = File(context!!.getExternalFilesDir(null), "files.zip")
        val files = generateSomeRandomFiles()

        val intent = Intent()
            .setClass(context!!, ZipService::class.java)
            .putParcelableArrayListExtra(
                KEY_COMPRESS_FILES,
                ArrayList(listOf(HybridFileParcelable(files[0].absolutePath)))
            )
            .putExtra(KEY_COMPRESS_PATH, zipPath.absolutePath)

        Robolectric.buildService(ZipService::class.java, intent).bind().startCommand(0, 0)

        shadowOf(getMainLooper()).idle()

        assertTrue(zipPath.exists())
        await().atMost(10, TimeUnit.SECONDS).until { zipPath.length() > 0 }
        val verify = ZipFile(zipPath)
        val entries = verify.fileHeaders
        assertEquals(files.filter { it.isFile }.size, entries.size)
        entries.sortBy { it.fileName }
        files.filter { it.isFile }.run {
            verify.fileHeaders.forEachIndexed { idx: Int, any: Any? ->
                run {
                    val entry = any as FileHeader
                    assertFalse(entry.fileName.startsWith('/'))
                    assertEquals(
                        "${entry.fileName} timestamp not equal. " +
                            "${Date(this[idx].lastModified())} " +
                            "vs ${Date(entry.lastModifiedTimeEpoch)}",
                        this[idx].lastModified(), entry.lastModifiedTimeEpoch
                    )
                }
            }
        }
    }

    private fun generateSomeRandomFiles(): ArrayList<File> {
        val files = ArrayList<File>()
        context!!.cacheDir.run {
            val root1 = File(this, "test-archive")
            assertTrue(root1.mkdirs())
            files.add(root1)
            for (x in 1..4) {
                File(root1, x.toString()).run {
                    assertTrue(mkdirs())
                    files.add(this)
                    File(this, (9 - x).toString()).apply {
                        mkdirs()
                        files.add(this)
                    }
                }
            }
            File(root1, "a").apply { mkdirs(); files.add(this); }
            File(root1, "a/b").apply { mkdirs(); files.add(this); }
            File(root1, "a/b/c").apply { mkdirs(); files.add(this); }
            File(root1, "a/b/c/d").apply { mkdirs(); files.add(this); }

            val root2 = File(root1, "a/b/c/d")
            for (x in 1..4) {
                val bytes = Random.nextBytes(ByteArray(Random.nextInt(10, 20)))
                val file = File(root2, "test$x.bin")
                file.writeBytes(bytes)
                files.add(file)
            }
        }

        dates.forEachIndexed { index, zonedDateTime ->
            run {
                Files.getFileAttributeView(
                    Paths.get(files[index].absolutePath),
                    BasicFileAttributeView::class.java
                )
                    .setTimes(
                        FileTime.from(zonedDateTime.toInstant()),
                        FileTime.from(zonedDateTime.toInstant()),
                        null
                    )
            }
        }

        files.sortBy { it.absolutePath }

        return files
    }
}
