package com.amaze.filemanager.filesystem.root

import com.amaze.filemanager.exceptions.ShellNotRunningException
import com.amaze.filemanager.filesystem.RootHelper
import com.amaze.filemanager.filesystem.root.api.IFindFileCommand

object FindFileCommand: IFindFileCommand {

    @Throws(ShellNotRunningException::class)
    override fun findFile(path: String): Boolean {
        val result = runShellCommandToList(
                "find \"${RootHelper.getCommandLineString(path)}\""
        )
        return result.isNotEmpty()
    }
}