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

package com.amaze.filemanager.filesystem.root.base

import com.amaze.filemanager.exceptions.ShellCommandInvalidException
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException
import com.amaze.filemanager.ui.activities.MainActivity
import eu.chainfire.libsuperuser.Shell
import eu.chainfire.libsuperuser.Shell.OnCommandResultListener

open class IRootCommand {

    /**
     * Runs the command and stores output in a list. The listener is set on the handler thread [ ]
     * [MainActivity.handlerThread] thus any code run in callback must be thread safe. Command is run
     * from the root context (u:r:SuperSU0)
     *
     * @param cmd the command
     * @return a list of results. Null only if the command passed is a blocking call or no output is
     * there for the command passed
     */
    @Throws(ShellNotRunningException::class, ShellCommandInvalidException::class)
    fun runShellCommandToList(cmd: String): List<String> {
        val result = ArrayList<String>()
        var interrupt = false
        var errorCode: Int = -1
        // callback being called on a background handler thread
        runShellCommandWithCallback(cmd) { _, exitCode, output ->
            if (exitCode in 1..127) {
                interrupt = true
                errorCode = exitCode
            }
            result.addAll(output)
        }
        if (interrupt) {
            throw ShellCommandInvalidException("$cmd , error code - $errorCode")
        }
        return result
    }

    /**
     * Command is run from the root context (u:r:SuperSU0)
     *
     * @param cmd the command
     */
    @Throws(ShellNotRunningException::class)
    fun runShellCommand(cmd: String?) {
        if (MainActivity.shellInteractive == null || !MainActivity.shellInteractive.isRunning) {
            throw ShellNotRunningException()
        }
        MainActivity.shellInteractive.addCommand(cmd)
        MainActivity.shellInteractive.waitForIdle()
    }

    /**
     * Runs the command on an interactive shell. Provides a listener for the caller to interact. The
     * caller is executed on a worker background thread, hence any calls from the callback should be
     * thread safe. Command is run from superuser context (u:r:SuperSU0)
     *
     * @param cmd the command
     */
    @Throws(ShellNotRunningException::class)
    fun runShellCommandWithCallback(cmd: String?, callback: OnCommandResultListener?) {
        if (MainActivity.shellInteractive == null || !MainActivity.shellInteractive.isRunning) {
            throw ShellNotRunningException()
        }
        MainActivity.shellInteractive.addCommand(cmd, 0, callback)
        MainActivity.shellInteractive.waitForIdle()
    }

    /**
     * @param cmd the command
     * @return a list of results. Null only if the command passed is a blocking call or no output is
     * there for the command passed
     */
    @Deprecated(
        """Use {@link #runShellCommand(String)} instead which runs command on an interactive
        shell
        <p>Runs the command and stores output in a list. The listener is set on the caller thread,
        thus any code run in callback must be thread safe. Command is run from a third-party level
        context (u:r:init_shell0) Not callback supported as the shell is not interactive""",
        ReplaceWith("Shell.SH.run(cmd)", "eu.chainfire.libsuperuser.Shell")
    )
    fun runNonRootShellCommand(cmd: String?): List<String?>? {
        return Shell.SH.run(cmd)
    }
}
