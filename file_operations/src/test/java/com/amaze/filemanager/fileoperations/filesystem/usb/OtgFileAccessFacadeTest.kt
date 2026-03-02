package com.amaze.filemanager.fileoperations.filesystem.usb

import android.content.Context
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File

class OtgFileAccessFacadeTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = Mockito.mock(Context::class.java)
    }

    /**
     * hasDirectAccess returns true for a readable path on pre-R devices.
     * On pre-R (< Android 11) direct access is allowed whenever canRead() is true,
     * regardless of MANAGE_EXTERNAL_STORAGE.
     */
    @Test
    fun testHasDirectAccess_readablePath_preR() {
        val testDir = File("/tmp/otg_direct_access_test")
        testDir.mkdirs()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // On pre-R the only gate is canRead()
                assertTrue(OtgFileAccessFacade.hasDirectAccess(testDir.absolutePath))
            }
            // On R+ we can't easily test without mocking Environment.isExternalStorageManager(),
            // but we verify the call doesn't throw
            OtgFileAccessFacade.hasDirectAccess(testDir.absolutePath)
        } finally {
            testDir.delete()
        }
    }

    /**
     * hasDirectAccess returns false for a non-existent or unreadable path.
     */
    @Test
    fun testHasDirectAccess_nonExistentPath_returnsFalse() {
        assertFalse(OtgFileAccessFacade.hasDirectAccess("/tmp/definitely_does_not_exist_xyz123"))
    }

    /**
     * Regression test: listFiles via direct access returns File objects with absolute paths.
     * The returned File paths must be usable as regular filesystem paths (OpenMode.FILE),
     * not as OTG SAF paths. This ensures consistency between the drawer path selection
     * (which uses the filesystem path when direct access is available) and file listing.
     */
    @Test
    fun testListFiles_directAccess_returnsAbsolutePaths() {
        val deviceKey = "vol:test-direct"
        val testDir = File("/tmp/otg_abs_path_test")
        testDir.mkdirs()
        val testFile = File(testDir, "document.pdf")
        testFile.writeText("test")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            val files = OtgFileAccessFacade.listFiles(context, deviceKey)
            assertTrue(files.isNotEmpty())
            // Files returned must have absolute paths rooted at the mount point
            files.forEach { f ->
                assertTrue(
                    "File path ${f.absolutePath} must start with mount path ${testDir.absolutePath}",
                    f.absolutePath.startsWith(testDir.absolutePath),
                )
                assertTrue("File path must be absolute", f.isAbsolute)
            }
        }
        testFile.delete()
        testDir.delete()
    }

    /**
     * listFiles with a non-empty subPath navigates into subdirectory.
     */
    @Test
    fun testListFiles_withSubPath_navigatesCorrectly() {
        val deviceKey = "vol:sub-path-test"
        val testDir = File("/tmp/otg_subpath_test")
        val subDir = File(testDir, "subfolder")
        subDir.mkdirs()
        val testFile = File(subDir, "file.txt")
        testFile.writeText("content")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            // List root — should contain the subfolder
            val rootFiles = OtgFileAccessFacade.listFiles(context, deviceKey, "")
            assertTrue(rootFiles.any { it.name == "subfolder" && it.isDirectory })

            // List subfolder — should contain the file
            val subFiles = OtgFileAccessFacade.listFiles(context, deviceKey, "subfolder")
            assertTrue(subFiles.any { it.name == "file.txt" })
        }
        testFile.delete()
        subDir.delete()
        testDir.delete()
    }

    @Test
    fun testDirectAccessSuccess() {
        val deviceKey = "vol:test"
        val testDir = File("/tmp/otgtestdir")
        testDir.mkdirs()
        val testFile = File(testDir, "file.txt")
        testFile.writeText("hello")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            val files = OtgFileAccessFacade.listFiles(context, deviceKey)
            assertTrue(files.any { it.name == "file.txt" })
        }
        testFile.delete()
        testDir.delete()
    }

    @Test
    fun testDirectAccessNoPermission() {
        val deviceKey = "vol:test"
        val testDir = File("/tmp/otgtestdir2")
        testDir.mkdirs()
        testDir.setReadable(false)

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            val files = OtgFileAccessFacade.listFiles(context, deviceKey)
            assertTrue(files.isEmpty()) // Should fallback to SAF stub
        }
        testDir.setReadable(true)
        testDir.delete()
    }

    @Test
    fun testDeviceNotFoundFallback() {
        val deviceKey = "vol:missing"
        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(null)
            val files = OtgFileAccessFacade.listFiles(context, deviceKey)
            assertTrue(files.isEmpty()) // Should fallback to SAF stub
        }
    }

    @Test
    fun testGetFileDirectAccess() {
        val deviceKey = "vol:test"
        val testDir = File("/tmp/otgtestdir3")
        testDir.mkdirs()
        val testFile = File(testDir, "file.txt")
        testFile.writeText("data")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            val file = OtgFileAccessFacade.getFile(context, deviceKey, "file.txt")
            assertNotNull(file)
            assertEquals("file.txt", file?.name)
        }
        testFile.delete()
        testDir.delete()
    }

    /**
     * Test that disabling preferNativeAccess forces SAF path.
     * hasDirectAccess should return false regardless of filesystem readability.
     */
    @Test
    fun testPreferNativeAccessDisabled_hasDirectAccessReturnsFalse() {
        val testDir = File("/tmp/otg_pref_test")
        testDir.mkdirs()
        try {
            // With preference enabled (default), should return true for readable path
            OtgFileAccessFacade.preferNativeAccess = true
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                assertTrue(OtgFileAccessFacade.hasDirectAccess(testDir.absolutePath))
            }

            // With preference disabled, should always return false
            OtgFileAccessFacade.preferNativeAccess = false
            assertFalse(OtgFileAccessFacade.hasDirectAccess(testDir.absolutePath))
        } finally {
            // Restore default
            OtgFileAccessFacade.preferNativeAccess = true
            testDir.delete()
        }
    }

    /**
     * Test that disabling preferNativeAccess makes listFiles return empty (SAF stub)
     * even for a readable directory.
     */
    @Test
    fun testPreferNativeAccessDisabled_listFilesReturnsSafStub() {
        val deviceKey = "vol:test-pref"
        val testDir = File("/tmp/otg_pref_list_test")
        testDir.mkdirs()
        val testFile = File(testDir, "sample.txt")
        testFile.writeText("data")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            try {
                // With preference enabled, should list the file
                OtgFileAccessFacade.preferNativeAccess = true
                val filesEnabled = OtgFileAccessFacade.listFiles(context, deviceKey)
                assertTrue(filesEnabled.any { it.name == "sample.txt" })

                // With preference disabled, should fall back to SAF stub (empty list)
                OtgFileAccessFacade.preferNativeAccess = false
                val filesDisabled = OtgFileAccessFacade.listFiles(context, deviceKey)
                assertTrue(filesDisabled.isEmpty())
            } finally {
                // Restore default
                OtgFileAccessFacade.preferNativeAccess = true
                testFile.delete()
                testDir.delete()
            }
        }
    }

    /**
     * Test that disabling preferNativeAccess makes getFile return null
     * even for an existing file.
     */
    @Test
    fun testPreferNativeAccessDisabled_getFileReturnsNull() {
        val deviceKey = "vol:test-pref-file"
        val testDir = File("/tmp/otg_pref_file_test")
        testDir.mkdirs()
        val testFile = File(testDir, "doc.pdf")
        testFile.writeText("content")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            try {
                // With preference enabled, should return the File
                OtgFileAccessFacade.preferNativeAccess = true
                val fileEnabled = OtgFileAccessFacade.getFile(context, deviceKey, "doc.pdf")
                assertNotNull(fileEnabled)
                assertEquals("doc.pdf", fileEnabled?.name)

                // With preference disabled, should return null
                OtgFileAccessFacade.preferNativeAccess = false
                val fileDisabled = OtgFileAccessFacade.getFile(context, deviceKey, "doc.pdf")
                assertNull(fileDisabled)
            } finally {
                // Restore default
                OtgFileAccessFacade.preferNativeAccess = true
                testFile.delete()
                testDir.delete()
            }
        }
    }

    /**
     * Test that listFiles returns empty when mount point becomes inaccessible
     * (simulating device unplug/unmount during operation).
     */
    @Test
    fun testListFiles_mountPointBecomesInaccessible_returnsSafStub() {
        val deviceKey = "vol:test-unplug"
        val testDir = File("/tmp/otg_unplug_test")
        testDir.mkdirs()
        val testFile = File(testDir, "data.txt")
        testFile.writeText("content")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            try {
                // With preference enabled and mount point accessible
                OtgFileAccessFacade.preferNativeAccess = true
                val filesAccessible = OtgFileAccessFacade.listFiles(context, deviceKey)
                assertTrue(filesAccessible.any { it.name == "data.txt" })

                // Simulate device unplug by deleting the directory
                testFile.delete()
                testDir.delete()

                // Now listFiles should fall back to SAF (empty list)
                val filesInaccessible = OtgFileAccessFacade.listFiles(context, deviceKey)
                assertTrue(
                    "When mount point is inaccessible, should return SAF stub (empty list)",
                    filesInaccessible.isEmpty(),
                )
            } finally {
                OtgFileAccessFacade.preferNativeAccess = true
                if (testFile.exists()) testFile.delete()
                if (testDir.exists()) testDir.delete()
            }
        }
    }

    /**
     * Test that getFile returns null when mount point becomes inaccessible
     * (simulating device unplug/unmount during operation).
     */
    @Test
    fun testGetFile_mountPointBecomesInaccessible_returnsNull() {
        val deviceKey = "vol:test-unplug-file"
        val testDir = File("/tmp/otg_unplug_file_test")
        testDir.mkdirs()
        val testFile = File(testDir, "doc.pdf")
        testFile.writeText("document")

        Mockito.mockStatic(StorageDeviceManager::class.java).use { mocked ->
            mocked.`when`<Any> { StorageDeviceManager.findDeviceByKey(context, deviceKey) }
                .thenReturn(TestStorageDevice(deviceKey, "Test Device", testDir.absolutePath))

            try {
                // With preference enabled and mount point accessible
                OtgFileAccessFacade.preferNativeAccess = true
                val fileAccessible = OtgFileAccessFacade.getFile(context, deviceKey, "doc.pdf")
                assertNotNull(fileAccessible)
                assertEquals("doc.pdf", fileAccessible?.name)

                // Simulate device unplug by deleting the directory
                testFile.delete()
                testDir.delete()

                // Now getFile should return null (mount point inaccessible)
                val fileInaccessible = OtgFileAccessFacade.getFile(context, deviceKey, "doc.pdf")
                assertNull("When mount point is inaccessible, should return null", fileInaccessible)
            } finally {
                OtgFileAccessFacade.preferNativeAccess = true
                if (testFile.exists()) testFile.delete()
                if (testDir.exists()) testDir.delete()
            }
        }
    }
}
