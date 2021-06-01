package com.amaze.filemanager.filesystem.cloud

import com.amaze.filemanager.database.CloudHandler
import com.amaze.filemanager.file_operations.filesystem.OpenMode
import com.amaze.filemanager.file_operations.filesystem.OpenMode.*
import com.amaze.filemanager.filesystem.RandomPathGenerator
import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

class CloudTest {
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
            prefix + RandomPathGenerator.SEPARATOR + path
        }

        val r = Random(123)

        for (i in 0..50) {
            val path = RandomPathGenerator.generateRandomPath(r, 50)

            assertForTest(DROPBOX, path, CloudUtil.stripPath(DROPBOX, generatePathForMode(DROPBOX, path)))
            assertForTest(BOX, path, CloudUtil.stripPath(BOX, generatePathForMode(BOX, path)))
            assertForTest(GDRIVE, path, CloudUtil.stripPath(GDRIVE, generatePathForMode(GDRIVE, path)))
            assertForTest(ONEDRIVE, path, CloudUtil.stripPath(ONEDRIVE, generatePathForMode(ONEDRIVE, path)))
        }
    }
}