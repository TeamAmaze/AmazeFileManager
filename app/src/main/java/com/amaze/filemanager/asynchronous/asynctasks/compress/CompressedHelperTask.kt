/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.filemanager.asynchronous.asynctasks.compress

import android.os.AsyncTask
import com.amaze.filemanager.adapters.data.CompressedObjectParcelable
import com.amaze.filemanager.asynchronous.asynctasks.AsyncTaskResult
import com.amaze.filemanager.utils.OnAsyncTaskFinished
import org.apache.commons.compress.archivers.ArchiveException
import java.io.IOException
import java.util.*

abstract class CompressedHelperTask internal constructor(
    private val createBackItem: Boolean,
    private val onFinish:
        OnAsyncTaskFinished<AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>
) :
    AsyncTask<Void, IOException, AsyncTaskResult<ArrayList<CompressedObjectParcelable>>>() {

    public override fun doInBackground(
        vararg voids: Void
    ): AsyncTaskResult<ArrayList<CompressedObjectParcelable>> {

        val elements = ArrayList<CompressedObjectParcelable>()
        if (createBackItem) elements.add(0, CompressedObjectParcelable())
        return try {
            addElements(elements)
            Collections.sort(elements, CompressedObjectParcelable.Sorter())
            AsyncTaskResult(elements)
        } catch (ifArchiveIsCorruptOrInvalid: ArchiveException) {
            AsyncTaskResult(ifArchiveIsCorruptOrInvalid)
        }
    }

    override fun onPostExecute(
        zipEntries: AsyncTaskResult<ArrayList<CompressedObjectParcelable>>
    ) {
        super.onPostExecute(zipEntries)
        onFinish.onAsyncTaskFinished(zipEntries)
    }

    @Throws(ArchiveException::class)
    protected abstract fun addElements(elements: ArrayList<CompressedObjectParcelable>)
}
