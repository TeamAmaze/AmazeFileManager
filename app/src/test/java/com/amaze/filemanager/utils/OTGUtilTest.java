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

package com.amaze.filemanager.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Test cases for OTG device key extraction and path building */
public class OTGUtilTest {

  @Test
  public void testExtractDeviceKeyFromOtgPath() {
    // Test valid device key format (vendorId:productId)
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678/");
    assertEquals("1234:5678", deviceKey);
  }

  @Test
  public void testExtractDeviceKeyWithSerial() {
    // Test device key with serial number
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678:ABC123/Documents");
    assertEquals("1234:5678:ABC123", deviceKey);
  }

  @Test
  public void testExtractDeviceKeyLegacyFormat() {
    // Test legacy format without device key
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("otg:/");
    assertNull(deviceKey);
  }

  @Test
  public void testExtractDeviceKeyNonOtgPath() {
    // Test non-OTG path
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("/mnt/media_rw/USB-DISK");
    assertNull(deviceKey);
  }

  @Test
  public void testBuildOtgPath() {
    // Test building OTG path with device key
    String path = OTGUtil.buildOtgPath("1234:5678", "Documents/File.txt");
    assertEquals("otg:/1234:5678/Documents/File.txt", path);
  }

  @Test
  public void testBuildOtgPathWithoutSubpath() {
    // Test building OTG path without subpath
    String path = OTGUtil.buildOtgPath("1234:5678", "");
    assertEquals("otg:/1234:5678/", path);
  }

  @Test
  public void testGetSubPathFromOtgPath() {
    // Test extracting subpath from OTG path
    String subPath = OTGUtil.getSubPathFromOtgPath("otg:/1234:5678/Documents/File.txt");
    assertEquals("Documents/File.txt", subPath);
  }

  @Test
  public void testGetSubPathLegacyFormat() {
    // Test subpath extraction for legacy format
    String subPath = OTGUtil.getSubPathFromOtgPath("otg:/Documents/File.txt");
    assertEquals("Documents/File.txt", subPath);
  }

  @Test
  public void testMediaRemovablePathDetection() {
    // Test detection of /mnt/media_rw paths
    assertEquals("/mnt/media_rw", OTGUtil.PREFIX_MEDIA_REMOVABLE);
  }

  @Test
  public void testOtgPathWithDeepDirectory() {
    // Test complex nested paths
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678/Folder1/Folder2/Folder3");
    assertEquals("1234:5678", deviceKey);

    String subPath = OTGUtil.getSubPathFromOtgPath("otg:/1234:5678/Folder1/Folder2/Folder3");
    assertEquals("Folder1/Folder2/Folder3", subPath);
  }

  @Test
  public void testDeviceKeyWithLeadingSlashes() {
    // Test path with extra slashes
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("otg:///1234:5678/");
    assertEquals("1234:5678", deviceKey);
  }

  @Test
  public void testMultipleColonDeviceKey() {
    // Test device key with multiple components separated by colons
    String deviceKey = OTGUtil.extractDeviceKeyFromPath("otg:/1234:5678:SERIAL:EXTRA");
    assertEquals("1234:5678:SERIAL:EXTRA", deviceKey);
  }
}
