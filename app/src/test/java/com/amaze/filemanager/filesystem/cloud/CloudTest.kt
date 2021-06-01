package com.amaze.filemanager.filesystem.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.database.CloudHandler
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.file_operations.filesystem.OpenMode.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

class CloudTest {
    private val CHARS_FOR_PATH = ('A'..'Z').toList() + ('a'..'z').toList()
    private val SEPARATOR = '/'

    @Test
    fun stripPathTest() {
        val assertForTest = { mode: OpenMode, path: String, completePath: String ->
            Assert.assertEquals(path, CloudUtil.stripPath(mode, completePath))
        }

        val generatePathForMode = { mode: OpenMode, path: String ->
            val prefix = when (mode) {
                DROPBOX -> CloudHandler.CLOUD_PREFIX_DROPBOX
                BOX -> CloudHandler.CLOUD_PREFIX_BOX
                GDRIVE  -> CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE
                ONEDRIVE  -> CloudHandler.CLOUD_PREFIX_ONE_DRIVE
                else -> throw RuntimeException()
            }
            prefix + SEPARATOR + path
        }

        val generateRandomPath = { random: Random, length: Int ->
            val randomString = (1..length)
                .map { i -> random.nextInt(0, CHARS_FOR_PATH.count()) }
                .map(CHARS_FOR_PATH::get)

            val path = randomString.mapIndexed { i, e ->
                if (random.nextInt(10) < 1 && i > 0 && randomString[i-1] != SEPARATOR) {
                    SEPARATOR
                } else {
                    e
                }
            }

            path.joinToString("")
        }

        val r = Random(123)

        for (i in 0..50) {
            val path = generateRandomPath(r, 50)

            assertForTest(DROPBOX, path, CloudUtil.stripPath(DROPBOX, generatePathForMode(DROPBOX, path)))
            assertForTest(BOX, path, CloudUtil.stripPath(BOX, generatePathForMode(BOX, path)))
            assertForTest(GDRIVE, path, CloudUtil.stripPath(GDRIVE, generatePathForMode(GDRIVE, path)))
            assertForTest(ONEDRIVE, path, CloudUtil.stripPath(ONEDRIVE, generatePathForMode(ONEDRIVE, path)))
        }
    }
}