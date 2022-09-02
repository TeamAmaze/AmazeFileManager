/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.services.ftp

import com.amaze.filemanager.filesystem.ftpserver.AndroidFtpFileSystemView
import com.amaze.filemanager.filesystem.ftpserver.commands.AVBL
import com.amaze.filemanager.filesystem.ftpserver.commands.FEAT
import com.amaze.filemanager.filesystem.ftpserver.commands.PWD
import org.apache.ftpserver.command.CommandFactory
import org.apache.ftpserver.command.CommandFactoryFactory

/**
 * Custom [CommandFactory] factory with custom commands.
 */
object CommandFactoryFactory {

    /**
     * Encapsulate custom [CommandFactory] construction logic. Append custom AVBL and PWD command,
     * as well as feature flag in FEAT command if not using [AndroidFtpFileSystemView].
     */
    fun create(useAndroidFileSystem: Boolean): CommandFactory {
        val cf = CommandFactoryFactory()
        if (!useAndroidFileSystem) {
            cf.addCommand("AVBL", AVBL())
            cf.addCommand("FEAT", FEAT())
            cf.addCommand("PWD", PWD())
        }
        return cf.createCommandFactory()
    }
}
