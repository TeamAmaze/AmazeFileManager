/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.filesystem.root

import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.base.IRootCommand

object ChangeFilePermissionsCommand : IRootCommand() {

    private const val CHMOD_COMMAND = "chmod %s %o \"%s\""

    /**
     * Change permissions for a given file path - requires root
     *
     * @param filePath given file path
     * @param updatedPermissions octal notation for permissions
     * @param isDirectory is given path a directory or file
     */
    @Throws(ShellNotRunningException::class)
    fun changeFilePermissions(
        filePath: String,
        updatedPermissions: Int,
        isDirectory: Boolean,
        onOperationPerform: (Boolean) -> Unit
    ) {
        val mountPoint = MountPathCommand.mountPath(filePath, MountPathCommand.READ_WRITE)

        val options = if (isDirectory) "-R" else ""
        val command = String.format(
            CHMOD_COMMAND,
            options,
            updatedPermissions,
            RootHelper.getCommandLineString(filePath)
        )

        runShellCommandWithCallback(
            command
        ) { _: Int, exitCode: Int, _: List<String?>? ->
            if (exitCode < 0) {
                onOperationPerform(false)
            } else {
                onOperationPerform(true)
            }
        }

        mountPoint?.let { MountPathCommand.mountPath(it, MountPathCommand.READ_ONLY) }
    }
}
