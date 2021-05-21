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

object DeleteFileCommand : IRootCommand() {

    /**
     * Recursively removes a path with it's contents (if any)
     *
     * @return boolean whether file was deleted or not
     */
    @Throws(ShellNotRunningException::class)
    fun deleteFile(path: String): Boolean {
        val mountPoint = MountPathCommand.mountPath(path, MountPathCommand.READ_WRITE)
        val result = runShellCommandToList(
            "rm -rf \"${RootHelper.getCommandLineString(path)}\""
        )

        mountPoint?.let { MountPathCommand.mountPath(it, MountPathCommand.READ_ONLY) }

        return result.isNotEmpty()
    }
}
