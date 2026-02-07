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
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for OTGUtil device key path helper functions.
 * Tests the parsing and building of OTG paths with device keys.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class OTGUtilDeviceKeyTest {
    // ============ extractDeviceKeyFromPath tests ============

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

    @Test
    fun testExtractDeviceKeyFromPath_legacyFormatReturnsNull() {
        // Legacy paths without device key should return null
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/folder"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/folder/file.txt"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/Documents/report.pdf"))
    }

    @Test
    fun testExtractDeviceKeyFromPath_invalidPathsReturnNull() {
        // Non-OTG paths
        assertNull(OTGUtil.extractDeviceKeyFromPath("/storage/emulated/0"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("content://com.android.providers"))
        assertNull(OTGUtil.extractDeviceKeyFromPath(""))
        assertNull(OTGUtil.extractDeviceKeyFromPath("/"))
        assertNull(OTGUtil.extractDeviceKeyFromPath("file:///sdcard/"))
    }

    @Test
    fun testExtractDeviceKeyFromPath_edgeCases() {
        // Single segment with colon - technically matches the pattern (has colon, splits to 2+ parts)
        // "single:" splits to ["single", ""] which is 2 parts, so it's treated as a device key
        assertEquals("single:", OTGUtil.extractDeviceKeyFromPath("otg:/single:/folder"))
        // Path that looks like device key but isn't (single number without colon)
        assertNull(OTGUtil.extractDeviceKeyFromPath("otg:/12345/folder"))
    }

    // ============ buildOtgPath tests ============

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

    // ============ getSubPathFromOtgPath tests ============

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

    // ============ Round-trip tests ============

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

    // ============ Integration-style tests ============

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
}
