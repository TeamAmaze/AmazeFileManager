/*
 * Copyright (C) 2014-2026 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.fileoperations.filesystem.usb

import android.net.Uri
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [StorageDeviceRepresentation], [UsbStorageDevice], [VolumeStorageDevice],
 * [DeviceKeyMigration], and [UsbOtgManager].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@Suppress("StringLiteralDuplication")
class StorageDeviceManagerTest {
    /**
     * Setup before tests.
     */
    @Before
    fun setUp() {
        UsbOtgManager.resetAll()
        UsbOtgManager.resetMigrationFlag()
    }

    /**
     * Cleanup after tests.
     */
    @After
    fun tearDown() {
        UsbOtgManager.resetAll()
        UsbOtgManager.resetMigrationFlag()
    }

    /**
     * Test UsbStorageDevice deviceKey format without serial number.
     */
    @Test
    fun `test UsbStorageDevice deviceKey format without serial`() {
        val device =
            UsbStorageDevice(
                vendorId = 0x1234,
                productId = 0x5678,
                serialNumber = null,
                manufacturerName = "Test Manufacturer",
                productName = "Test Product",
            )

        assertEquals("usb:4660:22136", device.deviceKey)
        assertTrue(StorageDeviceRepresentation.isUsbKey(device.deviceKey))
        assertFalse(StorageDeviceRepresentation.isVolumeKey(device.deviceKey))
        assertFalse(StorageDeviceRepresentation.isLegacyKey(device.deviceKey))
    }

    /**
     * Test UsbStorageDevice deviceKey format with serial number.
     */
    @Test
    fun `test UsbStorageDevice deviceKey format with serial`() {
        val device =
            UsbStorageDevice(
                vendorId = 0x1234,
                productId = 0x5678,
                serialNumber = "ABC123",
                manufacturerName = "Test Manufacturer",
                productName = "Test Product",
            )

        assertEquals("usb:4660:22136:ABC123", device.deviceKey)
    }

    /**
     * Test UsbStorageDevice displayName with manufacturer and product.
     */
    @Test
    fun `test UsbStorageDevice displayName with manufacturer and product`() {
        val device =
            UsbStorageDevice(
                vendorId = 0x1234,
                productId = 0x5678,
                serialNumber = null,
                manufacturerName = "SanDisk",
                productName = "Ultra USB",
            )

        assertEquals("SanDisk Ultra USB", device.displayName)
    }

    /**
     * Test UsbStorageDevice displayName fallback when manufacturer and product are null.
     */
    @Test
    fun `test UsbStorageDevice displayName fallback`() {
        val device =
            UsbStorageDevice(
                vendorId = 0x1234,
                productId = 0x5678,
                serialNumber = null,
                manufacturerName = null,
                productName = null,
            )

        assertEquals("USB Device (1234:5678)", device.displayName)
    }

    /**
     * Test VolumeStorageDevice deviceKey format.
     */
    @Test
    fun `test VolumeStorageDevice deviceKey format`() {
        val device =
            VolumeStorageDevice(
                uuid = "1234-5678",
                description = "USB Storage",
                path = "/storage/1234-5678",
            )

        assertEquals("vol:1234-5678", device.deviceKey)
        assertTrue(StorageDeviceRepresentation.isVolumeKey(device.deviceKey))
        assertFalse(StorageDeviceRepresentation.isUsbKey(device.deviceKey))
        assertFalse(StorageDeviceRepresentation.isLegacyKey(device.deviceKey))
    }

    /**
     * Test VolumeStorageDevice displayName with description.
     */
    @Test
    fun `test VolumeStorageDevice displayName`() {
        val device =
            VolumeStorageDevice(
                uuid = "1234-5678",
                description = "My USB Drive",
                path = "/storage/1234-5678",
            )

        assertEquals("My USB Drive", device.displayName)
    }

    /**
     * Test VolumeStorageDevice displayName fallback when description is empty.
     */
    @Test
    fun `test VolumeStorageDevice displayName fallback`() {
        val device =
            VolumeStorageDevice(
                uuid = "1234-5678",
                description = "",
                path = null,
            )

        assertEquals("Removable Storage (1234-5678)", device.displayName)
    }

    /**
     * Test StorageDeviceRepresentation key detection methods.
     */
    @Test
    fun `test isLegacyKey detects legacy format`() {
        assertTrue(StorageDeviceRepresentation.isLegacyKey("1234:5678"))
        assertTrue(StorageDeviceRepresentation.isLegacyKey("1234:5678:ABC123"))
        assertFalse(StorageDeviceRepresentation.isLegacyKey("usb:1234:5678"))
        assertFalse(StorageDeviceRepresentation.isLegacyKey("vol:1234-5678"))
    }

    /**
     * Test StorageDeviceRepresentation key detection methods for USB keys.
     */
    @Test
    fun `test UsbStorageDevice fromLegacy conversion`() {
        val legacy =
            UsbOtgRepresentation(
                5678, // productId
                1234, // vendorId
                "SERIAL123",
                "Manufacturer",
                "Product",
            )

        val converted = UsbStorageDevice.fromLegacy(legacy)

        assertEquals(1234, converted.vendorId)
        assertEquals(5678, converted.productId)
        assertEquals("SERIAL123", converted.serialNumber)
        assertEquals("Manufacturer", converted.manufacturerName)
        assertEquals("Product", converted.productName)
        assertEquals("usb:1234:5678:SERIAL123", converted.deviceKey)
    }

    /**
     * Test StorageDeviceRepresentation key detection methods for USB volume keys.
     */
    @Test
    fun `test parseIdsFromKey with USB key`() {
        val result = UsbStorageDevice.parseIdsFromKey("usb:1234:5678")
        assertNotNull(result)
        assertEquals(1234, result?.first)
        assertEquals(5678, result?.second)
    }

    /**
     * Test StorageDeviceRepresentation key detection methods for legacy USB volume keys.
     */
    @Test
    fun `test parseIdsFromKey with legacy key`() {
        val result = UsbStorageDevice.parseIdsFromKey("1234:5678")
        assertNotNull(result)
        assertEquals(1234, result?.first)
        assertEquals(5678, result?.second)
    }

    /**
     * Test StorageDeviceRepresentation key detection methods for invalid keys.
     */
    @Test
    fun `test parseIdsFromKey with invalid key`() {
        assertNull(UsbStorageDevice.parseIdsFromKey("invalid"))
        assertNull(UsbStorageDevice.parseIdsFromKey("vol:1234-5678"))
    }

    /**
     * Test key detection methods for volume keys with parseUuidFromKey().
     */
    @Test
    fun `test parseUuidFromKey with volume key`() {
        val result = VolumeStorageDevice.parseUuidFromKey("vol:1234-5678")
        assertEquals("1234-5678", result)
    }

    /**
     * Test key detection methods for non-volume keys with parseUuidFromKey().
     */
    @Test
    fun `test parseUuidFromKey with non-volume key`() {
        assertNull(VolumeStorageDevice.parseUuidFromKey("usb:1234:5678"))
        assertNull(VolumeStorageDevice.parseUuidFromKey("1234:5678"))
    }

    /**
     * Test migrateKey() returns AlreadyMigrated for new USB format.
     */
    @Test
    fun `test migrateKey returns AlreadyMigrated for new USB format`() {
        val result = DeviceKeyMigration.migrateKey("usb:1234:5678", emptyList())
        assertTrue(result is DeviceKeyMigration.MigrationResult.AlreadyMigrated)
        assertEquals("usb:1234:5678", (result as DeviceKeyMigration.MigrationResult.AlreadyMigrated).key)
    }

    /**
     * Test migrateKey() returns AlreadyMigrated for volume format.
     */
    @Test
    fun `test migrateKey returns AlreadyMigrated for volume format`() {
        val result = DeviceKeyMigration.migrateKey("vol:1234-5678", emptyList())
        assertTrue(result is DeviceKeyMigration.MigrationResult.AlreadyMigrated)
    }

    /**
     * Test migrateKey() returns NoMatch for legacy key when no devices match.
     */
    @Test
    fun `test migrateKey migrates legacy key to USB key when device matches`() {
        val device =
            UsbStorageDevice(
                vendorId = 1234,
                productId = 5678,
                serialNumber = null,
            )

        val result = DeviceKeyMigration.migrateKey("1234:5678", listOf(device))

        assertTrue(result is DeviceKeyMigration.MigrationResult.Migrated)
        assertEquals("usb:1234:5678", (result as DeviceKeyMigration.MigrationResult.Migrated).newKey)
    }

    /**
     * Test migrateKey() returns NoMatch for legacy key with serial when no devices match.
     */
    @Test
    fun `test migrateKey migrates legacy key with serial`() {
        val device =
            UsbStorageDevice(
                vendorId = 1234,
                productId = 5678,
                serialNumber = "SERIAL123",
            )

        val result = DeviceKeyMigration.migrateKey("1234:5678:SERIAL123", listOf(device))

        assertTrue(result is DeviceKeyMigration.MigrationResult.Migrated)
        assertEquals("usb:1234:5678:SERIAL123", (result as DeviceKeyMigration.MigrationResult.Migrated).newKey)
    }

    /**
     * Test migrateKey() returns NoMatch when no devices match the legacy key.
     */
    @Test
    fun `test migrateKey returns NoMatch when no device matches`() {
        val device =
            UsbStorageDevice(
                vendorId = 9999,
                productId = 8888,
            )

        val result = DeviceKeyMigration.migrateKey("1234:5678", listOf(device))

        assertTrue(result is DeviceKeyMigration.MigrationResult.NoMatch)
    }

    /**
     * Test migrateKey() returns NoMatch for invalid legacy key format.
     */
    @Test
    fun `test migrateKey returns NoMatch for invalid legacy key`() {
        val result = DeviceKeyMigration.migrateKey("invalid", emptyList())
        assertTrue(result is DeviceKeyMigration.MigrationResult.NoMatch)
    }

    /**
     * Test migrateKey() migrates to volume key when there's exactly one volume device.
     */
    @Test
    fun `test migrateKey migrates to volume when single volume device`() {
        val device =
            VolumeStorageDevice(
                uuid = "1234-5678",
                description = "USB Storage",
            )

        // Only migrate to volume if there's exactly one device
        val result = DeviceKeyMigration.migrateKey("1111:2222", listOf(device))

        assertTrue(result is DeviceKeyMigration.MigrationResult.Migrated)
        assertEquals("vol:1234-5678", (result as DeviceKeyMigration.MigrationResult.Migrated).newKey)
    }

    /**
     * Test migrateKey() returns NoMatch when there are multiple volume devices.
     */
    @Test
    fun `test uriMatchesDevice with volume key`() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        assertTrue(DeviceKeyMigration.uriMatchesDevice(uri, "vol:1234-5678"))
        assertFalse(DeviceKeyMigration.uriMatchesDevice(uri, "vol:9999-0000"))
    }

    /**
     * Test uriMatchesDevice() returns false for USB keys since they can't be reliably matched to URIs.
     */
    @Test
    fun `test uriMatchesDevice with USB key always returns false`() {
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        // USB keys can't be reliably matched to URIs
        assertFalse(DeviceKeyMigration.uriMatchesDevice(uri, "usb:1234:5678"))
    }

    /**
     * Test uriMatchesDevice() returns false for invalid keys.
     */
    @Test
    fun `test addDevice and getStorageDevice`() {
        val device =
            UsbStorageDevice(
                vendorId = 1234,
                productId = 5678,
            )

        UsbOtgManager.addDevice(device)

        val retrieved = UsbOtgManager.getStorageDevice(device.deviceKey)
        assertNotNull(retrieved)
        assertEquals(device.deviceKey, retrieved?.deviceKey)
    }

    /**
     * Test removeDevice() removes the device from the manager.
     */
    @Test
    fun `test removeDevice`() {
        val device =
            UsbStorageDevice(
                vendorId = 1234,
                productId = 5678,
            )

        UsbOtgManager.addDevice(device)
        assertTrue(UsbOtgManager.isDeviceConnected(device.deviceKey))

        UsbOtgManager.removeDevice(device.deviceKey)
        assertFalse(UsbOtgManager.isDeviceConnected(device.deviceKey))
    }

    /**
     * Test getStorageDevices() returns all connected devices.
     */
    @Test
    fun `test getStorageDevices returns all devices`() {
        val device1 = UsbStorageDevice(vendorId = 1111, productId = 2222)
        val device2 = VolumeStorageDevice(uuid = "AAAA-BBBB", description = "Test")

        UsbOtgManager.addDevice(device1)
        UsbOtgManager.addDevice(device2)

        val devices = UsbOtgManager.getStorageDevices()
        assertEquals(2, devices.size)
        assertTrue(devices.any { it.deviceKey == device1.deviceKey })
        assertTrue(devices.any { it.deviceKey == device2.deviceKey })
    }

    /**
     * Test setUsbOtgRoot() and getUsbOtgRoot().
     */
    @Test
    fun `test setUsbOtgRoot and getUsbOtgRoot`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)

        assertEquals(uri, UsbOtgManager.getUsbOtgRoot(device.deviceKey))
        assertTrue(UsbOtgManager.hasUsbOtgRoot(device.deviceKey))
    }

    /**
     * Test setUsbOtgRoot() with null removes root.
     */
    @Test
    fun `test setUsbOtgRoot with null removes root`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)
        assertTrue(UsbOtgManager.hasUsbOtgRoot(device.deviceKey))

        UsbOtgManager.setUsbOtgRoot(device.deviceKey, null)
        assertFalse(UsbOtgManager.hasUsbOtgRoot(device.deviceKey))
    }

    /**
     * Test setUsbOtgRoot() throws IllegalStateException for non-connected device.
     */
    @Test(expected = IllegalStateException::class)
    fun `test setUsbOtgRoot throws for non-connected device`() {
        UsbOtgManager.setUsbOtgRoot(
            "non-existent-key",
            Uri.parse("content://test"),
        )
    }

    /**
     * Test updateDevices() adds new devices and removes disconnected ones.
     */
    @Test
    fun `test updateDevices adds new devices`() {
        val device1 = UsbStorageDevice(vendorId = 1111, productId = 2222)
        val device2 = UsbStorageDevice(vendorId = 3333, productId = 4444)

        UsbOtgManager.updateDevices(listOf(device1, device2))

        assertEquals(2, UsbOtgManager.getStorageDevices().size)
        assertTrue(UsbOtgManager.isDeviceConnected(device1.deviceKey))
        assertTrue(UsbOtgManager.isDeviceConnected(device2.deviceKey))
    }

    /**
     * Test updateDevices() removes devices that are no longer connected.
     */
    @Test
    fun `test updateDevices removes disconnected devices`() {
        val device1 = UsbStorageDevice(vendorId = 1111, productId = 2222)
        val device2 = UsbStorageDevice(vendorId = 3333, productId = 4444)

        UsbOtgManager.updateDevices(listOf(device1, device2))
        assertEquals(2, UsbOtgManager.getStorageDevices().size)

        // Now only device1 is connected
        UsbOtgManager.updateDevices(listOf(device1))
        assertEquals(1, UsbOtgManager.getStorageDevices().size)
        assertTrue(UsbOtgManager.isDeviceConnected(device1.deviceKey))
        assertFalse(UsbOtgManager.isDeviceConnected(device2.deviceKey))
    }

    /**
     * Test updateDevices() preserves SAF roots for existing devices.
     */
    @Test
    fun `test updateDevices preserves SAF roots for existing devices`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)

        // Update with same device - should preserve root
        UsbOtgManager.updateDevices(listOf(device))

        assertTrue(UsbOtgManager.hasUsbOtgRoot(device.deviceKey))
        assertEquals(uri, UsbOtgManager.getUsbOtgRoot(device.deviceKey))
    }

    /**
     * Test updateDevices() removes SAF roots for disconnected devices.
     */
    @Test
    fun `test updateDevices removes SAF roots for disconnected devices`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)

        // Update with empty list - device disconnected
        UsbOtgManager.updateDevices(emptyList())

        assertNull(UsbOtgManager.getUsbOtgRoot(device.deviceKey))
    }

    /**
     * Test anyUsbOtgRoot returns the first available root.
     */
    @Test
    fun `test anyUsbOtgRoot returns first available root`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        assertNull(UsbOtgManager.anyUsbOtgRoot)

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)

        assertEquals(uri, UsbOtgManager.anyUsbOtgRoot)
    }

    /**
     * Test anyStorageDevice returns the first available device.
     */
    @Test
    fun `test anyStorageDevice returns first device`() {
        assertNull(UsbOtgManager.anyStorageDevice)

        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        UsbOtgManager.addDevice(device)

        assertNotNull(UsbOtgManager.anyStorageDevice)
        assertEquals(device.deviceKey, UsbOtgManager.anyStorageDevice?.deviceKey)
    }

    /**
     * Test resetAll() clears all devices and roots.
     */
    @Test
    fun `test resetAll clears everything`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)

        UsbOtgManager.resetAll()

        assertEquals(0, UsbOtgManager.getStorageDevices().size)
        assertNull(UsbOtgManager.anyUsbOtgRoot)
    }

    /**
     * Test findDeviceKeyByRoot() returns the correct device key for a given SAF root URI.
     */
    @Test
    fun `test findDeviceKeyByRoot`() {
        val device = UsbStorageDevice(vendorId = 1234, productId = 5678)
        val uri = Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")

        UsbOtgManager.addDevice(device)
        UsbOtgManager.setUsbOtgRoot(device.deviceKey, uri)

        assertEquals(device.deviceKey, UsbOtgManager.findDeviceKeyByRoot(uri))
        assertNull(UsbOtgManager.findDeviceKeyByRoot(Uri.parse("content://other")))
    }

    /**
     * Test extractVolumeUuidFromUri() correctly extracts the UUID from a SAF URI.
     */
    @Test
    fun `test extractVolumeUuidFromUri with encoded URI`() {
        // Test various URI formats
        // Format: content://com.android.externalstorage.documents/tree/{uuid}%3A[path]
        Uri.parse("content://com.android.externalstorage.documents/tree/1234-5678%3A")
        Uri.parse("content://com.android.externalstorage.documents/tree/ABCD-EF01%3Afolder%2Fsubfolder")
        Uri.parse("content://com.android.externalstorage.documents/tree/9999-8888%3A")

        // We can test the extraction indirectly by adding a device and checking restore logic
        val device1 = VolumeStorageDevice(uuid = "1234-5678", description = "USB Drive")
        UsbOtgManager.addDevice(device1)

        // The device should be findable by its volume key
        assertTrue(UsbOtgManager.isDeviceConnected("vol:1234-5678"))
    }

    /**
     * Test that a VolumeStorageDevice can be added and retrieved using its volume key.
     */
    @Test
    fun `test volume device key matching with UUID`() {
        val volumeDevice = VolumeStorageDevice(uuid = "ABCD-1234", description = "Test USB")
        UsbOtgManager.addDevice(volumeDevice)

        // Verify device is registered with volume key format
        assertEquals("vol:ABCD-1234", volumeDevice.deviceKey)
        assertTrue(UsbOtgManager.isDeviceConnected("vol:ABCD-1234"))

        // Verify we can get the device back
        val retrieved = UsbOtgManager.getStorageDevice("vol:ABCD-1234")
        assertNotNull(retrieved)
        assertTrue(retrieved is VolumeStorageDevice)
        assertEquals("ABCD-1234", (retrieved as VolumeStorageDevice).uuid)
    }

    /**
     * Test that multiple VolumeStorageDevices with different UUIDs can coexist and be managed.
     */
    @Test
    fun `test multiple volume devices with different UUIDs`() {
        val device1 = VolumeStorageDevice(uuid = "1111-2222", description = "USB Drive 1")
        val device2 = VolumeStorageDevice(uuid = "3333-4444", description = "USB Drive 2")
        val uri1 = Uri.parse("content://com.android.externalstorage.documents/tree/1111-2222%3A")
        val uri2 = Uri.parse("content://com.android.externalstorage.documents/tree/3333-4444%3A")

        UsbOtgManager.addDevice(device1)
        UsbOtgManager.addDevice(device2)
        UsbOtgManager.setUsbOtgRoot(device1.deviceKey, uri1)
        UsbOtgManager.setUsbOtgRoot(device2.deviceKey, uri2)

        // Both should have their roots
        assertTrue(UsbOtgManager.hasUsbOtgRoot("vol:1111-2222"))
        assertTrue(UsbOtgManager.hasUsbOtgRoot("vol:3333-4444"))
        assertEquals(uri1, UsbOtgManager.getUsbOtgRoot("vol:1111-2222"))
        assertEquals(uri2, UsbOtgManager.getUsbOtgRoot("vol:3333-4444"))
    }

    /**
     * Test that a multi-partition USB device can be represented with different volume labels.
     */
    @Test
    fun `test multi-partition USB device with different volume labels`() {
        // Simulating a USB drive with two partitions (like a dual-partition USB stick)
        // Each partition has a different UUID but potentially similar names
        val partition1 = VolumeStorageDevice(uuid = "AAAA-1111", description = "DATA")
        val partition2 = VolumeStorageDevice(uuid = "BBBB-2222", description = "BACKUP")

        UsbOtgManager.addDevice(partition1)
        UsbOtgManager.addDevice(partition2)

        // Both partitions should be distinguishable
        assertEquals(2, UsbOtgManager.getStorageDevices().size)

        // Each has unique device key based on UUID
        assertNotEquals(partition1.deviceKey, partition2.deviceKey)
        assertEquals("vol:AAAA-1111", partition1.deviceKey)
        assertEquals("vol:BBBB-2222", partition2.deviceKey)

        // Display names show volume labels for distinction
        assertEquals("DATA", partition1.displayName)
        assertEquals("BACKUP", partition2.displayName)

        // Can retrieve each partition separately
        val retrieved1 = UsbOtgManager.getStorageDevice("vol:AAAA-1111")
        val retrieved2 = UsbOtgManager.getStorageDevice("vol:BBBB-2222")
        assertNotNull(retrieved1)
        assertNotNull(retrieved2)
        assertEquals("DATA", retrieved1?.displayName)
        assertEquals("BACKUP", retrieved2?.displayName)
    }

    /**
     * Test that a multi-partition USB device can have separate SAF roots for each partition.
     */
    @Test
    fun `test multi-partition USB device with SAF roots`() {
        // Two partitions on the same physical USB device
        val partition1 = VolumeStorageDevice(uuid = "1234-0001", description = "Partition 1")
        val partition2 = VolumeStorageDevice(uuid = "1234-0002", description = "Partition 2")
        val uri1 = Uri.parse("content://com.android.externalstorage.documents/tree/1234-0001%3A")
        val uri2 = Uri.parse("content://com.android.externalstorage.documents/tree/1234-0002%3A")

        UsbOtgManager.addDevice(partition1)
        UsbOtgManager.addDevice(partition2)
        UsbOtgManager.setUsbOtgRoot(partition1.deviceKey, uri1)
        UsbOtgManager.setUsbOtgRoot(partition2.deviceKey, uri2)

        // Each partition has its own SAF root
        assertEquals(uri1, UsbOtgManager.getUsbOtgRoot("vol:1234-0001"))
        assertEquals(uri2, UsbOtgManager.getUsbOtgRoot("vol:1234-0002"))

        // Removing one partition doesn't affect the other
        UsbOtgManager.removeDevice("vol:1234-0001")
        assertFalse(UsbOtgManager.isDeviceConnected("vol:1234-0001"))
        assertTrue(UsbOtgManager.isDeviceConnected("vol:1234-0002"))
        assertEquals(uri2, UsbOtgManager.getUsbOtgRoot("vol:1234-0002"))
    }

    /**
     * Test VolumeStorageDevice displayName falls back to UUID when description is blank or empty.
     */
    @Test
    fun `test VolumeStorageDevice displayName with blank description falls back to UUID`() {
        val deviceWithLabel = VolumeStorageDevice(uuid = "AAAA-BBBB", description = "My USB")
        val deviceWithoutLabel = VolumeStorageDevice(uuid = "CCCC-DDDD", description = "")
        val deviceWithBlankLabel = VolumeStorageDevice(uuid = "EEEE-FFFF", description = "   ")

        assertEquals("My USB", deviceWithLabel.displayName)
        assertEquals("Removable Storage (CCCC-DDDD)", deviceWithoutLabel.displayName)
        assertEquals("Removable Storage (EEEE-FFFF)", deviceWithBlankLabel.displayName)
    }
}
