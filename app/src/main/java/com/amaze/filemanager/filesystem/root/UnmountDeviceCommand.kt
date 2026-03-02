/*
 * Copyright (C) 2014-2024 Arpit Khurana, Vishal Nehra,
 * Emmanuel Messulam, Raymond Lai and Contributors.
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
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package com.amaze.filemanager.filesystem.root

import com.amaze.filemanager.fileoperations.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.root.base.IRootCommand
import org.slf4j.LoggerFactory

/**
 * Command to unmount OTG USB devices with root access
 * Handles both /mnt/media_rw/ and /storage/ mount point formats
 */
object UnmountDeviceCommand : IRootCommand() {
    private val LOG = LoggerFactory.getLogger(UnmountDeviceCommand::class.java)

    /**
     * Unmounts an OTG device at the specified mount point
     * Attempts graceful unmount first, then lazy unmount if needed
     *
     * @param mountPoint the mount point to unmount (e.g., /mnt/media_rw/XXXX-XXXX or /storage/XXXX-XXXX)
     * @return boolean whether unmount was successful or not
     */
    @Throws(ShellNotRunningException::class)
    fun unmountDevice(mountPoint: String): Boolean {
        return try {
            LOG.info("Attempting to unmount device at: $mountPoint")

            // First, try graceful unmount
            val gracefulResult = runShellCommandToList("umount \"$mountPoint\"")

            if (gracefulResult.isNotEmpty() && gracefulResult[0].contains("busy")) {
                // If device is busy, try lazy unmount
                LOG.warn("Device busy, attempting lazy unmount")
                val lazyResult = runShellCommandToList("umount -l \"$mountPoint\"")
                lazyResult.isNotEmpty()
            } else {
                // Graceful unmount succeeded or other non-busy error
                gracefulResult.isEmpty() || !gracefulResult[0].contains("No such file")
            }
        } catch (e: Exception) {
            LOG.error("Failed to unmount device: ${e.message}", e)
            false
        }
    }
}
