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
import com.amaze.filemanager.filesystem.root.MountPathCommand.mountPath
import com.amaze.filemanager.filesystem.root.base.IRootCommand

object CopyFilesCommand : IRootCommand() {

    /**
     * Copies files using root
     * @param source given source
     * @param destination given destination
     */
    @Throws(ShellNotRunningException::class)
    fun copyFiles(source: String, destination: String) {
        // remounting destination as rw
        val mountPoint = mountPath(destination, MountPathCommand.READ_WRITE)

        runShellCommand(
            "cp -r \"${RootHelper.getCommandLineString(source)}\" " +
                "\"${RootHelper.getCommandLineString(destination)}\""
        )

        // we mounted the filesystem as rw, let's mount it back to ro
        mountPoint?.let { mountPath(it, MountPathCommand.READ_ONLY) }
    }
}
