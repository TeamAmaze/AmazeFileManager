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

package com.amaze.filemanager.asynchronous.asynctasks.ftp

import android.app.ProgressDialog
import android.widget.Toast
import androidx.annotation.MainThread
import com.amaze.filemanager.R
import com.amaze.filemanager.application.AppConfig
import com.amaze.filemanager.asynchronous.asynctasks.Task
import java.util.concurrent.Callable

abstract class AbstractGetHostInfoTask<V, T : Callable<V>>(
    private val hostname: String,
    private val port: Int,
    private val callback: (V) -> Unit
) : Task<V, T> {

    private lateinit var progressDialog: ProgressDialog

    /**
     * Routine to run before passing control to worker thread, usually for UI related operations.
     */
    @MainThread
    open fun onPreExecute() {
        AppConfig.getInstance().run {
            progressDialog = ProgressDialog.show(
                this.mainActivityContext,
                "",
                this.resources.getString(R.string.processing)
            )
        }
    }

    @MainThread
    override fun onError(error: Throwable) {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        Toast.makeText(
            AppConfig.getInstance(),
            AppConfig.getInstance()
                .resources
                .getString(
                    R.string.ssh_connect_failed,
                    hostname,
                    port,
                    error.localizedMessage
                ),
            Toast.LENGTH_LONG
        ).show()
    }

    @MainThread
    override fun onFinish(value: V) {
        callback(value)
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}
