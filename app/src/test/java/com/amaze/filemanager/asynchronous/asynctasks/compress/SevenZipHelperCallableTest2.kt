package com.amaze.filemanager.asynchronous.asynctasks.compress

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore("Test skipped due to problem at upstream library.")
class SevenZipHelperCallableTest2 : AbstractCompressedHelperCallableArchiveTest() {

    override val archiveFileName: String
        get() = "compress.7z"

    @Test
    override fun testRoot() {
        val task = createCallable("")
        val result = task.call()
        Assert.assertEquals(result.size.toLong(), 0)
    }

    @Test
    @Ignore("Not testing this one")
    override fun testSublevels() = Unit

    override fun doCreateCallable(archive: File, relativePath: String): CompressedHelperCallable =
            SevenZipHelperCallable(
        archive.absolutePath,
        relativePath,
        false
    )
}
