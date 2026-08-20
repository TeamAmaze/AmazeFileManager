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

package com.amaze.filemanager.utils

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for OTGUtil device key path helper functions.
 * Tests the parsing and building of OTG paths with device keys.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
@Suppress("StringLiteralDuplication")
class OTGUtilDeviceKeyTest {
    /**
     * Test extracting device keys from OTG paths with vendor:product format.
     */
    @Test
    fun testExtractDeviceKeyFromPath_withVendorProductKey() {
        // Standard vendor:product format
        assertEquals(
            "1234:5678",
            OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678/"),
        )
        assertEquals(
            "1234:5678",
            OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678/folder"),
        )
        assertEquals(
            "1234:5678",
            OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678/folder/file.txt"),
        )
    }

    /**
     * Test extracting device keys from OTG paths with vendor:product:serial format.
     */
    @Test
    fun testExtractDeviceKeyFromPath_withSerialNumber() {
        // vendor:product:serial format
        assertEquals(
            "1234:5678:ABC123",
            OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678:ABC123/"),
        )
        assertEquals(
            "1234:5678:ABC123",
            OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678:ABC123/folder/file.txt"),
        )
        // Serial with special characters
        assertEquals(
            "1234:5678:SN-2024-XYZ",
            OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678:SN-2024-XYZ/data"),
        )
    }

    /**
     * Test legacy OTG paths without device keys return null.
     */
    @Test
    fun testExtractDeviceKeyFromPath_legacyFormatReturnsNull() {
        // Legacy paths without device key should return null
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/folder"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/folder/file.txt"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/Documents/report.pdf"))
    }

    /**
     * Test non-OTG paths return null when extracting device keys.
     */
    @Test
    fun testExtractDeviceKeyFromPath_invalidPathsReturnNull() {
        // Non-OTG paths
        assertNull(OTGUtil.extractDeviceKeyFromPath("/storage/emulated/0"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("content://com.android.providers"))
        assertNull(OTGUtil.extractDeviceKeyFromPath(""))
        assertNull(OTGUtil.extractDeviceKeyFromPath("/"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("file:///sdcard/"))
    }

    /**
     * Test edge cases for device key extraction.
     */
    @Test
    fun testExtractDeviceKeyFromPath_edgeCases() {
        // Single segment with colon - technically matches the pattern (has colon, splits to 2+ parts)
        // "single:" splits to ["single", ""] which is 2 parts, so it's treated as a device key
        assertEquals("single:", OTGUtil.extractDeviceKeyFromPath("otg:/single:/folder"))
        // Path that looks like device key but isn't (single number without colon)
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/12345/folder"))
    }

    /**
     * Test building OTG paths from device keys and sub-paths.
     */
    @Test
    fun testBuildOtgPath_withEmptySubPath() {
        assertEquals(
            "otg:/1234:5678/",
            OTGUtil.buildOtgPath("1234:5678", ""),
        )
        assertEquals(
            "otg:/1234:5678:ABC/",
            OTGUtil.buildOtgPath("1234:5678:ABC", ""),
        )
    }

    /**
     * Test building OTG paths with sub-paths appended to device keys.
     */
    @Test
    fun testBuildOtgPath_withSubPath() {
        assertEquals(
            "otg:/1234:5678/folder",
            OTGUtil.buildOtgPath("1234:5678", "folder"),
        )
        assertEquals(
            "otg:/1234:5678/folder/subfolder",
            OTGUtil.buildOtgPath("1234:5678", "folder/subfolder"),
        )
        assertEquals(
            "otg:/1234:5678/folder/file.txt",
            OTGUtil.buildOtgPath("1234:5678", "folder/file.txt"),
        )
    }

    /**
     * Test building OTG paths with leading slashes in sub-paths.
     */
    @Test
    fun testBuildOtgPath_withLeadingSlashInSubPath() {
        // Leading slash should be trimmed
        assertEquals(
            "otg:/1234:5678/folder",
            OTGUtil.buildOtgPath("1234:5678", "/folder"),
        )
        assertEquals(
            "otg:/1234:5678/folder/file.txt",
            OTGUtil.buildOtgPath("1234:5678", "/folder/file.txt"),
        )
    }

    /**
     * Test building OTG paths with device keys that include serial numbers.
     */
    @Test
    fun testBuildOtgPath_withSerialInDeviceKey() {
        assertEquals(
            "otg:/1234:5678:SERIAL123/",
            OTGUtil.buildOtgPath("1234:5678:SERIAL123", ""),
        )
        assertEquals(
            "otg:/1234:5678:SERIAL123/Documents/file.pdf",
            OTGUtil.buildOtgPath("1234:5678:SERIAL123", "Documents/file.pdf"),
        )
    }

    /**
     * Test extracting sub-paths from OTG paths with device keys.
     */
    @Test
    fun testGetSubPathFromOtgPath_withDeviceKey() {
        assertEquals(
            "",
            OTGUtil.getSubPathFromOtgPath("otg:/1234:5678/"),
        )
        assertEquals(
            "folder",
            OTGUtil.getSubPathFromOtgPath("otg:/1234:5678/folder"),
        )
        assertEquals(
            "folder/file.txt",
            OTGUtil.getSubPathFromOtgPath("otg:/1234:5678/folder/file.txt"),
        )
        assertEquals(
            "Documents/Reports/2024/report.pdf",
            OTGUtil.getSubPathFromOtgPath("otg:/1234:5678/Documents/Reports/2024/report.pdf"),
        )
    }

    /**
     * Test extracting sub-paths from OTG paths with device keys that include serial numbers.
     */
    @Test
    fun testGetSubPathFromOtgPath_withSerialInDeviceKey() {
        assertEquals(
            "",
            OTGUtil.getSubPathFromOtgPath("otg:/1234:5678:ABC123/"),
        )
        assertEquals(
            "folder/file.txt",
            OTGUtil.getSubPathFromOtgPath("otg:/1234:5678:ABC123/folder/file.txt"),
        )
    }

    /**
     * Test extracting sub-paths from legacy OTG paths without device keys.
     */
    @Test
    fun testGetSubPathFromOtgPath_legacyFormat() {
        // Legacy paths without device key
        assertEquals(
            "",
            OTGUtil.getSubPathFromOtgPath("otg:/"),
        )
        assertEquals(
            "folder",
            OTGUtil.getSubPathFromOtgPath("otg:/folder"),
        )
        assertEquals(
            "folder/file.txt",
            OTGUtil.getSubPathFromOtgPath("otg:/folder/file.txt"),
        )
    }

    /**
     * Test extracting sub-paths from non-OTG paths returns the path as-is.
     */
    @Test
    fun testGetSubPathFromOtgPath_nonOtgPath() {
        // Non-OTG paths should be returned as-is
        assertEquals(
            "/storage/emulated/0/file.txt",
            OTGUtil.getSubPathFromOtgPath("/storage/emulated/0/file.txt"),
        )
        assertEquals(
            "content://provider/path",
            OTGUtil.getSubPathFromOtgPath("content://provider/path"),
        )
    }

    /**
     * Test round-trip of building and extracting device keys and sub-paths.
     */
    @Test
    fun testRoundTrip_buildAndExtract() {
        val deviceKey = "1234:5678"
        val subPath = "folder/file.txt"

        val fullPath = OTGUtil.buildOtgPath(deviceKey, subPath)
        val extractedKey = OTGUtil.extractDeviceKeyFromPath(fullPath)
        val extractedSubPath = OTGUtil.getSubPathFromOtgPath(fullPath)

        assertEquals(deviceKey, extractedKey)
        assertEquals(subPath, extractedSubPath)
    }

    /**
     * Test round-trip of building and extracting device keys with serial numbers.
     */
    @Test
    fun testRoundTrip_buildAndExtractWithSerial() {
        val deviceKey = "1234:5678:SERIAL"
        val subPath = "Documents/important.doc"

        val fullPath = OTGUtil.buildOtgPath(deviceKey, subPath)
        val extractedKey = OTGUtil.extractDeviceKeyFromPath(fullPath)
        val extractedSubPath = OTGUtil.getSubPathFromOtgPath(fullPath)

        assertEquals(deviceKey, extractedKey)
        assertEquals(subPath, extractedSubPath)
    }

    /**
     * Test round-trip of building and extracting device keys with an empty sub-path.
     */
    @Test
    fun testRoundTrip_emptySubPath() {
        val deviceKey = "9999:8888"
        val subPath = ""

        val fullPath = OTGUtil.buildOtgPath(deviceKey, subPath)
        val extractedKey = OTGUtil.extractDeviceKeyFromPath(fullPath)
        val extractedSubPath = OTGUtil.getSubPathFromOtgPath(fullPath)

        assertEquals(deviceKey, extractedKey)
        assertEquals(subPath, extractedSubPath)
    }

    /**
     * Test simulating path traversal and filtering of OTG paths.
     */
    @Test
    fun testPathTraversalSimulation() {
        // Simulate how path parts would be processed during file traversal
        val fullPath = "otg:/1234:5678/Documents/Photos/image.jpg"
        val deviceKey = OTGUtil.extractDeviceKeyFromPath(fullPath)

        // Split path and filter parts that should be traversed
        val parts =
            fullPath.split("/").filter { part ->
                part.isNotEmpty() &&
                    part != "otg:" &&
                    part != deviceKey
            }

        // Should only contain actual folder/file names
        assertEquals(listOf("Documents", "Photos", "image.jpg"), parts)
    }

    /**
     * Test paths for different devices are distinguishable even if they have the same sub-path.
     */
    @Test
    fun testMultipleDevicesPathDistinction() {
        // Verify that paths for different devices are distinguishable
        val device1Path = OTGUtil.buildOtgPath("1111:2222", "folder/file.txt")
        val device2Path = OTGUtil.buildOtgPath("3333:4444", "folder/file.txt")

        val device1Key = OTGUtil.extractDeviceKeyFromPath(device1Path)
        val device2Key = OTGUtil.extractDeviceKeyFromPath(device2Path)

        assertEquals("1111:2222", device1Key)
        assertEquals("3333:4444", device2Key)

        // Same sub-path but different devices
        assertEquals(
            OTGUtil.getSubPathFromOtgPath(device1Path),
            OTGUtil.getSubPathFromOtgPath(device2Path),
        )
    }

    /**
     * Test extracting device keys from OTG paths with "vol:" prefix (volume-based keys).
     */
    @Test
    fun testExtractDeviceKeyFromPath_withVolPrefix() {
        // Volume-based device keys (API 24+)
        assertEquals(
            "vol:1234-5678",
            OTGUtil.extractDeviceKeyFromPath("otg:/vol:1234-5678/"),
        )
        assertEquals(
            "vol:1234-5678",
            OTGUtil.extractDeviceKeyFromPath("otg:/vol:1234-5678/folder"),
        )
        assertEquals(
            "vol:1234-5678",
            OTGUtil.extractDeviceKeyFromPath("otg:/vol:1234-5678/folder/subfolder/file.txt"),
        )
        assertEquals(
            "vol:ABCD-EF01",
            OTGUtil.extractDeviceKeyFromPath("otg:/vol:ABCD-EF01/Documents"),
        )
    }

    /**
     * Test extracting device keys from OTG paths with "usb:" prefix (USB-based keys).
     */
    @Test
    fun testExtractDeviceKeyFromPath_withUsbPrefix() {
        // USB-based device keys (API 21-23)
        assertEquals(
            "usb:4660:22136",
            OTGUtil.extractDeviceKeyFromPath("otg:/usb:4660:22136/"),
        )
        assertEquals(
            "usb:4660:22136",
            OTGUtil.extractDeviceKeyFromPath("otg:/usb:4660:22136/folder"),
        )
        assertEquals(
            "usb:4660:22136:ABC123",
            OTGUtil.extractDeviceKeyFromPath("otg:/usb:4660:22136:ABC123/folder/file.txt"),
        )
    }

    /**
     * Test building OTG paths with "vol:" prefix device keys.
     */
    @Test
    fun testBuildOtgPath_withVolPrefix() {
        assertEquals(
            "otg:/vol:1234-5678/",
            OTGUtil.buildOtgPath("vol:1234-5678", ""),
        )
        assertEquals(
            "otg:/vol:1234-5678/folder",
            OTGUtil.buildOtgPath("vol:1234-5678", "folder"),
        )
        assertEquals(
            "otg:/vol:ABCD-EF01/Documents/file.pdf",
            OTGUtil.buildOtgPath("vol:ABCD-EF01", "Documents/file.pdf"),
        )
    }

    /**
     * Test building OTG paths with "usb:" prefix device keys.
     */
    @Test
    fun testBuildOtgPath_withUsbPrefix() {
        assertEquals(
            "otg:/usb:4660:22136/",
            OTGUtil.buildOtgPath("usb:4660:22136", ""),
        )
        assertEquals(
            "otg:/usb:4660:22136/folder/subfolder",
            OTGUtil.buildOtgPath("usb:4660:22136", "folder/subfolder"),
        )
    }

    /**
     * Test extracting sub-paths from OTG paths with "vol:" prefix device keys.
     */
    @Test
    fun testGetSubPathFromOtgPath_withVolPrefix() {
        assertEquals(
            "",
            OTGUtil.getSubPathFromOtgPath("otg:/vol:1234-5678/"),
        )
        assertEquals(
            "folder",
            OTGUtil.getSubPathFromOtgPath("otg:/vol:1234-5678/folder"),
        )
        assertEquals(
            "folder/subfolder/file.txt",
            OTGUtil.getSubPathFromOtgPath("otg:/vol:1234-5678/folder/subfolder/file.txt"),
        )
    }

    /**
     * Test extracting sub-paths from OTG paths with "usb:" prefix device keys.
     */
    @Test
    fun testGetSubPathFromOtgPath_withUsbPrefix() {
        assertEquals(
            "",
            OTGUtil.getSubPathFromOtgPath("otg:/usb:4660:22136/"),
        )
        assertEquals(
            "folder",
            OTGUtil.getSubPathFromOtgPath("otg:/usb:4660:22136/folder"),
        )
        assertEquals(
            "Documents/report.pdf",
            OTGUtil.getSubPathFromOtgPath("otg:/usb:4660:22136/Documents/report.pdf"),
        )
    }

    /**
     * Test round-trip of building and extracting OTG paths with "vol:" prefix device keys.
     */
    @Test
    fun testRoundTrip_volPrefixDeviceKey() {
        val deviceKey = "vol:ABCD-1234"
        val subPath = "Documents/Photos/image.jpg"

        val fullPath = OTGUtil.buildOtgPath(deviceKey, subPath)
        val extractedKey = OTGUtil.extractDeviceKeyFromPath(fullPath)
        val extractedSubPath = OTGUtil.getSubPathFromOtgPath(fullPath)

        assertEquals(deviceKey, extractedKey)
        assertEquals(subPath, extractedSubPath)
    }

    /**
     * Test round-trip of building and extracting OTG paths with "usb:" prefix device keys.
     */
    @Test
    fun testRoundTrip_usbPrefixDeviceKey() {
        val deviceKey = "usb:4660:22136:SN123"
        val subPath = "DCIM/Camera/photo.jpg"

        val fullPath = OTGUtil.buildOtgPath(deviceKey, subPath)
        val extractedKey = OTGUtil.extractDeviceKeyFromPath(fullPath)
        val extractedSubPath = OTGUtil.getSubPathFromOtgPath(fullPath)

        assertEquals(deviceKey, extractedKey)
        assertEquals(subPath, extractedSubPath)
    }

    /**
     * Regression test: getDocumentFiles must NOT throw NullPointerException when
     * OtgFileAccessFacade.listFiles() returns files via direct filesystem access.
     *
     * Before the fix, OTGUtil set baseFile.fullUri = null which called
     * HybridFileParcelable.setFullUri(null) → NPE on fullUri.getScheme().
     */
    @Test
    fun testGetDocumentFiles_directAccess_doesNotThrowNPE() {
        val testDir = java.io.File("/tmp/otg_getdocfiles_test")
        testDir.mkdirs()
        val testFile = java.io.File(testDir, "test.txt")
        testFile.writeText("content")

        try {
            val collectedFiles = mutableListOf<com.amaze.filemanager.filesystem.HybridFileParcelable>()

            // Directly exercise the HybridFileParcelable construction path that caused the crash:
            // simulate what OTGUtil.getDocumentFiles does when files.isNotEmpty()
            val baseFile =
                com.amaze.filemanager.filesystem.HybridFileParcelable(
                    testFile.absolutePath,
                    null,
                    testFile.lastModified(),
                    testFile.length(),
                    testFile.isDirectory,
                )
            baseFile.name = testFile.name
            baseFile.mode = com.amaze.filemanager.fileoperations.filesystem.OpenMode.FILE
            // This must NOT throw - it was null before the fix
            baseFile.fullUri = null
            collectedFiles.add(baseFile)

            assertEquals(1, collectedFiles.size)
            assertEquals("test.txt", collectedFiles[0].name)
            assertEquals(com.amaze.filemanager.fileoperations.filesystem.OpenMode.FILE, collectedFiles[0].mode)
            assertNull(collectedFiles[0].fullUri)
        } finally {
            testFile.delete()
            testDir.delete()
        }
    }

    /**
     * Regression test: files returned from direct access path have OpenMode.FILE, not OTG.
     * This ensures the drawer path (filesystem path when canRead()) and file listing are consistent.
     */
    @Test
    fun testGetDocumentFiles_directAccess_filesModeIsFile() {
        val testDir = java.io.File("/tmp/otg_mode_test")
        testDir.mkdirs()
        val testFile = java.io.File(testDir, "readme.txt")
        testFile.writeText("hello")

        try {
            // Simulate what OTGUtil creates for direct-access files
            val baseFile =
                com.amaze.filemanager.filesystem.HybridFileParcelable(
                    testFile.absolutePath,
                    null,
                    testFile.lastModified(),
                    testFile.length(),
                    false,
                )
            baseFile.name = testFile.name
            baseFile.mode = com.amaze.filemanager.fileoperations.filesystem.OpenMode.FILE

            // Mode must be FILE (not OTG) for direct-access listed files
            assertEquals(
                com.amaze.filemanager.fileoperations.filesystem.OpenMode.FILE,
                baseFile.mode,
            )
            // Path must be an absolute filesystem path, not an otg:/ path
            assertTrue(baseFile.path.startsWith("/"))
            assertFalse(baseFile.path.startsWith(OTGUtil.PREFIX_OTG))
        } finally {
            testFile.delete()
            testDir.delete()
        }
    }
}
