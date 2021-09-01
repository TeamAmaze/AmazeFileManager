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

import com.amaze.filemanager.exceptions.ShellCommandInvalidException
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.base.IRootCommand

object RenameFileCommand : IRootCommand() {

    /**
     * Renames file using root
     *
     * @param oldPath path to file before rename
     * @param newPath path to file after rename
     * @return if rename was successful or not
     */
    @Throws(ShellNotRunningException::class)
    fun renameFile(oldPath: String, newPath: String): Boolean {
        val mountPoint = MountPathCommand.mountPath(oldPath, MountPathCommand.READ_WRITE)
        val command = "mv \"${RootHelper.getCommandLineString(oldPath)}\"" +
            " \"${RootHelper.getCommandLineString(newPath)}\""
        return try {
            val output = runShellCommandToList(command)
            mountPoint?.let { MountPathCommand.mountPath(it, MountPathCommand.READ_ONLY) }
            output.isEmpty()
        } catch (e: ShellCommandInvalidException) {
            e.printStackTrace()
            false
        }
    }
}
