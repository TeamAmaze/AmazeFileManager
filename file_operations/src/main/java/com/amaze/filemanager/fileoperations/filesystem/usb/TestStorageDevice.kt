package com.amaze.filemanager.fileoperations.filesystem.usb

internal class TestStorageDevice(
    override val deviceKey: String,
    override val displayName: String,
    override val filePath: String?,
) : StorageDeviceRepresentation
